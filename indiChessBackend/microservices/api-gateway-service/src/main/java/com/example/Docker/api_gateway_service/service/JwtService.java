package com.example.Docker.api_gateway_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret.key}")
    private String SECRET_KEY; // Secret key for signing the JWT

    @Value("${jwt.expiration.time}")
    private long EXPIRATION_TIME; // Expiration time in milliseconds

    // Generate JWT Token
    public String generateToken(String username, Long userId) {
        return Jwts.builder()
                .setSubject(username)  // Subject = Username
                .claim("userId", userId) // Add custom claim (userId)
                .setIssuedAt(new Date()) // Set issued date
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Set expiration time
                .signWith(SignatureAlgorithm.HS256, getKey()) // Sign the JWT with the secret key
                .compact(); // Return the compact JWT token
    }

    Key getKey(){
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // Extract Username from JWT Token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract UserId from JWT Token (custom claim)
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    // Extract specific claim from the JWT Token
    public <T> T extractClaim(String token, ClaimsResolver<T> claimsResolver) {
        final Claims claims = extractAllClaims(token); // Get all claims
        return claimsResolver.resolve(claims); // Extract specific claim
    }

    // Extract all claims from the JWT Token
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey()) // Use the secret key for signing validation
                .build()
                .parseClaimsJws(token)
                .getBody(); // Return claims body
    }

    // Validate if the JWT Token is expired
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Extract expiration date from the JWT Token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Validate the JWT Token
    public boolean isTokenValid(String token) {
        return !isTokenExpired(token); // Check if the token is expired
    }

    // Interface to extract claims
    @FunctionalInterface
    public interface ClaimsResolver<T> {
        T resolve(Claims claims);
    }
}

