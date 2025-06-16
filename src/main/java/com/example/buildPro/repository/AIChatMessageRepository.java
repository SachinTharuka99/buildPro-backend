package com.example.buildPro.repository;


import com.example.buildPro.entity.AIChatBot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface AIChatMessageRepository extends MongoRepository<AIChatBot, String> {
    List<AIChatBot> findByUserNameOrderByCreatedTimeAsc(String userName);
}
