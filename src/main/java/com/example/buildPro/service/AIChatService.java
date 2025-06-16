package com.example.buildPro.service;


import com.example.buildPro.entity.AIChatBot;
import com.example.buildPro.repository.AIChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AIChatService {
    @Autowired
    private AIChatMessageRepository aiChatMessageRepository;

    public List<AIChatBot> getChatHistoryForUser(String userName) {
        return aiChatMessageRepository.findByUserNameOrderByCreatedTimeAsc(userName);
    }

    public AIChatBot saveUserMessage(String userName, String message) {
        AIChatBot chatMessage = new AIChatBot();
        chatMessage.setMessageId(UUID.randomUUID().toString());
        chatMessage.setUserName(userName);
        chatMessage.setMessage(message);
        chatMessage.setRole("user");
        chatMessage.setCreatedTime(LocalDateTime.now());

        return aiChatMessageRepository.save(chatMessage);
    }

    public AIChatBot saveAIResponse(String userName, String message) {
        AIChatBot chatMessage = new AIChatBot();
        chatMessage.setMessageId(UUID.randomUUID().toString());
        chatMessage.setUserName(userName);
        chatMessage.setMessage(message);
        chatMessage.setRole("ai");
        chatMessage.setCreatedTime(LocalDateTime.now());

        return aiChatMessageRepository.save(chatMessage);
    }
}
