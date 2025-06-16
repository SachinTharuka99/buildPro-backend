package com.example.buildPro.service;


import com.example.buildPro.bean.ResponseDTO;
import com.example.buildPro.controller.FreelancerGigController;
import com.example.buildPro.entity.AuthUser;
import com.example.buildPro.entity.FreelancerPost;
import com.example.buildPro.repository.AuthUserRepository;
import com.example.buildPro.repository.FreelancerPostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FreelancerPostService {

    private final FreelancerPostRepository freelancerPostRepository;
    private final ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(FreelancerGigController.class);

    private final AuthUserRepository authUserRepository;
    private final JWTService jwtService;  // JWT utility class
    private final AuthenticationManager authenticationManager;

    public ResponseEntity<ResponseDTO> createTutorPost(String postJson, List<MultipartFile> images, String token) {
        ResponseDTO responseDTO = new ResponseDTO();

        try {
            // Authenticate user
            Optional<AuthUser> userOptional = authenticateUser(token);
            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            FreelancerPost post = objectMapper.readValue(postJson, FreelancerPost.class);

            post.setTutorUserName(userOptional.get().getUsername());
            if (post.getCreatedDate() == null || post.getCreatedDate().isEmpty()) {
                post.setCreatedDate(LocalDate.now().toString());
            }

            // Generate ID
            if (post.getId() == null || post.getId().isEmpty()) {
                post.setId(UUID.randomUUID().toString());
            }

            // Save image locally and set path
            List<String> imagePaths = saveImagesToLocal(post.getId(), images);
            post.setImage(imagePaths);
            post.setStatus("ACTIVE");
            post.setTutorName(userOptional.get().getName());
            post.setTutorImgUrl(userOptional.get().getImage());

            // Save post
            FreelancerPost savedPost = freelancerPostRepository.save(post);

            responseDTO.setStatusCode(HttpStatus.CREATED.value());
            responseDTO.setMessage("Post created successfully");
            responseDTO.setData(savedPost);

            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);

        } catch (IOException e) {
            responseDTO.setStatusCode(HttpStatus.BAD_REQUEST.value());
            responseDTO.setMessage("Invalid post JSON");
            return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("Error occurred while saving post");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<String> saveImagesToLocal(String postId, List<MultipartFile> images) throws IOException {
        List<String> paths = new ArrayList<>();
        String baseDir = "C:\\xampp\\htdocs\\myPro\\BuildPro\\BuildProFrontEnd\\images\\postPics\\" + postId;

        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        int count = 1;
        for (MultipartFile file : images) {
            String fileName = postId + "_image" + count + ".jpg";
            File dest = new File(dir, fileName);
            file.transferTo(dest);
            paths.add("images/postPics/" + postId + "/" + fileName); // Relative path to be used in frontend
            count++;
        }

        return paths;
    }

    public List<FreelancerPost> getActivePostsByTutorUsername(String username) {
        return freelancerPostRepository.findByTutorUserNameAndStatus(username, "ACTIVE");
    }

    public List<FreelancerPost> getAllActivePosts() {
        return freelancerPostRepository.findByStatus("ACTIVE");
    }


    private Optional<AuthUser> authenticateUser(String token) {
        String username = jwtService.extractUsername(token);
        return authUserRepository.findByUsername(username)
                .filter(user -> jwtService.isTokenValid(token, user));
    }

    public Optional<FreelancerPost> getPostById(String postId) {
        return freelancerPostRepository.findById(postId);
    }

    public void savePost(FreelancerPost post) {
        freelancerPostRepository.save(post);
    }

}
