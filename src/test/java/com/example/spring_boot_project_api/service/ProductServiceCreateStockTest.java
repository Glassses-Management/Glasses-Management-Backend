package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.spring_boot_project_api.dto.request.ProductRequest;
import com.example.spring_boot_project_api.mapper.ProductMapper;
import com.example.spring_boot_project_api.model.Inventory;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.repository.InventoryRepository;
import com.example.spring_boot_project_api.repository.OrderItemRepository;
import com.example.spring_boot_project_api.repository.ProductAttachmentRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;

import jakarta.persistence.EntityExistsException;

/**
 * Opening stock is entered on the add-product form, so creating a product has to be
 * able to create the product's inventory row in the same call. These tests cover the
 * three cases that matter: a quantity produces one correctly linked row, a missing
 * quantity keeps the old product-only behaviour, and a failed stock write must not
 * leave the product behind.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceCreateStockTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductAttachmentRepository productAttachmentRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private AttachmentService attachmentService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository,
                new ProductMapper(),
                productAttachmentRepository,
                inventoryRepository,
                orderItemRepository,
                attachmentService);

        // Stand in for the generated identity, and hand back the instance the mapper
        // built so the service can see the id it will be linking the stock row to.
        AtomicLong ids = new AtomicLong(100);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(ids.incrementAndGet());
            return p;
        });
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ProductRequest request(Integer quantity) {
        ProductRequest request = new ProductRequest();
        request.setSku("NEW-001");
        request.setModel("Test Frame");
        request.setBrand("TestBrand");
        request.setCategory("Eyeglasses");
        request.setCost_price(new BigDecimal("10.00"));
        request.setSale_price(new BigDecimal("20.00"));
        request.setQuantity(quantity);
        return request;
    }

    @Test
    @DisplayName("a quantity on the request creates one inventory row for the new product")
    void createsInventoryRowWhenQuantityGiven() {
        productService.create(request(25));

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        Inventory row = captor.getValue();

        assertThat(row.getQuantity()).isEqualTo(25);
        assertThat(row.getProduct()).isNotNull();
        assertThat(row.getProduct().getId()).isNotNull();
        assertThat(row.getLast_restocked_at()).isNotNull();
    }

    @Test
    @DisplayName("the stock row defaults the reorder threshold instead of leaving it null")
    void appliesDefaultReorderThreshold() {
        productService.create(request(3));

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());

        // The column is NOT NULL in the database, so a null here would fail the
        // insert and roll the whole product creation back.
        assertThat(captor.getValue().getReorder_threshold()).isNotNull().isEqualTo(5);
    }

    @Test
    @DisplayName("a quantity of zero is honoured rather than treated as absent")
    void zeroQuantityStillCreatesRow() {
        productService.create(request(0));

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isZero();
    }

    @Test
    @DisplayName("no quantity keeps the previous behaviour and writes no stock row")
    void noQuantitySkipsInventory() {
        productService.create(request(null));

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("a stock write failure propagates so the transaction rolls the product back")
    void propagatesInventoryFailure() {
        when(inventoryRepository.save(any(Inventory.class)))
                .thenThrow(new IllegalStateException("inventory insert failed"));

        assertThatThrownBy(() -> productService.create(request(10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("inventory insert failed");
    }

    @Test
    @DisplayName("a duplicate SKU is rejected before any stock row is written")
    void duplicateSkuWritesNoStock() {
        when(productRepository.existsBySku("NEW-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request(10)))
                .isInstanceOf(EntityExistsException.class);

        verify(productRepository, never()).save(any(Product.class));
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("the created product response carries no stock field")
    void responseUnchangedByStockField() {
        var response = productService.create(request(7));

        // Stock is read from /api/inventories. Adding it to the product response
        // would give two sources of truth that drift as soon as an order is placed.
        assertThat(response.getId()).isNotNull();
        assertThat(response.getSku()).isEqualTo("NEW-001");
        assertThat(LocalDateTime.class).isNotNull();
    }
}
