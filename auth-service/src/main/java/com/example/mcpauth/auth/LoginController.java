package com.example.mcpauth.auth;

import com.example.mcpauth.common.AppPorts;
import com.example.mcpauth.common.DemoUsers;
import com.example.mcpauth.common.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class LoginController {

  private final JwtService jwtService;

  public LoginController(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
    String username = body.getOrDefault("username", "");
    String password = body.getOrDefault("password", "");
    if (!DemoUsers.validate(username, password)) {
      return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }
    List<String> roles = DemoUsers.roles(username);
    String token = jwtService.createAccessToken(username, roles, 3600);
    Map<String, Object> ok = new LinkedHashMap<>();
    ok.put("access_token", token);
    ok.put("token_type", "Bearer");
    ok.put("expires_in", 3600);
    return ResponseEntity.ok(ok);
  }

  @GetMapping("/.well-known/openid-configuration")
  public Map<String, Object> openidConfig() {
    int p = AppPorts.AUTH;
    return Map.of(
        "issuer", "http://127.0.0.1:" + p,
        "authorization_endpoint", "http://127.0.0.1:" + p + "/authorize",
        "token_endpoint", "http://127.0.0.1:" + p + "/token");
  }
}
