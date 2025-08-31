package org.games.matchmakingservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/home")
    public String home() {
        return "demo.html";
    }

    @GetMapping("/login")
    public String login() {
        return "login.html";
    }
    
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }
    
    @GetMapping("/auth-success")
    public String authSuccess() {
        return "auth-success.html";
    }
}
