package com.lliscano.eia.service;

import com.lliscano.commons.components.RequestContextHolder;
import com.lliscano.commons.dtos.RequestContextData;
import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.commons.exceptions.RecordNotFoundException;
import com.lliscano.commons.exceptions.UnauthorizedEntityException;
import com.lliscano.eia.mapper.ProjectMapper;
import com.lliscano.eia.model.dto.request.ProjectCreateRequestDTO;
import com.lliscano.eia.model.dto.request.ProjectTerritoryRequestDTO;
import com.lliscano.eia.model.dto.request.ProjectUpdateRequestDTO;
import com.lliscano.eia.model.dto.response.ProjectResponseDTO;
import com.lliscano.eia.model.entity.Project;
import com.lliscano.eia.model.entity.ProjectMember;
import com.lliscano.eia.model.entity.Territory;
import com.lliscano.eia.repository.ProjectRepository;
import com.lliscano.eia.repository.TerritoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TerritoryRepository territoryRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectRoleService projectRoleService;

    @InjectMocks
    private ProjectService projectService;

    private final String tenantId = "tenant-uuid-123";
    private final String username = "testuser@domain.com";
    private final String userUuid = "user-uuid-456";

    @BeforeEach
    void setUp() {
        RequestContextHolder.setContext(RequestContextData.builder()
                .tenant(tenantId)
                .sub(username)
                .uuid(userUuid)
                .build());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("createProject - éxito creando proyecto con director y territorios")
    void createProject_Success() {
        ProjectCreateRequestDTO request = ProjectCreateRequestDTO.builder()
                .code("PRJ-001")
                .name("Proyecto Test")
                .sector("ENERGIA")
                .typology("EOLICA")
                .stage("PLANNING")
                .budgetEstimate(BigDecimal.valueOf(1000000))
                .currency("COP")
                .leadUserUuid("lead-uuid-789")
                .territories(List.of(ProjectTerritoryRequestDTO.builder()
                        .territoryUuid("terr-uuid-1")
                        .influenceType("DIRECT")
                        .build()))
                .build();

        Project entity = Project.builder()
                .code("PRJ-001")
                .name("Proyecto Test")
                .sector("ENERGIA")
                .members(new ArrayList<>())
                .territories(new ArrayList<>())
                .build();

        Territory territory = Territory.builder()
                .uuid("terr-uuid-1")
                .tenantId(tenantId)
                .name("Alta Guajira")
                .build();

        ProjectResponseDTO responseDTO = ProjectResponseDTO.builder()
                .uuid("proj-uuid-999")
                .code("PRJ-001")
                .name("Proyecto Test")
                .sector("ENERGIA")
                .build();

        when(projectRoleService.getLeadRoleCode()).thenReturn("PROJECT_LEAD");
        when(projectRepository.existsByTenantIdAndCodeAndIsDeletedFalse(tenantId, "PRJ-001")).thenReturn(false);
        when(projectMapper.toEntity(request)).thenReturn(entity);
        when(territoryRepository.findAllByUuidInAndTenantIdAndIsDeletedFalse(anySet(), eq(tenantId)))
                .thenReturn(List.of(territory));
        when(projectRepository.save(any(Project.class))).thenReturn(entity);
        when(projectMapper.toDto(entity)).thenReturn(responseDTO);

        ResponseDTO<ProjectResponseDTO> result = projectService.createProject(request);

        assertNotNull(result);
        assertEquals("Proyecto creado exitosamente", result.getMessage());
        assertEquals("PRJ-001", result.getData().getCode());
        assertEquals("PROJECT_LEAD", result.getData().getUserRole());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("createProject - lanza excepción si el código de proyecto ya existe")
    void createProject_DuplicateCode_ThrowsException() {
        ProjectCreateRequestDTO request = ProjectCreateRequestDTO.builder()
                .code("PRJ-DUPLICATE")
                .name("Proyecto Duplicado")
                .sector("MINERIA")
                .build();

        when(projectRepository.existsByTenantIdAndCodeAndIsDeletedFalse(tenantId, "PRJ-DUPLICATE")).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class, () -> projectService.createProject(request));
        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProject - lanza UnauthorizedEntityException si no hay tenant")
    void createProject_MissingTenant_ThrowsException() {
        RequestContextHolder.clear();
        ProjectCreateRequestDTO request = ProjectCreateRequestDTO.builder()
                .code("PRJ-002")
                .name("Sin Tenant")
                .sector("INFRAESTRUCTURA")
                .build();

        assertThrows(UnauthorizedEntityException.class, () -> projectService.createProject(request));
    }

    @Test
    @DisplayName("listProjects - como EIA_ADMIN retorna todos los proyectos del tenant")
    void listProjects_AsAdmin_ReturnsAllProjects() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "n/a", List.of(new SimpleGrantedAuthority("EIA_ADMIN"))));

        Project p1 = Project.builder().code("PRJ-001").members(new ArrayList<>()).build();
        Project p2 = Project.builder().code("PRJ-002").members(new ArrayList<>()).build();

        when(projectRoleService.getDefaultMemberRoleCode()).thenReturn("MEMBER");
        when(projectRepository.findAllByTenantIdAndIsDeletedFalse(tenantId)).thenReturn(List.of(p1, p2));
        when(projectMapper.toDto(p1)).thenReturn(ProjectResponseDTO.builder().code("PRJ-001").build());
        when(projectMapper.toDto(p2)).thenReturn(ProjectResponseDTO.builder().code("PRJ-002").build());

        ResponseDTO<ArrayList<ProjectResponseDTO>> result = projectService.listProjects();

        assertNotNull(result);
        assertEquals(2, result.getData().size());
        assertEquals("EIA_ADMIN", result.getData().get(0).getUserRole());
    }

    @Test
    @DisplayName("listProjects - como usuario regular retorna solo proyectos asociados")
    void listProjects_AsUser_ReturnsAssociatedProjects() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "n/a", List.of(new SimpleGrantedAuthority("EIA_USER"))));

        Project p1 = Project.builder().code("PRJ-001").members(new ArrayList<>()).build();
        ProjectMember member = ProjectMember.builder()
                .userUuid(userUuid)
                .projectRole("PROJECT_ANALYST")
                .isActive(true)
                .isDeleted(false)
                .build();
        p1.getMembers().add(member);

        when(projectRoleService.getDefaultMemberRoleCode()).thenReturn("MEMBER");
        when(projectRepository.findAllByTenantIdAndMemberUserUuid(tenantId, userUuid, username)).thenReturn(List.of(p1));
        when(projectMapper.toDto(p1)).thenReturn(ProjectResponseDTO.builder().code("PRJ-001").build());

        ResponseDTO<ArrayList<ProjectResponseDTO>> result = projectService.listProjects();

        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals("PROJECT_ANALYST", result.getData().get(0).getUserRole());
    }

    @Test
    @DisplayName("getProjectByUuid - éxito recuperando detalle")
    void getProjectByUuid_Success() {
        Project p = Project.builder().uuid("proj-uuid-1").code("PRJ-001").build();
        when(projectRepository.findByUuidAndTenantIdAndIsDeletedFalse("proj-uuid-1", tenantId))
                .thenReturn(Optional.of(p));
        when(projectMapper.toDto(p)).thenReturn(ProjectResponseDTO.builder().uuid("proj-uuid-1").build());

        ResponseDTO<ProjectResponseDTO> result = projectService.getProjectByUuid("proj-uuid-1");

        assertNotNull(result);
        assertEquals("proj-uuid-1", result.getData().getUuid());
    }

    @Test
    @DisplayName("getProjectByUuid - no encontrado lanza RecordNotFoundException")
    void getProjectByUuid_NotFound_ThrowsException() {
        when(projectRepository.findByUuidAndTenantIdAndIsDeletedFalse("invalid-uuid", tenantId))
                .thenReturn(Optional.empty());

        assertThrows(RecordNotFoundException.class, () -> projectService.getProjectByUuid("invalid-uuid"));
    }

    @Test
    @DisplayName("updateProject - éxito actualizando campos")
    void updateProject_Success() {
        Project p = Project.builder()
                .uuid("proj-uuid-1")
                .name("Nombre Antiguo")
                .sector("ENERGIA")
                .build();

        ProjectUpdateRequestDTO updateDto = ProjectUpdateRequestDTO.builder()
                .name("Nombre Nuevo")
                .stage("EJECUCION")
                .build();

        when(projectRepository.findByUuidAndTenantIdAndIsDeletedFalse("proj-uuid-1", tenantId))
                .thenReturn(Optional.of(p));
        when(projectRepository.save(p)).thenReturn(p);
        when(projectMapper.toDto(p)).thenReturn(ProjectResponseDTO.builder()
                .uuid("proj-uuid-1")
                .name("Nombre Nuevo")
                .stage("EJECUCION")
                .build());

        ResponseDTO<ProjectResponseDTO> result = projectService.updateProject("proj-uuid-1", updateDto);

        assertNotNull(result);
        assertEquals("Nombre Nuevo", result.getData().getName());
        assertEquals("EJECUCION", result.getData().getStage());
    }

    @Test
    @DisplayName("deleteProject - éxito realizando soft-delete")
    void deleteProject_Success() {
        Project p = Project.builder().uuid("proj-uuid-1").isDeleted(false).build();
        when(projectRepository.findByUuidAndTenantIdAndIsDeletedFalse("proj-uuid-1", tenantId))
                .thenReturn(Optional.of(p));

        ResponseDTO<String> result = projectService.deleteProject("proj-uuid-1");

        assertNotNull(result);
        assertTrue(p.isDeleted());
        verify(projectRepository).save(p);
    }
}
