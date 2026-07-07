package com.nexus.NeuroForge.services;


import com.nexus.NeuroForge.models.interfaces.Role;
import com.nexus.NeuroForge.models.Team;
import com.nexus.NeuroForge.models.User;
import com.nexus.NeuroForge.repositories.TeamRepository;
import com.nexus.NeuroForge.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeamRepository teamRepository;


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
