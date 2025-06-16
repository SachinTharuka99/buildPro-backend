package com.example.buildPro.repository;


import com.example.buildPro.entity.AuthUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthUserRepository extends MongoRepository<AuthUser, String> {
    Optional<AuthUser> findByUsername(String username);
    String findByName(String username);

    List<AuthUser> findAll(); // Use this method instead of getAllUsers

    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'username': { $ne: ?1 } }")
    List<AuthUser> searchByName(String nameRegex, String excludedUsername);



}

