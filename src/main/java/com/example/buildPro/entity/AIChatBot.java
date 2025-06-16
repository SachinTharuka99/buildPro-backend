package com.example.buildPro.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "AIBotChat")
public class AIChatBot {

    @Id
    private String messageId;
    private String userName;
    private String message;
    private String role;  // "user" or "ai"
    private LocalDateTime createdTime;


    public AIChatBot() {
    }

    // Constructor with all fields
    public AIChatBot(String messageId, String userName, String message, String role, LocalDateTime createdTime) {
        this.messageId = messageId;
        this.userName = userName;
        this.message = message;
        this.role = role;
        this.createdTime = createdTime;
    }
}
