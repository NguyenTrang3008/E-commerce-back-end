package fs.fresher.SystemE_commerce.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Commerce API Documentation")
                        .version("1.0.0")
                        .description("""
                                API documentation for E-Commerce System
                                
                                ## Features:
                                - Product Catalog with filtering and pagination
                                - Shopping Cart management
                                - Checkout and Order processing
                                - Order tracking
                                - Admin order management
                                - Inventory reservation system
                                
                                ## Authentication:
                                - Admin endpoints require X-API-Key header
                                - Guest cart uses cartId cookie or query parameter
                                """)
                        .contact(new Contact()
                                .name("E-Commerce Support")
                                .email("support@ecommerce.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url(baseUrl)
                                .description("Server URL")
                ))
                .addSecurityItem(new SecurityRequirement().addList("API Key"))
                .schemaRequirement("API Key", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-API-Key")
                        .description("API Key for admin endpoints"));
    }
}
