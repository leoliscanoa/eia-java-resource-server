package com.lliscano.eia.security;

import com.lliscano.commons.components.RequestContextHolder;
import com.lliscano.commons.dtos.RequestContextData;
import com.lliscano.eia.repository.ProjectMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectSecurityTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectSecurity projectSecurity;

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("isProjectLead - EIA_ADMIN siempre tiene acceso")
    void isProjectLead_Admin_ReturnsTrue() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a", List.of(new SimpleGrantedAuthority("EIA_ADMIN"))));

        boolean result = projectSecurity.isProjectLead("proj-1");

        assertTrue(result);
        verifyNoInteractions(projectMemberRepository);
    }

    @Test
    @DisplayName("isProjectLead - usuario con rol PROJECT_LEAD retorna true")
    void isProjectLead_LeadMember_ReturnsTrue() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("leaduser", "n/a", List.of(new SimpleGrantedAuthority("EIA_USER"))));
        RequestContextHolder.setContext(RequestContextData.builder()
                .uuid("user-lead-uuid")
                .sub("leaduser")
                .build());

        when(projectMemberRepository.existsByProjectUuidAndUserIdentifierAndRole(
                "proj-1", "user-lead-uuid", "leaduser", "PROJECT_LEAD")).thenReturn(true);

        boolean result = projectSecurity.isProjectLead("proj-1");

        assertTrue(result);
    }

    @Test
    @DisplayName("isProjectLead - usuario sin rol PROJECT_LEAD retorna false")
    void isProjectLead_NonLead_ReturnsFalse() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("analyst", "n/a", List.of(new SimpleGrantedAuthority("EIA_USER"))));
        RequestContextHolder.setContext(RequestContextData.builder()
                .uuid("user-analyst-uuid")
                .sub("analyst")
                .build());

        when(projectMemberRepository.existsByProjectUuidAndUserIdentifierAndRole(
                "proj-1", "user-analyst-uuid", "analyst", "PROJECT_LEAD")).thenReturn(false);

        boolean result = projectSecurity.isProjectLead("proj-1");

        assertFalse(result);
    }

    @Test
    @DisplayName("isProjectLead - uuid nulo retorna false inmediatamente")
    void isProjectLead_NullUuid_ReturnsFalse() {
        assertFalse(projectSecurity.isProjectLead(null));
        assertFalse(projectSecurity.isProjectLead("   "));
    }
}
