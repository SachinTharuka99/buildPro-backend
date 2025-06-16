package com.example.buildPro.entity;

import lombok.Data;

import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class DiscussionComment {
    @Id
    private String id;
    {
        UUID.randomUUID();
    }

    private String user;
    private String userName;
    private String imageUrl;
    private String comment;
    private String createdAt;
    private List<DiscussionCommentReply> commentReply = new ArrayList<>();
}
