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
@Document("FreelancerPost")
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerPost {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private String tutorUserName;
    private String tutorName;
    private String tutorImgUrl;
    private String title;
    private String description;
    private List<String> image;
    private String status;
    private int noOfLikes;
    private Set<String> likes = new HashSet<>();
    private List<PostLike> likesList = new ArrayList<>();
//    private Set<Like> likes = new HashSet<>();
    private List<Comment> comment = new ArrayList<>();


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private String createdDate;
}
