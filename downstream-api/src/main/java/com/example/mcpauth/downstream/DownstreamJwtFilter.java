package com.example.mcpauth.downstream;

import com.example.mcpauth.common.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(0)
public class DownstreamJwtFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public static final String ATTR_CLAIMS = "jwt.claims";

  public DownstreamJwtFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !"/data".equals(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String auth = request.getHeader("Authorization");
    if (auth == null || !auth.startsWith("Bearer ")) {
      response.setStatus(401);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
      return;
    }
    String token = auth.substring(7);
    try {
      var jws = jwtService.parseAndVerify(token);
      Claims claims = jws.getPayload();
      request.setAttribute(ATTR_CLAIMS, claims);
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      response.setStatus(401);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
    }
  }
}
