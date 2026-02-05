package com.example.mcpauth.downstream;

import com.example.mcpauth.common.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DownstreamApplication {

  public static void main(String[] args) {
    SpringApplication.run(DownstreamApplication.class, args);
  }

  @Bean
  JwtService jwtService(
      @Value("${mcp-auth.jwt.secret}") String secret,
      @Value("${mcp-auth.jwt.issuer}") String issuer,
      @Value("${mcp-auth.jwt.audience}") String audience) {
    return new JwtService(secret, issuer, audience);
  }
}
