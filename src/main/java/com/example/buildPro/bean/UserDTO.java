package com.example.buildPro.bean;

import lombok.Data;

import java.util.Date;

@Data
public class UserDTO {
    private String email;
    private String username;
    private String name;
    private String password;
    private Date birthday;
    private String image;


    public UserDTO(String id, String name, String username, String email, String image) {
    }
}
