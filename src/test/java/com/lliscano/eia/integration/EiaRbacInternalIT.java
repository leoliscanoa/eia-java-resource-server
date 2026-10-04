package com.lliscano.eia.integration;

import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.eia.base.AbstractBaseIntegrationIT;
import com.lliscano.eia.controller.EiaRbacInternalController;
import com.lliscano.eia.model.dto.response.EiaRbacProfileDTO;
import com.lliscano.eia.model.entity.Project;
import com.lliscano.eia.model.entity.ProjectMember;
import com.lliscano.eia.repository.ProjectMemberRepository;
import com.lliscano.eia.repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EiaRbacInternalIT extends AbstractBaseIntegrationIT {

    @Autowired
    private EiaRbacInternalController eiaRbacInternalController;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    private static final String TENANT_ID = "tenant-eia-it";
    private Project project1;
    private Project project2;

    @BeforeEach
    void setUp() {
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();

        project1 = projectRepository.save(Project.builder()
                .tenantId(TENANT_ID)
                .uuid(UUID.randomUUID().toString())
                .code("PRJ-RBAC-01")
                .name("Proyecto Alfa ReBAC")
                .sector("ENERGIA")
                .status("ACTIVE")
                .isDeleted(false)
                .createdBy("admin")
                .build());

        project2 = projectRepository.save(Project.builder()
                .tenantId(TENANT_ID)
                .uuid(UUID.randomUUID().toString())
                .code("PRJ-RBAC-02")
                .name("Proyecto Beta ReBAC")
                .sector("MINERIA")
                .status("ACTIVE")
                .isDeleted(false)
                .createdBy("admin")
                .build());
    }

    @AfterEach
    void tearDown() {
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    @DisplayName("given_userWithAdminRoleInProjectMember_when_getEiaRbacProfile_then_returnsIsEiaAdminTrue")
    void given_userWithAdminRoleInProjectMember_when_getEiaRbacProfile_then_returnsIsEiaAdminTrue() {
        String adminUserUuid = "admin-user-uuid-999";

        projectMemberRepository.save(ProjectMember.builder()
                .project(project1)
                .userUuid(adminUserUuid)
                .projectRole("ADMIN")
                .isActive(true)
                .isDeleted(false)
                .build());

        ResponseEntity<ResponseDTO<EiaRbacProfileDTO>> response =
                eiaRbacInternalController.getEiaRbacProfile(adminUserUuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().isEiaAdmin()).isTrue();
        assertThat(response.getBody().getData().getAssignedProjectIds()).contains(project1.getUuid());
    }

    @Test
    @DisplayName("given_userWithProjectLeadRoleInOneProject_when_getEiaRbacProfile_then_returnsIsEiaAdminFalseAndProjectUuid")
    void given_userWithProjectLeadRoleInOneProject_when_getEiaRbacProfile_then_returnsIsEiaAdminFalseAndProjectUuid() {
        String leadUserUuid = "lead-user-uuid-111";

        projectMemberRepository.save(ProjectMember.builder()
                .project(project1)
                .userUuid(leadUserUuid)
                .projectRole("PROJECT_LEAD")
                .isActive(true)
                .isDeleted(false)
                .build());

        ResponseEntity<ResponseDTO<EiaRbacProfileDTO>> response =
                eiaRbacInternalController.getEiaRbacProfile(leadUserUuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().isEiaAdmin()).isFalse();
        assertThat(response.getBody().getData().getAssignedProjectIds())
                .containsExactly(project1.getUuid());
    }

    @Test
    @DisplayName("given_userWithNoAssignedProjects_when_getEiaRbacProfile_then_returnsEmptyList")
    void given_userWithNoAssignedProjects_when_getEiaRbacProfile_then_returnsEmptyList() {
        String unknownUserUuid = "unknown-user-uuid-000";

        ResponseEntity<ResponseDTO<EiaRbacProfileDTO>> response =
                eiaRbacInternalController.getEiaRbacProfile(unknownUserUuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().isEiaAdmin()).isFalse();
        assertThat(response.getBody().getData().getAssignedProjectIds()).isEmpty();
    }
}
