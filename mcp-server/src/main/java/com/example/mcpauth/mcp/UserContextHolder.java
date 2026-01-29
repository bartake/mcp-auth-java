package com.example.mcpauth.mcp;

import com.example.mcpauth.common.UserPrincipal;

/** Request-scoped user (ThreadLocal) — same idea as Node AsyncLocalStorage. */
public final class UserContextHolder {

  private static final ThreadLocal<UserPrincipal> CTX = new ThreadLocal<>();

  public static void set(UserPrincipal user) {
    CTX.set(user);
  }

  public static UserPrincipal get() {
    return CTX.get();
  }

  public static void clear() {
    CTX.remove();
  }

  private UserContextHolder() {}
}
