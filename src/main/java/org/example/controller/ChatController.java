package org.example.controller;

import org.example.entity.Message;
import org.example.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversation/{user1}/{user2}")
    public ResponseEntity<?> getConversationHistory(
            @PathVariable String user1,
            @PathVariable String user2) {
        try {
            List<Message> messages = chatService.getConversationHistory(user1, user2);

            List<Map<String, Object>> messageList = messages.stream()
                    .map(msg -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", msg.getId());
                        map.put("sender", msg.getSenderUsername());
                        map.put("recipient", msg.getRecipientUsername());
                        map.put("content", msg.getContent());
                        map.put("sentAt", msg.getSentAt().toString());
                        map.put("delivered", msg.isDelivered());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("messages", messageList));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch conversation history"));
        }
    }

    @GetMapping("/recent/{username}")
    public ResponseEntity<?> getRecentConversations(@PathVariable String username) {
        try {
            List<Message> messages = chatService.getRecentConversations(username);

            List<Map<String, Object>> conversationList = messages.stream()
                    .map(msg -> {
                        String contactUser = msg.getSenderUsername().equals(username)
                                ? msg.getRecipientUsername()
                                : msg.getSenderUsername();

                        Map<String, Object> map = new HashMap<>();
                        map.put("contactUser", contactUser);
                        map.put("lastMessage", msg.getContent());
                        map.put("lastMessageTime", msg.getSentAt().toString());
                        map.put("lastMessageSender", msg.getSenderUsername());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("conversations", conversationList));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch recent conversations"));
        }
    }
}