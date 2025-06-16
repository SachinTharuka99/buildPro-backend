package com.example.buildPro.repository;


import com.example.buildPro.entity.FreelancerPost;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FreelancerPostRepository extends MongoRepository<FreelancerPost, String> {
    List<FreelancerPost> findByTutorUserNameAndStatus(String tutorUserName, String status);
    List<FreelancerPost> findByStatus(String status);
    Optional<FreelancerPost> findById(String id);


}
