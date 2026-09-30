package com.example.spring_boot_project_api.service;

import java.io.IOException;
import java.time.LocalDateTime;
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
import com.example.spring_boot_project_api.model.Inventory;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductAttachment;
import com.example.spring_boot_project_api.repository.InventoryRepository;
import com.example.spring_boot_project_api.repository.OrderItemRepository;
import com.example.spring_boot_project_api.repository.ProductAttachmentRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.util.SpecificationSort;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

@Service
public class ProductService {
    // Opening stock created together with a product starts at this reorder
    // threshold. The column is NOT NULL, so a product created with a quantity
    // always needs one; staff can change it later from the inventory page.
    private static final int DEFAULT_REORDER_THRESHOLD = 5;

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
        Product saved = productRepository.save(product);

        // Opening stock is optional. When the caller sends no quantity the product
        // is created exactly as it was before this field existed: no inventory row,
        // and the quantity gets added from the inventory page afterwards.
        //
        // When a quantity is sent the inventory row is written here rather than
        // through a second request, so the product and its stock always appear
        // together. Both writes share this method's transaction, so if the row
        // cannot be saved the product save is rolled back with it and the caller
        // gets an error instead of a product with no stock.
        if (request.getQuantity() != null) {
            Inventory inventory = new Inventory();
            inventory.setProduct(saved);
            inventory.setQuantity(request.getQuantity());
            inventory.setReorder_threshold(DEFAULT_REORDER_THRESHOLD);
            inventory.setLast_restocked_at(LocalDateTime.now());
            inventoryRepository.save(inventory);
        }

        return productMapper.toCreatedAtResponse(saved);
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

        // sale_price is on the allowlist above and is a real column, but Spring
        // Data splits the sort property on "_" as well as "." and cannot resolve
        // it, which used to surface as a 500. The ordering is therefore applied to
        // the query instead of to the Pageable.
        return productRepository
                .findAll(SpecificationSort.apply(spec, pageable.getSort()),
                        SpecificationSort.withoutSort(pageable))
                .map(productMapper::toResponse);
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
