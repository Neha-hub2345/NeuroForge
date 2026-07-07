package com.nexus.NeuroForge.config;

import com.nexus.NeuroForge.models.interfaces.Role;
import com.nexus.NeuroForge.models.User;
import com.nexus.NeuroForge.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UserSyncFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    // Note: Removed @Autowired on the field since you are using constructor injection.
    // Spring automatically injects dependencies into the constructor.
    public UserSyncFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user is authenticated via JWT
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            var jwt = jwtAuth.getToken();

            // Extract claims
            String keycloakId = jwt.getSubject(); // The unique Keycloak ID
            String username = jwt.getClaimAsString("preferred_username");
            String email = jwt.getClaimAsString("email");

            // 1. Extract role as a raw String first
            Object roleObj = jwt.getClaim("app_role");
            String roleStr = roleObj instanceof java.util.List ?
                    ((java.util.List<?>) roleObj).get(0).toString() :
                    String.valueOf(roleObj);

            // Sync with local database
            User user = userRepository.findByKeycloakId(keycloakId)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setKeycloakId(keycloakId);
                        return newUser;
                    });

            user.setUsername(username);
            user.setEmail(email);

            // 2. Safely convert the String to your Enum
            try {
                if (roleStr != null && !roleStr.equals("null")) {
                    // Convert to uppercase to match standard Enum naming conventions (e.g., ADMIN, DEVELOPER)
                    user.setRole(Role.valueOf(roleStr.toUpperCase()));
                }
            } catch (IllegalArgumentException e) {
                // If Keycloak sends a role that doesn't exist in your Enum, it lands here.
                // You can either leave the role as is, or set a fallback default role.
                System.err.println("Warning: Unknown role received from Keycloak: " + roleStr);
                // user.setRole(Role.DEFAULT_USER); // Optional fallback
            }

            userRepository.save(user);
        }

        // Continue the request chain
        filterChain.doFilter(request, response);
    }
}