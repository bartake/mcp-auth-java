package com.example.mcpauth.agent;

import com.example.mcpauth.common.AppPorts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * CLI MCP client — same flow as Node {@code agent.js}: login → JWT → MCP tools → downstream.
 */
public final class AgentMain {

  private static final String AUTH_URL = "http://127.0.0.1:" + AppPorts.AUTH;
  private static final String MCP_ENDPOINT = "http://127.0.0.1:" + AppPorts.MCP + "/mcp";

  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static void main(String[] args) throws Exception {
    String username = args.length > 0 ? args[0] : "alice";
    String password = args.length > 1 ? args[1] : "pass123";

    System.out.println("=== MCP Auth Demo (Java) ===\n");
    System.out.println("1. User login");
    String token = login(username, password);
    System.out.println("   Logged in as " + username + ", got JWT\n");

    System.out.println("2. Agent passes JWT to MCP\n");
    System.out.println("3. MCP verifies JWT + extracts roles");
    System.out.println("4. Context attached to tool calls\n");

    try {
      McpResult init = mcpRequest(token, "initialize", buildInitParams(), null);
      String sid = init.sessionId().orElse("");

      McpResult tools = mcpRequest(token, "tools/list", MAPPER.createObjectNode(), sid);
      System.out.print("   Available tools: ");
      printToolNames(tools.result());

      System.out.println("\n   Calling get_user_info:");
      McpResult u =
          mcpRequest(
              token,
              "tools/call",
              MAPPER.readTree(
                  "{\"name\":\"get_user_info\",\"arguments\":{}}"),
              sid);
      printContent(u.result());

      System.out.println("\n   Calling fetch_downstream_data:");
      McpResult d =
          mcpRequest(
              token,
              "tools/call",
              MAPPER.readTree(
                  "{\"name\":\"fetch_downstream_data\",\"arguments\":{\"endpoint\":\"data\"}}"),
              sid);
      printContent(d.result());

      System.out.println("\n5. Downstream APIs used scoped tokens ✓");
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
      if (e.getMessage() != null
          && (e.getMessage().contains("401") || e.getMessage().contains("Authorization"))) {
        System.out.println("\nMake sure MCP server is running (mcp-server module).");
      }
      System.exit(1);
    }
  }

  private static JsonNode buildInitParams() throws Exception {
    return MAPPER.readTree(
        "{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},\"clientInfo\":{\"name\":\"demo-agent\",\"version\":\"1.0.0\"}}");
  }

  private static String login(String username, String password) throws Exception {
    ObjectNode body = MAPPER.createObjectNode();
    body.put("username", username);
    body.put("password", password);
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(AUTH_URL + "/login"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
            .build();
    HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
    if (res.statusCode() != 200) {
      throw new RuntimeException("Login failed: " + res.statusCode() + " " + res.body());
    }
    JsonNode data = MAPPER.readTree(res.body());
    return data.get("access_token").asText();
  }

  private record McpResult(JsonNode result, Optional<String> sessionId) {}

  private static McpResult mcpRequest(String token, String method, JsonNode params, String sessionId)
      throws Exception {
    ObjectNode envelope = MAPPER.createObjectNode();
    envelope.put("jsonrpc", "2.0");
    envelope.put("id", 1);
    envelope.put("method", method);
    envelope.set("params", params);

    HttpRequest.Builder b =
        HttpRequest.newBuilder()
            .uri(URI.create(MCP_ENDPOINT))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + token);
    if (sessionId != null && !sessionId.isEmpty()) {
      b.header("Mcp-Session-Id", sessionId);
    }
    HttpRequest req = b.POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(envelope))).build();
    HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
    if (res.statusCode() != 200) {
      throw new RuntimeException("MCP request failed: " + res.statusCode() + " " + res.body());
    }
    JsonNode data = MAPPER.readTree(res.body());
    if (data.has("error")) {
      throw new RuntimeException(data.get("error").path("message").asText("MCP error"));
    }
    Optional<String> sid =
        res.headers().firstValue("mcp-session-id").or(() -> res.headers().firstValue("Mcp-Session-Id"));
    return new McpResult(data.get("result"), sid);
  }

  private static void printToolNames(JsonNode result) {
    if (result != null && result.has("tools")) {
      var names = new StringBuilder();
      for (JsonNode t : result.get("tools")) {
        if (names.length() > 0) {
          names.append(", ");
        }
        names.append(t.get("name").asText());
      }
      System.out.println(names);
    } else {
      System.out.println("none");
    }
  }

  private static void printContent(JsonNode result) {
    if (result != null && result.has("content") && result.get("content").isArray()) {
      JsonNode first = result.get("content").get(0);
      if (first != null && first.has("text")) {
        String t = first.get("text").asText();
        for (String line : t.split("\n")) {
          System.out.println("    " + line);
        }
        return;
      }
    }
    System.out.println("    " + (result != null ? result.toString() : "null"));
  }
}
