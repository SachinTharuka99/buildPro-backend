package com.example.buildPro.controller;


import com.example.buildPro.bean.MessageDTO;
import com.example.buildPro.entity.AIChatBot;
import com.example.buildPro.service.AIChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Controller
public class WebSocketChatController {
    @Autowired
    private AIChatService aiChatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{userId}")
    @SendTo("/topic/messages/{userId}")
    public AIChatBot sendMessage(@DestinationVariable String userId, Map<String, String> payload) {
        String message = payload.get("message");

        // Save user message
        AIChatBot userMessage = aiChatService.saveUserMessage(userId, message);

        // Call Flask AI service
        String aiResponse = callFlaskForAIResponse(userId, message);

        // Save AI response
        AIChatBot aiMessage = aiChatService.saveAIResponse(userId, aiResponse);

        // Return AI message to broadcast
        return aiMessage;
    }

    // Helper method to call Flask app
    private String callFlaskForAIResponse(String userId, String message) {
        try {
            String flaskUrl = "http://localhost:5001/chat/" + userId;  // Assuming Flask is on localhost
            HttpClient client = HttpClient.newHttpClient();
            String requestBody = new ObjectMapper().writeValueAsString(Map.of("message", message));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(flaskUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Read AI response from JSON
            Map<String, Object> responseMap = new ObjectMapper().readValue(response.body(), Map.class);
            return (String) responseMap.get("message");

        } catch (Exception e) {
            e.printStackTrace();
            return "AI response error: " + e.getMessage();
        }
    }

    public void sendToReceiver(String receiverId, MessageDTO messageDTO) {
        String destination = "/topic/messages/" + receiverId;
        messagingTemplate.convertAndSend(destination, messageDTO);
    }

}
