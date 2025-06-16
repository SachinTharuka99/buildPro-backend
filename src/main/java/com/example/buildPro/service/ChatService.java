package com.example.buildPro.service;

import com.example.buildPro.bean.MediaDTO;
import com.example.buildPro.bean.MessageDTO;
import com.example.buildPro.bean.SendMessageRequest;
import com.example.buildPro.bean.UserWithLastMessageDTO;
import com.example.buildPro.entity.AuthUser;
import com.example.buildPro.entity.Media;
import com.example.buildPro.entity.Message;
import com.example.buildPro.repository.AuthUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ChatService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    AuthUserRepository authUserRepository;

    public List<MessageDTO> getConversationMessages(String loggedUserId, String selectedUserId) {
        // Create conversation IDs for both possible combinations
        String conversationId1 = loggedUserId + "_" + selectedUserId;
        String conversationId2 = selectedUserId + "_" + loggedUserId;

        // Query to find messages with either conversation ID
        Query query = new Query();
        query.addCriteria(Criteria.where("conversationId").in(conversationId1, conversationId2));
        query.with(Sort.by(Sort.Direction.ASC, "timestamp"));

        List<Message> messages = mongoTemplate.find(query, Message.class, "messages");

        // Convert to DTOs and add formatting
        return messages.stream()
                .map(message -> convertToDTO(message))
                .collect(Collectors.toList());
    }

    public MessageDTO sendMessage(SendMessageRequest request) {
        // Create conversation ID
        String conversationId = request.getSenderId() + "_" + request.getReceiverId();

        // Create new message
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(request.getSenderId());
        message.setReceiverId(request.getReceiverId());
        message.setMessageType(request.getMessageType());
        message.setContent(request.getContent());
        message.setMedia(convertMediaDTOToEntity(request.getMedia()));
        message.setTimestamp(LocalDateTime.now());
        message.setRead(false);

        // Save to database
        Message savedMessage = mongoTemplate.save(message, "messages");

        return convertToDTO(savedMessage);
    }



    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversationId());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setMessageType(message.getMessageType());
        dto.setContent(message.getContent());
        dto.setMedia(convertMediaEntityToDTO(message.getMedia()));
        dto.setTimestamp(message.getTimestamp());
        dto.setRead(message.isRead());

        // Format time for display
        dto.setFormattedTime(formatTime(message.getTimestamp()));

        // Get sender name (assuming you have user service)
        try {
//            String senderName = userService.getUserName(message.getSenderId());
            dto.setSenderName("h");
        } catch (Exception e) {
            dto.setSenderName("Unknown User");
        }

        return dto;
    }

    private MediaDTO convertMediaEntityToDTO(Media media) {
        if (media == null) return null;

        MediaDTO dto = new MediaDTO();
        dto.setUrl(media.getUrl());
        dto.setFilename(media.getFilename());
        dto.setSize(media.getSize());
        dto.setContentType(media.getContentType());
        return dto;
    }

    private Media convertMediaDTOToEntity(MediaDTO mediaDTO) {
        if (mediaDTO == null) return null;

        Media media = new Media();
        media.setUrl(mediaDTO.getUrl());
        media.setFilename(mediaDTO.getFilename());
        media.setSize(mediaDTO.getSize());
        media.setContentType(mediaDTO.getContentType());
        return media;
    }

    private String formatTime(LocalDateTime timestamp) {
        if (timestamp == null) return "";

        LocalDateTime now = LocalDateTime.now();
        if (timestamp.toLocalDate().equals(now.toLocalDate())) {
            // Same day - show time only
            return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (timestamp.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
            // Yesterday
            return "Yesterday";
        } else {
            // Other days - show date
            return timestamp.format(DateTimeFormatter.ofPattern("MMM dd"));
        }
    }

    public List<UserWithLastMessageDTO> getLatestMessagesPerConversation(String currentUsername) {
        // Match messages where the user is either sender or receiver
        MatchOperation matchStage = Aggregation.match(
                new Criteria().orOperator(
                        Criteria.where("senderId").is(currentUsername),
                        Criteria.where("receiverId").is(currentUsername)
                )
        );

        // Sort by timestamp descending
        SortOperation sortStage = Aggregation.sort(Sort.by(Sort.Direction.DESC, "timestamp"));

        // Run aggregation
        Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                sortStage
        );

        AggregationResults<Message> results =
                mongoTemplate.aggregate(aggregation, "messages", Message.class);

        List<Message> allMessages = results.getMappedResults();

        // Normalize conversation IDs using a Set to track processed conversations
        Map<String, Message> latestMessages = new LinkedHashMap<>();

        for (Message msg : allMessages) {
            String u1 = msg.getSenderId();
            String u2 = msg.getReceiverId();

            // Normalize conversationId as min_u_max_u
            String convId = Stream.of(u1, u2).sorted().collect(Collectors.joining("_"));

            if (!latestMessages.containsKey(convId)) {
                latestMessages.put(convId, msg);
            }
        }

        // Convert to UserWithLastMessageDTO
        List<UserWithLastMessageDTO> result = new ArrayList<>();
        for (Message msg : latestMessages.values()) {
            String otherUsername = msg.getSenderId().equals(currentUsername)
                    ? msg.getReceiverId()
                    : msg.getSenderId();

            Optional<AuthUser> otherUserOpt = authUserRepository.findByUsername(otherUsername);

            if (otherUserOpt.isPresent()) {
                AuthUser otherUser = otherUserOpt.get();

                UserWithLastMessageDTO dto = new UserWithLastMessageDTO();
                dto.setId(otherUser.getId());
                dto.setUsername(otherUser.getUsername());
                dto.setName(otherUser.getName());
                dto.setImage(otherUser.getImage());
                dto.setLastMessage(msg.getContent());
                dto.setFormattedTime(msg.getTimestamp().toString());

                result.add(dto);
            }

        }

        return result;
    }
}
