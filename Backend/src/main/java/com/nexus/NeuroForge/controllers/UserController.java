package com.nexus.NeuroForge.controllers;

import com.nexus.NeuroForge.models.Role;
import com.nexus.NeuroForge.models.User;
import com.nexus.NeuroForge.repositories.UserRepository;
import com.nexus.NeuroForge.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    UserService userService;
    @Autowired
    UserRepository user;

    //anyone can access
    @PostMapping("/create")
    public ResponseEntity<?> createUser(@AuthenticationPrincipal Jwt jwt) {
        User currentUser = userService.getorCreateUser(jwt);
        return ResponseEntity.ok("User created successfully");
    }

    //only admin route , admin can assign any role to any user
    @PostMapping("/{id}/assignRole")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assigns(@PathVariable Long userId, @RequestBody Role role){
        User cuurentUser=userService.assignRole(userId,role);
        return ResponseEntity.status(200).body("User Role updated");
    }

    @PostMapping("/{userId}/assignTeam")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<?> assignTeam(@PathVariable Long userId, @RequestBody Long teamId) {
        userService.assignUserToTeam(userId, teamId);
        return ResponseEntity.ok("User ID " + userId + " successfully assigned to team " + teamId);
    }



}
