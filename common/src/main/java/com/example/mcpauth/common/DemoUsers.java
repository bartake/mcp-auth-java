package com.example.mcpauth.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DemoUsers {

  public record UserRecord(String password, List<String> roles) {}

  private static final Map<String, UserRecord> USERS = Map.of(
      "alice", new UserRecord("pass123", List.of("admin", "reader")),
      "bob", new UserRecord("pass123", List.of("reader")),
      "charlie", new UserRecord("pass123", List.of()));

  public static Optional<UserRecord> find(String username) {
    return Optional.ofNullable(USERS.get(username));
  }

  public static boolean validate(String username, String password) {
    return find(username).filter(u -> u.password().equals(password)).isPresent();
  }

  public static List<String> roles(String username) {
    return find(username).map(UserRecord::roles).orElse(Collections.emptyList());
  }

  private DemoUsers() {}
}
