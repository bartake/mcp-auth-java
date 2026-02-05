package com.example.mcpauth.downstream;

import com.example.mcpauth.common.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class DataController {

  private static final Map<String, Object> DATA_PUBLIC =
      Map.of("message", "Public data - no auth needed");
  private static final Map<String, Object> DATA_READER =
      Map.of("docs", List.of("Doc A", "Doc B", "Doc C"), "message", "Reader-only data");
  private static final Map<String, Object> DATA_ADMIN =
      Map.of("users", List.of("alice", "bob"), "config", Map.of("feature", true), "message", "Admin-only data");

  @GetMapping("/public")
  public Map<String, Object> publicData() {
    return DATA_PUBLIC;
  }

  @GetMapping("/data")
  public ResponseEntity<?> getData(@RequestAttribute(DownstreamJwtFilter.ATTR_CLAIMS) Claims claims) {
    List<String> roles = JwtService.rolesFromClaims(claims);
    boolean canRead = roles.contains("reader") || roles.contains("admin");
    boolean canAdmin = roles.contains("admin");
    if (!canRead) {
      return ResponseEntity.status(403).body(Map.of("error", "Requires reader or admin role"));
    }
    Map<String, Object> payload = new LinkedHashMap<>(DATA_READER);
    if (canAdmin) {
      payload.putAll(DATA_ADMIN);
    }
    return ResponseEntity.ok(payload);
  }

  @PostMapping("/data")
  public ResponseEntity<?> postData(@RequestAttribute(DownstreamJwtFilter.ATTR_CLAIMS) Claims claims) {
    List<String> roles = JwtService.rolesFromClaims(claims);
    if (!roles.contains("admin")) {
      return ResponseEntity.status(403).body(Map.of("error", "Requires admin role"));
    }
    return ResponseEntity.ok(Map.of("success", true, "message", "Data updated (admin)"));
  }
}
