package com.example.buildPro.controller;


import com.example.buildPro.bean.ResponseDTO;
import com.example.buildPro.entity.*;
import com.example.buildPro.repository.AuthUserRepository;
import com.example.buildPro.repository.DiscussionForumRepository;
import com.example.buildPro.service.JWTService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

@CrossOrigin(origins = "http://localhost:8012")
@RestController
@RequestMapping("/discussion")
public class DiscussionForumController {

    @Autowired
    private DiscussionForumRepository discussionForumRepository;

    private static final Logger logger = LoggerFactory.getLogger(FreelancerGigController.class);

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;  // JWT utility class
    private final AuthenticationManager authenticationManager;

    public DiscussionForumController(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder, JWTService jwtService, AuthenticationManager authenticationManager) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDTO> createDiscussion(
            @RequestPart("discussion") String discussionJson,
            @RequestPart("image") MultipartFile image,
            @RequestPart("documents") List<MultipartFile> documents,
            @RequestHeader("Authorization") String token
    ) {
        ResponseDTO responseDTO = new ResponseDTO();

        try {
            // Parse JSON
            ObjectMapper objectMapper = new ObjectMapper();
            Discussion discussion = objectMapper.readValue(discussionJson, Discussion.class);

            // Authenticate
            Optional<AuthUser> userOptional = authenticateUser(token);
            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                logger.warn("Unauthorized access");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            // Set creator info
            String username = userOptional.get().getUsername();
            String name = userOptional.get().getName();
            String role = userOptional.get().getRole();
            String userImgUrl = userOptional.get().getImage();
            discussion.setUserName(username);

            // Set created date
            if (discussion.getCreatedDate() == null || discussion.getCreatedDate().isEmpty()) {
                discussion.setCreatedDate(LocalDate.now().toString());
            }

            // Save image
            if (image != null && !image.isEmpty()) {
                String imagePath = "C:\\xampp\\htdocs\\myPro\\BuildPro\\BuildProFrontEnd\\images\\discussion\\" + discussion.getId() + "\\" + discussion.getId() + "_cover.jpg";
                Files.createDirectories(Paths.get(imagePath).getParent());
                Files.write(Paths.get(imagePath), image.getBytes());
                discussion.setImage("images\\discussion\\" + discussion.getId() + "\\" + discussion.getId() + "_cover.jpg");
            }

            // Save documents
            List<String> docPaths = new ArrayList<>();
            for (int i = 0; i < documents.size(); i++) {
                MultipartFile doc = documents.get(i);
                String docName = discussion.getId() + "_doc" + (i + 1) + ".pdf";
                Path docPath = Paths.get("C:\\xampp\\htdocs\\myPro\\BuildPro\\BuildProFrontEnd\\documents\\discussion\\" + discussion.getId() + "\\" + docName);
                Files.createDirectories(docPath.getParent());
                Files.write(docPath, doc.getBytes());
                docPaths.add("documents\\discussion\\" + discussion.getId() + "\\" + docName);
            }
            discussion.setDocuments(docPaths);
            discussion.setName(name);
            discussion.setRole(role);
            discussion.setImgUrl(userImgUrl);

            // Save to DB
            Discussion saved = discussionForumRepository.save(discussion);
            logger.info("Discussion created: {}", saved.getId());

            responseDTO.setStatusCode(201);
            responseDTO.setMessage("Discussion created successfully");
            responseDTO.setData(saved);
            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);

        } catch (Exception ex) {
            logger.error("Error creating discussion", ex);
            responseDTO.setStatusCode(500);
            responseDTO.setMessage("Internal server error");
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ResponseDTO> getAllDiscussions(@RequestHeader("Authorization") String token) {
        ResponseDTO responseDTO = new ResponseDTO();

        try {
            // Authenticate the user
            Optional<AuthUser> userOptional = authenticateUser(token);
            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                logger.warn("Unauthorized access to get discussions");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            List<Discussion> discussions = discussionForumRepository.findAll(Sort.by(Sort.Direction.DESC, "createdDate"));

            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Fetched discussions successfully");
            responseDTO.setData(discussions);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception ex) {
            logger.error("Error fetching discussions", ex);
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("Internal server error");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getByUser")
    public ResponseEntity<ResponseDTO> getDiscussionsByUserName(@RequestHeader("Authorization") String token) {
        ResponseDTO responseDTO = new ResponseDTO();

        try {
            // Authenticate the user
            Optional<AuthUser> userOptional = authenticateUser(token);
            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                logger.warn("Unauthorized access to get discussions");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            String userName = userOptional.get().getUsername();

//            List<Discussion> discussions = discussionForumRepository.findAll(Sort.by(Sort.Direction.DESC, "createdDate"));
            List<Discussion> discussions = discussionForumRepository.findByUserNameOrderByCreatedDateDesc(userName);

            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Fetched discussions successfully");
            responseDTO.setData(discussions);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception ex) {
            logger.error("Error fetching discussions", ex);
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("Internal server error");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/by-user/{discussionId}")
    public ResponseEntity<ResponseDTO> getDiscussionsByUsername(
            @PathVariable("discussionId") String ForumId,
            @RequestHeader("Authorization") String token
    ) {
        ResponseDTO responseDTO = new ResponseDTO();

        try {
            // Authenticate the user
            Optional<AuthUser> userOptional = authenticateUser(token);
            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                logger.warn("Unauthorized access to get discussions by username");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

//            List<Discussion> discussions = discussionForumRepository.findByUserNameOrderByCreatedDateDesc(ForumId);
            Optional<Discussion> discussions = discussionForumRepository.findById(ForumId);



            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Fetched discussions for ForumId: " + ForumId);
            responseDTO.setData(discussions);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception ex) {
            logger.error("Error fetching discussions by username", ex);
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("Internal server error");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @GetMapping("/getById/{id}")
    public ResponseEntity<ResponseDTO> getDiscussionById(
            @PathVariable("id") String id,
            @RequestHeader("Authorization") String token
    ) {
        ResponseDTO responseDTO = new ResponseDTO();

        try {
            // Authenticate the user
            Optional<AuthUser> userOptional = authenticateUser(token);
            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                logger.warn("Unauthorized access to get discussion by ID");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            Optional<Discussion> discussionOptional = discussionForumRepository.findById(id);

            if (!discussionOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("Discussion not found for ID: " + id);
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Discussion found");
            responseDTO.setData(discussionOptional.get());
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception ex) {
            logger.error("Error fetching discussion by ID", ex);
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("Internal server error");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{discussionId}/like")
    public ResponseEntity<ResponseDTO> toggleLike(
            @PathVariable String discussionId,
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
            Optional<Discussion> postOptional = discussionForumRepository.findById(discussionId);

            if (!postOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("Post not found");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            Discussion post = postOptional.get();



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
            post.setNoOfLikes(likes.size());

            discussionForumRepository.save(post);

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

    @PostMapping("/{discussionId}/comment")
    public ResponseEntity<ResponseDTO> addCommentToPost(@PathVariable String discussionId,
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
            Optional<Discussion> postOptional = discussionForumRepository.findById(discussionId);

            if (!postOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("Post not found");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            Discussion post = postOptional.get();

            // Create and add comment
            DiscussionComment comment = new DiscussionComment();
            comment.setId(UUID.randomUUID().toString());
            comment.setUser(authUser.getUsername());
            comment.setUserName(authUser.getName());
            comment.setImageUrl(authUser.getImage());
            comment.setComment(newComment.getComment());
            comment.setCreatedAt(LocalDate.now().toString());

            if (post.getComment() == null) {
                post.setComment(new ArrayList<>());
            }

            post.getComment().add(comment);
            discussionForumRepository.save(post);

            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Comment added successfully");
            responseDTO.setData(post.getComment());

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("Error adding comment: " + e.getMessage());
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{discussionId}/comment/{commentId}/reply")
    public ResponseEntity<ResponseDTO> addReplyToComment(@PathVariable String discussionId,
                                                         @PathVariable String commentId,
                                                         @RequestBody DiscussionCommentReply reply,
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
            Optional<Discussion> postOptional = discussionForumRepository.findById(discussionId);
            if (!postOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("Post not found");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            Discussion post = postOptional.get();
            DiscussionComment comment = post.getComment()
                    .stream()
                    .filter(c -> commentId.equals(c.getId()))
                    .findFirst()
                    .orElse(null);


            if (comment == null) {
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("Comment not found");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            DiscussionCommentReply newReply = new DiscussionCommentReply();
            newReply.setUser(authUser.getUsername());
            newReply.setImageUrl(authUser.getImage());
            newReply.setReply(reply.getReply());
            newReply.setCreatedAt(LocalDate.now());
            newReply.setName(userOptional.get().getName());

            comment.getCommentReply().add(newReply);
            discussionForumRepository.save(post);

            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Reply added successfully");
            responseDTO.setData(comment.getCommentReply());

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("Error adding reply: " + e.getMessage());
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    private Optional<AuthUser> authenticateUser(String token) {
        String username = jwtService.extractUsername(token);
        return authUserRepository.findByUsername(username)
                .filter(user -> jwtService.isTokenValid(token, user));
    }
}
