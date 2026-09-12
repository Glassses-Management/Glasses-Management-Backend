package com.example.spring_boot_project_api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.AppointmentRequest;
import com.example.spring_boot_project_api.dto.request.CustomerRequestRequest;
import com.example.spring_boot_project_api.dto.request.OrderRequest;
import com.example.spring_boot_project_api.dto.response.AppointmentResponse;
import com.example.spring_boot_project_api.dto.response.OrderResponse;
import com.example.spring_boot_project_api.mapper.OrderMapper;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.Inventory;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.OrderItem;
import com.example.spring_boot_project_api.model.Prescription;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.AppointmentRepository;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.InventoryRepository;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.PrescriptionRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.AppointmentService;

import jakarta.persistence.EntityNotFoundException;

@Service
public class OrderService {

    private static final Set<String> KNOWN_STATUSES = Set.of(
            "PENDING", "CONFIRMED", "IN_PROGRESS", "READY_FOR_PICKUP", "COMPLETED", "CANCELLED");

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = new HashMap<>();

    static {
        ALLOWED_TRANSITIONS.put("PENDING", Set.of("CONFIRMED", "CANCELLED"));
        ALLOWED_TRANSITIONS.put("CONFIRMED", Set.of("IN_PROGRESS", "CANCELLED"));
        ALLOWED_TRANSITIONS.put("IN_PROGRESS", Set.of("READY_FOR_PICKUP", "CANCELLED"));
        ALLOWED_TRANSITIONS.put("READY_FOR_PICKUP", Set.of("COMPLETED", "CANCELLED"));
        ALLOWED_TRANSITIONS.put("COMPLETED", Set.of());
        ALLOWED_TRANSITIONS.put("CANCELLED", Set.of());
    }

    private final OrderRepository orderRepository;
    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final OrderMapper orderMapper;
    private final AppointmentService appointmentService;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        InventoryRepository inventoryRepository,
                        UserRepository userRepository,
                        PrescriptionRepository prescriptionRepository,
                        OrderMapper orderMapper,
                        AppointmentService appointmentService,
                        AppointmentRepository appointmentRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.orderMapper = orderMapper;
        this.appointmentService = appointmentService;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomer_id())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        Prescription prescription = null;
        if (request.getPrescription_id() != null) {
            prescription = prescriptionRepository.findById(request.getPrescription_id())
                    .filter(p -> p.getCustomer() != null
                            && p.getCustomer().getId().equals(customer.getId()))
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Prescription not found for id: " + request.getPrescription_id()));
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setPrescription(prescription);
        order.setStatus("PENDING");
        order.setOrder_date(LocalDate.now());
        order.setCreatedAt(LocalDateTime.now());

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
            orderItem.setCreatedAt(LocalDateTime.now());

            orderItems.add(orderItem);

            total = total.add(subtotal);
        }

        order.setTotal(total);
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional
    public Object submitCustomerRequest(Long uid, CustomerRequestRequest request) {
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Customer customer = customerRepository.findByUserId(uid)
                .orElseThrow(() -> new EntityNotFoundException("Customer profile not found"));

        String type = request.getType() == null ? null : request.getType().trim().toLowerCase();
        if (type == null || !("exam".equals(type) || "product".equals(type))) {
            throw new IllegalArgumentException("Request type must be 'exam' or 'product'");
        }

        if ("exam".equals(type)) {
            return createExamAppointment(customer, request.getNotes());
        }

        throw new IllegalArgumentException("Use /api/orders to create a product order with prescription_id");
    }

    @Transactional
    public AppointmentResponse createExamAppointment(Customer customer, String notes) {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();

        if (appointmentRepository.existsByCustomerIdAndCreatedAtBetween(customer.getId(), dayStart, dayEnd)) {
            throw new IllegalArgumentException(
                    "You already submitted an exam request today. Please try again tomorrow.");
        }

        AppointmentRequest req = new AppointmentRequest();
        req.setCustomer_id(customer.getId());
        req.setOptometrist_id(null);
        req.setScheduled_at(null);
        req.setStatus("PENDING_REVIEW");
        req.setNotes(notes);

        return appointmentService.create(req);
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
    public Page<OrderResponse> search(Long customerId, String status,
                                      LocalDate dateFrom, LocalDate dateTo,
                                      Pageable pageable) {
        assertSortAllowed(pageable, Set.of("id", "order_date", "status", "createdAt", "updatedAt"));

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("dateFrom must not be after dateTo");
        }

        Specification<Order> spec = (root, query, cb) -> cb.conjunction();

        if (customerId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("customer").get("id"), customerId));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
        }
        if (dateFrom != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("order_date"), dateFrom));
        }
        if (dateTo != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("order_date"), dateTo));
        }

        return orderRepository.findAll(spec, pageable).map(orderMapper::toResponse);
    }

    private void assertSortAllowed(Pageable pageable, Set<String> allowed) {
        pageable.getSort().forEach(order -> {
            if (!allowed.contains(order.getProperty())) {
                throw new IllegalArgumentException("Sorting by '" + order.getProperty() + "' is not allowed");
            }
        });
    }

    @Transactional
    public OrderResponse update(Long id, OrderRequest request) {
        Order order = getEntity(id);

        Customer customer = customerRepository.findById(request.getCustomer_id())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        order.setCustomer(customer);
        order.setUpdatedAt(LocalDateTime.now());

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse changeStatus(Long id, String newStatus) {
        Order order = getEntity(id);

        String target = newStatus == null ? null : newStatus.trim().toUpperCase();
        if (!KNOWN_STATUSES.contains(target)) {
            throw new IllegalArgumentException("Invalid order status: " + newStatus);
        }

        String current = order.getStatus() != null ? order.getStatus() : "PENDING";
        if (current.equals(target)) {
            return orderMapper.toResponse(order);
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new IllegalArgumentException(
                    "Cannot transition order from " + current + " to " + target);
        }

        order.setStatus(target);
        order.setUpdatedAt(LocalDateTime.now());
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
