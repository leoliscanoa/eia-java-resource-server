package com.lliscano.eia.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.eia.model.dto.request.ProjectCreateRequestDTO;
import com.lliscano.eia.model.dto.request.ProjectUpdateRequestDTO;
import com.lliscano.eia.model.dto.response.ProjectResponseDTO;
import com.lliscano.eia.security.ProjectSecurity;
import com.lliscano.eia.service.ProjectService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private ProjectSecurity projectSecurity;

    @Test
    @DisplayName("POST /v1/projects - crea proyecto exitosamente")
    @WithMockUser(authorities = "EIA_ADMIN")
    void createProject_Success() throws Exception {
        ProjectCreateRequestDTO request = ProjectCreateRequestDTO.builder()
                .code("PRJ-001")
                .name("Proyecto Test")
                .sector("ENERGIA")
                .build();

        ProjectResponseDTO responseDTO = ProjectResponseDTO.builder()
                .uuid("proj-uuid-1")
                .code("PRJ-001")
                .name("Proyecto Test")
                .build();

        when(projectService.createProject(any(ProjectCreateRequestDTO.class)))
                .thenReturn(ResponseDTO.<ProjectResponseDTO>builder()
                        .message("Proyecto creado exitosamente")
                        .data(responseDTO)
                        .build());

        mockMvc.perform(post("/v1/projects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.uuid").value("proj-uuid-1"))
                .andExpect(jsonPath("$.data.code").value("PRJ-001"));
    }

    @Test
    @DisplayName("GET /v1/projects - lista proyectos exitosamente")
    @WithMockUser(authorities = "EIA_USER")
    void listProjects_Success() throws Exception {
        ProjectResponseDTO responseDTO = ProjectResponseDTO.builder()
                .uuid("proj-uuid-1")
                .code("PRJ-001")
                .name("Proyecto Test")
                .build();

        when(projectService.listProjects())
                .thenReturn(ResponseDTO.<ArrayList<ProjectResponseDTO>>builder()
                        .message("Operación exitosa")
                        .data(new ArrayList<>(List.of(responseDTO)))
                        .build());

        mockMvc.perform(get("/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].uuid").value("proj-uuid-1"));
    }

    @Test
    @DisplayName("GET /v1/projects/{uuid} - obtiene detalle de un proyecto")
    @WithMockUser(authorities = "EIA_USER")
    void getProjectByUuid_Success() throws Exception {
        ProjectResponseDTO responseDTO = ProjectResponseDTO.builder()
                .uuid("proj-uuid-1")
                .code("PRJ-001")
                .build();

        when(projectService.getProjectByUuid("proj-uuid-1"))
                .thenReturn(ResponseDTO.<ProjectResponseDTO>builder()
                        .message("Operación exitosa")
                        .data(responseDTO)
                        .build());

        mockMvc.perform(get("/v1/projects/proj-uuid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uuid").value("proj-uuid-1"));
    }

    @Test
    @DisplayName("PUT /v1/projects/{uuid} - actualiza proyecto")
    @WithMockUser(authorities = "EIA_ADMIN")
    void updateProject_Success() throws Exception {
        ProjectUpdateRequestDTO request = ProjectUpdateRequestDTO.builder()
                .name("Nombre Actualizado")
                .build();

        ProjectResponseDTO responseDTO = ProjectResponseDTO.builder()
                .uuid("proj-uuid-1")
                .name("Nombre Actualizado")
                .build();

        when(projectService.updateProject(eq("proj-uuid-1"), any(ProjectUpdateRequestDTO.class)))
                .thenReturn(ResponseDTO.<ProjectResponseDTO>builder()
                        .message("Proyecto actualizado exitosamente")
                        .data(responseDTO)
                        .build());

        mockMvc.perform(put("/v1/projects/proj-uuid-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Nombre Actualizado"));
    }

    @Test
    @DisplayName("DELETE /v1/projects/{uuid} - elimina proyecto")
    @WithMockUser(authorities = "EIA_ADMIN")
    void deleteProject_Success() throws Exception {
        when(projectService.deleteProject("proj-uuid-1"))
                .thenReturn(ResponseDTO.<String>builder()
                        .message("Proyecto eliminado exitosamente")
                        .data("proj-uuid-1")
                        .build());

        mockMvc.perform(delete("/v1/projects/proj-uuid-1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Proyecto eliminado exitosamente"));
    }
}
