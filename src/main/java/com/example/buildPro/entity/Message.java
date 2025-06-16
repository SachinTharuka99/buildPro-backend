package com.example.buildPro.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.Id;
import java.time.LocalDateTime;

@Document(collection = "messages")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    @Id
    private String id;

    @Field("conversationId")
    private String conversationId;

    @Field("senderId")
    private String senderId;

    @Field("receiverId")
    private String receiverId;

    @Field("messageType")
    private String messageType; // "text", "image", "document", etc.

    @Field("content")
    private String content;

    @Field("media")
    private Media media;

    @Field("timestamp")
    private LocalDateTime timestamp;

    @Field("read")
    private boolean read;
}
