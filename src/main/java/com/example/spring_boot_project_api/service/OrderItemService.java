package com.example.spring_boot_project_api.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import jakarta.persistence.EntityNotFoundException;

@Service
public class OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderItemMapper orderItemMapper;

    public OrderItemService(OrderItemRepository orderItemRepository,
                            OrderRepository orderRepository,
                            ProductRepository productRepository,
                            InventoryRepository inventoryRepository,
                            OrderItemMapper orderItemMapper){
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderItemMapper = orderItemMapper;
    }

    @Transactional
    public OrderItemResponse create(OrderItemRequest request){
        OrderItem orderItem = buildOrderItem(request);
        orderItem.setCreated_at(LocalDateTime.now());

        return orderItemMapper.toResponse(orderItemRepository.save(orderItem));
    }

    @Transactional(readOnly = true)
    public List<OrderItemResponse> getAll(){
        return orderItemRepository.findAll().stream().map(orderItemMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderItemResponse getById(Long id){
        return orderItemMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<OrderItemResponse> getByOrderId(Long orderId){
        return orderItemRepository.findByOrderId(orderId).stream()
                .map(orderItemMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderItemResponse> getByProductId(Long productId){
        return orderItemRepository.findByProductId(productId).stream()
                .map(orderItemMapper::toResponse).toList();
    }

    @Transactional
    public OrderItemResponse update(Long id, OrderItemRequest request){
        OrderItem orderItem = getEntity(id);
        OrderItem updated = buildOrderItem(request);

        orderItem.setOrder(updated.getOrder());
        orderItem.setProduct(updated.getProduct());
        orderItem.setQuantity(updated.getQuantity());
        orderItem.setLen_type(updated.getLen_type());
        orderItem.setLen_coating(updated.getLen_coating());
        orderItem.setLen_index(updated.getLen_index());
        orderItem.setLen_price(updated.getLen_price());
        orderItem.setUnit_price(updated.getUnit_price());
        orderItem.setTotal_price(updated.getTotal_price());
        orderItem.setUpdated_at(LocalDateTime.now());

        return orderItemMapper.toResponse(orderItemRepository.save(orderItem));
    }

    public void delete(Long id){
        orderItemRepository.delete(getEntity(id));
    }

    private OrderItem buildOrderItem(OrderItemRequest request){
        Order order = orderRepository.findById(request.getOrder_id())
                     .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        Product product = productRepository.findById(request.getProduct_id())
                          .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        Inventory inventory = inventoryRepository.findByProductId(product.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Inventory not found for product " + product.getId()));

        if (inventory.getQuantity() < request.getQuantity()) {
            throw new IllegalArgumentException(
                    "Insufficient stock for product " + product.getId()
                    + ". Available: " + inventory.getQuantity()
                    + ", requested: " + request.getQuantity());
        }

        inventory.setQuantity(inventory.getQuantity() - request.getQuantity());
        inventoryRepository.save(inventory);

        BigDecimal unitPrice = product.getSale_price() != null
                ? BigDecimal.valueOf(product.getSale_price())
                : BigDecimal.ZERO;
        BigDecimal lenPrice = request.getLen_price() != null ? request.getLen_price() : BigDecimal.ZERO;

        OrderItem orderItem = orderItemMapper.toEntity(request);
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setUnit_price(unitPrice);
        orderItem.setTotal_price(unitPrice.add(lenPrice).multiply(BigDecimal.valueOf(request.getQuantity())));

        return orderItem;
    }

    private OrderItem getEntity(Long id){
        return orderItemRepository.findById(id)
               .orElseThrow(() -> new EntityNotFoundException("Order item not found"));
    }
}
