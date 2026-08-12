package com.medour.security;

import com.medour.model.Role;
import com.medour.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class JwtUtilTest {

  private JwtUtil jwtUtil;

  // minimum 32 bytes for HS256
  private static final String TEST_SECRET = "test-secret-that-is-at-least-32-bytes-long!";

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
  }

  @Test
  void generateToken_containsCorrectClaims() {
    User user = User.builder()
        .id(42L)
        .email("user@test.com")
        .role(Role.PATIENT)
        .mustChangePassword(false)
        .build();

    String token = jwtUtil.generateToken(user);

    Claims claims = Jwts.parser()
        .verifyWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
        .build()
        .parseSignedClaims(token)
        .getPayload();

    assertThat(claims.getSubject()).isEqualTo("42");
    assertThat(claims.get("email", String.class)).isEqualTo("user@test.com");
    assertThat(claims.get("role", String.class)).isEqualTo("PATIENT");
    // expiry should be ~1 hour from now
    assertThat(claims.getExpiration()).isAfter(new Date());
    assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
        .isCloseTo(3_600_000L, within(5_000L));
  }
}
