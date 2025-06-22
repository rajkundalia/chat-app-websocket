package org.example.service;

import org.example.entity.Message;
import org.example.entity.User;
import org.example.repository.MessageRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChatService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public ChatService(UserRepository userRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    public User registerUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User(username, password);
        return userRepository.save(user);
    }

    public Optional<User> authenticateUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            return userOpt;
        }

        return Optional.empty();
    }

    @Transactional
    public void updateLastLogin(String username) {
        // @Transactional is needed here because we're updating entity state
        // outside of a repository method, ensuring the update is persisted
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public Message saveMessage(String senderUsername, String recipientUsername, String content) {
        Message message = new Message(senderUsername, recipientUsername, content);
        return messageRepository.save(message);
    }

    public List<Message> getUndeliveredMessages(String recipientUsername) {
        return messageRepository.findByRecipientUsernameAndDeliveredFalseOrderBySentAt(recipientUsername);
    }

    @Transactional
    public void markMessageAsDelivered(Long messageId) {
        // @Transactional ensures the delivery status update is atomically committed
        messageRepository.findById(messageId).ifPresent(message -> {
            message.setDelivered(true);
            message.setDeliveredAt(LocalDateTime.now());
            messageRepository.save(message);
        });
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<Message> getConversationHistory(String user1, String user2) {
        return messageRepository.findConversationHistory(user1, user2);
    }

    public List<Message> getRecentConversations(String username) {
        return messageRepository.findRecentConversations(username);
    }
}