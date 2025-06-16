package com.example.buildPro.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO {
    private String id;
    private String conversationId;
    private String senderId;
    private String receiverId;
    private String messageType;
    private String content;
    private MediaDTO media;
    private LocalDateTime timestamp;
    private boolean read;
    private String senderName;
    private String formattedTime;
}
