package com.example.spring_boot_project_api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import jakarta.persistence.EntityNotFoundException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        InventoryRepository inventoryRepository,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomer_id())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setStatus("PENDING");
        order.setOrder_date(LocalDate.now());
        order.setCreated_at(LocalDateTime.now());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderRequest.ItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProduct_id())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Product not found with id: " + itemRequest.getProduct_id()));

            BigDecimal unitPrice = product.getSale_price() != null
                    ? product.getSale_price()
                    : BigDecimal.ZERO;
            BigDecimal lenPrice = itemRequest.getLen_price() != null
                    ? itemRequest.getLen_price()
                    : BigDecimal.ZERO;

            Inventory inventory = inventoryRepository.findByProductId(product.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Inventory not found for product " + product.getId()));

            if (inventory.getQuantity() < itemRequest.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product " + product.getId()
                        + ". Available: " + inventory.getQuantity()
                        + ", requested: " + itemRequest.getQuantity());
            }

            inventory.setQuantity(inventory.getQuantity() - itemRequest.getQuantity());
            inventoryRepository.save(inventory);

            BigDecimal subtotal = unitPrice.add(lenPrice).multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnit_price(unitPrice);
            orderItem.setLen_price(lenPrice);
            orderItem.setLen_type(itemRequest.getLen_type());
            orderItem.setLen_coating(itemRequest.getLen_coating());
            if (itemRequest.getLen_index() != null) {
                orderItem.setLen_index(Float.parseFloat(itemRequest.getLen_index()));
            }
            orderItem.setTotal_price(subtotal);
            orderItem.setCreated_at(LocalDateTime.now());

            orderItems.add(orderItem);

            total = total.add(subtotal);
        }

        order.setTotal(total);
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAll() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return orderMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getByStatus(String status) {
        return orderRepository.findByStatus(status).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse update(Long id, OrderRequest request) {
        Order order = getEntity(id);

        Customer customer = customerRepository.findById(request.getCustomer_id())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        order.setCustomer(customer);
        order.setUpdated_at(LocalDateTime.now());

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public void delete(Long id) {
        Order order = getEntity(id);

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    inventoryRepository.findByProductId(item.getProduct().getId())
                            .ifPresent(inventory -> {
                                inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
                                inventoryRepository.save(inventory);
                            });
                }
            }
        }

        orderRepository.delete(order);
    }

    private Order getEntity(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }
}
