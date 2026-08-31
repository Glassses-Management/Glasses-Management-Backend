package com.example.spring_boot_project_api.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.service.QrCodeService;

@RestController
@RequestMapping("/api/qr")
public class QrCodeController {

    private final QrCodeService qrCodeService;
    private final String baseUrl;
    private final Path tunnelUrlFile;
    private final Path tunnelQrFile;

    public QrCodeController(
            QrCodeService qrCodeService,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl,
            @Value("${app.tunnel.url-file:}") String tunnelUrlFile,
            @Value("${app.tunnel.qr-file:}") String tunnelQrFile) {
        this.qrCodeService = qrCodeService;
        this.baseUrl = baseUrl;
        this.tunnelUrlFile = tunnelUrlFile.isBlank() ? null : Path.of(tunnelUrlFile);
        this.tunnelQrFile = tunnelQrFile.isBlank() ? null : Path.of(tunnelQrFile);
    }

    /**
     * Dynamically resolves the current request-form URL.
     * Prefers the Cloudflare tunnel URL when available,
     * falls back to APP_BASE_URL.
     */
    private String resolveRequestFormUrl() {
        String url = readTunnelUrl();
        if (url != null) {
            return url + "/request-form";
        }
        return baseUrl + "/request-form";
    }

    /**
     * Returns the raw tunnel URL (without /request-form) or null.
     */
    private String readTunnelUrl() {
        if (tunnelUrlFile == null || !Files.exists(tunnelUrlFile)) {
            return null;
        }
        try {
            String url = Files.readString(tunnelUrlFile).trim();
            return url.isEmpty() ? null : url;
        } catch (IOException e) {
            return null;
        }
    }

    @GetMapping(value = "/request", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> requestFormQr() {
        String url = resolveRequestFormUrl();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCodeService.generateQrPng(url, 300));
    }

    @GetMapping(value = "/current", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> currentPreGeneratedQr() {
        if (tunnelQrFile == null || !Files.exists(tunnelQrFile)) {
            // Fall back to dynamically generated QR
            return requestFormQr();
        }
        try {
            byte[] png = Files.readAllBytes(tunnelQrFile);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(png);
        } catch (IOException e) {
            return requestFormQr();
        }
    }

    @GetMapping(value = "/tunnel-url", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<java.util.Map<String, String>> tunnelUrl() {
        String tunnelUrl = readTunnelUrl();
        String qrUrl = tunnelUrl != null
                ? tunnelUrl + "/request-form"
                : baseUrl + "/request-form";
        java.util.Map<String, String> body = new java.util.LinkedHashMap<>();
        body.put("tunnelUrl", tunnelUrl != null ? tunnelUrl : baseUrl);
        body.put("qrUrl", qrUrl);
        body.put("source", tunnelUrl != null ? "cloudflare-tunnel" : "app-base-url");
        return ResponseEntity.ok(body);
    }
}
