package com.lliscano.eia.security;

import com.lliscano.commons.components.RequestContextHolder;
import com.lliscano.commons.dtos.RequestContextData;
import com.lliscano.eia.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("projectSecurity")
@RequiredArgsConstructor
@Slf4j
public class ProjectSecurity {

    private final ProjectMemberRepository projectMemberRepository;

    public boolean isProjectLead(String projectUuid) {
        if (projectUuid == null || projectUuid.isBlank()) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "EIA_ADMIN".equals(a.getAuthority()) || "ROLE_EIA_ADMIN".equals(a.getAuthority()))) {
            return true;
        }

        RequestContextData context = RequestContextHolder.getContext();
        if (context == null) {
            return false;
        }

        String userUuid = context.getUuid();
        String username = context.getSub();

        return projectMemberRepository.existsByProjectUuidAndUserIdentifierAndRole(
                projectUuid, userUuid, username, "PROJECT_LEAD");
    }
}
