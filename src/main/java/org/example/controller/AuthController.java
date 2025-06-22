package org.example.controller;

import org.example.entity.User;
import org.example.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final ChatService chatService;
    
    public AuthController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
            }
            
            User user = chatService.registerUser(username.trim(), password);
            return ResponseEntity.ok(Map.of("message", "User registered successfully", "username", user.getUsername()));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Registration failed"));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
            }
            
            Optional<User> userOpt = chatService.authenticateUser(username.trim(), password);

            if (userOpt.isPresent()) {
                return ResponseEntity.ok(Map.of("message", "Login successful", "username", userOpt.get().getUsername()));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid username or password"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Login failed"));
        }
    }
    
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            var users = chatService.getAllUsers().stream()
                    .map(User::getUsername)
                    .toList();
            return ResponseEntity.ok(Map.of("users", users));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch users"));
        }
    }
}