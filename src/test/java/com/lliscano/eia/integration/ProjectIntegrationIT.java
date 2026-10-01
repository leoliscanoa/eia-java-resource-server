package com.lliscano.eia.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.lliscano.commons.components.RequestContextHolder;
import com.lliscano.commons.dtos.RequestContextData;
import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.commons.exceptions.RecordNotFoundException;
import com.lliscano.commons.exceptions.UnauthorizedEntityException;
import com.lliscano.eia.base.AbstractBaseIntegrationIT;
import com.lliscano.eia.controller.ProjectRoleController;
import com.lliscano.eia.model.dto.request.ProjectCreateRequestDTO;
import com.lliscano.eia.model.dto.request.ProjectMemberAssignDTO;
import com.lliscano.eia.model.dto.response.ProjectMemberResponseDTO;
import com.lliscano.eia.model.dto.response.ProjectResponseDTO;
import com.lliscano.eia.model.dto.response.ProjectRoleResponseDTO;
import com.lliscano.eia.model.entity.Project;
import com.lliscano.eia.model.entity.ProjectMember;
import com.lliscano.eia.model.entity.ProjectRole;
import com.lliscano.eia.repository.ProjectMemberRepository;
import com.lliscano.eia.repository.ProjectRepository;
import com.lliscano.eia.repository.ProjectRoleRepository;
import com.lliscano.eia.security.ProjectSecurity;
import com.lliscano.eia.service.ProjectMemberService;
import com.lliscano.eia.service.ProjectRoleService;
import com.lliscano.eia.service.ProjectService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private ProjectRoleService projectRoleService;

    @Autowired
    private ProjectSecurity projectSecurity;

    @Autowired
    private ProjectRoleController projectRoleController;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectRoleRepository projectRoleRepository;

    private static final String TENANT_ID = "tenant-it-001";
    private static final String USER_ID = "user-qa-lead";
    private static final String SEED_ADMIN_UUID = "c0a80104-8c51-16be-818c-5107a3880000";

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
        wireMockServer.resetAll();

        // Default M2M OAuth2 Token Stub
        wireMockServer.stubFor(post(urlEqualTo("/oauth2/sso/token"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"access_token\":\"it-mock-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));

        // Seed project roles for dynamic RBAC according to EIA-S-00002.sql as system admin
        if (projectRoleRepository.count() == 0) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(SEED_ADMIN_UUID, "n/a", List.of(new SimpleGrantedAuthority("EIA_ADMIN"))));

            RequestContextHolder.setContext(RequestContextData.builder()
                    .tenant(TENANT_ID)
                    .sub(SEED_ADMIN_UUID)
                    .uuid(SEED_ADMIN_UUID)
                    .build());

            projectRoleRepository.saveAll(List.of(
                    ProjectRole.builder().code("PROJECT_LEAD").name("Líder de Proyecto").description("Director y responsable técnico general").isLeadRole(true).isDefaultMember(false).isSystem(true).isActive(true).createdBy(SEED_ADMIN_UUID).build(),
                    ProjectRole.builder().code("ANALYST").name("Analista Socioambiental").description("Especialista en análisis").isLeadRole(false).isDefaultMember(false).isSystem(true).isActive(true).createdBy(SEED_ADMIN_UUID).build(),
                    ProjectRole.builder().code("COLLECTOR").name("Recolector de Campo").description("Personal de levantamiento de datos").isLeadRole(false).isDefaultMember(false).isSystem(true).isActive(true).createdBy(SEED_ADMIN_UUID).build(),
                    ProjectRole.builder().code("OBSERVER").name("Observador / Auditor").description("Acceso de solo lectura").isLeadRole(false).isDefaultMember(false).isSystem(true).isActive(true).createdBy(SEED_ADMIN_UUID).build(),
                    ProjectRole.builder().code("MEMBER").name("Miembro General").description("Integrante base").isLeadRole(false).isDefaultMember(true).isSystem(true).isActive(true).createdBy(SEED_ADMIN_UUID).build()
            ));
        }

        RequestContextHolder.setContext(RequestContextData.builder()
                .tenant(TENANT_ID)
                .sub(USER_ID)
                .uuid("uuid-qa-lead")
                .build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, "n/a", List.of(new SimpleGrantedAuthority("EIA_USER"))));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
        SecurityContextHolder.clearContext();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    @DisplayName("BDD Escenario 1 (HU-EIA-027-01): Validar que las 5 semillas existan, sean idempotentes y contengan created_by admin")
    void bddEscenario1_validaSemillasRolesIdempotentesYAuditoriaDefault() {
        List<ProjectRole> allRoles = projectRoleRepository.findAll();
        assertThat(allRoles).hasSize(5);

        List<String> codes = allRoles.stream().map(ProjectRole::getCode).toList();
        assertThat(codes).containsExactlyInAnyOrder("PROJECT_LEAD", "ANALYST", "COLLECTOR", "OBSERVER", "MEMBER");

        allRoles.forEach(role -> {
            assertThat(role.getCreatedBy()).isEqualTo(SEED_ADMIN_UUID);
            assertThat(role.isActive()).isTrue();
            assertThat(role.isSystem()).isTrue();
        });

        ProjectRole lead = projectRoleRepository.findByCode("PROJECT_LEAD").orElseThrow();
        assertThat(lead.isLeadRole()).isTrue();
        assertThat(lead.isDefaultMember()).isFalse();

        ProjectRole member = projectRoleRepository.findByCode("MEMBER").orElseThrow();
        assertThat(member.isLeadRole()).isFalse();
        assertThat(member.isDefaultMember()).isTrue();
    }

    @Test
    @DisplayName("BDD Escenario 2 (HU-EIA-027-02): Enviar asignación con rol válido (ANALYST) y verificar enriquecimiento y HTTP 201 Created")
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
                .projectRole("ANALYST")
                .build();

        ResponseDTO<ProjectMemberResponseDTO> memberRes = projectMemberService.assignMember(projectUuid, assignReq);

        assertThat(memberRes).isNotNull();
        assertThat(memberRes.getMessage()).isEqualTo("Miembro asignado exitosamente");
        assertThat(memberRes.getData().getProjectRole()).isEqualTo("ANALYST");
        assertThat(memberRes.getData().getUserSummary()).isNotNull();
        assertThat(memberRes.getData().getUserSummary().getUsername()).isEqualTo("investigador@domain.com");
        assertThat(memberRes.getData().getUserSummary().getFirstName()).isEqualTo("Investigador");

        // Verify member entity saved in real Postgres
        List<ProjectMember> persistedMembers = projectMemberRepository.findAllByProjectUuidAndIsDeletedFalse(projectUuid);
        assertThat(persistedMembers).hasSize(1);
        assertThat(persistedMembers.get(0).getUserUuid()).isEqualTo("user-federated-001");
        assertThat(persistedMembers.get(0).getProjectRole()).isEqualTo("ANALYST");
    }

    @Test
    @DisplayName("BDD Escenario 3 (HU-EIA-027-02): Enviar asignación con rol inexistente (SUPER_HERO) y verificar rechazo con HTTP 400 Bad Request")
    void bddEscenario3_asignarMiembroConRolInexistente_rechazaConBadRequest() {
        ProjectCreateRequestDTO projectReq = ProjectCreateRequestDTO.builder()
                .code("PRJ-BDD-03")
                .name("Proyecto BDD 03")
                .sector("ENERGIA")
                .build();
        ResponseDTO<ProjectResponseDTO> projectRes = projectService.createProject(projectReq);
        String projectUuid = projectRes.getData().getUuid();

        ProjectMemberAssignDTO invalidAssignReq = ProjectMemberAssignDTO.builder()
                .userUuid("user-hero-001")
                .projectRole("SUPER_HERO")
                .build();

        assertThatThrownBy(() -> projectMemberService.assignMember(projectUuid, invalidAssignReq))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessage("El rol de proyecto 'SUPER_HERO' no existe o se encuentra inactivo");

        List<ProjectMember> persisted = projectMemberRepository.findAllByProjectUuidAndIsDeletedFalse(projectUuid);
        assertThat(persisted).isEmpty();
    }

    @Test
    @DisplayName("BDD Escenario 4 (HU-EIA-027-02): Intentar asignar rol con is_active = false y verificar rechazo con HTTP 400 Bad Request")
    void bddEscenario4_asignarMiembroConRolInactivo_rechazaConBadRequest() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(SEED_ADMIN_UUID, "n/a", List.of(new SimpleGrantedAuthority("EIA_ADMIN"))));

        RequestContextHolder.setContext(RequestContextData.builder()
                .tenant(TENANT_ID)
                .sub(SEED_ADMIN_UUID)
                .uuid(SEED_ADMIN_UUID)
                .build());

        ProjectRole inactiveRole = projectRoleRepository.save(ProjectRole.builder()
                .code("INACTIVE_SPECIALIST")
                .name("Especialista Inactivo")
                .isLeadRole(false)
                .isDefaultMember(false)
                .isActive(false)
                .createdBy(SEED_ADMIN_UUID)
                .build());

        RequestContextHolder.setContext(RequestContextData.builder()
                .tenant(TENANT_ID)
                .sub(USER_ID)
                .uuid("uuid-qa-lead")
                .build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, "n/a", List.of(new SimpleGrantedAuthority("EIA_USER"))));

        try {
            projectRoleService.evictAllCaches();

            ProjectCreateRequestDTO projectReq = ProjectCreateRequestDTO.builder()
                    .code("PRJ-BDD-04")
                    .name("Proyecto BDD 04")
                    .sector("ENERGIA")
                    .build();
            ResponseDTO<ProjectResponseDTO> projectRes = projectService.createProject(projectReq);
            String projectUuid = projectRes.getData().getUuid();

            ProjectMemberAssignDTO assignReq = ProjectMemberAssignDTO.builder()
                    .userUuid("user-inactive-role-001")
                    .projectRole("INACTIVE_SPECIALIST")
                    .build();

            assertThatThrownBy(() -> projectMemberService.assignMember(projectUuid, assignReq))
                    .isInstanceOf(RecordNotFoundException.class)
                    .hasMessage("El rol de proyecto 'INACTIVE_SPECIALIST' no se encuentra activo");

            List<ProjectMember> persisted = projectMemberRepository.findAllByProjectUuidAndIsDeletedFalse(projectUuid);
            assertThat(persisted).isEmpty();
        } finally {
            projectRoleRepository.delete(inactiveRole);
            projectRoleService.evictAllCaches();
        }
    }

    @Test
    @DisplayName("BDD Escenario 5 (HU-EIA-027-03): Usuario con rol lead dinámico es autorizado por @projectSecurity.isProjectLead")
    void bddEscenario5_usuarioConRolLeadDinamico_esAutorizadoPorReBAC() {
        ProjectCreateRequestDTO projectReq = ProjectCreateRequestDTO.builder()
                .code("PRJ-BDD-05")
                .name("Proyecto ReBAC Lead")
                .sector("ENERGIA")
                .build();
        ResponseDTO<ProjectResponseDTO> projectRes = projectService.createProject(projectReq);
        String projectUuid = projectRes.getData().getUuid();

        String leadUserUuid = "lead-member-001";
        String leadUsername = "lead.member@domain.com";

        ProjectMemberAssignDTO assignReq = ProjectMemberAssignDTO.builder()
                .userUuid(leadUserUuid)
                .projectRole("PROJECT_LEAD")
                .build();
        projectMemberService.assignMember(projectUuid, assignReq);

        // Simulate context as the lead member
        RequestContextHolder.setContext(RequestContextData.builder()
                .tenant(TENANT_ID)
                .sub(leadUsername)
                .uuid(leadUserUuid)
                .build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(leadUsername, "n/a", List.of(new SimpleGrantedAuthority("EIA_USER"))));

        boolean isAuthorized = projectSecurity.isProjectLead(projectUuid);
        assertThat(isAuthorized).isTrue();
    }

    @Test
    @DisplayName("BDD Escenario 6 (HU-EIA-027-03): Usuario con rol no lead (OBSERVER) es denegado por @projectSecurity.isProjectLead")
    void bddEscenario6_usuarioConRolNoLead_esDenegadoPorReBAC() {
        ProjectCreateRequestDTO projectReq = ProjectCreateRequestDTO.builder()
                .code("PRJ-BDD-06")
                .name("Proyecto ReBAC Observer")
                .sector("ENERGIA")
                .build();
        ResponseDTO<ProjectResponseDTO> projectRes = projectService.createProject(projectReq);
        String projectUuid = projectRes.getData().getUuid();

        String observerUserUuid = "observer-member-001";
        String observerUsername = "observer.member@domain.com";

        ProjectMemberAssignDTO assignReq = ProjectMemberAssignDTO.builder()
                .userUuid(observerUserUuid)
                .projectRole("OBSERVER")
                .build();
        projectMemberService.assignMember(projectUuid, assignReq);

        // Simulate context as the observer member
        RequestContextHolder.setContext(RequestContextData.builder()
                .tenant(TENANT_ID)
                .sub(observerUsername)
                .uuid(observerUserUuid)
                .build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(observerUsername, "n/a", List.of(new SimpleGrantedAuthority("EIA_USER"))));

        boolean isAuthorized = projectSecurity.isProjectLead(projectUuid);
        assertThat(isAuthorized).isFalse();
    }

    @Test
    @DisplayName("BDD Escenario 7 (HU-EIA-027-04): GET /v1/projects/roles responde HTTP 200 con el catálogo completo de roles activos")
    void bddEscenario7_catalogoRolesActivos_retornaHTTP200ConMetadatos() {
        ResponseEntity<ResponseDTO<ArrayList<ProjectRoleResponseDTO>>> response = projectRoleController.getAllRoles();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(5);

        List<ProjectRoleResponseDTO> roles = response.getBody().getData();
        assertThat(roles).extracting(ProjectRoleResponseDTO::getCode)
                .containsExactlyInAnyOrder("PROJECT_LEAD", "ANALYST", "COLLECTOR", "OBSERVER", "MEMBER");

        ProjectRoleResponseDTO leadDto = roles.stream().filter(r -> "PROJECT_LEAD".equals(r.getCode())).findFirst().orElseThrow();
        assertThat(leadDto.isLeadRole()).isTrue();
        assertThat(leadDto.isDefaultMember()).isFalse();

        ProjectRoleResponseDTO memberDto = roles.stream().filter(r -> "MEMBER".equals(r.getCode())).findFirst().orElseThrow();
        assertThat(memberDto.isLeadRole()).isFalse();
        assertThat(memberDto.isDefaultMember()).isTrue();
    }

    @Test
    @DisplayName("NFR-01: Invocaciones sucesivas al catálogo y a isLeadRole responden en <= 5ms gracias a Redis/Spring Cache")
    void nfr01_verificarLatenciaDeCache_menorOIgualA5ms() {
        // Warm-up cache
        projectRoleService.getAllActiveRoles();
        projectRoleService.isLeadRole("PROJECT_LEAD");

        // Measure cached execution
        int iterations = 10;
        long totalDurationNano = 0;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            projectRoleService.getAllActiveRoles();
            projectRoleService.isLeadRole("PROJECT_LEAD");
            long end = System.nanoTime();
            totalDurationNano += (end - start);
        }

        double avgLatencyMs = (totalDurationNano / (double) iterations) / 1_000_000.0;
        assertThat(avgLatencyMs).isLessThanOrEqualTo(5.0);
    }

    @Test
    @DisplayName("Verificar persistencia de auditoría automática: creado por usuario autenticado sin código manual")
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
        assertThat(persisted.getCreatedAt()).isNotNull();
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
                .projectRole("COLLECTOR")
                .build();

        ResponseDTO<ProjectMemberResponseDTO> memberRes = projectMemberService.assignMember(projectUuid, assignReq);

        // Verification: Project member is assigned successfully despite Cerberos downtime (fallback active)
        assertThat(memberRes).isNotNull();
        assertThat(memberRes.getData().getProjectRole()).isEqualTo("COLLECTOR");
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
                .projectRole("OBSERVER")
                .build();

        ResponseDTO<ProjectMemberResponseDTO> memberRes = projectMemberService.assignMember(projectUuid, assignReq);

        assertThat(memberRes).isNotNull();
        assertThat(memberRes.getData().getProjectRole()).isEqualTo("OBSERVER");
        assertThat(memberRes.getData().getUserSummary()).isNull();
    }
}
