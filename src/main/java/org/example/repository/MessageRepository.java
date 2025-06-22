package org.example.repository;

import org.example.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRecipientUsernameAndDeliveredFalseOrderBySentAt(String recipientUsername);

    // Fetch conversation history between two users
    @Query("SELECT m FROM Message m WHERE " +
            "(m.senderUsername = :user1 AND m.recipientUsername = :user2) OR " +
            "(m.senderUsername = :user2 AND m.recipientUsername = :user1) " +
            "ORDER BY m.sentAt ASC")
    List<Message> findConversationHistory(@Param("user1") String user1, @Param("user2") String user2);

    // Fetch recent conversations for a user (last message with each contact)
    @Query("SELECT m FROM Message m WHERE m.id IN (" +
            "SELECT MAX(m2.id) FROM Message m2 WHERE " +
            "(m2.senderUsername = :username OR m2.recipientUsername = :username) " +
            "GROUP BY CASE WHEN m2.senderUsername = :username THEN m2.recipientUsername ELSE m2.senderUsername END" +
            ") ORDER BY m.sentAt DESC")
    List<Message> findRecentConversations(@Param("username") String username);
}