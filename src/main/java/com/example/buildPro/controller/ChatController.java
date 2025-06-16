package com.example.buildPro.controller;

import com.example.buildPro.bean.*;
import com.example.buildPro.entity.AuthUser;
import com.example.buildPro.entity.Message;
import com.example.buildPro.repository.AuthUserRepository;
import com.example.buildPro.service.ChatService;
import com.example.buildPro.service.JWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
@CrossOrigin // allows frontend calls
public class ChatController {
    private final AuthUserRepository authUserRepository;
    private final WebSocketChatController webSocketMessageController;
    private final JWTService jwtService;  // JWT utility class
    private final AuthenticationManager authenticationManager;
    private final MongoTemplate mongoTemplate;
    private final ChatService chatService;
    private static final Logger logger = LoggerFactory.getLogger(FreelancerGigController.class);

    public ChatController(AuthUserRepository authUserRepository, WebSocketChatController webSocketMessageController, JWTService jwtService, AuthenticationManager authenticationManager, MongoTemplate mongoTemplate, ChatService chatService) {
        this.authUserRepository = authUserRepository;
        this.webSocketMessageController = webSocketMessageController;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.mongoTemplate = mongoTemplate;
        this.chatService = chatService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsersByName(@RequestHeader("Authorization") String token,
                                               @RequestParam String name) {
        ResponseDTO responseDTO = new ResponseDTO();
        Optional<AuthUser> userOptional = authenticateUser(token);

        if (!userOptional.isPresent()) {
            responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
            responseDTO.setMessage("Invalid or expired token");
            logger.warn("Unauthorized access");
            return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
        }

        String keyword = ".*" + Pattern.quote(name) + ".*";
        String userName = userOptional.get().getUsername();
        List<AuthUser> matchedUsers = authUserRepository.searchByName(keyword,userName);
        List<UserWithLastMessageDTO> result = matchedUsers.stream().map(user -> {
            UserWithLastMessageDTO dto = new UserWithLastMessageDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setName(user.getName());
            dto.setImage(user.getImage());

            // Fetch the last message between current user and this user
            Optional<Message> lastMsgOpt  = Optional.ofNullable(this.getLastMessageBetweenUsers(userName, user.getUsername()));

            if (lastMsgOpt.isPresent()) {
                Message lastMsg = lastMsgOpt.get();
                dto.setLastMessage(lastMsg.getContent());

            } else {
                dto.setLastMessage(""); // Or "No messages yet"
                dto.setFormattedTime("");
            }

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);


    }

    private Message getLastMessageBetweenUsers(String user1, String user2) {

        Query query = new Query();

        query.addCriteria(new Criteria().orOperator(
                Criteria.where("senderId").is(user1).and("receiverId").is(user2),
                Criteria.where("senderId").is(user2).and("receiverId").is(user1)
        ));

        query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
        query.limit(1);

        return mongoTemplate.findOne(query, Message.class);
    }


    @GetMapping("/messages")
    public ResponseEntity<ResponseDTO> getMessages(
            @RequestParam String loggedUserId,
            @RequestParam String selectedUserId,
            @RequestHeader("Authorization") String token) {

        ResponseDTO responseDTO = new ResponseDTO();

        try{

            Optional<AuthUser> userOptional = authenticateUser(token);

            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                logger.warn("Unauthorized access");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }
            // Get messages
            List<MessageDTO> messages = chatService.getConversationMessages(loggedUserId, selectedUserId);

            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Messages retrieved successfully");
            responseDTO.setData(messages);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error retrieving messages for users {} and {}: {}", loggedUserId, selectedUserId, e.getMessage(), e);
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("Failed to retrieve messages");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

//    @PostMapping("/send")
//    public ResponseEntity<?> sendMessage(@RequestHeader("Authorization") String token,
//                                         @RequestBody SendMessageRequest request) {
//        ResponseDTO responseDTO = new ResponseDTO();
//
//        Optional<AuthUser> userOptional = authenticateUser(token);
//
//        if (!userOptional.isPresent()) {
//            responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
//            responseDTO.setMessage("Invalid or expired token");
//            logger.warn("Unauthorized access");
//            return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
//        }
//
//        MessageDTO sentMessage = chatService.sendMessage(request);
//        return ResponseEntity.ok(sentMessage);
//    }

    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendMessageWithFile(
            @RequestHeader("Authorization") String token,
            @RequestParam("senderId") String senderId,
            @RequestParam("receiverId") String receiverId,
            @RequestParam("messageType") String messageType,
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {

        // 1. Authenticate the user
        Optional<AuthUser> userOptional = authenticateUser(token);
        if (!userOptional.isPresent()) {
            ResponseDTO responseDTO = new ResponseDTO();
            responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
            responseDTO.setMessage("Invalid or expired token");
            logger.warn("Unauthorized access");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseDTO);
        }

        // 2. Process the file if available
        MediaDTO mediaDTO = null;
        if (file != null && !file.isEmpty()) {
            try {
                String uploadDir = "C:\\xampp\\htdocs\\myPro\\BuildPro\\BuildProFrontEnd\\chatAssests\\" + senderId +"_"+receiverId;
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs(); // Ensure upload directory exists
                }

                String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                File dest = new File(dir, filename);
                file.transferTo(dest); // Save the file to disk

                mediaDTO = new MediaDTO();
                mediaDTO.setFilename(filename);
                mediaDTO.setSize(file.getSize());
                mediaDTO.setContentType(file.getContentType());
                String urlPath = ("chatAssests\\"+ senderId +"_"+receiverId + "\\" + filename); // Set a URL pattern to serve files (e.g., via static resources)
                mediaDTO.setUrl(urlPath.replace("\\", "/"));    // Ensure forward slashes

            } catch (IOException e) {
                logger.error("File upload error", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed");
            }
        }

        // 3. Construct the request and call service
        SendMessageRequest request = new SendMessageRequest(
                senderId, receiverId, messageType, content, mediaDTO
        );

        MessageDTO sentMessage = chatService.sendMessage(request);
        // Push to WebSocket
        webSocketMessageController.sendToReceiver(receiverId, sentMessage);

        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        response.put("message", "Message sent successfully");
        response.put("data", sentMessage); // the message object you shared

        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getUserConversations(@RequestHeader("Authorization") String token) {
        Optional<AuthUser> userOptional = authenticateUser(token);
        if (!userOptional.isPresent()) {
            ResponseDTO responseDTO = new ResponseDTO();
            responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
            responseDTO.setMessage("Invalid or expired token");
            return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
        }

        String username = userOptional.get().getUsername();
        List<UserWithLastMessageDTO> messages = chatService.getLatestMessagesPerConversation(username);
        return ResponseEntity.ok(messages);
    }







    private Optional<AuthUser> authenticateUser(String token) {
        String username = jwtService.extractUsername(token);
        return authUserRepository.findByUsername(username)
                .filter(user -> jwtService.isTokenValid(token, user));
    }


}
