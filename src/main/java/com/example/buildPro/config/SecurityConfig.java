package com.example.buildPro.config;


import com.example.buildPro.service.AuthUserDetailsService;
import com.example.buildPro.service.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthUserDetailsService authUserDetailsService;
    private final JWTService jwtService;

    public SecurityConfig(AuthUserDetailsService authUserDetailsService, JWTService jwtService) {
        this.authUserDetailsService = authUserDetailsService;
        this.jwtService = jwtService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(csrf -> csrf.disable()) // Disable CSRF for API; consider enabling for web apps
                .cors() // Enable CORS globally
                .and()
                .authorizeHttpRequests(auth -> auth
                        // WebSocket endpoints
                        .requestMatchers("/ws-chat/**").permitAll()
                        .requestMatchers("/topic/**").permitAll()
                        .requestMatchers("/app/chat/**").permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/users/register",
                                "/users/login",
                                "/users/logout",
                                "/users/update",
                                "/gigs/createGig",
                                "/chatBot/{userId}/send",
                                "/chatBot/{userId}/ai-response",
                                "/tutorPost/createPost",
                                "tutorPost/post/{postId}/comment",
                                "tutorPost/post/{postId}/like",
                                "discussion/create",
                                "/discussion/{discussionId}/like",
                                "/discussion/{discussionId}/comment",
                                "/discussion/{discussionId}/comment/{commentIndex}/reply",
                                "/email/send",
                                "/customerContact/contact",
                                "/chat/send") // Permit all POST requests for these endpoints
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/users/allUsers",
                                "/users/{username}",
                                "/gigs/getPersonalGig",
                                "/gigs/getAllGigs",
                                "/gigs/getPersonalGig/{gigId}",
                                "/gigs/getGig/{gigId}",
                                "/chatBot/{userId}",
                                "/tutorPost/myActivePosts",
                                "/tutorPost/post/{postId}",
                                "/tutorPost/allActivePosts",
                                "/discussion/all",
                                "/discussion/by-user/{username}", // Permit all GET requests for these endpoints
                                "/discussion/getById/{id}",
                                "discussion/getByUser",
                                "/chat/search",
                                "/chat/messages",
                                "/chat/conversations") // Permit all GET requests for these endpoints
                        .permitAll()
                        .requestMatchers(HttpMethod.PUT, "/reminders/update/{id}", "/summerizeText/{id}", "/gigs/updateGig/{gigId}").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/gigs/deletePersonalGig/{gigId}").permitAll()
                        .anyRequest().authenticated()) // All other requests require authentication
                .userDetailsService(authUserDetailsService) // Custom UserDetailsService for authentication
                .logout(logout -> logout
                        .logoutUrl("/users/logout") // Logout URL
                        .logoutSuccessUrl("/users/login") // Redirect after successful logout
                        .invalidateHttpSession(true) // Invalidate session on logout
                        .deleteCookies("JSESSIONID") // Optionally delete session cookies
                        .addLogoutHandler((request, response, authentication) -> {
                            // Blacklist JWT token on logout
                            String token = extractTokenFromRequest(request);
                            if (token != null) {
                                jwtService.blacklistToken(token);
                            }
                        }));

        return httpSecurity.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(authUserDetailsService);
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Password encoder for security
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // Extract JWT token from request
    private String extractTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7); // Remove "Bearer " prefix
        }
        return null;
    }
}
