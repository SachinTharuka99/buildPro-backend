package com.example.buildPro.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.Id;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document("user")
public class AuthUser implements UserDetails {

    @Id
    private String id;

    {
        UUID.randomUUID();
    }

    private String name;
    private String username;
    private String email;
    private String password;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    private String image;
    private boolean active;
    private String role;
    private String contactNo;
    private String location;
    private String about;
    private String experience;
    private List<String> subject;


    // Optionally, you can add authorities if needed
    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities != null ? authorities : List.of(); // Return an empty list if authorities are not set
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return active; // You can change this based on your requirements
    }

    @Override
    public boolean isAccountNonLocked() {
        return active; // You can change this based on your requirements
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return active; // You can change this based on your requirements
    }

    @Override
    public boolean isEnabled() {
        return active; // You can change this based on your requirements
    }
}