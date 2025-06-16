package com.example.buildPro.repository;


import com.example.buildPro.bean.FreelancerGigDTO;
import com.example.buildPro.entity.FreelancerGig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreelancerGigRepository extends MongoRepository<FreelancerGig, String> {
    Optional<FreelancerGig> findById(String id);

    List<FreelancerGigDTO> findAllByTutorUserName(String tutorUserName);

    Optional<FreelancerGigDTO> findByIdAndTutorUserName(String gigId, String tokenUsername);

    @Query("{'$or':["
            + "{'title': {$regex: ?0, $options: 'i'}},"
            + "{'subject': {$regex: ?0, $options: 'i'}},"
            + "{'name': {$regex: ?0, $options: 'i'}}"
            + "]}")
    List<FreelancerGig> searchByKeyword(String keyword);


}
