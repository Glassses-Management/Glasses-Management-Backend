package com.example.spring_boot_project_api.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ProductRequest;
import com.example.spring_boot_project_api.mapper.ProductMapper;
import com.example.spring_boot_project_api.model.Attachment;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductAttachment;
import com.example.spring_boot_project_api.repository.AttachmentRepository;
import com.example.spring_boot_project_api.repository.ProductAttachmentRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProductSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final AttachmentRepository attachmentRepository;
    private final ProductAttachmentRepository productAttachmentRepository;

    @Value("${app.seed-products:true}")
    private boolean seedProducts;

    public ProductSeeder(ProductRepository productRepository, ProductMapper productMapper,
                         AttachmentRepository attachmentRepository,
                         ProductAttachmentRepository productAttachmentRepository) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.attachmentRepository = attachmentRepository;
        this.productAttachmentRepository = productAttachmentRepository;
    }

     private static final List<String> IMAGE_URLS = List.of(
             "https://images.unsplash.com/photo-1574258495973-f010dfbb5371?auto=format&fit=crop&w=600&q=80",
             "https://images.unsplash.com/photo-1577803645773-f96470509666?auto=format&fit=crop&w=600&q=80",
             "https://images.unsplash.com/photo-1577400983943-874919eca6ce?auto=format&fit=crop&w=600&q=80",
             "https://images.unsplash.com/photo-1591076482161-42ce6da69f67?auto=format&fit=crop&w=600&q=80",
             "https://images.unsplash.com/photo-1711564354066-2a6ec5fe5b66?auto=format&fit=crop&w=600&q=80",
             "https://images.unsplash.com/photo-1701252374731-191f1b10a18b?auto=format&fit=crop&w=600&q=80",
             "https://images.unsplash.com/photo-1701447428494-af7a13fc62e7?auto=format&fit=crop&w=600&q=80",
             "https://images.unsplash.com/photo-1559668364-b609c3b5d6f5?auto=format&fit=crop&w=600&q=80",
             "https://images.unsplash.com/photo-1648364210384-1d843fe77c29?auto=format&fit=crop&w=600&q=80",
             "https://images.unsplash.com/photo-1612458385022-67e811703251?auto=format&fit=crop&w=600&q=80");

     @Override
     @Transactional
     public void run(ApplicationArguments args) {
         if (!seedProducts) {
             log.info("Product seeding disabled (app.seed-products=false)");
             return;
         }

         List<ProductRequest> samples = buildSamples();

         if (productRepository.count() == 0) {
             for (ProductRequest request : samples) {
                 productRepository.save(productMapper.toEntity(request));
             }
             log.info("Seeded {} sample products", samples.size());
         }

         for (int i = 0; i < samples.size(); i++) {
             ProductRequest request = samples.get(i);
             String imageUrl = IMAGE_URLS.get(i % IMAGE_URLS.size());
             productRepository.findBySku(request.getSku()).ifPresent(product -> {
                 removeExistingAttachments(product);
                 attachImage(product, imageUrl);
             });
         }
     }

     private void removeExistingAttachments(Product product) {
         List<ProductAttachment> existingLinks = productAttachmentRepository.findByProductId(product.getId());
         for (ProductAttachment link : existingLinks) {
             Attachment attachment = link.getAttachment();
             productAttachmentRepository.delete(link);
             attachmentRepository.delete(attachment);
         }
     }

     private void attachImage(Product product, String imageUrl) {
        Attachment attachment = new Attachment();
        attachment.setFileName(product.getSku() + "-image.jpg");
        attachment.setFileType("image/jpeg");
        attachment.setFilePath(imageUrl);
        attachment.setFileSize(0L);
        attachment = attachmentRepository.save(attachment);

        ProductAttachment link = new ProductAttachment();
        link.setProduct(product);
        link.setAttachment(attachment);
        productAttachmentRepository.save(link);

        log.info("Attached image to product {}", product.getSku());
    }

    private List<ProductRequest> buildSamples() {
        return List.of(
                sample("GLS-001", "Aurelia", "Ray-Ban", "Frames", "Matte Black", "Acetate", "52-18-145", "85.00", "199.00", "LuxOptica", "0912-555-101"),
                sample("GLS-002", "Monroe", "Oakley", "Sunglasses", "Tortoise", "Acetate", "54-20-140", "60.00", "149.00", "LuxOptica", "0912-555-102"),
                sample("GLS-003", "Seraphine", "Prada", "Frames", "Gold", "Metal", "55-17-140", "120.00", "289.00", "FrameHouse", "0912-555-103"),
                sample("GLS-004", "Tavistock", "Persol", "Sunglasses", "Havana", "Acetate", "51-19-145", "95.00", "229.00", "FrameHouse", "0912-555-104"),
                sample("GLS-005", "Cygnus", "Gucci", "Frames", "Transparent", "Acetate", "53-16-140", "130.00", "319.00", "VistaSupply", "0912-555-105"),
                sample("GLS-006", "Orion", "Tom Ford", "Sunglasses", "Gloss Black", "Acetate", "57-16-145", "110.00", "269.00", "VistaSupply", "0912-555-106"),
                sample("GLS-007", "Nimbus", "Lindberg", "Frames", "Silver", "Titanium", "50-18-135", "180.00", "459.00", "NordicGlass", "0912-555-107"),
                sample("GLS-008", "Comet", "Ray-Ban", "Sunglasses", "Green", "Metal", "56-17-145", "75.00", "179.00", "LuxOptica", "0912-555-108"),
                sample("GLS-009", "Vesper", "Chanel", "Frames", "Crystal", "Acetate", "52-17-140", "160.00", "399.00", "FrameHouse", "0912-555-109"),
                sample("GLS-010", "Atlas", "Oakley", "Sunglasses", "Matte Sky", "Plastic", "55-17-140", "90.00", "209.00", "LuxOptica", "0912-555-110"));
    }

    private ProductRequest sample(String sku, String model, String brand, String category,
                                   String color, String material, String size,
                                   String costPrice, String salePrice,
                                   String supplierName, String supplierContact) {
        ProductRequest request = new ProductRequest();
        request.setSku(sku);
        request.setModel(model);
        request.setBrand(brand);
        request.setCategory(category);
        request.setColor(color);
        request.setMaterial(material);
        request.setSize(size);
        request.setCost_price(new BigDecimal(costPrice));
        request.setSale_price(new BigDecimal(salePrice));
        request.setSupplier_name(supplierName);
        request.setSupplier_contact(supplierContact);
        return request;
    }
}