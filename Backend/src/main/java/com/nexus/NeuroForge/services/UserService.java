package com.nexus.NeuroForge.services;


import com.nexus.NeuroForge.models.Role;
import com.nexus.NeuroForge.models.Team;
import com.nexus.NeuroForge.models.User;
import com.nexus.NeuroForge.repositories.TeamRepository;
import com.nexus.NeuroForge.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeamRepository teamRepository;


    private Role extractRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || realmAccess.get("roles") == null) {
            return Role.DEVELOPER; // fallback default
        }

        List<String> roles = (List<String>) realmAccess.get("roles");

        // Pick the "highest" matching role based on your enum priority
        if (roles.contains("ADMIN")) return Role.ADMIN;
        if (roles.contains("PROJECT_MANAGER")) return Role.PROJECT_MANAGER;
        if (roles.contains("TESTER")) return Role.TESTER;
        if(roles.contains("DEVOPS_ENGINEER")) return Role.DEVOPS_ENGINEER;
        if (roles.contains("DEVELOPER")) return Role.DEVELOPER;

        return Role.DEVELOPER; // fallback default
    }


    //For Keyclock method create user
    @Transactional
    public User getorCreateUser(Jwt jwt) {
        String keycloakId = jwt.getSubject();

        return userRepository.findByKeycloakId(keycloakId).orElseGet(() -> {
            User newUser = new User();
            newUser.setKeycloakId(keycloakId);
            newUser.setUsername(jwt.getClaimAsString("preferred_username"));
            newUser.setEmail(jwt.getClaimAsString("email"));
            newUser.setRole(extractRole(jwt));


            return userRepository.save(newUser);
        });
    }

    @Transactional
    public User assignRole(long userId,Role role){
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found in local database"));

        currentUser.setRole(role);
        return userRepository.save(currentUser);
    }



    @Transactional
    public void assignUserToTeam(Long userId, Long teamId) {
        // Fetch by your DB primary key (the Long id)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with ID: " + teamId));

        user.setTeam(team);
        userRepository.save(user);
    }

}
