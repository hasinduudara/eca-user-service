package com.eca.shop.user_service.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@PropertySource("classpath:env.properties")
public class JwtService {

    // Read the secret key from the env.properties file
    @Value("${JWT_SECRET}")
    private String secretKey;

    // Access token validity (e.g., 15 minutes)
    private static final long ACCESS_TOKEN_VALIDITY = 1000 * 60 * 15;

    // Refresh token validity (e.g., 7 days)
    private static final long REFRESH_TOKEN_VALIDITY = 1000 * 60 * 60 * 24 * 7;

    /**
     * Generates an Access Token for the given email
     */
    public String generateAccessToken(String email) {
        return createToken(new HashMap<>(), email, ACCESS_TOKEN_VALIDITY);
    }

    /**
     * Generates a Refresh Token for the given email
     */
    public String generateRefreshToken(String email) {
        return createToken(new HashMap<>(), email, REFRESH_TOKEN_VALIDITY);
    }

    /**
     * Internal method to build the JWT token with claims, subject, and expiration
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Decodes the secret key and returns a cryptographic Key object
     */
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}