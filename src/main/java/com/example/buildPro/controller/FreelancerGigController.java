package com.example.buildPro.controller;


import com.example.buildPro.bean.FreelancerGigDTO;
import com.example.buildPro.bean.ResponseDTO;
import com.example.buildPro.entity.AuthUser;
import com.example.buildPro.entity.FreelancerGig;
import com.example.buildPro.repository.AuthUserRepository;
import com.example.buildPro.repository.FreelancerGigRepository;
import com.example.buildPro.service.JWTService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@CrossOrigin(origins = "http://localhost:8012")
@RestController
@RequestMapping("/gigs")
public class FreelancerGigController {

    @Autowired
    private FreelancerGigRepository freelancerGigRepository;

    private static final Logger logger = LoggerFactory.getLogger(FreelancerGigController.class);

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;  // JWT utility class
    private final AuthenticationManager authenticationManager;

    public FreelancerGigController(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder, JWTService jwtService, AuthenticationManager authenticationManager) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping(value = "/createGig", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDTO> createGig(
            @RequestPart("gig") String gigJson,
            @RequestPart("image") MultipartFile image,
            @RequestPart("documents") List<MultipartFile> documents,
            @RequestHeader("Authorization") String token
    ) {
        ResponseDTO responseDTO = new ResponseDTO();

        try {
            // Parse JSON string to TutorGig object
            ObjectMapper objectMapper = new ObjectMapper();
            FreelancerGig gig = objectMapper.readValue(gigJson, FreelancerGig.class);

            // Authenticate user
            Optional<AuthUser> userOptional = authenticateUser(token);
            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                logger.warn("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            String username = userOptional.get().getUsername();
            String imgUrl = userOptional.get().getImage();
            gig.setTutorUserName(username);
            gig.setTutorImg(imgUrl);
            gig.setName(userOptional.get().getName());

            if (gig.getCreatedDate() == null || gig.getCreatedDate().isEmpty()) {
                gig.setCreatedDate(LocalDate.now().toString());
            }

            // Save image
            if (image != null && !image.isEmpty()) {
                String imagePath = "C:\\xampp\\htdocs\\myPro\\BuildPro\\BuildProFrontEnd\\images\\gigPics\\" + gig.getId()+"\\" + gig.getId() + "_coverImage.jpg";
                Files.createDirectories(Paths.get(imagePath).getParent());
                Files.write(Paths.get(imagePath), image.getBytes());
                gig.setImage("images\\gigPics\\" +gig.getId() +"\\" + gig.getId() + "_coverImage.jpg");
            }

            // Save documents
            if(documents != null && !documents.isEmpty()){
                List<String> savedDocPaths = new ArrayList<>();
                for (int i = 0; i < documents.size(); i++) {
                    MultipartFile file = documents.get(i);
                    String docFileName = gig.getId() + "_doc" + (i + 1) + ".pdf";
                    Path docPath = Paths.get("C:\\xampp\\htdocs\\myPro\\BuildPro\\BuildProFrontEnd\\documents\\gigDocs\\"+ gig.getId()+"\\" + docFileName);
                    Files.createDirectories(docPath.getParent());
                    Files.write(docPath, file.getBytes());
                    savedDocPaths.add("documents\\gigDocs\\" + gig.getId()+"\\"+ docFileName);
                }
                gig.setDocuments(savedDocPaths);
            }




            // Save to DB
            FreelancerGig newGig = freelancerGigRepository.save(gig);
            logger.info("Gig created successfully: {}", newGig.getId());

            responseDTO.setStatusCode(201);
            responseDTO.setMessage("Gig created successfully");
            responseDTO.setData(newGig);
            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);

        } catch (Exception ex) {
            logger.error("Error creating gig", ex);
            responseDTO.setStatusCode(500);
            responseDTO.setMessage("Internal server error");
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/getPersonalGig")
    public ResponseEntity<ResponseDTO<List<FreelancerGig>>> getUserGigsByToken(
            @RequestHeader("Authorization") String token
    ) {
        ResponseDTO<List<FreelancerGig>> responseDTO = new ResponseDTO<>();

        try {
            // 1. Extract username from JWT
            String tokenUsername = jwtService.extractUsername(token);

            // 2. Authenticate user
            Optional<AuthUser> authenticatedUser = authUserRepository.findByUsername(tokenUsername);
            if (!authenticatedUser.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                logger.warn("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            // 3. Get the user's display name (e.g., full name)
            String displayName = authenticatedUser.get().getName(); // or any other display name field
            String imageUrl = authenticatedUser.get().getImage();

            // 4. Fetch user gigs
            List<FreelancerGigDTO> userGigs = freelancerGigRepository.findAllByTutorUserName(tokenUsername);
            if (userGigs.isEmpty()) {
                logger.warn("No gigs found for user {}", tokenUsername);
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("No gigs found for the user");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            // 5. Replace tutorUserName in each gig with display name
            for (FreelancerGigDTO gig : userGigs) {
                gig.setTutorUserName(displayName);
                gig.setUserImage(imageUrl);
            }

            // 6. Return modified gigs
            logger.info("Gigs fetched successfully for user {}", tokenUsername);
            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Gigs fetched successfully");
            responseDTO.setData(userGigs);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error fetching gigs for user", e);
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("An error occurred while fetching the gigs");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


//    @GetMapping("/getAllGigs")
//    public ResponseEntity<ResponseDTO<List<FreelancerGig>>> getAllGigsByToken(
//            @RequestHeader("Authorization") String token
//    ) {
//        ResponseDTO<List<FreelancerGig>> responseDTO = new ResponseDTO<>();
//
//        try {
//            // 1. Extract username from JWT
//            String tokenUsername = jwtService.extractUsername(token);
//
//            // 2. Authenticate user
//            Optional<AuthUser> authenticatedUser = authUserRepository.findByUsername(tokenUsername);
//            if (!authenticatedUser.isPresent()) {
//                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
//                responseDTO.setMessage("Invalid or expired token");
//                logger.warn("Invalid or expired token");
//                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
//            }
//
////            // 3. Get the user's display name (e.g., full name)
////            String displayName = authenticatedUser.get().getName(); // or any other display name field
////            String imageUrl = authenticatedUser.get().getImage();
//
//            // 4. Fetch user gigs
//            List<FreelancerGig> userGigs = freelancerGigRepository.findAll();
//            if (userGigs.isEmpty()) {
//                logger.warn("No gigs found for user {}", tokenUsername);
//                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
//                responseDTO.setMessage("No gigs found for the user");
//                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
//            }
//
//
////            // 5. Replace tutorUserName in each gig with display name
////            for (FreelancerGig gig : userGigs) {
////                gig.setTutorUserName(displayName);
////                gig.setTutorImg(imageUrl);
////            }
//
//            // 6. Return modified gigs
//            logger.info("Gigs fetched successfully for user {}", tokenUsername);
//            responseDTO.setStatusCode(HttpStatus.OK.value());
//            responseDTO.setMessage("Gigs fetched successfully");
//            responseDTO.setData(userGigs);
//            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
//
//        } catch (Exception e) {
//            logger.error("Error fetching gigs for user", e);
//            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
//            responseDTO.setMessage("An error occurred while fetching the gigs");
//            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    @GetMapping("/getAllGigs")
    public ResponseEntity<ResponseDTO<List<FreelancerGig>>> getAllGigsByToken(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "search", required = false) String search
    ) {
        ResponseDTO<List<FreelancerGig>> responseDTO = new ResponseDTO<>();

        try {
            String tokenUsername = jwtService.extractUsername(token);
            Optional<AuthUser> authenticatedUser = authUserRepository.findByUsername(tokenUsername);

            if (!authenticatedUser.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            List<FreelancerGig> gigs;

            if (search != null && !search.isEmpty()) {
                gigs = freelancerGigRepository.searchByKeyword(search.toLowerCase());
            } else {
                gigs = freelancerGigRepository.findAll();
            }

            if (gigs.isEmpty()) {
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("No gigs found");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Gigs fetched successfully");
            responseDTO.setData(gigs);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error fetching gigs", e);
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("An error occurred while fetching the gigs");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/getPersonalGig/{gigId}")
    public ResponseEntity<ResponseDTO<FreelancerGigDTO>> getGigById(
            @RequestHeader("Authorization") String token,
            @PathVariable String gigId
    ) {
        ResponseDTO<FreelancerGigDTO> responseDTO = new ResponseDTO<>();

        try {
            // 1. Validate token and extract username
            String tokenUsername = jwtService.extractUsername(token);
            Optional<AuthUser> authenticatedUser = authUserRepository.findByUsername(tokenUsername);

            if (!authenticatedUser.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            // 2. Fetch gig by ID and ensure it belongs to the authenticated user
            Optional<FreelancerGigDTO> gigOptional = freelancerGigRepository.findByIdAndTutorUserName(gigId, tokenUsername);

            if (!gigOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("Gig not found for user");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            // 3. Add user display name and image
            FreelancerGigDTO gig = gigOptional.get();
            gig.setTutorUserName(authenticatedUser.get().getName());
            gig.setUserImage(authenticatedUser.get().getImage());

            // 4. Return the result
            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Gig fetched successfully");
            responseDTO.setData(gig);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error fetching gig by ID", e);
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("An error occurred while fetching the gig");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getGig/{gigId}")
    public ResponseEntity<ResponseDTO<FreelancerGigDTO>> getSingleGigById(
            @RequestHeader("Authorization") String token,
            @PathVariable String gigId
    ) {
        ResponseDTO<FreelancerGigDTO> responseDTO = new ResponseDTO<>();

        try {
            // 1. Validate token and extract username
            String tokenUsername = jwtService.extractUsername(token);
            Optional<AuthUser> authenticatedUser = authUserRepository.findByUsername(tokenUsername);

            if (!authenticatedUser.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            // 2. Fetch gig by ID and ensure it belongs to the authenticated user
//            Optional<FreelancerGigDTO> gigOptional = freelancerGigRepository.findByIdAndTutorUserName(gigId, tokenUsername);
            Optional<FreelancerGig> gigOptional = freelancerGigRepository.findById(gigId);

            if (!gigOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("Gig not found for user");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            // 3. Add user display name and image
            FreelancerGig gig = gigOptional.get();

            // 4. Return the result
            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Gig fetched successfully");
            responseDTO.setData(gig);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error fetching gig by ID", e);
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("An error occurred while fetching the gig");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @DeleteMapping("/deletePersonalGig/{gigId}")
    public ResponseEntity<ResponseDTO<String>> deleteGigById(
            @RequestHeader("Authorization") String token,
            @PathVariable String gigId
    ) {
        ResponseDTO<String> responseDTO = new ResponseDTO<>();

        try {
            // 1. Validate token
            String tokenUsername = jwtService.extractUsername(token.replace("Bearer ", "").trim());
            Optional<AuthUser> authenticatedUser = authUserRepository.findByUsername(tokenUsername);

            if (!authenticatedUser.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            // 2. Check if the gig exists and belongs to the user
            Optional<FreelancerGigDTO> gigOptional = freelancerGigRepository.findByIdAndTutorUserName(gigId, tokenUsername);
            if (!gigOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("Gig not found or does not belong to the user");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            // 3. Delete the gig
            freelancerGigRepository.deleteById(gigId);

            // 4. Success response
            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Gig deleted successfully");
            responseDTO.setData("Gig ID: " + gigId);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error deleting gig", e);
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("An error occurred while deleting the gig");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping(value = "/updateGig/{gigId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDTO> updateGig(
            @PathVariable String gigId,
            @RequestPart("gig") String gigJson,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents,
            @RequestHeader("Authorization") String token
    ) {
        ResponseDTO responseDTO = new ResponseDTO();
        try {
            // Authenticate user
            String tokenUsername = jwtService.extractUsername(token.replace("Bearer ", "").trim());
            Optional<AuthUser> userOptional = authUserRepository.findByUsername(tokenUsername);

            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            // Fetch existing gig
            Optional<FreelancerGig> existingGigOptional = freelancerGigRepository.findById(gigId);
            if (!existingGigOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("Gig not found");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            FreelancerGig existingGig = existingGigOptional.get();
            if (!existingGig.getTutorUserName().equals(tokenUsername)) {
                responseDTO.setStatusCode(HttpStatus.FORBIDDEN.value());
                responseDTO.setMessage("You are not authorized to update this gig");
                return new ResponseEntity<>(responseDTO, HttpStatus.FORBIDDEN);
            }

            // Update fields from gig JSON
            ObjectMapper objectMapper = new ObjectMapper();
            FreelancerGig updatedGigData = objectMapper.readValue(gigJson, FreelancerGig.class);

            existingGig.setTitle(updatedGigData.getTitle());
            existingGig.setDescription(updatedGigData.getDescription());
            existingGig.setSubject(updatedGigData.getSubject());
            existingGig.setPricePerHour(updatedGigData.getPricePerHour());

            // Update image if provided
            if (image != null && !image.isEmpty()) {
                String imagePath = "C:\\xampp\\htdocs\\myPro\\BuildPro\\BuildProFrontEnd\\images\\gigPics\\"+ existingGig.getId()+ "\\" + existingGig.getId() + "_coverImage.jpg";
                Files.write(Paths.get(imagePath), image.getBytes());
                existingGig.setImage("images\\gigPics\\" + existingGig.getId()+ "\\" + existingGig.getId() + "_coverImage.jpg");
            }

            // Manage document paths
            List<String> documentPaths = new ArrayList<>();


            // Add new documents
            if (documents != null && !documents.isEmpty()) {
                String mainPath = "C:\\xampp\\htdocs\\myPro\\BuildPro\\BuildProFrontEnd\\documents\\gigDocs\\"+gigId;
                Path mainDirPath = Paths.get(mainPath);
                if (Files.exists(mainDirPath)) {
                    try (Stream<Path> walk = Files.walk(mainDirPath)) {
                        walk.sorted(Comparator.reverseOrder())
                                .forEach(path -> {
                                    try {
                                        Files.delete(path);
                                    } catch (IOException e) {
                                        throw new RuntimeException("Failed to delete " + path, e);
                                    }
                                });
                    }
                }

                // Create directory again after deletion
                Files.createDirectories(mainDirPath);

                for (int i = 0; i < documents.size(); i++) {
                    String docPath = "C:\\xampp\\htdocs\\myPro\\BuildPro\\BuildProFrontEnd\\documents\\gigDocs\\"+gigId+"\\" + gigId + "_doc" + (i + 1) + ".pdf";
                    Files.write(Paths.get(docPath), documents.get(i).getBytes());
                    documentPaths.add("documents\\gigDocs\\"+gigId+"\\"  + gigId + "_doc" + (i + 1) + ".pdf");
                }
            }

            existingGig.setDocuments(documentPaths);

            // Save updated gig
            FreelancerGig savedGig = freelancerGigRepository.save(existingGig);

            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Gig updated successfully");
            responseDTO.setData(savedGig);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception ex) {
            logger.error("Error updating gig", ex);
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("Internal server error");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }




    private Optional<AuthUser> authenticateUser(String token) {
        String username = jwtService.extractUsername(token);
        return authUserRepository.findByUsername(username)
                .filter(user -> jwtService.isTokenValid(token, user));
    }




}
