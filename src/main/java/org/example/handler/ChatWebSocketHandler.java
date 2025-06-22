package org.example.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Message;
import org.example.service.ChatService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    // Map to store active WebSocket sessions by username
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket connection established: " + session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = (String) message.getPayload();
        Map<String, Object> messageData = objectMapper.readValue(payload, new TypeReference<>() {});

        String type = (String) messageData.get("type");

        switch (type) {
            case "authenticate" -> handleAuthentication(session, messageData);
            case "chat" -> handleChatMessage(session, messageData);
            case "get_users" -> sendOnlineUsers(session);
        }
    }

    private void handleAuthentication(WebSocketSession session, Map<String, Object> messageData)
            throws IOException {
        String username = (String) messageData.get("username");

        if (username != null && !username.trim().isEmpty()) {
            // Store username in session attributes
            session.getAttributes().put("username", username);

            // Add to active sessions
            activeSessions.put(username, session);

            // Update last login time
            chatService.updateLastLogin(username);

            // Send authentication success
            Map<String, Object> response = Map.of(
                    "type", "auth_success",
                    "username", username
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));

            // Deliver any undelivered messages
            deliverUndeliveredMessages(username, session);

            // Broadcast updated online users list
            broadcastOnlineUsers();

            System.out.println("User authenticated: " + username);
        }
    }

    private void handleChatMessage(WebSocketSession session, Map<String, Object> messageData)
            throws IOException {
        String senderUsername = (String) session.getAttributes().get("username");
        String recipientUsername = (String) messageData.get("recipient");
        String content = (String) messageData.get("content");

        if (senderUsername == null || recipientUsername == null || content == null) {
            return;
        }

        // Save message to database
        Message message = chatService.saveMessage(senderUsername, recipientUsername, content);

        // Try to deliver immediately if recipient is online
        WebSocketSession recipientSession = activeSessions.get(recipientUsername);
        if (recipientSession != null && recipientSession.isOpen()) {
            Map<String, Object> messageToSend = Map.of(
                    "type", "message",
                    "sender", senderUsername,
                    "content", content,
                    "timestamp", message.getSentAt().toString()
            );

            try {
                recipientSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(messageToSend)));
                // Mark as delivered
                chatService.markMessageAsDelivered(message.getId());
            } catch (IOException e) {
                // If sending fails, message remains undelivered in database
                System.err.println("Failed to send message to " + recipientUsername + ": " + e.getMessage());
                // Remove inactive session
                activeSessions.remove(recipientUsername);
            }
        }

        // Send confirmation to sender
        Map<String, Object> confirmation = Map.of(
                "type", "message_sent",
                "recipient", recipientUsername,
                "delivered", recipientSession != null && recipientSession.isOpen()
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmation)));
    }

    private void deliverUndeliveredMessages(String username, WebSocketSession session) throws IOException {
        var undeliveredMessages = chatService.getUndeliveredMessages(username);

        for (Message message : undeliveredMessages) {
            Map<String, Object> messageToSend = Map.of(
                    "type", "message",
                    "sender", message.getSenderUsername(),
                    "content", message.getContent(),
                    "timestamp", message.getSentAt().toString()
            );

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(messageToSend)));
            chatService.markMessageAsDelivered(message.getId());
        }
    }

    private void sendOnlineUsers(WebSocketSession session) throws IOException {
        Map<String, Object> response = Map.of(
                "type", "online_users",
                "users", activeSessions.keySet()
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void broadcastOnlineUsers() {
        Map<String, Object> message = Map.of(
                "type", "online_users",
                "users", activeSessions.keySet()
        );

        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            System.err.println("Failed to serialize online users message");
            return;
        }

        activeSessions.values().removeIf(session -> !session.isOpen());
        activeSessions.forEach((username, session) -> {
            try {
                session.sendMessage(new TextMessage(messageJson));
            } catch (IOException e) {
                System.err.println("Failed to send online users to " + username);
            }
        });
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket transport error: " + exception.getMessage());
        cleanupSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        System.out.println("WebSocket connection closed: " + session.getId());
        cleanupSession(session);
    }

    private void cleanupSession(WebSocketSession session) {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            activeSessions.remove(username);
            broadcastOnlineUsers();
            System.out.println("User disconnected: " + username);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}