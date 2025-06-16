package com.example.buildPro.repository;


import com.example.buildPro.entity.Discussion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionForumRepository extends MongoRepository<Discussion, String> {
    List<Discussion> findByUserNameOrderByCreatedDateDesc(String username);
}
