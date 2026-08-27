package com.example.spring_boot_project_api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.OrderRequest;
import com.example.spring_boot_project_api.dto.response.OrderResponse;
import com.example.spring_boot_project_api.mapper.OrderMapper;
import com.example.spring_boot_project_api.model.Branch;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.Inventory;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.OrderItem;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.repository.BranchRepository;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.InventoryRepository;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        BranchRepository branchRepository,
                        ProductRepository productRepository,
                        InventoryRepository inventoryRepository,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        // Step 1: Validate Customer
        Customer customer = customerRepository.findById(request.getCustomer_id())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        // Step 1: Validate Branch
        Branch branch = branchRepository.findById(request.getBranch_id())
                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));

        // Step 1: Validate at least one item
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        // Create Order shell
        Order order = new Order();
        order.setCustomer(customer);
        order.setBranch(branch);
        order.setStatus("PENDING");
        order.setOrder_date(LocalDate.now());
        order.setCreated_at(LocalDateTime.now());

        List<OrderItem> orderItems = new ArrayList<>();
        Float total = 0f;

        // Step 2: Process each item
        for (OrderRequest.ItemRequest itemRequest : request.getItems()) {
            // Step 2.1: Find Product
            Product product = productRepository.findById(itemRequest.getProduct_id())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Product not found with id: " + itemRequest.getProduct_id()));

            // Step 2.2: Get current product price (snapshot)
            Float unitPrice = product.getSale_price() != null ? product.getSale_price() : 0f;

            // Step 2.3: Find Inventory by product + branch
            Inventory inventory = inventoryRepository.findByProduct_IdAndBranch_Id(
                    product.getId(), branch.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Inventory not found for product " + product.getId()
                            + " in branch " + branch.getId()));

            // Step 2.4: Check available stock
            if (inventory.getQuantity() < itemRequest.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product " + product.getId()
                        + ". Available: " + inventory.getQuantity()
                        + ", requested: " + itemRequest.getQuantity());
            }

            // Step 2.5: Decrease inventory
            inventory.setQuantity(inventory.getQuantity() - itemRequest.getQuantity());

            // Step 2.6: Create OrderItem
            Float subtotal = unitPrice * itemRequest.getQuantity();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnit_price(unitPrice);
            orderItem.setTotal_price(subtotal);
            orderItem.setCreated_at(LocalDateTime.now());

            orderItems.add(orderItem);

            // Step 3: Accumulate total
            total += subtotal;
        }

        // Step 3: Set calculated total
        order.setTotal(total);

        // Step 4: Attach items and save
        order.setItems(orderItems);

        // Step 5: Save (single transaction — inventory changes, order, and items all commit or rollback together)
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
    public List<OrderResponse> getByBranchId(Long branchId) {
        return orderRepository.findByBranchId(branchId).stream()
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

        Branch branch = branchRepository.findById(request.getBranch_id())
                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));

        order.setCustomer(customer);
        order.setBranch(branch);
        order.setUpdated_at(LocalDateTime.now());

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public void delete(Long id) {
        orderRepository.delete(getEntity(id));
    }

    private Order getEntity(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }
}
