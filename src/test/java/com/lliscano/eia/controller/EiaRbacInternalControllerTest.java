package com.lliscano.eia.controller;

import com.lliscano.eia.repository.ProjectMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EiaRbacInternalController.class)
@AutoConfigureMockMvc(addFilters = false)
class EiaRbacInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectMemberRepository projectMemberRepository;

    @Test
    @DisplayName("GET /internal/v1/users/{uuid}/eia-rbac - retorna perfil ReBAC de admin")
    void getEiaRbacProfile_Admin() throws Exception {
        when(projectMemberRepository.isUserEiaAdmin("admin-uuid", "admin-uuid")).thenReturn(true);
        when(projectMemberRepository.findAssignedProjectUuidsByUserIdentifier("admin-uuid", "admin-uuid"))
                .thenReturn(List.of("prj-1", "prj-2"));

        mockMvc.perform(get("/internal/v1/users/admin-uuid/eia-rbac")
                        .header("X-Tenant-ID", "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eiaAdmin").value(true))
                .andExpect(jsonPath("$.data.assignedProjectIds[0]").value("prj-1"))
                .andExpect(jsonPath("$.data.assignedProjectIds[1]").value("prj-2"));
    }

    @Test
    @DisplayName("GET /internal/v1/users/{uuid}/eia-rbac - retorna perfil ReBAC de project lead")
    void getEiaRbacProfile_Lead() throws Exception {
        when(projectMemberRepository.isUserEiaAdmin("lead-uuid", "lead-uuid")).thenReturn(false);
        when(projectMemberRepository.findAssignedProjectUuidsByUserIdentifier("lead-uuid", "lead-uuid"))
                .thenReturn(List.of("prj-lead-only"));

        mockMvc.perform(get("/internal/v1/users/lead-uuid/eia-rbac")
                        .header("X-Tenant-ID", "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eiaAdmin").value(false))
                .andExpect(jsonPath("$.data.assignedProjectIds[0]").value("prj-lead-only"));
    }
}
