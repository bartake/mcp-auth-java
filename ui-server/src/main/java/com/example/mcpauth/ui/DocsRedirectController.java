package com.example.mcpauth.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocsRedirectController {

  @GetMapping("/docs")
  public String docs() {
    return "redirect:/docs.html";
  }
}
