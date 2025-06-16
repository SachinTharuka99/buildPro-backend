package com.example.buildPro.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@Document("gig")
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerGig {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private String tutorUserName;
    private String name;
    private String tutorImg;
    private String title;
    private String description;
    private String image;
    private List<String> documents;
    private String status;
    private double pricePerHour;
    private int noOfFollowers;
//    private String userImage;
    private String subject;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private String createdDate;

}
