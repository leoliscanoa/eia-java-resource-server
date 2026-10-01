package com.lliscano.eia.service;

import com.lliscano.commons.components.RequestContextHolder;
import com.lliscano.commons.dtos.AppUserSummaryDTO;
import com.lliscano.commons.dtos.RequestContextData;
import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.commons.exceptions.RecordNotFoundException;
import com.lliscano.eia.mapper.ProjectMemberMapper;
import com.lliscano.eia.model.dto.request.ProjectMemberAssignDTO;
import com.lliscano.eia.model.dto.response.ProjectMemberResponseDTO;
import com.lliscano.eia.model.entity.Project;
import com.lliscano.eia.model.entity.ProjectMember;
import com.lliscano.eia.model.entity.ProjectRole;
import com.lliscano.eia.repository.ProjectMemberRepository;
import com.lliscano.eia.repository.ProjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private CerberosClientService cerberosClientService;

    @Mock
    private ProjectRoleService projectRoleService;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    private final String tenantId = "tenant-uuid-123";
    private final String username = "testuser@domain.com";
    private final String projectUuid = "proj-uuid-001";
    private final String memberUserUuid = "member-uuid-555";

    @BeforeEach
    void setUp() {
        RequestContextHolder.setContext(RequestContextData.builder()
                .tenant(tenantId)
                .sub(username)
                .uuid("user-lead-uuid")
                .build());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    @DisplayName("assignMember - asigna un nuevo miembro y enriquece con usuario de Cerberos")
    void assignMember_NewMember_Success() {
        Project project = Project.builder().id(1L).uuid(projectUuid).build();
        ProjectMemberAssignDTO request = ProjectMemberAssignDTO.builder()
                .userUuid(memberUserUuid)
                .projectRole("PROJECT_ANALYST")
                .build();

        ProjectRole roleEntity = ProjectRole.builder()
                .code("PROJECT_ANALYST")
                .isActive(true)
                .build();

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .userUuid(memberUserUuid)
                .projectRole("PROJECT_ANALYST")
                .isActive(true)
                .build();

        AppUserSummaryDTO userSummary = AppUserSummaryDTO.builder()
                .uuid(memberUserUuid)
                .username("analyst@cerberos.com")
                .firstName("Carlos")
                .lastName("Mendoza")
                .build();

        ProjectMemberResponseDTO responseDTO = ProjectMemberResponseDTO.builder()
                .uuid(memberUserUuid)
                .projectRole("PROJECT_ANALYST")
                .isActive(true)
                .build();

        when(projectRepository.findByUuidAndIsDeletedFalse(projectUuid)).thenReturn(Optional.of(project));
        when(projectRoleService.getRoleByCode("PROJECT_ANALYST")).thenReturn(Optional.of(roleEntity));
        when(projectMemberRepository.findByProjectUuidAndUserUuidAndIsDeletedFalse(projectUuid, memberUserUuid))
                .thenReturn(Optional.empty());
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(member);
        when(projectMemberMapper.toDto(member)).thenReturn(responseDTO);
        when(cerberosClientService.getTenantUsers(tenantId)).thenReturn(List.of(userSummary));

        ResponseDTO<ProjectMemberResponseDTO> result = projectMemberService.assignMember(projectUuid, request);

        assertNotNull(result);
        assertEquals("Miembro asignado exitosamente", result.getMessage());
        assertEquals("PROJECT_ANALYST", result.getData().getProjectRole());
        assertNotNull(result.getData().getUserSummary());
        assertEquals("Carlos", result.getData().getUserSummary().getFirstName());
    }

    @Test
    @DisplayName("assignMember - actualiza rol de miembro existente")
    void assignMember_ExistingMember_UpdatesRole() {
        Project project = Project.builder().id(1L).uuid(projectUuid).build();
        ProjectMemberAssignDTO request = ProjectMemberAssignDTO.builder()
                .userUuid(memberUserUuid)
                .projectRole("PROJECT_LEAD")
                .build();

        ProjectRole roleEntity = ProjectRole.builder()
                .code("PROJECT_LEAD")
                .isActive(true)
                .build();

        ProjectMember existingMember = ProjectMember.builder()
                .project(project)
                .userUuid(memberUserUuid)
                .projectRole("PROJECT_ANALYST")
                .isActive(false)
                .build();

        when(projectRepository.findByUuidAndIsDeletedFalse(projectUuid)).thenReturn(Optional.of(project));
        when(projectRoleService.getRoleByCode("PROJECT_LEAD")).thenReturn(Optional.of(roleEntity));
        when(projectMemberRepository.findByProjectUuidAndUserUuidAndIsDeletedFalse(projectUuid, memberUserUuid))
                .thenReturn(Optional.of(existingMember));
        when(projectMemberRepository.save(existingMember)).thenReturn(existingMember);
        when(projectMemberMapper.toDto(existingMember)).thenReturn(ProjectMemberResponseDTO.builder()
                .uuid(memberUserUuid)
                .projectRole("PROJECT_LEAD")
                .isActive(true)
                .build());

        ResponseDTO<ProjectMemberResponseDTO> result = projectMemberService.assignMember(projectUuid, request);

        assertNotNull(result);
        assertEquals("PROJECT_LEAD", result.getData().getProjectRole());
        assertTrue(existingMember.isActive());
        assertEquals("PROJECT_LEAD", existingMember.getProjectRole());
    }

    @Test
    @DisplayName("assignMember - proyecto inexistente lanza RecordNotFoundException")
    void assignMember_ProjectNotFound_ThrowsException() {
        when(projectRepository.findByUuidAndIsDeletedFalse(projectUuid)).thenReturn(Optional.empty());

        assertThrows(RecordNotFoundException.class, () ->
                projectMemberService.assignMember(projectUuid, ProjectMemberAssignDTO.builder()
                        .userUuid(memberUserUuid)
                        .projectRole("ANALYST")
                        .build()));
    }

    @Test
    @DisplayName("assignMember - rol no encontrado lanza RecordNotFoundException")
    void assignMember_InvalidRole_ThrowsException() {
        Project project = Project.builder().id(1L).uuid(projectUuid).build();
        when(projectRepository.findByUuidAndIsDeletedFalse(projectUuid)).thenReturn(Optional.of(project));
        when(projectRoleService.getRoleByCode("INVALID_ROLE")).thenReturn(Optional.empty());

        RecordNotFoundException exception = assertThrows(RecordNotFoundException.class, () ->
                projectMemberService.assignMember(projectUuid, ProjectMemberAssignDTO.builder()
                        .userUuid(memberUserUuid)
                        .projectRole("INVALID_ROLE")
                        .build()));
        assertTrue(exception.getMessage().contains("no existe o se encuentra inactivo"));
    }

    @Test
    @DisplayName("assignMember - rol inactivo lanza RecordNotFoundException")
    void assignMember_InactiveRole_ThrowsException() {
        Project project = Project.builder().id(1L).uuid(projectUuid).build();
        ProjectRole inactiveRole = ProjectRole.builder()
                .code("INACTIVE_ROLE")
                .isActive(false)
                .build();

        when(projectRepository.findByUuidAndIsDeletedFalse(projectUuid)).thenReturn(Optional.of(project));
        when(projectRoleService.getRoleByCode("INACTIVE_ROLE")).thenReturn(Optional.of(inactiveRole));

        RecordNotFoundException exception = assertThrows(RecordNotFoundException.class, () ->
                projectMemberService.assignMember(projectUuid, ProjectMemberAssignDTO.builder()
                        .userUuid(memberUserUuid)
                        .projectRole("INACTIVE_ROLE")
                        .build()));

        assertTrue(exception.getMessage().contains("no se encuentra activo"));
    }

    @Test
    @DisplayName("getMembers - éxito listando miembros de un proyecto")
    void getMembers_Success() {
        Project project = Project.builder().id(1L).uuid(projectUuid).build();
        ProjectMember m1 = ProjectMember.builder().userUuid("u1").projectRole("LEAD").build();
        ProjectMember m2 = ProjectMember.builder().userUuid("u2").projectRole("ANALYST").build();

        when(projectRepository.findByUuidAndIsDeletedFalse(projectUuid)).thenReturn(Optional.of(project));
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(projectMemberRepository.findAllByProjectUuidAndIsDeletedFalse(projectUuid)).thenReturn(List.of(m1, m2));
        when(projectMemberMapper.toDto(m1)).thenReturn(ProjectMemberResponseDTO.builder().uuid("u1").build());
        when(projectMemberMapper.toDto(m2)).thenReturn(ProjectMemberResponseDTO.builder().uuid("u2").build());

        ResponseDTO<ArrayList<ProjectMemberResponseDTO>> result = projectMemberService.getMembers(projectUuid);

        assertNotNull(result);
        assertEquals(2, result.getData().size());
    }

    @Test
    @DisplayName("removeMember - éxito desvinculando miembro")
    void removeMember_Success() {
        ProjectMember member = ProjectMember.builder()
                .userUuid(memberUserUuid)
                .isActive(true)
                .isDeleted(false)
                .build();

        when(projectMemberRepository.findByProjectUuidAndUserUuidAndIsDeletedFalse(projectUuid, memberUserUuid))
                .thenReturn(Optional.of(member));

        ResponseDTO<String> result = projectMemberService.removeMember(projectUuid, memberUserUuid);

        assertNotNull(result);
        assertTrue(member.isDeleted());
        assertFalse(member.isActive());
        verify(projectMemberRepository).save(member);
    }
}
