package com.lliscano.eia.controller;

import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.eia.model.dto.response.ProjectRoleResponseDTO;
import com.lliscano.eia.service.ProjectRoleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectRoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectRoleService projectRoleService;

    @Test
    @DisplayName("GET /v1/projects/roles - retorna catálogo de roles activos")
    @WithMockUser(authorities = "EIA_USER")
    void getAllRoles_Success() throws Exception {
        ProjectRoleResponseDTO role1 = ProjectRoleResponseDTO.builder()
                .code("PROJECT_LEAD")
                .name("Líder de Proyecto")
                .description("Director general")
                .isLeadRole(true)
                .isDefaultMember(false)
                .isActive(true)
                .build();

        ProjectRoleResponseDTO role2 = ProjectRoleResponseDTO.builder()
                .code("MEMBER")
                .name("Miembro General")
                .description("Integrante base")
                .isLeadRole(false)
                .isDefaultMember(true)
                .isActive(true)
                .build();

        ArrayList<ProjectRoleResponseDTO> dtos = new ArrayList<>(List.of(role1, role2));

        when(projectRoleService.getAllActiveRoles()).thenReturn(
                ResponseDTO.<ArrayList<ProjectRoleResponseDTO>>builder()
                        .message("Operación exitosa")
                        .data(dtos)
                        .build()
        );

        mockMvc.perform(get("/v1/projects/roles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Operación exitosa"))
                .andExpect(jsonPath("$.data[0].code").value("PROJECT_LEAD"))
                .andExpect(jsonPath("$.data[0].leadRole").value(true))
                .andExpect(jsonPath("$.data[1].code").value("MEMBER"))
                .andExpect(jsonPath("$.data[1].defaultMember").value(true));
    }
}
