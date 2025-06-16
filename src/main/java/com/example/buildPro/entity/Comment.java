package com.example.buildPro.entity;

import lombok.Data;

@Data
public class Comment  {
    private String user;
    private String imageUrl;
    private String comment;
    private String createdAt;
}
