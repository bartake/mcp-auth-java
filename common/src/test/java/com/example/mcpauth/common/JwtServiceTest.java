package com.example.mcpauth.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET = "mcp-auth-demo-secret-change-in-production";
  private static final String ISSUER = "mcp-auth-example";
  private static final String AUDIENCE = "mcp-server";

  private final JwtService jwt = new JwtService(SECRET, ISSUER, AUDIENCE);

  @Test
  void buildScopeForAdminIncludesWriteAndAdmin() {
    assertEquals("data:read data:write admin", jwt.buildScope(List.of("admin", "reader")));
  }

  @Test
  void buildScopeForReaderIsReadOnly() {
    assertEquals("data:read", jwt.buildScope(List.of("reader")));
  }

  @Test
  void buildScopeEmptyWhenNoMatchingRoles() {
    assertEquals("", jwt.buildScope(List.of()));
  }

  @Test
  void createAndParseRoundTrip() {
    String token = jwt.createAccessToken("alice", List.of("reader"), 120);
    var jws = jwt.parseAndVerify(token);
    Claims c = jws.getPayload();
    assertEquals("alice", c.getSubject());
    assertEquals(ISSUER, c.getIssuer());
    assertEquals(List.of("reader"), JwtService.rolesFromClaims(c));
  }

  @Test
  void parseFailsWhenIssuerWrong() {
    var other = new JwtService(SECRET, "other-issuer", AUDIENCE);
    String token = other.createAccessToken("u", List.of(), 60);
    assertThrows(JwtException.class, () -> jwt.parseAndVerify(token));
  }
}
