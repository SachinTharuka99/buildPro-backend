package com.example.buildPro.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;
import java.util.*;

@Data
@Builder
@Document("discussion")
@NoArgsConstructor
@AllArgsConstructor
public class Discussion {
    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private String userName;
    private String name;
    private String imgUrl;
    private String title;
    private String description;
    private String image;
    private List<String> documents;
    private String role;
    private String status;
    private int noOfLikes;
    private Set<String> likes = new HashSet<>();
    private List<PostLike> likesList = new ArrayList<>();
    private List<DiscussionComment> comment = new ArrayList<>();


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private String createdDate;
}
