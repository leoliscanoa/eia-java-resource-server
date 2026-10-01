package com.lliscano.eia.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.lliscano.commons.components.RequestContextHolder;
import com.lliscano.commons.dtos.RequestContextData;
import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.commons.exceptions.UnauthorizedEntityException;
import com.lliscano.eia.base.AbstractBaseIntegrationIT;
import com.lliscano.eia.model.dto.request.ProjectCreateRequestDTO;
import com.lliscano.eia.model.dto.request.ProjectMemberAssignDTO;
import com.lliscano.eia.model.dto.response.ProjectMemberResponseDTO;
import com.lliscano.eia.model.dto.response.ProjectResponseDTO;
import com.lliscano.eia.model.entity.Project;
import com.lliscano.eia.model.entity.ProjectMember;
import com.lliscano.eia.repository.ProjectMemberRepository;
import com.lliscano.eia.repository.ProjectRepository;
import com.lliscano.eia.service.ProjectMemberService;
import com.lliscano.eia.service.ProjectService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectIntegrationIT extends AbstractBaseIntegrationIT {

    private static WireMockServer wireMockServer;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectMemberService projectMemberService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    private static final String TENANT_ID = "tenant-it-001";
    private static final String USER_ID = "user-qa-lead";

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureWireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("app.cerbos-api-url", () -> wireMockServer.baseUrl() + "/api/cerbos-resource-server");
        registry.add("commons.m2m.token-uri", () -> wireMockServer.baseUrl() + "/oauth2/sso/token");
    }

    @BeforeEach
    void setUp() {
        RequestContextHolder.setContext(RequestContextData.builder()
                .tenant(TENANT_ID)
                .sub(USER_ID)
                .uuid("uuid-qa-lead")
                .build());

        wireMockServer.resetAll();

        // Default M2M OAuth2 Token Stub
        wireMockServer.stubFor(post(urlEqualTo("/oauth2/sso/token"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"access_token\":\"it-mock-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    @DisplayName("given_validProjectPayload_when_createProject_then_persistsToDatabaseAndReturns201")
    void given_validProjectPayload_when_createProject_then_persistsToDatabaseAndReturns201() {
        ProjectCreateRequestDTO request = ProjectCreateRequestDTO.builder()
                .code("PRJ-IT-001")
                .name("Parque Solar Guajira")
                .description("Proyecto de energía solar fotovoltaica")
                .sector("ENERGIA")
                .typology("SOLAR_FOTOVOLTAICA")
                .stage("PLANNING")
                .currency("COP")
                .budgetEstimate(BigDecimal.valueOf(5000000000.00))
                .startDate(LocalDate.of(2026, 1, 1))
                .estimatedEndDate(LocalDate.of(2028, 12, 31))
                .build();

        ResponseDTO<ProjectResponseDTO> response = projectService.createProject(request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Proyecto creado exitosamente");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getCode()).isEqualTo("PRJ-IT-001");
        assertThat(response.getData().getName()).isEqualTo("Parque Solar Guajira");

        String projectUuid = response.getData().getUuid();
        assertThat(projectUuid).isNotBlank();

        // Verify direct state in real PostgreSQL DB
        Optional<Project> persistedOpt = projectRepository.findByUuidAndIsDeletedFalse(projectUuid);
        assertThat(persistedOpt).isPresent();
        Project persisted = persistedOpt.get();
        assertThat(persisted.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(persisted.getCode()).isEqualTo("PRJ-IT-001");
        assertThat(persisted.getStatus()).isEqualTo("ACTIVE");
        assertThat(persisted.getCreatedBy()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("given_duplicateProjectCode_when_createProject_then_throwsDataIntegrityViolationException")
    void given_duplicateProjectCode_when_createProject_then_throwsDataIntegrityViolationException() {
        ProjectCreateRequestDTO request1 = ProjectCreateRequestDTO.builder()
                .code("PRJ-UNIQUE-01")
                .name("Proyecto Inicial")
                .sector("MINERIA")
                .build();
        projectService.createProject(request1);

        ProjectCreateRequestDTO duplicateRequest = ProjectCreateRequestDTO.builder()
                .code("PRJ-UNIQUE-01")
                .name("Proyecto Repetido")
                .sector("MINERIA")
                .build();

        assertThatThrownBy(() -> projectService.createProject(duplicateRequest))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Ya existe un proyecto con el código especificado");
    }

    @Test
    @DisplayName("given_noTenantContext_when_createProject_then_throwsUnauthorizedEntityException")
    void given_noTenantContext_when_createProject_then_throwsUnauthorizedEntityException() {
        RequestContextHolder.clear();

        ProjectCreateRequestDTO request = ProjectCreateRequestDTO.builder()
                .code("PRJ-NO-TENANT")
                .name("Proyecto Huérfano")
                .sector("INFRAESTRUCTURA")
                .build();

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(UnauthorizedEntityException.class)
                .hasMessageContaining("No se ha especificado un tenant válido");
    }

    @Test
    @DisplayName("given_validMemberAndCerberosOnline_when_assignMember_then_enrichesWithCerberosUserSummary")
    void given_validMemberAndCerberosOnline_when_assignMember_then_enrichesWithCerberosUserSummary() {
        // Create base project
        ProjectCreateRequestDTO projectReq = ProjectCreateRequestDTO.builder()
                .code("PRJ-MEMBERS-01")
                .name("Proyecto con Miembros")
                .sector("AGROINDUSTRIA")
                .build();
        ResponseDTO<ProjectResponseDTO> projectRes = projectService.createProject(projectReq);
        String projectUuid = projectRes.getData().getUuid();

        // Stub Cerberos SSO Users endpoint
        wireMockServer.stubFor(get(urlEqualTo("/api/cerbos-resource-server/client/tenant/" + TENANT_ID + "/users"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "message": "Operación exitosa",
                                  "data": [
                                    {
                                      "uuid": "user-federated-001",
                                      "username": "investigador@domain.com",
                                      "firstName": "Investigador",
                                      "lastName": "Ambiental",
                                      "email": "investigador@domain.com",
                                      "status": "ACTIVE"
                                    }
                                  ]
                                }
                                """)));

        ProjectMemberAssignDTO assignReq = ProjectMemberAssignDTO.builder()
                .userUuid("user-federated-001")
                .projectRole("PROJECT_ANALYST")
                .build();

        ResponseDTO<ProjectMemberResponseDTO> memberRes = projectMemberService.assignMember(projectUuid, assignReq);

        assertThat(memberRes).isNotNull();
        assertThat(memberRes.getMessage()).isEqualTo("Miembro asignado exitosamente");
        assertThat(memberRes.getData().getProjectRole()).isEqualTo("PROJECT_ANALYST");
        assertThat(memberRes.getData().getUserSummary()).isNotNull();
        assertThat(memberRes.getData().getUserSummary().getUsername()).isEqualTo("investigador@domain.com");
        assertThat(memberRes.getData().getUserSummary().getFirstName()).isEqualTo("Investigador");

        // Verify member entity saved in real Postgres
        List<ProjectMember> persistedMembers = projectMemberRepository.findAllByProjectUuidAndIsDeletedFalse(projectUuid);
        assertThat(persistedMembers).hasSize(1);
        assertThat(persistedMembers.get(0).getUserUuid()).isEqualTo("user-federated-001");
        assertThat(persistedMembers.get(0).getProjectRole()).isEqualTo("PROJECT_ANALYST");
    }

    @Test
    @DisplayName("given_cerberos500InternalError_when_assignMember_then_fallsBackGracefullyWithoutFailing")
    void given_cerberos500InternalError_when_assignMember_then_fallsBackGracefullyWithoutFailing() {
        ProjectCreateRequestDTO projectReq = ProjectCreateRequestDTO.builder()
                .code("PRJ-FALLBACK-01")
                .name("Proyecto Resiliencia")
                .sector("FORESTAL")
                .build();
        ResponseDTO<ProjectResponseDTO> projectRes = projectService.createProject(projectReq);
        String projectUuid = projectRes.getData().getUuid();

        // Stub Cerberos SSO failing with 500
        wireMockServer.stubFor(get(urlEqualTo("/api/cerbos-resource-server/client/tenant/" + TENANT_ID + "/users"))
                .willReturn(aResponse().withStatus(500)));

        ProjectMemberAssignDTO assignReq = ProjectMemberAssignDTO.builder()
                .userUuid("user-offline-001")
                .projectRole("PROJECT_SPECIALIST")
                .build();

        ResponseDTO<ProjectMemberResponseDTO> memberRes = projectMemberService.assignMember(projectUuid, assignReq);

        // Verification: Project member is assigned successfully despite Cerberos downtime (fallback active)
        assertThat(memberRes).isNotNull();
        assertThat(memberRes.getData().getProjectRole()).isEqualTo("PROJECT_SPECIALIST");
        assertThat(memberRes.getData().getUserSummary()).isNull();

        Optional<ProjectMember> memberInDb = projectMemberRepository
                .findByProjectUuidAndUserUuidAndIsDeletedFalse(projectUuid, "user-offline-001");
        assertThat(memberInDb).isPresent();
    }

    @Test
    @DisplayName("given_cerberosTimeout_when_assignMember_then_fallsBackGracefully")
    void given_cerberosTimeout_when_assignMember_then_fallsBackGracefully() {
        ProjectCreateRequestDTO projectReq = ProjectCreateRequestDTO.builder()
                .code("PRJ-TIMEOUT-01")
                .name("Proyecto Timeout")
                .sector("MINERIA")
                .build();
        ResponseDTO<ProjectResponseDTO> projectRes = projectService.createProject(projectReq);
        String projectUuid = projectRes.getData().getUuid();

        // Stub Cerberos with delay of 4500ms (timeout configured at 3000ms)
        wireMockServer.stubFor(get(urlEqualTo("/api/cerbos-resource-server/client/tenant/" + TENANT_ID + "/users"))
                .willReturn(aResponse()
                        .withFixedDelay(4500)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"data\":[]}")));

        ProjectMemberAssignDTO assignReq = ProjectMemberAssignDTO.builder()
                .userUuid("user-delayed-001")
                .projectRole("PROJECT_CONSULTANT")
                .build();

        ResponseDTO<ProjectMemberResponseDTO> memberRes = projectMemberService.assignMember(projectUuid, assignReq);

        assertThat(memberRes).isNotNull();
        assertThat(memberRes.getData().getProjectRole()).isEqualTo("PROJECT_CONSULTANT");
        assertThat(memberRes.getData().getUserSummary()).isNull();
    }
}
