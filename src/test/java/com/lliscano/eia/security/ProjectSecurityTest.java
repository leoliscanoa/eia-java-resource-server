package com.lliscano.eia.security;

import com.lliscano.commons.components.RequestContextHolder;
import com.lliscano.commons.dtos.RequestContextData;
import com.lliscano.eia.model.entity.ProjectMember;
import com.lliscano.eia.repository.ProjectMemberRepository;
import com.lliscano.eia.service.ProjectRoleService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectSecurityTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectRoleService projectRoleService;

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
        verifyNoInteractions(projectRoleService);
    }

    @Test
    @DisplayName("isProjectLead - usuario con rol lead dinámico retorna true")
    void isProjectLead_LeadMember_ReturnsTrue() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("leaduser", "n/a", List.of(new SimpleGrantedAuthority("EIA_USER"))));
        RequestContextHolder.setContext(RequestContextData.builder()
                .uuid("user-lead-uuid")
                .sub("leaduser")
                .build());

        ProjectMember member = ProjectMember.builder()
                .projectRole("PROJECT_LEAD")
                .isActive(true)
                .build();

        when(projectMemberRepository.findActiveMemberByProjectUuidAndUserIdentifier("proj-1", "user-lead-uuid", "leaduser"))
                .thenReturn(Optional.of(member));
        when(projectRoleService.isLeadRole("PROJECT_LEAD")).thenReturn(true);

        boolean result = projectSecurity.isProjectLead("proj-1");

        assertTrue(result);
    }

    @Test
    @DisplayName("isProjectLead - usuario sin rol lead retorna false")
    void isProjectLead_NonLead_ReturnsFalse() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("analyst", "n/a", List.of(new SimpleGrantedAuthority("EIA_USER"))));
        RequestContextHolder.setContext(RequestContextData.builder()
                .uuid("user-analyst-uuid")
                .sub("analyst")
                .build());

        ProjectMember member = ProjectMember.builder()
                .projectRole("ANALYST")
                .isActive(true)
                .build();

        when(projectMemberRepository.findActiveMemberByProjectUuidAndUserIdentifier("proj-1", "user-analyst-uuid", "analyst"))
                .thenReturn(Optional.of(member));
        when(projectRoleService.isLeadRole("ANALYST")).thenReturn(false);

        boolean result = projectSecurity.isProjectLead("proj-1");

        assertFalse(result);
    }

    @Test
    @DisplayName("isProjectLead - miembro no encontrado retorna false")
    void isProjectLead_MemberNotFound_ReturnsFalse() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("other", "n/a", List.of(new SimpleGrantedAuthority("EIA_USER"))));
        RequestContextHolder.setContext(RequestContextData.builder()
                .uuid("other-uuid")
                .sub("other")
                .build());

        when(projectMemberRepository.findActiveMemberByProjectUuidAndUserIdentifier("proj-1", "other-uuid", "other"))
                .thenReturn(Optional.empty());

        boolean result = projectSecurity.isProjectLead("proj-1");

        assertFalse(result);
        verifyNoInteractions(projectRoleService);
    }

    @Test
    @DisplayName("isProjectLead - uuid nulo retorna false inmediatamente")
    void isProjectLead_NullUuid_ReturnsFalse() {
        assertFalse(projectSecurity.isProjectLead(null));
        assertFalse(projectSecurity.isProjectLead("   "));
    }
}
