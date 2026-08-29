package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.request.OrderItemRequest;
import com.example.spring_boot_project_api.dto.response.OrderItemResponse;
import com.example.spring_boot_project_api.mapper.OrderItemMapper;
import com.example.spring_boot_project_api.model.Inventory;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.OrderItem;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.repository.InventoryRepository;
import com.example.spring_boot_project_api.repository.OrderItemRepository;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private OrderItemMapper orderItemMapper;

    @InjectMocks
    private OrderItemService orderItemService;

    private Order order;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);

        product = new Product();
        product.setId(10L);
        product.setSale_price(new BigDecimal("50.00"));

        inventory = new Inventory();
        inventory.setId(100L);
        inventory.setProduct(product);
        inventory.setQuantity(5);
    }

    private OrderItemRequest sampleRequest(int quantity, BigDecimal lenPrice) {
        OrderItemRequest request = new OrderItemRequest();
        request.setOrder_id(1L);
        request.setProduct_id(10L);
        request.setQuantity(quantity);
        request.setLen_price(lenPrice);
        request.setLen_type("Bifocal");
        return request;
    }

    @Test
    void create_checksInventoryDecrementsAndComputesTotal() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
        when(orderItemMapper.toEntity(any(OrderItemRequest.class))).thenAnswer(inv -> new OrderItem());
        when(orderItemMapper.toResponse(any(OrderItem.class))).thenReturn(new OrderItemResponse());
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        orderItemService.create(sampleRequest(2, new BigDecimal("10.00")));

        assertThat(inventory.getQuantity()).isEqualTo(3);
        verify(inventoryRepository).save(inventory);
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    void create_throwsOnInsufficientStock() {
        inventory.setQuantity(1);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> orderItemService.create(sampleRequest(4, BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
        verify(inventoryRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void create_throwsWhenOrderNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.create(sampleRequest(1, BigDecimal.ZERO)))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void update_restoresOldInventoryAndDecrementsNew() {
        OrderItem existing = new OrderItem();
        existing.setId(5L);
        existing.setProduct(product);
        existing.setQuantity(2);
        existing.setOrder(order);

        when(orderItemRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
        when(orderItemMapper.toEntity(any(OrderItemRequest.class))).thenAnswer(inv -> new OrderItem());
        when(orderItemMapper.toResponse(any(OrderItem.class))).thenReturn(new OrderItemResponse());
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        inventory.setQuantity(5);

        orderItemService.update(5L, sampleRequest(3, new BigDecimal("10.00")));

        // restore +2 then decrement -3 => 5 + 2 - 3 = 4
        assertThat(inventory.getQuantity()).isEqualTo(4);
        verify(inventoryRepository, times(2)).save(inventory);
    }

    @Test
    void update_changingProductRestoresOldAndDecrementsNew() {
        Product product2 = new Product();
        product2.setId(20L);
        product2.setSale_price(new BigDecimal("30.00"));
        Inventory inventory2 = new Inventory();
        inventory2.setId(200L);
        inventory2.setProduct(product2);
        inventory2.setQuantity(8);

        OrderItem existing = new OrderItem();
        existing.setId(5L);
        existing.setProduct(product);
        existing.setQuantity(2);
        existing.setOrder(order);

        when(orderItemRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product2));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.findByProductId(20L)).thenReturn(Optional.of(inventory2));
        when(orderItemMapper.toEntity(any(OrderItemRequest.class))).thenAnswer(inv -> new OrderItem());
        when(orderItemMapper.toResponse(any(OrderItem.class))).thenReturn(new OrderItemResponse());
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        inventory.setQuantity(5);
        inventory2.setQuantity(8);

        OrderItemRequest req = sampleRequest(1, new BigDecimal("5.00"));
        req.setProduct_id(20L);

        orderItemService.update(5L, req);

        assertThat(inventory.getQuantity()).isEqualTo(7);   // 5 + 2 (old restored)
        assertThat(inventory2.getQuantity()).isEqualTo(7);  // 8 - 1 (new decremented)
    }

    @Test
    void update_recomputesTotal() {
        OrderItem existing = new OrderItem();
        existing.setId(5L);
        existing.setProduct(product);
        existing.setQuantity(2);
        existing.setOrder(order);

        when(orderItemRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
        when(orderItemMapper.toEntity(any(OrderItemRequest.class))).thenAnswer(inv -> new OrderItem());
        when(orderItemMapper.toResponse(any(OrderItem.class))).thenReturn(new OrderItemResponse());
        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        when(orderItemRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        orderItemService.update(5L, sampleRequest(3, new BigDecimal("10.00")));

        // (50 + 10) * 3 = 180
        assertThat(captor.getValue().getTotal_price()).isEqualByComparingTo("180.00");
    }

    @Test
    void delete_restoresInventory() {
        OrderItem existing = new OrderItem();
        existing.setId(5L);
        existing.setProduct(product);
        existing.setQuantity(2);

        when(orderItemRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

        inventory.setQuantity(5);

        orderItemService.delete(5L);

        assertThat(inventory.getQuantity()).isEqualTo(7); // 5 + 2
        verify(inventoryRepository).save(inventory);
        verify(orderItemRepository).delete(existing);
    }
}
