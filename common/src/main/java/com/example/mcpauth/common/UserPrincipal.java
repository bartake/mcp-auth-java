package com.example.mcpauth.common;

import java.util.List;

public record UserPrincipal(String sub, List<String> roles, String scope, String rawToken) {

  public UserPrincipal withoutToken() {
    return new UserPrincipal(sub, roles, scope, null);
  }
}
