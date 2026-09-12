package com.example.spring_boot_project_api.config;

import java.util.ArrayList;
import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    private static final String PAGEABLE_ALLOWED_SORT =
            "Allowed properties: id, name, createdAt, updatedAt.";

    @Bean
    public OpenApiCustomizer pageableQueryParamCustomizer() {
        return this::customizePageable;
    }

    private void customizePageable(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        for (PathItem pathItem : openApi.getPaths().values()) {
            for (Operation operation : pathItem.readOperations()) {
                List<Parameter> parameters = operation.getParameters();
                if (parameters == null || parameters.isEmpty()) {
                    continue;
                }
                boolean hasPageable = false;
                for (Parameter parameter : parameters) {
                    if (isPageableParameter(parameter)) {
                        hasPageable = true;
                        break;
                    }
                }
                if (!hasPageable) {
                    continue;
                }
                List<Parameter> result = new ArrayList<>();
                for (Parameter parameter : parameters) {
                    if (isPageableParameter(parameter)) {
                        result.add(pageParameter());
                        result.add(sizeParameter());
                        result.add(sortParameter());
                    } else {
                        result.add(parameter);
                    }
                }
                operation.setParameters(result);
            }
        }
    }

    private boolean isPageableParameter(Parameter parameter) {
        if (parameter == null) {
            return false;
        }
        if ("pageable".equals(parameter.getName())) {
            return true;
        }
        if (parameter.get$ref() != null && parameter.get$ref().contains("Pageable")) {
            return true;
        }
        Schema<?> schema = parameter.getSchema();
        if (schema != null && schema.get$ref() != null && schema.get$ref().contains("Pageable")) {
            return true;
        }
        return false;
    }

    private QueryParameter pageParameter() {
        QueryParameter parameter = new QueryParameter();
        parameter.setName("page");
        parameter.setDescription("Zero-based page index (0..N).");
        parameter.setRequired(false);
        IntegerSchema schema = new IntegerSchema();
        schema.setDefault(0);
        parameter.setSchema(schema);
        return parameter;
    }

    private QueryParameter sizeParameter() {
        QueryParameter parameter = new QueryParameter();
        parameter.setName("size");
        parameter.setDescription("Number of records per page.");
        parameter.setRequired(false);
        IntegerSchema schema = new IntegerSchema();
        schema.setDefault(10);
        parameter.setSchema(schema);
        return parameter;
    }

    private QueryParameter sortParameter() {
        QueryParameter parameter = new QueryParameter();
        parameter.setName("sort");
        parameter.setDescription("Sorting criteria in the format: property,(asc|desc). "
                + "Multiple criteria are supported. " + PAGEABLE_ALLOWED_SORT);
        parameter.setRequired(false);
        StringSchema schema = new StringSchema();
        schema.setDefault("id,asc");
        parameter.setSchema(schema);
        return parameter;
    }

    @Bean
    public OpenAPI openAPI() {
        String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
