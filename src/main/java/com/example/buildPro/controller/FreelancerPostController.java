package com.example.buildPro.controller;


import com.example.buildPro.bean.FreelancerPostDTO;
import com.example.buildPro.bean.ResponseDTO;
import com.example.buildPro.entity.AuthUser;
import com.example.buildPro.entity.Comment;
import com.example.buildPro.entity.FreelancerPost;
import com.example.buildPro.entity.PostLike;
import com.example.buildPro.repository.AuthUserRepository;
import com.example.buildPro.repository.FreelancerPostRepository;
import com.example.buildPro.service.FreelancerPostService;
import com.example.buildPro.service.JWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

@CrossOrigin(origins = "http://localhost:8012")
@RestController
@RequestMapping("/tutorPost")
public class FreelancerPostController {
    @Autowired
    private FreelancerPostRepository freelancerPostRepository;

    private final FreelancerPostService freelancerPostService;
    private static final Logger logger = LoggerFactory.getLogger(FreelancerGigController.class);

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;  // JWT utility class
    private final AuthenticationManager authenticationManager;

    public FreelancerPostController(FreelancerPostService freelancerPostService, AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder, JWTService jwtService, AuthenticationManager authenticationManager) {
        this.freelancerPostService = freelancerPostService;
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }
    private Optional<AuthUser> authenticateUser(String token) {
        String username = jwtService.extractUsername(token);
        return authUserRepository.findByUsername(username)
                .filter(user -> jwtService.isTokenValid(token, user));
    }


    @PostMapping(value = "/createPost", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDTO> createPost(
            @RequestPart("post") String postJson,
            @RequestPart("images") List<MultipartFile> images,
            @RequestHeader("Authorization") String token
    ) {
        return freelancerPostService.createTutorPost(postJson, images, token);
    }

    @GetMapping("/myActivePosts")
    public ResponseEntity<ResponseDTO> getActivePostsByTutorUserName(
            @RequestHeader("Authorization") String token
    ) {
        ResponseDTO responseDTO = new ResponseDTO();

        try {
            Optional<AuthUser> userOptional = authenticateUser(token);
            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            String username = userOptional.get().getUsername();
            List<FreelancerPost> activePosts = freelancerPostService.getActivePostsByTutorUsername(username);

            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Active posts fetched successfully");
            responseDTO.setData(activePosts);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("Error fetching active posts");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/allActivePosts")
    public ResponseEntity<ResponseDTO> getAllActivePosts(
            @RequestHeader("Authorization") String token
    ) {
        ResponseDTO responseDTO = new ResponseDTO();

        try {
            Optional<AuthUser> userOptional = authenticateUser(token);
            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }


            List<FreelancerPost> activePosts = freelancerPostService.getAllActivePosts();

            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("All Active posts fetched successfully");
            responseDTO.setData(activePosts);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("Error fetching active posts");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<ResponseDTO> getPostById(@PathVariable String postId, @RequestHeader("Authorization") String token) {
        ResponseDTO responseDTO = new ResponseDTO();

        try {
            Optional<AuthUser> userOptional = authenticateUser(token);
            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            Optional<FreelancerPost> postOptional = freelancerPostService.getPostById(postId);
            if (!postOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("Post not found");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            FreelancerPost post = postOptional.get();

            FreelancerPostDTO dto = FreelancerPostDTO.builder()
                    .id(post.getId())
                    .tutorUserName(post.getTutorUserName())
                    .title(post.getTitle())
                    .description(post.getDescription())
                    .image(post.getImage())
                    .status(post.getStatus())
                    .noOfLikes(post.getLikes().size())
                    .comment(post.getComment())
                    .likesList(post.getLikesList())
                    .commentCount(post.getComment().size())
                    .createdDate(post.getCreatedDate())
                    .build();

            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Post fetched successfully");
            responseDTO.setData(dto);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("Error fetching post by ID");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/post/{postId}/comment")
    public ResponseEntity<ResponseDTO> addCommentToPost(@PathVariable String postId,
                                                        @RequestBody Comment newComment,
                                                        @RequestHeader("Authorization") String token) {
        ResponseDTO responseDTO = new ResponseDTO();

        try {
            Optional<AuthUser> userOptional = authenticateUser(token);
            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }
            AuthUser authUser = userOptional.get();

            Optional<FreelancerPost> postOptional = freelancerPostService.getPostById(postId);
            if (!postOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("Post not found");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            FreelancerPost post = postOptional.get();


            // Build comment using AuthUser details
            Comment comment = new Comment();
            comment.setUser(authUser.getUsername());
            comment.setImageUrl(authUser.getImage());
            comment.setComment(newComment.getComment());
            comment.setCreatedAt(LocalDate.now().toString());


            post.getComment().add(comment);
            freelancerPostService.savePost(post);


            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Comment added successfully");
            responseDTO.setData(post.getComment());

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("Error adding comment");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/post/{postId}/like")
    public ResponseEntity<ResponseDTO> toggleLike(
            @PathVariable String postId,
            @RequestHeader("Authorization") String token) {

        ResponseDTO responseDTO = new ResponseDTO();

        try {
            Optional<AuthUser> userOptional = authenticateUser(token);
            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            AuthUser authUser = userOptional.get();
            Optional<FreelancerPost> postOptional = freelancerPostService.getPostById(postId);

            if (!postOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("Post not found");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            FreelancerPost post = postOptional.get();



            // Build comment using AuthUser details
            PostLike postLike = new PostLike();
            postLike.setUsername(authUser.getName());
            postLike.setImageUrl(authUser.getImage());


            Set<String> likes = post.getLikes(); // Assuming Set<String> usernames

            boolean liked;
            if (likes.contains(authUser.getUsername())) {
                likes.remove(authUser.getUsername());
                post.getLikesList().remove(postLike);
                liked = false;
            } else {
                likes.add(authUser.getUsername());
                post.getLikesList().add(postLike);
                liked = true;
            }

            post.setLikes(likes);


            freelancerPostService.savePost(post);

            Map<String, Object> result = new HashMap<>();
            result.put("liked", liked);
            result.put("totalLikes", likes.size()); // REAL-TIME from DB
            result.put("userImg", authUser.getImage()); // REAL-TIME from DB

            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Like status updated");
            responseDTO.setData(result);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("Error updating like status");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }







}
