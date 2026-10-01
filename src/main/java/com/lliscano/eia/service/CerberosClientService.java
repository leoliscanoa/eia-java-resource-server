package com.lliscano.eia.service;

import com.lliscano.commons.components.ReactiveHttpClient;
import com.lliscano.commons.dtos.AppUserSummaryDTO;
import com.lliscano.commons.dtos.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CerberosClientService {

    private final ReactiveHttpClient reactiveHttpClient;

    @Value("${app.cerbos-api-url:http://localhost:8081/api/cerbos-resource-server}")
    private String cerbosApiBaseUrl;

    public List<AppUserSummaryDTO> getTenantUsers(String tenantUuid) {
        if (tenantUuid == null || tenantUuid.isBlank()) {
            return Collections.emptyList();
        }

        try {
            ResponseDTO<ArrayList<AppUserSummaryDTO>> response = reactiveHttpClient.executeBlocking(
                ReactiveHttpClient.request(HttpMethod.GET, cerbosApiBaseUrl + "/client/tenant/{tenantUuid}/users",
                        new ParameterizedTypeReference<ResponseDTO<ArrayList<AppUserSummaryDTO>>>() {})
                    .uriVariable("tenantUuid", tenantUuid)
                    .withM2mAuth("EIA")
                    .circuitBreaker("cerbos-service")
                    .timeout(Duration.ofSeconds(3))
                    .fallback(ex -> {
                        log.warn("CircuitBreaker/Fallback triggered when calling Cerberos SSO: {}", ex.getMessage());
                        return Mono.just(ResponseDTO.<ArrayList<AppUserSummaryDTO>>builder()
                                .data(new ArrayList<>())
                                .message("Servicio Cerberos no disponible temporalmente")
                                .build());
                    })
                    .build()
            );

            return (response != null && response.getData() != null) ? response.getData() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch users from Cerberos SSO for tenant {}: {}", tenantUuid, e.getMessage());
            return Collections.emptyList();
        }
    }
}
