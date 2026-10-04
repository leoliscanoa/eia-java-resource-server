package com.lliscano.eia.service;

import com.lliscano.commons.components.RequestContextHolder;
import com.lliscano.commons.components.audit.AsyncAuditPublisher;
import com.lliscano.commons.components.audit.SensitiveDataSanitizer;
import com.lliscano.commons.dtos.RequestContextData;
import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.commons.dtos.audit.ActorDTO;
import com.lliscano.commons.dtos.audit.AuditEventDTO;
import com.lliscano.commons.exceptions.RecordNotFoundException;
import com.lliscano.commons.exceptions.UnauthorizedEntityException;
import com.lliscano.eia.mapper.ProjectMapper;
import com.lliscano.eia.model.dto.request.ProjectCreateRequestDTO;
import com.lliscano.eia.model.dto.request.ProjectTerritoryRequestDTO;
import com.lliscano.eia.model.dto.request.ProjectUpdateRequestDTO;
import com.lliscano.eia.model.dto.response.ProjectResponseDTO;
import com.lliscano.eia.model.entity.Project;
import com.lliscano.eia.model.entity.ProjectMember;
import com.lliscano.eia.model.entity.ProjectTerritory;
import com.lliscano.eia.model.entity.Territory;
import com.lliscano.eia.repository.ProjectRepository;
import com.lliscano.eia.repository.TerritoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TerritoryRepository territoryRepository;
    private final ProjectMapper projectMapper;
    private final ProjectRoleService projectRoleService;
    private final AsyncAuditPublisher auditPublisher;
    private final SensitiveDataSanitizer sensitiveDataSanitizer;

    private void publishAudit(String entityId, String tenantId, String action, Object previousState, Object currentState) {
        try {
            RequestContextData context = RequestContextHolder.getContext();
            String username = (context != null && context.getSub() != null) ? context.getSub() : "system";
            String userUuid = (context != null && context.getUuid() != null) ? context.getUuid() : null;
            String ipAddress = (context != null && context.getIpAddress() != null) ? context.getIpAddress() : null;

            Map<String, Object> prevSanitized = previousState != null ? sensitiveDataSanitizer.sanitizeObject(previousState) : null;
            Map<String, Object> currSanitized = currentState != null ? sensitiveDataSanitizer.sanitizeObject(currentState) : null;

            AuditEventDTO auditEvent = AuditEventDTO.builder()
                    .eventId(UUID.randomUUID().toString())
                    .correlationId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .projectId(entityId) // FEAT-029: Propagación de projectId para control ReBAC e índice sparse
                    .serviceName("eia-java-resource-server")
                    .entityName("Project")
                    .entityId(entityId)
                    .action(action)
                    .timestamp(Instant.now())
                    .actor(ActorDTO.builder()
                            .username(username)
                            .userUuid(userUuid)
                            .clientId("EIA")
                            .ipAddress(ipAddress)
                            .build())
                    .previousState(prevSanitized)
                    .currentState(currSanitized)
                    .build();

            auditPublisher.publishEvent(auditEvent);
        } catch (Exception ex) {
            log.warn("Error emitiendo evento de auditoría para proyecto [{}]: {}", entityId, ex.getMessage());
        }
    }

    @Transactional
    public ResponseDTO<ProjectResponseDTO> createProject(ProjectCreateRequestDTO request) {
        RequestContextData context = RequestContextHolder.getContext();
        String tenantId = (context != null && context.getTenant() != null) ? context.getTenant() : null;
        if (tenantId == null || tenantId.isBlank()) {
            throw new UnauthorizedEntityException("No se ha especificado un tenant válido en la solicitud");
        }

        String username = (context != null && context.getSub() != null) ? context.getSub() : "system";

        if (projectRepository.existsByTenantIdAndCodeAndIsDeletedFalse(tenantId, request.getCode())) {
            throw new DataIntegrityViolationException("Ya existe un proyecto con el código especificado: " + request.getCode());
        }

        Project project = projectMapper.toEntity(request);
        project.setTenantId(tenantId);
        project.setCreatedBy(username);
        project.setStatus("ACTIVE");
        project.setDeleted(false);

        // Asociar director de proyecto inicial resolviendo el rol de liderazgo desde el catálogo dinámico
        if (request.getLeadUserUuid() != null && !request.getLeadUserUuid().isBlank()) {
            String leadRoleCode = projectRoleService.getLeadRoleCode();
            ProjectMember leadMember = ProjectMember.builder()
                    .project(project)
                    .userUuid(request.getLeadUserUuid())
                    .projectRole(leadRoleCode)
                    .isActive(true)
                    .isDeleted(false)
                    .build();
            project.getMembers().add(leadMember);
        }

        // Asociar territorios de influencia
        if (request.getTerritories() != null && !request.getTerritories().isEmpty()) {
            Set<String> territoryUuids = request.getTerritories().stream()
                    .map(ProjectTerritoryRequestDTO::getTerritoryUuid)
                    .collect(Collectors.toSet());

            List<Territory> territories = territoryRepository.findAllByUuidInAndTenantIdAndIsDeletedFalse(territoryUuids, tenantId);
            Map<String, Territory> territoryMap = territories.stream()
                    .collect(Collectors.toMap(Territory::getUuid, t -> t));

            for (ProjectTerritoryRequestDTO tReq : request.getTerritories()) {
                Territory territory = territoryMap.get(tReq.getTerritoryUuid());
                if (territory != null) {
                    ProjectTerritory pt = ProjectTerritory.builder()
                            .project(project)
                            .territory(territory)
                            .influenceType(tReq.getInfluenceType())
                            .createdBy(username)
                            .build();
                    project.getTerritories().add(pt);
                } else {
                    log.warn("Territorio no encontrado para UUID: {} en tenant: {}", tReq.getTerritoryUuid(), tenantId);
                }
            }
        }

        Project savedProject = projectRepository.save(project);
        publishAudit(savedProject.getUuid(), tenantId, "CREATE", null, savedProject);

        ProjectResponseDTO responseDTO = projectMapper.toDto(savedProject);
        responseDTO.setUserRole(projectRoleService.getLeadRoleCode());

        return ResponseDTO.<ProjectResponseDTO>builder()
                .message("Proyecto creado exitosamente")
                .data(responseDTO)
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseDTO<ArrayList<ProjectResponseDTO>> listProjects() {
        RequestContextData context = RequestContextHolder.getContext();
        String tenantId = (context != null && context.getTenant() != null) ? context.getTenant() : null;
        if (tenantId == null || tenantId.isBlank()) {
            throw new UnauthorizedEntityException("No se ha especificado un tenant válido en la solicitud");
        }

        String userUuid = (context != null && context.getUuid() != null) ? context.getUuid() : null;
        String username = (context != null && context.getSub() != null) ? context.getSub() : null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "EIA_ADMIN".equals(a.getAuthority()) || "ROLE_EIA_ADMIN".equals(a.getAuthority()));

        List<Project> projects;
        if (isAdmin) {
            projects = projectRepository.findAllByTenantIdAndIsDeletedFalse(tenantId);
        } else {
            projects = projectRepository.findAllByTenantIdAndMemberUserUuid(tenantId, userUuid, username);
        }

        String defaultRole = projectRoleService.getDefaultMemberRoleCode();
        ArrayList<ProjectResponseDTO> dtos = projects.stream().map(p -> {
            ProjectResponseDTO dto = projectMapper.toDto(p);
            String role = defaultRole;
            if (p.getMembers() != null) {
                for (ProjectMember m : p.getMembers()) {
                    if (m.isActive() && !m.isDeleted() &&
                            ((userUuid != null && userUuid.equalsIgnoreCase(m.getUserUuid())) ||
                             (username != null && username.equalsIgnoreCase(m.getUserUuid())))) {
                        role = m.getProjectRole();
                        break;
                    }
                }
            }
            if (isAdmin && defaultRole.equals(role)) {
                role = "EIA_ADMIN";
            }
            dto.setUserRole(role);
            return dto;
        }).collect(Collectors.toCollection(ArrayList::new));

        return ResponseDTO.<ArrayList<ProjectResponseDTO>>builder()
                .message("Operación exitosa")
                .data(dtos)
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseDTO<ProjectResponseDTO> getProjectByUuid(String uuid) {
        RequestContextData context = RequestContextHolder.getContext();
        String tenantId = (context != null && context.getTenant() != null) ? context.getTenant() : null;

        Project project = (tenantId != null && !tenantId.isBlank())
                ? projectRepository.findByUuidAndTenantIdAndIsDeletedFalse(uuid, tenantId)
                    .orElseThrow(() -> new RecordNotFoundException("Proyecto no encontrado con UUID: " + uuid))
                : projectRepository.findByUuidAndIsDeletedFalse(uuid)
                    .orElseThrow(() -> new RecordNotFoundException("Proyecto no encontrado con UUID: " + uuid));

        ProjectResponseDTO dto = projectMapper.toDto(project);
        return ResponseDTO.<ProjectResponseDTO>builder()
                .message("Operación exitosa")
                .data(dto)
                .build();
    }

    @Transactional
    public ResponseDTO<ProjectResponseDTO> updateProject(String uuid, ProjectUpdateRequestDTO request) {
        RequestContextData context = RequestContextHolder.getContext();
        String tenantId = (context != null && context.getTenant() != null) ? context.getTenant() : null;
        String username = (context != null && context.getSub() != null) ? context.getSub() : "system";

        Project project = (tenantId != null && !tenantId.isBlank())
                ? projectRepository.findByUuidAndTenantIdAndIsDeletedFalse(uuid, tenantId)
                    .orElseThrow(() -> new RecordNotFoundException("Proyecto no encontrado con UUID: " + uuid))
                : projectRepository.findByUuidAndIsDeletedFalse(uuid)
                    .orElseThrow(() -> new RecordNotFoundException("Proyecto no encontrado con UUID: " + uuid));

        ProjectResponseDTO previousState = projectMapper.toDto(project);

        if (request.getName() != null) project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getSector() != null) project.setSector(request.getSector());
        if (request.getTypology() != null) project.setTypology(request.getTypology());
        if (request.getStage() != null) project.setStage(request.getStage());
        if (request.getStartDate() != null) project.setStartDate(request.getStartDate());
        if (request.getEstimatedEndDate() != null) project.setEstimatedEndDate(request.getEstimatedEndDate());
        if (request.getBudgetEstimate() != null) project.setBudgetEstimate(request.getBudgetEstimate());
        if (request.getCurrency() != null) project.setCurrency(request.getCurrency());
        if (request.getStatus() != null) project.setStatus(request.getStatus());

        project.setLastModifiedBy(username);
        Project saved = projectRepository.save(project);

        publishAudit(saved.getUuid(), tenantId, "UPDATE", previousState, projectMapper.toDto(saved));

        return ResponseDTO.<ProjectResponseDTO>builder()
                .message("Proyecto actualizado exitosamente")
                .data(projectMapper.toDto(saved))
                .build();
    }

    @Transactional
    public ResponseDTO<String> deleteProject(String uuid) {
        RequestContextData context = RequestContextHolder.getContext();
        String tenantId = (context != null && context.getTenant() != null) ? context.getTenant() : null;
        String username = (context != null && context.getSub() != null) ? context.getSub() : "system";

        Project project = (tenantId != null && !tenantId.isBlank())
                ? projectRepository.findByUuidAndTenantIdAndIsDeletedFalse(uuid, tenantId)
                    .orElseThrow(() -> new RecordNotFoundException("Proyecto no encontrado con UUID: " + uuid))
                : projectRepository.findByUuidAndIsDeletedFalse(uuid)
                    .orElseThrow(() -> new RecordNotFoundException("Proyecto no encontrado con UUID: " + uuid));

        ProjectResponseDTO previousState = projectMapper.toDto(project);

        project.setDeleted(true);
        project.setLastModifiedBy(username);
        projectRepository.save(project);

        publishAudit(uuid, tenantId, "DELETE", previousState, null);

        return ResponseDTO.<String>builder()
                .message("Proyecto eliminado exitosamente")
                .data(uuid)
                .build();
    }
}
