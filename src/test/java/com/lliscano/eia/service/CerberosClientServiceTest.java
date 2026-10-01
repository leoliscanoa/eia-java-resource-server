package com.lliscano.eia.service;

import com.lliscano.commons.components.ReactiveHttpClient;
import com.lliscano.commons.dtos.AppUserSummaryDTO;
import com.lliscano.commons.dtos.ResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CerberosClientServiceTest {

    @Mock
    private ReactiveHttpClient reactiveHttpClient;

    @InjectMocks
    private CerberosClientService cerberosClientService;

    @Test
    @DisplayName("getTenantUsers - éxito obteniendo usuarios vía ReactiveHttpClient M2M")
    void getTenantUsers_Success() {
        ReflectionTestUtils.setField(cerberosClientService, "cerbosApiBaseUrl", "http://localhost:8081/api/cerbos-resource-server");

        AppUserSummaryDTO user = AppUserSummaryDTO.builder()
                .uuid("user-uuid-1")
                .username("test@test.com")
                .firstName("Juan")
                .lastName("Perez")
                .build();

        ResponseDTO<ArrayList<AppUserSummaryDTO>> clientResponse = ResponseDTO.<ArrayList<AppUserSummaryDTO>>builder()
                .data(new ArrayList<>(List.of(user)))
                .message("OK")
                .build();

        when(reactiveHttpClient.executeBlocking(any())).thenReturn(clientResponse);

        List<AppUserSummaryDTO> result = cerberosClientService.getTenantUsers("tenant-uuid-123");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Juan", result.get(0).getFirstName());
    }

    @Test
    @DisplayName("getTenantUsers - tenant nulo o en blanco retorna lista vacía sin invocar HTTP")
    void getTenantUsers_NullTenant_ReturnsEmpty() {
        List<AppUserSummaryDTO> result = cerberosClientService.getTenantUsers(null);
        assertTrue(result.isEmpty());

        result = cerberosClientService.getTenantUsers("   ");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getTenantUsers - cuando ocurre una excepción retorna lista vacía de forma resiliente")
    void getTenantUsers_Exception_ReturnsEmpty() {
        ReflectionTestUtils.setField(cerberosClientService, "cerbosApiBaseUrl", "http://localhost:8081/api/cerbos-resource-server");
        when(reactiveHttpClient.executeBlocking(any())).thenThrow(new RuntimeException("Connection timeout"));

        List<AppUserSummaryDTO> result = cerberosClientService.getTenantUsers("tenant-uuid-123");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
