package com.example.buildPro.controller;


import com.example.buildPro.entity.AIChatBot;
import com.example.buildPro.service.AIChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:8012")
@RestController
@RequestMapping("/chatBot")
public class AIChatBotController {

    @Autowired
    private AIChatService aiChatService;

    @GetMapping("/{userName}")
    public ResponseEntity<List<AIChatBot>> getChatHistory(@PathVariable String userName) {
        return ResponseEntity.ok(aiChatService.getChatHistoryForUser(userName));
    }

    @PostMapping("/{userName}/send")
    public ResponseEntity<AIChatBot> sendMessage(@PathVariable String userName, @RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        AIChatBot savedMessage = aiChatService.saveUserMessage(userName, message);

        // Here you would typically call your AI service to get a response
        // For this example, we'll just echo back a simple response
        String aiResponse = "Thanks for your message: " + message;
        AIChatBot aiMessage = aiChatService.saveAIResponse(userName, aiResponse);

        return ResponseEntity.ok(savedMessage);
    }

    @PostMapping("/{userName}/ai-response")
    public ResponseEntity<AIChatBot> saveAIResponse(@PathVariable String userName, @RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        AIChatBot savedMessage = aiChatService.saveAIResponse(userName, message);
        return ResponseEntity.ok(savedMessage);
    }

}
