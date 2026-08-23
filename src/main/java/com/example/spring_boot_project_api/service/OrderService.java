package com.example.spring_boot_project_api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.OrderRequest;
import com.example.spring_boot_project_api.dto.response.OrderResponse;
import com.example.spring_boot_project_api.mapper.OrderMapper;
import com.example.spring_boot_project_api.model.Branch;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Prescription;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.BranchRepository;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.PrescriptionRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        UserRepository userRepository,
                        BranchRepository branchRepository,
                        PrescriptionRepository prescriptionRepository,
                        ProductRepository productRepository,
                        OrderMapper orderMapper){
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderResponse create(OrderRequest request){
        Order order = buildOrder(request);
        order.setCreated_at(LocalDateTime.now());

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAll(){
        return orderRepository.findAll().stream().map(orderMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id){
        return orderMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getByCustomerId(Long customerId){
        return orderRepository.findByCustomerId(customerId).stream()
                .map(orderMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getByBranchId(Long branchId){
        return orderRepository.findByBranchId(branchId).stream()
                .map(orderMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getByStatus(String status){
        return orderRepository.findByStatus(status).stream()
                .map(orderMapper::toResponse).toList();
    }

    @Transactional
    public OrderResponse update(Long id, OrderRequest request){
        Order order = getEntity(id);

        Order updated = buildOrder(request);

        order.setCustomer(updated.getCustomer());
        order.setUser(updated.getUser());
        order.setBranch(updated.getBranch());
        order.setPrescription(updated.getPrescription());
        order.setProduct(updated.getProduct());
        order.setQuantity(updated.getQuantity());
        order.setUnit_price(updated.getUnit_price());
        order.setTotal_amount(updated.getTotal_amount());
        order.setStatus(updated.getStatus());
        order.setPayment_method(updated.getPayment_method());
        order.setPayment_status(updated.getPayment_status());

        return orderMapper.toResponse(orderRepository.save(order));
    }

    public void delete(Long id){
        orderRepository.delete(getEntity(id));
    }

    private Order buildOrder(OrderRequest request){
        Customer customer = customerRepository.findById(request.getCustomer_id())
                            .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        Branch branch = branchRepository.findById(request.getBranch_id())
                        .orElseThrow(() -> new EntityNotFoundException("Branch not found"));

        Product product = productRepository.findById(request.getProduct_id())
                          .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        User user = resolveUser(request.getUser_id());
        Prescription prescription = resolvePrescription(request.getPrescription_id());

        Float unitPrice = product.getSale_price() != null ? product.getSale_price() : 0f;

        Order order = orderMapper.toEntity(request);
        order.setCustomer(customer);
        order.setUser(user);
        order.setBranch(branch);
        order.setPrescription(prescription);
        order.setProduct(product);

        order.setUnit_price(unitPrice);
        order.setTotal_amount(unitPrice * request.getQuantity());
        order.setOrder_date(LocalDate.now());

        if (order.getStatus() == null || order.getStatus().isBlank()) {
            order.setStatus("PENDING");
        }
        if (order.getPayment_status() == null || order.getPayment_status().isBlank()) {
            order.setPayment_status("UNPAID");
        }

        return order;
    }

    private User resolveUser(Long userId){
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
               .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private Prescription resolvePrescription(Long prescriptionId){
        if (prescriptionId == null) {
            return null;
        }
        return prescriptionRepository.findById(prescriptionId)
               .orElseThrow(() -> new EntityNotFoundException("Prescription not found"));
    }

    private Order getEntity(Long id){
        return orderRepository.findById(id)
               .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }
}
