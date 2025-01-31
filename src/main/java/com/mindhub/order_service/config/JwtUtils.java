package com.mindhub.order_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// Injection of dependencies with Autowired and with empty constructor: inside the context of Spring with @Component
@Component
public class JwtUtils {
    // secretKey
    private final SecretKey secretKey;
    // expiration time jwt: information provide in the application properties @Value
    @Value("${jwt.expiration}")
    private long expiration;
    // setting the secretKey
    public JwtUtils(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }


    public String generateToken(String username, Long id, String role) {
        Map<String, String> claims = generateClaims(id,role);
        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
    }

    public Map<String, String> generateClaims(Long id, String role){
        Map<String, String> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("id", id.toString());
        return claims;
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public Long extractId(String token) {
        String id = parseClaims(token).get("id", String.class);
        return Long.parseLong(id);
    }

    public boolean validateToken(String token, String username) {
        final String tokenUsername = extractUsername(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    } //parsClaims(token).getExpiration: return the expiration date

    public String tokenParser(String authorization){
        return authorization.substring(7);
    }

    public String getEmailFromToken(String authorization){
        String token = authorization.substring(7);
        return extractUsername(token);
    }

    public Long getIdFromToken(String authorization){
        String token = authorization.substring(7);
        return extractId(token);
    }
}