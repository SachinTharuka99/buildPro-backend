package com.example.buildPro.controller;


import com.example.buildPro.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customerContact/contact")
@CrossOrigin // allows frontend calls
public class ContactController {
    @Autowired
    private EmailService emailService;

    @PostMapping
    public ResponseEntity<String> sendContactMessage(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String subject,
            @RequestParam String message) {

        String subjectMail = "You Have a message from "+ name;
        String myEmail = "sachinalawaththa.99@gmail.com";
        String subjectBody =
                "Dear Admin,\n\n" +
                        "You have received a new inquiry from the BuilPro website contact form.\n\n" +
                        "Details are as follows:\n" +
                        "----------------------------------------\n" +
                        "Name    : " + name + "\n" +
                        "Email   : " + email + "\n" +
                        "Subject : " + subject + "\n" +
                        "Message :\n" + message + "\n" +
                        "----------------------------------------\n\n" +
                        "Please respond to the user as soon as possible.\n\n" +
                        "Regards,\n" +
                        "BuilPro System";

                emailService.sendEmail(myEmail, subjectMail, subjectBody);




//        Thnak You Reply
        String thanksSubject = "Thank You for Contacting Us!";
        String thanksMessage = "Hi " + name + ",\n\n"
                + "Thank you for reaching out to us. We have received your message:\n"
                + "-----------------------------------------------------\n"
                + "Subject: " + subject + "\n"
                + "Message: " + message + "\n"
                + "-----------------------------------------------------\n\n"
                + "We’ll get back to you shortly.\n\n"
                + "Best regards,\nThe Support Team";


        emailService.sendEmail(email, thanksSubject, thanksMessage);
        return ResponseEntity.ok("Message sent successfully!");
    }
}
