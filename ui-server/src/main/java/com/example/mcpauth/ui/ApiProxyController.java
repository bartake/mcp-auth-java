package com.example.mcpauth.ui;

import com.example.mcpauth.common.AppPorts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
public class ApiProxyController {

  private static final String AUTH_URL = "http://127.0.0.1:" + AppPorts.AUTH;
  private static final String MCP_URL = "http://127.0.0.1:" + AppPorts.MCP;
  private static final String API_URL = "http://127.0.0.1:" + AppPorts.DOWNSTREAM_API;

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public ApiProxyController(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @PostMapping("/api/login")
  public ResponseEntity<JsonNode> login(@RequestBody Map<String, String> body) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
      ResponseEntity<String> r =
          restTemplate.exchange(AUTH_URL + "/login", HttpMethod.POST, entity, String.class);
      JsonNode data = objectMapper.readTree(r.getBody() != null ? r.getBody() : "{}");
      return ResponseEntity.status(r.getStatusCode()).body(data);
    } catch (Exception e) {
      ObjectNode err = objectMapper.createObjectNode();
      err.put("error", e.getMessage());
      return ResponseEntity.status(500).body(err);
    }
  }

  @PostMapping("/api/mcp")
  public ResponseEntity<JsonNode> mcpProxy(@RequestBody Map<String, Object> req) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> bodyMap = (Map<String, Object>) req.getOrDefault("body", Map.of());
      String authorization = (String) req.get("authorization");
      String sessionId = (String) req.get("sessionId");

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      if (authorization != null) {
        headers.set("Authorization", authorization);
      }
      if (sessionId != null && !sessionId.isEmpty()) {
        headers.set("Mcp-Session-Id", sessionId);
      }

      String bodyJson = objectMapper.writeValueAsString(bodyMap);
      HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
      ResponseEntity<String> r =
          restTemplate.exchange(MCP_URL + "/mcp", HttpMethod.POST, entity, String.class);

      JsonNode data = objectMapper.readTree(r.getBody() != null ? r.getBody() : "{}");
      ObjectNode merged = (ObjectNode) data;
      String sid =
          r.getHeaders().getFirst("mcp-session-id");
      if (sid == null) {
        sid = r.getHeaders().getFirst("Mcp-Session-Id");
      }
      if (sid != null) {
        merged.put("_sessionId", sid);
      }
      return ResponseEntity.status(r.getStatusCode()).body(merged);
    } catch (Exception e) {
      ObjectNode err = objectMapper.createObjectNode();
      err.put("error", e.getMessage());
      return ResponseEntity.status(500).body(err);
    }
  }

  @GetMapping("/api/data")
  public ResponseEntity<JsonNode> dataProxy(@RequestHeader(value = "Authorization", required = false) String auth) {
    try {
      HttpHeaders headers = new HttpHeaders();
      if (auth != null) {
        headers.set("Authorization", auth);
      }
      HttpEntity<Void> entity = new HttpEntity<>(headers);
      ResponseEntity<String> r =
          restTemplate.exchange(API_URL + "/data", HttpMethod.GET, entity, String.class);
      JsonNode data = objectMapper.readTree(r.getBody() != null ? r.getBody() : "{}");
      return ResponseEntity.status(r.getStatusCode()).body(data);
    } catch (Exception e) {
      ObjectNode err = objectMapper.createObjectNode();
      err.put("error", e.getMessage());
      return ResponseEntity.status(500).body(err);
    }
  }
}
