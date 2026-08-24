package com.example.spring_boot_project_api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.OrderItemRequest;
import com.example.spring_boot_project_api.dto.response.OrderItemResponse;
import com.example.spring_boot_project_api.mapper.OrderItemMapper;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.OrderItem;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.repository.OrderItemRepository;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemMapper orderItemMapper;

    public OrderItemService(OrderItemRepository orderItemRepository,
                            OrderRepository orderRepository,
                            ProductRepository productRepository,
                            OrderItemMapper orderItemMapper){
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
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

        Float unitPrice = product.getSale_price() != null ? product.getSale_price() : 0f;
        Float lenPrice = request.getLen_price() != null ? request.getLen_price() : 0f;

        OrderItem orderItem = orderItemMapper.toEntity(request);
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setUnit_price(unitPrice);
        orderItem.setTotal_price((unitPrice + lenPrice) * request.getQuantity());

        return orderItem;
    }

    private OrderItem getEntity(Long id){
        return orderItemRepository.findById(id)
               .orElseThrow(() -> new EntityNotFoundException("Order item not found"));
    }
}
