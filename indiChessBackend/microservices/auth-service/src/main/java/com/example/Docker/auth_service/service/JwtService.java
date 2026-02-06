package com.example.Docker.auth_service.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;



@Service
public class JwtService {
    private final String SECRET = "aluesgo8q37g4tifqbhrefg8g3124ib801g7br18b7gb17g4b";

    public String generateToken(String username, Long userId) {
        return Jwts.builder()
                .setSubject(username)  // Set the username as the subject of the token
                .claim("userId", userId) // Add the userId as a custom claim
                .setIssuedAt(new Date(System.currentTimeMillis())) // Set the issued date
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 5)) // Set the expiration time (5 hours)
                .signWith(getKey(), SignatureAlgorithm.HS256) // Sign the JWT with the secret key
                .compact(); // Return the compact JWT token
    }

    Key getKey(){
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }
    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }
    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

}
