package com.example.buildPro.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Media {
    private String url;
    private String filename;
    private long size;
    private String contentType;
}
