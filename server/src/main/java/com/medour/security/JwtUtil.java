package com.medour.security;

import com.medour.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  public String generateToken(User user) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + 3_600_000L);
    return Jwts.builder()
        .subject(String.valueOf(user.getId()))
        .claim("email", user.getEmail())
        .claim("role", user.getRole().name())
        .issuedAt(now)
        .expiration(expiry)
        .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
        .compact();
  }
}
