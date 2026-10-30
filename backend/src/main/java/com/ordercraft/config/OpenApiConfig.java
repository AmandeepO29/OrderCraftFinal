package com.ordercraft.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "BearerAuth";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT Bearer token to authorize")))
                .info(new Info()
                        .title("OrderCraft - Manufacturing Order Management System (OMS) REST API")
                        .version("1.0.0")
                        .description("Enterprise-grade RESTful APIs powering OrderCraft manufacturing processes including BOM, Production, Inventory, Orders, Invoicing, and Business Intelligence.")
                        .contact(new Contact().name("OrderCraft Engineering").email("dev@ordercraft.internal"))
                        .license(new License().name("Apache 2.0").url("https://spring.io")));
    }
}
