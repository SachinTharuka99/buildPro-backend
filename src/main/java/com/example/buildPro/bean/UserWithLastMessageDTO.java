package com.example.buildPro.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserWithLastMessageDTO {
    private String id;
    private String username;
    private String name;
    private String lastMessage;
    private String image;
    private String formattedTime; // optional
}
