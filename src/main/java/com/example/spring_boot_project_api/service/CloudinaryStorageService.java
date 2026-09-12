package com.example.spring_boot_project_api.service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryStorageService {

    private final Cloudinary cloudinary;
    private final String uploadFolder;

    public CloudinaryStorageService(
            @Value("${app.cloudinary.cloud-name}") String cloudName,
            @Value("${app.cloudinary.api-key}") String apiKey,
            @Value("${app.cloudinary.api-secret}") String apiSecret,
            @Value("${app.cloudinary.upload-folder:attachments}") String uploadFolder) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret));
        this.uploadFolder = uploadFolder;
    }

    public String upload(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", uploadFolder,
                "public_id", UUID.randomUUID().toString(),
                "resource_type", "auto",
                "overwrite", true));
        return (String) result.get("secure_url");
    }

    public void delete(String secureUrl) throws IOException {
        ResolvedId resolved = resolve(secureUrl);
        if (resolved == null) {
            return;
        }
        cloudinary.uploader().destroy(resolved.publicId,
                ObjectUtils.asMap("resource_type", resolved.resourceType));
    }

    private ResolvedId resolve(String secureUrl) {
        if (secureUrl == null || secureUrl.isBlank()) {
            return null;
        }
        try {
            String path = new java.net.URI(secureUrl).getPath();
            String[] segments = path.split("/");
            int uploadIdx = -1;
            for (int i = 0; i < segments.length; i++) {
                if ("upload".equals(segments[i])) {
                    uploadIdx = i;
                    break;
                }
            }
            if (uploadIdx < 0) {
                return null;
            }
            String resourceType = uploadIdx > 0 ? segments[uploadIdx - 1] : "auto";

            StringBuilder publicId = new StringBuilder();
            boolean first = true;
            for (int i = uploadIdx + 1; i < segments.length; i++) {
                String seg = segments[i];
                if (i == uploadIdx + 1 && seg.matches("v\\d+")) {
                    continue;
                }
                if (!first) {
                    publicId.append("/");
                }
                publicId.append(seg);
                first = false;
            }

            String id = publicId.toString();
            int lastDot = id.lastIndexOf('.');
            if (lastDot > id.lastIndexOf('/')) {
                id = id.substring(0, lastDot);
            }
            return new ResolvedId(id, resourceType);
        } catch (Exception e) {
            return null;
        }
    }

    private record ResolvedId(String publicId, String resourceType) {
    }
}
