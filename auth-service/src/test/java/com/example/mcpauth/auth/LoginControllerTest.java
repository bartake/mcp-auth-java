package com.example.mcpauth.auth;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.mcpauth.common.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LoginController.class)
class LoginControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private JwtService jwtService;

  @Test
  void loginValidReturnsToken() throws Exception {
    when(jwtService.createAccessToken(eq("alice"), anyList(), eq(3600L))).thenReturn("jwt-here");
    mockMvc
        .perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"pass123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").value("jwt-here"))
        .andExpect(jsonPath("$.token_type").value("Bearer"));
  }

  @Test
  void loginInvalidReturns401() throws Exception {
    mockMvc
        .perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void openidDiscovery() throws Exception {
    mockMvc
        .perform(get("/.well-known/openid-configuration"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.issuer").exists())
        .andExpect(jsonPath("$.token_endpoint").exists());
  }
}
