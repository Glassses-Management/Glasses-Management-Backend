package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
}
