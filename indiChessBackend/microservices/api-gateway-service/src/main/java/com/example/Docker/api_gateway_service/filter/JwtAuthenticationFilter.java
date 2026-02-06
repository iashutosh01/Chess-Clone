package com.example.Docker.api_gateway_service.filter;

import com.example.Docker.api_gateway_service.service.JwtService;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        System.out.println("=== JwtAuthenticationFilter ===");
        System.out.println("Path: " + path);
        System.out.println("Method: " + method);
        System.out.println("Request URI: " + exchange.getRequest().getURI());

        // Skip JWT validation for OPTIONS and public endpoints
        if (shouldSkipJwtValidation(path, method)) {
            System.out.println("Skipping JWT validation for: " + path);
            return chain.filter(exchange);
        }

        // Extract JWT from cookies
        String token = extractTokenFromCookies(exchange);

        if (token == null) {
            System.out.println("No JWT token found in cookies");
            // Try to extract from Authorization header as fallback
            token = extractTokenFromHeader(exchange);
        }

        // Validate token
        if (token == null || !jwtService.isTokenValid(token)) {
            System.out.println("Invalid or missing JWT token");
            return unauthorizedResponse(exchange, "Missing or invalid authentication token");
        }

        // Extract user info and add to headers
        String username = jwtService.extractUsername(token);
        Long userId = jwtService.extractUserId(token);

        System.out.println("JWT validated for user: " + username + ", userId: " + userId);

        // Add headers to the request
        exchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("x-header-username", username)
                        .header("x-header-userid", String.valueOf(userId))
                        .header("x-header-auth-source", "jwt")
                        .build())
                .build();

        return chain.filter(exchange);
    }

    private boolean shouldSkipJwtValidation(String path, String method) {
        // Skip for OPTIONS preflight requests
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // Skip for public endpoints
        List<String> publicPaths = Arrays.asList(
                "/api/v1/auth/login",
                "/api/v1/auth/signup",
                "/api/v1/auth/oauth2",
                "/oauth2",
                "/actuator/health",
                "/gateway-test"
        );

        return publicPaths.stream().anyMatch(path::startsWith);
    }

    private String extractTokenFromCookies(ServerWebExchange exchange) {
        // Get cookies from the request
        MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();

        // Get the JWT cookie
        List<HttpCookie> jwtCookies = cookies.get("JWT");

        if (jwtCookies != null && !jwtCookies.isEmpty()) {
            return jwtCookies.get(0).getValue();
        }

        return null;
    }
    private String extractTokenFromHeader(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String body = "{\"error\": \"" + message + "\", \"status\": 401}";
        byte[] bytes = body.getBytes();

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }
}