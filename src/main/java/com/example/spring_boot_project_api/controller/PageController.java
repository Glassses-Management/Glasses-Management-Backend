package com.example.spring_boot_project_api.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PageController {

    @GetMapping(value = {"/request-form", "/request-form/"}, produces = MediaType.TEXT_HTML_VALUE)
    public String requestForm() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/request-form.html");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @GetMapping(value = {"/qr-dashboard", "/qr-dashboard/"}, produces = MediaType.TEXT_HTML_VALUE)
    public String qrDashboard() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/qr-dashboard.html");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}