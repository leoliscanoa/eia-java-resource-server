package com.lliscano.eia.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lliscano.commons.advices.RestAdvice;
import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.commons.exceptions.RecordNotFoundException;
import com.lliscano.eia.model.dto.request.ProjectMemberAssignDTO;
import com.lliscano.eia.model.dto.response.ProjectMemberResponseDTO;
import com.lliscano.eia.security.ProjectSecurity;
import com.lliscano.eia.service.ProjectMemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectMemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestAdvice.class)
class ProjectMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectMemberService projectMemberService;

    @MockBean
    private ProjectSecurity projectSecurity;

    @Test
    @DisplayName("POST /v1/projects/{projectUuid}/members - asigna miembro")
    @WithMockUser(authorities = "EIA_ADMIN")
    void assignMember_Success() throws Exception {
        ProjectMemberAssignDTO request = ProjectMemberAssignDTO.builder()
                .userUuid("user-uuid-1")
                .projectRole("PROJECT_ANALYST")
                .build();

        ProjectMemberResponseDTO responseDTO = ProjectMemberResponseDTO.builder()
                .uuid("user-uuid-1")
                .projectRole("PROJECT_ANALYST")
                .isActive(true)
                .build();

        when(projectMemberService.assignMember(eq("proj-1"), any(ProjectMemberAssignDTO.class)))
                .thenReturn(ResponseDTO.<ProjectMemberResponseDTO>builder()
                        .message("Miembro asignado exitosamente")
                        .data(responseDTO)
                        .build());

        mockMvc.perform(post("/v1/projects/proj-1/members")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.uuid").value("user-uuid-1"))
                .andExpect(jsonPath("$.data.projectRole").value("PROJECT_ANALYST"));
    }

    @Test
    @DisplayName("POST /v1/projects/{projectUuid}/members - rol inexistente retorna 400 Bad Request")
    @WithMockUser(authorities = "EIA_ADMIN")
    void assignMember_InvalidRole_Returns400() throws Exception {
        ProjectMemberAssignDTO request = ProjectMemberAssignDTO.builder()
                .userUuid("user-uuid-1")
                .projectRole("SUPER_HERO")
                .build();

        when(projectMemberService.assignMember(eq("proj-1"), any(ProjectMemberAssignDTO.class)))
                .thenThrow(new RecordNotFoundException("El rol de proyecto 'SUPER_HERO' no existe o se encuentra inactivo"));

        mockMvc.perform(post("/v1/projects/proj-1/members")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("RecordNotFoundException"))
                .andExpect(jsonPath("$.data").value("El rol de proyecto 'SUPER_HERO' no existe o se encuentra inactivo"));
    }

    @Test
    @DisplayName("POST /v1/projects/{projectUuid}/members - rol inactivo retorna 400 Bad Request")
    @WithMockUser(authorities = "EIA_ADMIN")
    void assignMember_InactiveRole_Returns400() throws Exception {
        ProjectMemberAssignDTO request = ProjectMemberAssignDTO.builder()
                .userUuid("user-uuid-1")
                .projectRole("OLD_COLLECTOR")
                .build();

        when(projectMemberService.assignMember(eq("proj-1"), any(ProjectMemberAssignDTO.class)))
                .thenThrow(new RecordNotFoundException("El rol de proyecto 'OLD_COLLECTOR' no se encuentra activo"));

        mockMvc.perform(post("/v1/projects/proj-1/members")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("RecordNotFoundException"))
                .andExpect(jsonPath("$.data").value("El rol de proyecto 'OLD_COLLECTOR' no se encuentra activo"));
    }

    @Test
    @DisplayName("GET /v1/projects/{projectUuid}/members - lista miembros")
    @WithMockUser(authorities = "EIA_USER")
    void getMembers_Success() throws Exception {
        ProjectMemberResponseDTO responseDTO = ProjectMemberResponseDTO.builder()
                .uuid("user-uuid-1")
                .projectRole("PROJECT_ANALYST")
                .isActive(true)
                .build();

        when(projectMemberService.getMembers("proj-1"))
                .thenReturn(ResponseDTO.<ArrayList<ProjectMemberResponseDTO>>builder()
                        .message("Operación exitosa")
                        .data(new ArrayList<>(List.of(responseDTO)))
                        .build());

        mockMvc.perform(get("/v1/projects/proj-1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].uuid").value("user-uuid-1"));
    }

    @Test
    @DisplayName("DELETE /v1/projects/{projectUuid}/members/{userUuid} - remueve miembro")
    @WithMockUser(authorities = "EIA_ADMIN")
    void removeMember_Success() throws Exception {
        when(projectMemberService.removeMember("proj-1", "user-uuid-1"))
                .thenReturn(ResponseDTO.<String>builder()
                        .message("Miembro removido exitosamente")
                        .data("user-uuid-1")
                        .build());

        mockMvc.perform(delete("/v1/projects/proj-1/members/user-uuid-1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Miembro removido exitosamente"));
    }
}
