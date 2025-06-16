package com.example.buildPro.controller;


import com.example.buildPro.bean.LoginDTO;
import com.example.buildPro.bean.ResponseDTO;
import com.example.buildPro.entity.AuthUser;
import com.example.buildPro.repository.AuthUserRepository;
import com.example.buildPro.service.EmailService;
import com.example.buildPro.service.JWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:8012")
@RestController
@RequestMapping("/users")
public class AuthUserController {

    private static final Logger logger = LoggerFactory.getLogger(AuthUserController.class);

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;  // JWT utility class
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthUserController(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder, JWTService jwtService, AuthenticationManager authenticationManager, EmailService emailService) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    private Optional<AuthUser> authenticateUser(String token) {
        String username = jwtService.extractUsername(token);
        return authUserRepository.findByUsername(username)
                .filter(user -> jwtService.isTokenValid(token, user));
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseDTO<AuthUser>> registerUser(
            @RequestParam("name") String name,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("birthday") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date birthday,
            @RequestParam("role") String role,
            @RequestParam(value = "image", required = false) String base64Image) {

        ResponseDTO<AuthUser> responseDTO = new ResponseDTO<>();

        try {
            logger.info("Attempting to register user: {}", username);

            // Check if the username already exists
            if (authUserRepository.findByUsername(username).isPresent()) {
                logger.warn("Username already exists: {}", username);
                responseDTO.setStatusCode(400);
                responseDTO.setMessage("Username already exists");
                return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
            }
            String imagePathView = "images\\profilePics\\" + username + "_profileImage.jpg";
            String imagePath = "C:\\xampp\\htdocs\\myPro\\BuildPro\\BuildProFrontEnd\\images\\profilePics\\" + username + "_profileImage.jpg";



            if (base64Image != null && !base64Image.isEmpty()) {
                try {
                    // Remove the base64 prefix if present (e.g., data:image/jpeg;base64,...)
                    if (base64Image.contains(",")) {
                        base64Image = base64Image.split(",")[1];
                    }

                    // Decode base64 string to byte array
                    byte[] imageBytes = Base64.getDecoder().decode(base64Image);

                    // Define image file path
                    Path destinationFile = Paths.get(imagePath);

                    // Create parent directories if they don't exist
                    Files.createDirectories(destinationFile.getParent());

                    // Write to file
                    Files.write(destinationFile, imageBytes);


                } catch (IOException e) {
                    logger.error("Error saving image file for user: {}", username, e);
                    responseDTO.setStatusCode(500);
                    responseDTO.setMessage("User created, but image saving failed");
                    return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }


            // Create and save new user with the base64 image string if provided
            AuthUser newUser = AuthUser.builder()
                    .name(name)
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .birthday(birthday.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                    .image(imagePathView) // Save the base64 string in the image field
                    .active(true)
                    .role(role)
                    .about(null)
                    .contactNo(null)
                    .experience(null)
                    .location(null)
                    .subject(null)
                    .build();
            authUserRepository.save(newUser);

            String subject = "Welcome to BuildPro – Let’s Build Your Success Together!";
            String message =
                    "Dear " + newUser.getName() + ",\n\n" +
                            "Welcome to BuildPro!\n\n" +
                            "We’re excited to have you on board. Your registration has been successfully completed, and you are now part of a growing community connecting skilled construction professionals with clients who need trusted services.\n\n" +
                            "🔧 What’s Next?\n" +
                            "- Log in to your dashboard to create your service listings or browse available projects\n" +
                            "- Check your inbox regularly for job requests, updates, and platform tips\n" +
                            "- Reach out to us any time if you have questions or need assistance\n\n" +
                            "Once again, welcome to BuildPro. We look forward to supporting your journey in the construction freelancing industry!\n\n" +
                            "Best regards,\n" +
                            "The BuildPro Team\n" +
                            "📞 +94-11-1234567\n" +
                            "📧 support@buildpro.lk\n" +
                            "🌐 www.buildpro.lk";



            emailService.sendEmail(newUser.getEmail(), subject,message);

            logger.info("User registered successfully: {}", username);
            responseDTO.setStatusCode(201);
            responseDTO.setMessage("User registered successfully");
            responseDTO.setData(newUser);
            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);

        } catch (Exception e) {
            logger.error("Error registering user: {}", username, e);
            responseDTO.setStatusCode(500);
            responseDTO.setMessage("An error occurred while registering the user");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/update")
    public ResponseEntity<ResponseDTO<AuthUser>> updateUserDetails(
            @RequestParam("username") String username,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "contactNo", required = false) String contactNo,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "about", required = false) String about,
            @RequestParam(value = "subject", required = false) List<String> subject,
            @RequestParam(value = "birthday", required = false) @DateTimeFormat(pattern = "MM/dd/yyyy") LocalDate birthday,
            @RequestParam(value = "image", required = false) String base64Image) {



        ResponseDTO<AuthUser> responseDTO = new ResponseDTO<>();

        try {
            logger.info("Attempting to update user: {}", username);

            // Fetch the existing user by username
            Optional<AuthUser> existingUserOpt = authUserRepository.findByUsername(username);
            if (!existingUserOpt.isPresent()) {
                logger.warn("User not found: {}", username);
                responseDTO.setStatusCode(404);
                responseDTO.setMessage("User not found");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            AuthUser existingUser = existingUserOpt.get();
            String imagePathView = "images\\profilePics\\" + username + "_profileImage.jpg";
            String imagePath = "C:\\xampp\\htdocs\\myPro\\BuildPro\\BuildProFrontEnd\\images\\profilePics\\" + username + "_profileImage.jpg";

            if (base64Image != null && !base64Image.isEmpty()) {
                try {
                    // Remove the base64 prefix if present (e.g., data:image/jpeg;base64,...)
                    if (base64Image.contains(",")) {
                        base64Image = base64Image.split(",")[1];
                    }

                    // Decode base64 string to byte array
                    byte[] imageBytes = Base64.getDecoder().decode(base64Image);

                    // Define image file path
                    Path destinationFile = Paths.get(imagePath);

                    // Create parent directories if they don't exist
                    Files.createDirectories(destinationFile.getParent());

                    // Write to file
                    Files.write(destinationFile, imageBytes);


                } catch (IOException e) {
                    logger.error("Error saving image file for user: {}", username, e);
                    responseDTO.setStatusCode(500);
                    responseDTO.setMessage("User created, but image saving failed");
                    return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            existingUser.setName(name);
            existingUser.setContactNo(contactNo);
            existingUser.setEmail(email);
            existingUser.setLocation(location);
            existingUser.setExperience(experience);
            existingUser.setAbout(about);
            existingUser.setSubject(subject);
            existingUser.setBirthday(birthday);
            existingUser.setImage(imagePathView);

//            // Update the user with the new data
//            existingUser.setImage(imageFilename); // Update the image filename if a new image was uploaded
            authUserRepository.save(existingUser);

            logger.info("User updated successfully: {}", username);
            responseDTO.setStatusCode(200);
            responseDTO.setMessage("User updated successfully");
//            responseDTO.setData(existingUser);
            responseDTO.setData(Map.of("user", existingUser));
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);


//            // Update fields only if they are provided
//            if (email != null) {
//                existingUser.setEmail(email);
//            }
////
////            if (password != null) {
////                existingUser.setPassword(passwordEncoder.encode(password)); // Encode the new password
////            }
//
//            if (birthday != null) {
//                existingUser.setBirthday(birthday);
//            }

//            String imageFilename = existingUser.getImage(); // Keep existing image by default
//            if (image != null && !image.isEmpty()) {
//                try {
//                    // Define the path where you want to save the image
//                    String uploadDir = "D:/ESOFT/RemindifyApp/backend/images/profileImages"; // Change this to your desired path
//
//                    // Create the directory if it doesn't exist
//                    File directory = new File(uploadDir);
//                    if (!directory.exists()) {
//                        directory.mkdirs();
//                    }
//
//                    // Save the new file to the specified path
//                    imageFilename = username + "_" + image.getOriginalFilename(); // Create a unique filename if necessary
//                    File fileToSave = new File(directory, imageFilename);
//                    image.transferTo(fileToSave); // Save the file
//
//                } catch (IOException e) {
//                    logger.error("Error processing image for user: {}", username, e);
//                    responseDTO.setStatusCode(400);
//                    responseDTO.setMessage("Error processing image");
//                    return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
//                }
//            }



        } catch (Exception e) {
            logger.error("Error updating user: {}", username, e);
            responseDTO.setStatusCode(500);
            responseDTO.setMessage("An error occurred while updating the user");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> loginUser(@RequestBody @Valid LoginDTO loginDTO) {
        ResponseDTO<Map<String, Object>> responseDTO = new ResponseDTO<>();

        try {
            logger.info("Attempting to login user: {}", loginDTO.getUsername());

            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword()));

            if (authentication.isAuthenticated()) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                Optional<AuthUser> optionalAuthUser = authUserRepository.findByUsername(userDetails.getUsername());

                if (!optionalAuthUser.isPresent()) {
                    responseDTO.setStatusCode(404);
                    responseDTO.setMessage("User not found");
                    return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
                }

                AuthUser authUser = optionalAuthUser.get();
                String token = jwtService.generateToken(authUser);

                // Build the image URL based on username
                String imageUrl = authUser.getImage();

                responseDTO.setStatusCode(200);
                responseDTO.setMessage("Login successful");
                responseDTO.setData(Map.of("user", authUser, "token", token, "imageUrl", imageUrl));
                return new ResponseEntity<>(responseDTO, HttpStatus.OK);
            } else {
                responseDTO.setStatusCode(401);
                responseDTO.setMessage("Invalid credentials");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

        } catch (AuthenticationException e) {
            responseDTO.setStatusCode(500);
            responseDTO.setMessage("Invalid credentials for user");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/allUsers")
    public ResponseEntity<ResponseDTO<List<AuthUser>>> getAllUsers(@RequestHeader("Authorization") String token) {
        ResponseDTO<List<AuthUser>> responseDTO = new ResponseDTO<>();

        try {
            // Remove the "Bearer " prefix from the token
            String jwtToken = token.replace("Bearer ", "");

            // Authenticate the user using the token
            Optional<AuthUser> userOptional = authenticateUser(jwtToken);

            // If user authentication fails, return an unauthorized response
            if (!userOptional.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                logger.warn("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            // Get the currently logged-in user
            AuthUser loggedInUser = userOptional.get();
            String loggedInUsername = loggedInUser.getUsername();

            // Fetch all users from the repository
            List<AuthUser> users = authUserRepository.findAll();

            // Filter out the currently logged-in user from the list
            List<AuthUser> filteredUsers = users.stream()
                    .filter(user -> !user.getUsername().equals(loggedInUsername))
                    .collect(Collectors.toList());

            // If no other users are found, return a not found response
            if (filteredUsers.isEmpty()) {
                logger.warn("No other users found");
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("No other users found");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            // Users found, return them in the response
            logger.info("Users fetched successfully");
            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("Users fetched successfully");
            responseDTO.setData(filteredUsers);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            // Handle any unexpected exceptions
            logger.error("Error fetching users", e);
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("An error occurred while fetching users");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{username}")
    public ResponseEntity<ResponseDTO<AuthUser>> getUserByUsername(
            @PathVariable("username") String username,
            @RequestHeader("Authorization") String token
    ) {
        ResponseDTO<AuthUser> responseDTO = new ResponseDTO<>();

        try {
            // Extract the username from the JWT token
            String tokenUsername = jwtService.extractUsername(token);

            // Authenticate the user using the token
            Optional<AuthUser> authenticatedUser = authUserRepository.findByUsername(tokenUsername);

            // If user authentication fails, return an unauthorized response
            if (!authenticatedUser.isPresent()) {
                responseDTO.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                responseDTO.setMessage("Invalid or expired token");
                logger.warn("Invalid or expired token");
                return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
            }

            // Fetch the requested user by username from the path variable
            Optional<AuthUser> user = authUserRepository.findByUsername(username);

            // If the requested user is not found, return a not found response
            if (!user.isPresent()) {
                logger.warn("User with username {} not found", username);
                responseDTO.setStatusCode(HttpStatus.NOT_FOUND.value());
                responseDTO.setMessage("User not found");
                return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
            }

            // User found, return user details in the response
            logger.info("User fetched successfully");
            responseDTO.setStatusCode(HttpStatus.OK.value());
            responseDTO.setMessage("User fetched successfully");
            responseDTO.setData(user.get());
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);

        } catch (Exception e) {
            // Handle any unexpected exceptions
            logger.error("Error fetching user by username", e);
            responseDTO.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setMessage("An error occurred while fetching the user");
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        jwtService.blacklistToken(jwtToken);
        return ResponseEntity.ok("Logged out successfully");
    }
}
