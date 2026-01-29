package com.example.mcpauth.mcp;

import com.example.mcpauth.common.JwtService;
import com.example.mcpauth.common.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(0)
public class McpJwtFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public McpJwtFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !"/mcp".equals(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String auth = request.getHeader("Authorization");
    if (auth == null || !auth.startsWith("Bearer ")) {
      writeJsonRpcError(response, "Authorization required. Use Authorization: Bearer <token>");
      return;
    }
    String token = auth.substring(7);
    try {
      Jws<io.jsonwebtoken.Claims> jws = jwtService.parseAndVerify(token);
      Claims claims = jws.getPayload();
      String sub = claims.getSubject();
      List<String> roles = JwtService.rolesFromClaims(claims);
      String scope = claims.get("scope", String.class);
      if (scope == null) {
        scope = "";
      }
      UserContextHolder.set(new UserPrincipal(sub, roles, scope, token));
      try {
        filterChain.doFilter(request, response);
      } finally {
        UserContextHolder.clear();
      }
    } catch (Exception e) {
      writeJsonRpcError(response, "Invalid or expired token");
    }
  }

  private void writeJsonRpcError(HttpServletResponse response, String message) throws IOException {
    response.setStatus(401);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    Map<String, Object> err = new LinkedHashMap<>();
    err.put("jsonrpc", "2.0");
    err.put("id", null);
    err.put("error", Map.of("code", -32001, "message", message));
    response.getWriter().write(objectMapper.writeValueAsString(err));
  }
}
