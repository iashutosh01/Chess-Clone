package com.example.Docker.api_gateway_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("x-header-username", "x-header-userid", "x-header-email", "x-header-auth-source")
                .allowCredentials(true)
                .maxAge(3600);
    }

    // Add a separate CORS filter for handling preflight requests
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter corsWebFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            // Get the origin from the request
            String origin = exchange.getRequest().getHeaders().getOrigin();
            if (origin == null) {
                origin = "http://localhost:3000";
            }

            // Add CORS headers to ALL responses
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", origin);
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Headers",
                    "Content-Type, Authorization, X-Requested-With, Accept, Origin, " +
                            "Access-Control-Request-Method, Access-Control-Request-Headers, " +
                            "x-header-username, x-header-userid, x-header-email, x-header-auth-source");
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Credentials", "true");
            exchange.getResponse().getHeaders().add("Access-Control-Max-Age", "3600");
            exchange.getResponse().getHeaders().add("Access-Control-Expose-Headers",
                    "x-header-username, x-header-userid, x-header-email, x-header-auth-source");

            // Handle OPTIONS preflight requests
            if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                System.out.println("CORS WebFilter handling OPTIONS preflight for: " +
                        exchange.getRequest().getURI().getPath());
                exchange.getResponse().setStatusCode(HttpStatus.OK);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }

    // Optional: Add a logging filter to debug CORS issues
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public WebFilter corsDebugFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String method = exchange.getRequest().getMethod().name();
            String path = exchange.getRequest().getURI().getPath();

            if ("OPTIONS".equals(method)) {
                System.out.println("=== CORS DEBUG ===");
                System.out.println("OPTIONS request to: " + path);
                System.out.println("Origin: " + exchange.getRequest().getHeaders().getOrigin());
                System.out.println("Access-Control-Request-Method: " +
                        exchange.getRequest().getHeaders().getFirst("Access-Control-Request-Method"));
                System.out.println("Access-Control-Request-Headers: " +
                        exchange.getRequest().getHeaders().getFirst("Access-Control-Request-Headers"));
                System.out.println("==================");
            }

            return chain.filter(exchange);
        };
    }
}