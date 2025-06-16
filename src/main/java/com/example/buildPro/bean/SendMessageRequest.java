package com.example.buildPro.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageRequest {
    private String senderId;
    private String receiverId;
    private String messageType;
    private String content;
    private MediaDTO media;
}
