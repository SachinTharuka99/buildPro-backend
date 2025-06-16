package com.example.buildPro.entity;

import lombok.Data;

import javax.persistence.Embeddable;

@Data
@Embeddable
public class PostLike {
    private String username;
    private String imageUrl;
}
