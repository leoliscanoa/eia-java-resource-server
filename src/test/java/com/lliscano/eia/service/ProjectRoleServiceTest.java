package com.lliscano.eia.service;

import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.eia.mapper.ProjectRoleMapper;
import com.lliscano.eia.model.dto.response.ProjectRoleResponseDTO;
import com.lliscano.eia.model.entity.ProjectRole;
import com.lliscano.eia.repository.ProjectRoleRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectRoleServiceTest {

    @Mock
    private ProjectRoleRepository projectRoleRepository;

    @Mock
    private ProjectRoleMapper projectRoleMapper;

    @InjectMocks
    private ProjectRoleService projectRoleService;

    @Test
    @DisplayName("getAllActiveRoles - éxito retornando catálogo de roles activos")
    void getAllActiveRoles_Success() {
        ProjectRole role1 = ProjectRole.builder().code("PROJECT_LEAD").isLeadRole(true).isActive(true).build();
        ProjectRole role2 = ProjectRole.builder().code("MEMBER").isDefaultMember(true).isActive(true).build();
        List<ProjectRole> roles = List.of(role1, role2);

        ProjectRoleResponseDTO dto1 = ProjectRoleResponseDTO.builder().code("PROJECT_LEAD").isLeadRole(true).isActive(true).build();
        ProjectRoleResponseDTO dto2 = ProjectRoleResponseDTO.builder().code("MEMBER").isDefaultMember(true).isActive(true).build();
        ArrayList<ProjectRoleResponseDTO> dtos = new ArrayList<>(List.of(dto1, dto2));

        when(projectRoleRepository.findAllByIsActiveTrueOrderByCreatedAtAsc()).thenReturn(roles);
        when(projectRoleMapper.toDtoList(roles)).thenReturn(dtos);

        ResponseDTO<ArrayList<ProjectRoleResponseDTO>> result = projectRoleService.getAllActiveRoles();

        assertNotNull(result);
        assertEquals("Operación exitosa", result.getMessage());
        assertEquals(2, result.getData().size());
        assertEquals("PROJECT_LEAD", result.getData().get(0).getCode());
    }

    @Test
    @DisplayName("getRoleByCode - éxito retornando rol normalizado en mayúsculas")
    void getRoleByCode_Success() {
        ProjectRole role = ProjectRole.builder().code("ANALYST").isActive(true).build();
        when(projectRoleRepository.findByCode("ANALYST")).thenReturn(Optional.of(role));

        Optional<ProjectRole> result = projectRoleService.getRoleByCode(" analyst ");

        assertTrue(result.isPresent());
        assertEquals("ANALYST", result.get().getCode());
    }

    @Test
    @DisplayName("getRoleByCode - código nulo o vacío retorna Optional.empty()")
    void getRoleByCode_NullOrBlank_ReturnsEmpty() {
        assertTrue(projectRoleService.getRoleByCode(null).isEmpty());
        assertTrue(projectRoleService.getRoleByCode("   ").isEmpty());
        verifyNoInteractions(projectRoleRepository);
    }

    @Test
    @DisplayName("isLeadRole - retorna true si el rol existe, está activo y es lead")
    void isLeadRole_TrueWhenConfigured() {
        ProjectRole role = ProjectRole.builder().code("PROJECT_LEAD").isLeadRole(true).isActive(true).build();
        when(projectRoleRepository.findByCode("PROJECT_LEAD")).thenReturn(Optional.of(role));

        boolean isLead = projectRoleService.isLeadRole("PROJECT_LEAD");

        assertTrue(isLead);
    }

    @Test
    @DisplayName("isLeadRole - retorna false si no es lead role")
    void isLeadRole_FalseWhenNotLead() {
        ProjectRole role = ProjectRole.builder().code("MEMBER").isLeadRole(false).isActive(true).build();
        when(projectRoleRepository.findByCode("MEMBER")).thenReturn(Optional.of(role));

        boolean isLead = projectRoleService.isLeadRole("MEMBER");

        assertFalse(isLead);
    }

    @Test
    @DisplayName("isLeadRole - retorna false si está inactivo, no existe o código es nulo")
    void isLeadRole_FalseWhenInactiveOrNotFound() {
        ProjectRole inactiveRole = ProjectRole.builder().code("OLD_LEAD").isLeadRole(true).isActive(false).build();
        when(projectRoleRepository.findByCode("OLD_LEAD")).thenReturn(Optional.of(inactiveRole));
        when(projectRoleRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertFalse(projectRoleService.isLeadRole("OLD_LEAD"));
        assertFalse(projectRoleService.isLeadRole("UNKNOWN"));
        assertFalse(projectRoleService.isLeadRole(null));
        assertFalse(projectRoleService.isLeadRole("   "));
    }

    @Test
    @DisplayName("getLeadRoleCode - retorna código de rol lead configurado")
    void getLeadRoleCode_ReturnsConfiguredCode() {
        ProjectRole role = ProjectRole.builder().code("PROJECT_LEAD").isLeadRole(true).isActive(true).build();
        when(projectRoleRepository.findFirstByIsLeadRoleTrueAndIsActiveTrue()).thenReturn(Optional.of(role));

        String code = projectRoleService.getLeadRoleCode();

        assertEquals("PROJECT_LEAD", code);
    }

    @Test
    @DisplayName("getLeadRoleCode - retorna fallback PROJECT_LEAD si no hay registro")
    void getLeadRoleCode_ReturnsFallback() {
        when(projectRoleRepository.findFirstByIsLeadRoleTrueAndIsActiveTrue()).thenReturn(Optional.empty());

        String code = projectRoleService.getLeadRoleCode();

        assertEquals("PROJECT_LEAD", code);
    }

    @Test
    @DisplayName("getDefaultMemberRoleCode - retorna código de rol por defecto configurado")
    void getDefaultMemberRoleCode_ReturnsConfiguredCode() {
        ProjectRole role = ProjectRole.builder().code("MEMBER").isDefaultMember(true).isActive(true).build();
        when(projectRoleRepository.findFirstByIsDefaultMemberTrueAndIsActiveTrue()).thenReturn(Optional.of(role));

        String code = projectRoleService.getDefaultMemberRoleCode();

        assertEquals("MEMBER", code);
    }

    @Test
    @DisplayName("getDefaultMemberRoleCode - retorna fallback MEMBER si no hay registro")
    void getDefaultMemberRoleCode_ReturnsFallback() {
        when(projectRoleRepository.findFirstByIsDefaultMemberTrueAndIsActiveTrue()).thenReturn(Optional.empty());

        String code = projectRoleService.getDefaultMemberRoleCode();

        assertEquals("MEMBER", code);
    }

    @Test
    @DisplayName("evictAllCaches - ejecuta sin errores")
    void evictAllCaches_ExecutesSuccessfully() {
        assertDoesNotThrow(() -> projectRoleService.evictAllCaches());
    }
}
