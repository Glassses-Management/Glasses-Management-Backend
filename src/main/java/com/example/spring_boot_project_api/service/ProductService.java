package com.example.spring_boot_project_api.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ProductRequest;
import com.example.spring_boot_project_api.dto.response.ProductResponse;
import com.example.spring_boot_project_api.mapper.ProductMapper;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductAttachment;
import com.example.spring_boot_project_api.repository.InventoryRepository;
import com.example.spring_boot_project_api.repository.OrderItemRepository;
import com.example.spring_boot_project_api.repository.ProductAttachmentRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductAttachmentRepository productAttachmentRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final AttachmentService attachmentService;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper,
                          ProductAttachmentRepository productAttachmentRepository,
                          InventoryRepository inventoryRepository,
                          OrderItemRepository orderItemRepository,
                          AttachmentService attachmentService){
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.productAttachmentRepository = productAttachmentRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.attachmentService = attachmentService;
    }

    @Transactional
    public ProductResponse create(ProductRequest request){
        if (productRepository.existsBySku(request.getSku())) {
            throw new EntityExistsException("SKU already exists");
        }

        Product product = productMapper.toEntity(request);

        return productMapper.toCreatedAtResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll(){
        return productRepository.findAll().stream().map(productMapper::toCreatedAtResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String category, String brand, String model,
                                       String search, Pageable pageable) {
        assertSortAllowed(pageable, Set.of("id", "sku", "model", "brand", "category", "sale_price", "createdAt", "updatedAt"));

        Specification<Product> spec = (root, query, cb) -> cb.conjunction();

        if (category != null && !category.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
        }
        if (brand != null && !brand.isBlank()) {
            String like = "%" + brand.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("brand")), like));
        }
        if (model != null && !model.isBlank()) {
            String like = "%" + model.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("model")), like));
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("sku")), like),
                    cb.like(cb.lower(root.get("model")), like),
                    cb.like(cb.lower(root.get("brand")), like)));
        }

        return productRepository.findAll(spec, pageable).map(productMapper::toResponse);
    }

    private void assertSortAllowed(Pageable pageable, Set<String> allowed) {
        pageable.getSort().forEach(order -> {
            if (!allowed.contains(order.getProperty())) {
                throw new IllegalArgumentException("Sorting by '" + order.getProperty() + "' is not allowed");
            }
        });
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id){
        return productMapper.toCreatedAtResponse(getEntity(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request){
        Product product = getEntity(id);

        if (productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
            throw new EntityExistsException("SKU already exists");
        }

        product.setSku(request.getSku());
        product.setModel(request.getModel());
        product.setBrand(request.getBrand());
        product.setColor(request.getColor());
        product.setMaterial(request.getMaterial());
        product.setSize(request.getSize());
        product.setCost_price(request.getCost_price());
        product.setSale_price(request.getSale_price());
        product.setSupplier_name(request.getSupplier_name());
        product.setSupplier_contact(request.getSupplier_contact());
        product.setCategory(request.getCategory());

        return productMapper.toUpdatedAtResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) throws IOException {
        Product product = getEntity(id);

        if (!orderItemRepository.findByProductId(id).isEmpty()) {
            throw new IllegalStateException("Product cannot be deleted because it is referenced by existing orders");
        }

        List<ProductAttachment> productAttachments = productAttachmentRepository.findByProductId(id);

        inventoryRepository.findAllByProductId(id).forEach(inventoryRepository::delete);

        for (ProductAttachment pa : productAttachments) {
            Long attachmentId = pa.getAttachment().getId();
            boolean sharedWithOtherProducts = productAttachmentRepository.findByAttachmentId(attachmentId).stream()
                    .anyMatch(other -> !other.getProduct().getId().equals(id));
            productAttachmentRepository.delete(pa);
            if (!sharedWithOtherProducts) {
                attachmentService.delete(attachmentId);
            }
        }

        productRepository.delete(product);
    }

    private Product getEntity(Long id){
        return productRepository.findById(id)
               .orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }
}
