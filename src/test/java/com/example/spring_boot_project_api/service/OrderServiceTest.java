package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.request.OrderRequest;
import com.example.spring_boot_project_api.dto.response.OrderResponse;
import com.example.spring_boot_project_api.mapper.OrderMapper;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.Inventory;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.OrderItem;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.InventoryRepository;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private Customer customer;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);

        product = new Product();
        product.setId(10L);
        product.setSale_price(new BigDecimal("100.00"));

        inventory = new Inventory();
        inventory.setId(100L);
        inventory.setProduct(product);
        inventory.setQuantity(10);
    }

    private OrderRequest sampleRequest(int quantity, BigDecimal lenPrice) {
        OrderRequest.ItemRequest item = new OrderRequest.ItemRequest();
        item.setProduct_id(10L);
        item.setQuantity(quantity);
        item.setLen_price(lenPrice);
        item.setLen_type("Single Vision");
        item.setLen_coating("AR");

        OrderRequest request = new OrderRequest();
        request.setCustomer_id(1L);
        request.setItems(List.of(item));
        return request;
    }

    @Test
    void create_calculatesTotalWithLensPriceAndDecrementsInventory() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(new OrderResponse());

        orderService.create(sampleRequest(2, new BigDecimal("20.00")));

        Order saved = orderCaptor.getValue();
        assertThat(saved.getTotal()).isEqualByComparingTo("240.00");
        assertThat(inventory.getQuantity()).isEqualTo(8);
        verify(inventoryRepository, times(1)).save(inventory);
        verify(orderRepository, times(1)).save(saved);
    }

    @Test
    void create_throwsWhenInsufficientStock() {
        inventory.setQuantity(1);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> orderService.create(sampleRequest(5, BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
        verify(inventoryRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_throwsWhenProductNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(sampleRequest(1, BigDecimal.ZERO)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void create_throwsWhenCustomerNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(sampleRequest(1, BigDecimal.ZERO)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void delete_restoresInventoryForEachItem() {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(3);
        Order order = new Order();
        order.setId(1L);
        order.setItems(List.of(item));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

        inventory.setQuantity(7);

        orderService.delete(1L);

        assertThat(inventory.getQuantity()).isEqualTo(10);
        verify(inventoryRepository, times(1)).save(inventory);
        verify(orderRepository, times(1)).delete(order);
    }

    private Order orderWithStatus(String status) {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(status);
        return order;
    }

    @Test
    void changeStatus_allowsValidTransition() {
        Order order = orderWithStatus("PENDING");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(new OrderResponse());

        orderService.changeStatus(1L, "CONFIRMED");

        assertThat(order.getStatus()).isEqualTo("CONFIRMED");
        verify(orderRepository).save(order);
    }

    @Test
    void changeStatus_rejectsInvalidTransition() {
        Order order = orderWithStatus("PENDING");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus(1L, "COMPLETED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transition");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void changeStatus_rejectsUnknownStatus() {
        Order order = orderWithStatus("PENDING");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus(1L, "FOO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid order status");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void search_appliesFiltersAndReturnsPage() {
        Page<Order> page = new PageImpl<>(List.of(new Order()));
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(new OrderResponse());

        Page<OrderResponse> result = orderService.search(null, "PENDING", null, null, Pageable.ofSize(10));

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_throwsWhenDateFromAfterDateTo() {
        LocalDate from = LocalDate.of(2025, 1, 10);
        LocalDate to = LocalDate.of(2025, 1, 1);

        assertThatThrownBy(() -> orderService.search(null, null, from, to, Pageable.ofSize(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateFrom must not be after dateTo");
    }

    @Test
    void search_throwsOnInvalidSortProperty() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("unknownField"));

        assertThatThrownBy(() -> orderService.search(null, null, null, null, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void create_multiItemSumsTotalCorrectly() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(new OrderResponse());

        OrderRequest.ItemRequest i1 = new OrderRequest.ItemRequest();
        i1.setProduct_id(10L);
        i1.setQuantity(2);
        i1.setLen_price(new BigDecimal("20.00"));
        i1.setLen_type("Single Vision");
        i1.setLen_coating("AR");

        OrderRequest.ItemRequest i2 = new OrderRequest.ItemRequest();
        i2.setProduct_id(10L);
        i2.setQuantity(1);
        i2.setLen_price(new BigDecimal("50.00"));
        i2.setLen_type("Single Vision");
        i2.setLen_coating("AR");

        OrderRequest request = new OrderRequest();
        request.setCustomer_id(1L);
        request.setItems(List.of(i1, i2));

        orderService.create(request);

        // (100 + 20) * 2 + (100 + 50) * 1 = 240 + 150 = 390
        assertThat(orderCaptor.getValue().getTotal()).isEqualByComparingTo("390.00");
    }

    @Test
    void create_mapsLensFieldsOntoOrderItem() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(new OrderResponse());

        OrderRequest request = sampleRequest(1, new BigDecimal("20.00"));
        request.getItems().get(0).setLen_index("1.67");

        orderService.create(request);

        OrderItem item = orderCaptor.getValue().getItems().get(0);
        assertThat(item.getLen_type()).isEqualTo("Single Vision");
        assertThat(item.getLen_coating()).isEqualTo("AR");
        assertThat(item.getLen_index()).isEqualTo(1.67f);
    }
}
