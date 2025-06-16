package com.example.buildPro.bean;

import lombok.Data;

import java.util.List;

@Data
public class FreelancerGigDTO {
    private String tutorUserName;
    private String id;
    private String title;
    private String description;
    private String image;
    private List<String> documents;
    private String status;
    private double pricePerHour;
    private int noOfFollowers;
    private String userImage;
    private String subject;
    private String createdDate;
}
