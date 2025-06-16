package com.example.buildPro.bean;


import com.example.buildPro.entity.Comment;
import com.example.buildPro.entity.PostLike;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FreelancerPostDTO {
    private String id;
    private String tutorUserName;
    private String tutorName;
    private String tutorImgUrl;
    private String title;
    private String description;
    private List<String> image;
    private String status;
    private int noOfLikes;
    private int commentCount;
    private List<Comment> comment;
    private List<PostLike> likesList;
    private String createdDate;
}
