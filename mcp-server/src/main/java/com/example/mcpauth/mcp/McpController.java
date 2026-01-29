package com.example.mcpauth.mcp;

import com.example.mcpauth.common.AppPorts;
import com.example.mcpauth.common.UserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class McpController {

  private static final ConcurrentHashMap<String, Boolean> SESSIONS = new ConcurrentHashMap<>();

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public McpController(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @PostMapping(value = "/mcp", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> mcp(
      @RequestBody JsonNode body,
      @RequestHeader(value = "Mcp-Session-Id", required = false) String mcpSessionHeader,
      jakarta.servlet.http.HttpServletRequest servletRequest)
      throws JsonProcessingException {

    JsonNode idNode = body.get("id");
    String method = body.has("method") && !body.get("method").isNull() ? body.get("method").asText() : "";
    JsonNode params = body.has("params") ? body.get("params") : objectMapper.createObjectNode();

    String sessionId = mcpSessionHeader;
    if (sessionId == null || sessionId.isEmpty()) {
      sessionId = servletRequest.getHeader("mcp-session-id");
    }

    if ("notifications/initialized".equals(method)) {
      return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    String responseSessionId = sessionId;

    if ("initialize".equals(method)) {
      responseSessionId = UUID.randomUUID().toString();
      SESSIONS.put(responseSessionId, true);
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("protocolVersion", "2024-11-05");
      result.put("serverInfo", Map.of("name", "mcp-auth-example", "version", "1.0.0"));
      result.put("capabilities", Map.of("tools", Map.of()));
      return jsonRpcResponse(idNode, result, responseSessionId);
    }

    if ("tools/list".equals(method)) {
      return jsonRpcResponse(idNode, Map.of("tools", toolsList()), responseSessionId);
    }

    if ("tools/call".equals(method)) {
      String name = params.has("name") ? params.get("name").asText() : "";
      JsonNode args = params.has("arguments") ? params.get("arguments") : objectMapper.createObjectNode();
      try {
        ObjectNode toolResult = handleToolsCall(name, args);
        return jsonRpcResponse(idNode, toolResult, responseSessionId);
      } catch (Exception e) {
        return jsonRpcError(idNode, -32603, e.getMessage(), responseSessionId);
      }
    }

    return jsonRpcError(idNode, -32601, "Method not found: " + method, responseSessionId);
  }

  private List<Map<String, Object>> toolsList() {
    List<Map<String, Object>> tools = new ArrayList<>();
    tools.add(
        Map.of(
            "name",
            "get_user_info",
            "description",
            "Returns the current authenticated user (sub, roles) from the JWT context",
            "inputSchema",
            Map.of("type", "object", "properties", Map.of())));
    Map<String, Object> ep = new LinkedHashMap<>();
    ep.put("type", "string");
    ep.put("enum", List.of("public", "data"));
    ep.put("description", "API endpoint to call");
    tools.add(
        Map.of(
            "name",
            "fetch_downstream_data",
            "description",
            "Calls the downstream API with the user's JWT. Access depends on roles (reader/admin).",
            "inputSchema",
            Map.of(
                "type",
                "object",
                "properties",
                Map.of("endpoint", ep),
                "required",
                List.of("endpoint"))));
    return tools;
  }

  private ObjectNode handleToolsCall(String name, JsonNode args) throws JsonProcessingException {
    if ("get_user_info".equals(name)) {
      UserPrincipal user = UserContextHolder.get();
      String text =
          user != null
              ? objectMapper.writerWithDefaultPrettyPrinter()
                  .writeValueAsString(
                      Map.of("sub", user.sub(), "roles", user.roles(), "scope", user.scope()))
              : "No user context (unauthenticated)";
      return contentText(text);
    }
    if ("fetch_downstream_data".equals(name)) {
      UserPrincipal ctx = UserContextHolder.get();
      if (ctx == null) {
        return contentText("Error: No user context");
      }
      String endpoint = args.has("endpoint") ? args.get("endpoint").asText() : "data";
      String url = "http://127.0.0.1:" + AppPorts.DOWNSTREAM_API + "/" + endpoint;
      try {
        HttpHeaders headers = new HttpHeaders();
        if (ctx.rawToken() != null) {
          headers.setBearerAuth(ctx.rawToken());
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> res =
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        JsonNode data =
            objectMapper.readTree(
                res.getBody() != null && !res.getBody().isEmpty() ? res.getBody() : "{}");
        String text =
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(Map.of("status", res.getStatusCode().value(), "data", data));
        return contentText(text);
      } catch (Exception e) {
        return contentText("Error: " + e.getMessage());
      }
    }
    throw new IllegalArgumentException("Unknown tool: " + name);
  }

  private ObjectNode contentText(String text) {
    ObjectNode root = objectMapper.createObjectNode();
    var arr = objectMapper.createArrayNode();
    ObjectNode item = objectMapper.createObjectNode();
    item.put("type", "text");
    item.put("text", text);
    arr.add(item);
    root.set("content", arr);
    return root;
  }

  private ResponseEntity<String> jsonRpcResponse(JsonNode idNode, Object result, String sessionId)
      throws JsonProcessingException {
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("jsonrpc", "2.0");
    envelope.put("id", parseId(idNode));
    envelope.put("result", result);
    return buildJsonResponse(envelope, sessionId, HttpStatus.OK);
  }

  private ResponseEntity<String> jsonRpcError(
      JsonNode idNode, int code, String message, String sessionId) throws JsonProcessingException {
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("jsonrpc", "2.0");
    envelope.put("id", parseId(idNode));
    envelope.put("error", Map.of("code", code, "message", message));
    return buildJsonResponse(envelope, sessionId, HttpStatus.OK);
  }

  private ResponseEntity<String> buildJsonResponse(
      Map<String, Object> envelope, String sessionId, HttpStatus status) throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(envelope);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (sessionId != null && !sessionId.isEmpty()) {
      headers.set("Mcp-Session-Id", sessionId);
    }
    return new ResponseEntity<>(json, headers, status);
  }

  private static Object parseId(JsonNode idNode) {
    if (idNode == null || idNode.isNull()) {
      return null;
    }
    if (idNode.isIntegralNumber()) {
      return idNode.longValue();
    }
    if (idNode.isTextual()) {
      return idNode.asText();
    }
    return idNode.toString();
  }
}
