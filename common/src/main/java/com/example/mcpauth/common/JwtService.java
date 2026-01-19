package com.example.mcpauth.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public final class JwtService {

  private final SecretKey key;
  private final String issuer;
  private final String audience;

  public JwtService(String secret, String issuer, String audience) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.issuer = issuer;
    this.audience = audience;
  }

  public String buildScope(List<String> roles) {
    if (roles.contains("admin")) {
      return "data:read data:write admin";
    }
    if (roles.contains("reader")) {
      return "data:read";
    }
    return "";
  }

  public String createAccessToken(String username, List<String> roles, long ttlSeconds) {
    String scope = buildScope(roles);
    return Jwts.builder()
        .subject(username)
        .issuer(issuer)
        .audience().add(audience).and()
        .claim("roles", roles)
        .claim("scope", scope)
        .expiration(Date.from(Instant.now().plusSeconds(ttlSeconds)))
        .signWith(key)
        .compact();
  }

  public Jws<Claims> parseAndVerify(String token) throws JwtException {
    return Jwts.parser()
        .verifyWith(key)
        .requireIssuer(issuer)
        .requireAudience(audience)
        .build()
        .parseSignedClaims(token);
  }

  @SuppressWarnings("unchecked")
  public static List<String> rolesFromClaims(Claims claims) {
    Object r = claims.get("roles");
    if (r instanceof List<?> list) {
      return list.stream().map(String::valueOf).toList();
    }
    return List.of();
  }
}
