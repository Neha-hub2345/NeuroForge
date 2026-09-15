// Backend/src/main/java/com/nexus/NeuroForge/services/security/ProjectAccessGuard.java
package com.nexus.NeuroForge.services.security;

import com.nexus.NeuroForge.models.interfaces.Role;
import com.nexus.NeuroForge.models.project.Project;
import com.nexus.NeuroForge.models.user.User;
import com.nexus.NeuroForge.repositories.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class ProjectAccessGuard {

    @Autowired private UserRepository userRepository;

    /** Throws 403 unless the caller is ADMIN or belongs to the project's team. */
    public void assertMember(Jwt jwt, Project project) {
        User user = userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new AccessDeniedException("User not recognized"));

        if (user.getRole() == Role.ADMIN) return;

        if (project == null || project.getTeam() == null
                || user.getTeam() == null
                || !project.getTeam().getId().equals(user.getTeam().getId())) {
            throw new AccessDeniedException("You are not a member of this project's team");
        }
    }
}