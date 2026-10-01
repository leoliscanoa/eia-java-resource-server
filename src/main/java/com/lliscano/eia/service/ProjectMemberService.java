package com.lliscano.eia.service;

import com.lliscano.commons.components.RequestContextHolder;
import com.lliscano.commons.dtos.AppUserSummaryDTO;
import com.lliscano.commons.dtos.RequestContextData;
import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.commons.exceptions.RecordNotFoundException;
import com.lliscano.eia.mapper.ProjectMemberMapper;
import com.lliscano.eia.model.dto.request.ProjectMemberAssignDTO;
import com.lliscano.eia.model.dto.response.ProjectMemberResponseDTO;
import com.lliscano.eia.model.entity.Project;
import com.lliscano.eia.model.entity.ProjectMember;
import com.lliscano.eia.repository.ProjectMemberRepository;
import com.lliscano.eia.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMemberMapper projectMemberMapper;
    private final CerberosClientService cerberosClientService;

    @Transactional
    public ResponseDTO<ProjectMemberResponseDTO> assignMember(String projectUuid, ProjectMemberAssignDTO request) {
        RequestContextData context = RequestContextHolder.getContext();
        String username = (context != null && context.getSub() != null) ? context.getSub() : "system";
        String tenantId = (context != null && context.getTenant() != null) ? context.getTenant() : null;

        Project project = projectRepository.findByUuidAndIsDeletedFalse(projectUuid)
                .orElseThrow(() -> new RecordNotFoundException("Proyecto no encontrado con UUID: " + projectUuid));

        Optional<ProjectMember> existingMemberOpt = projectMemberRepository
                .findByProjectUuidAndUserUuidAndIsDeletedFalse(projectUuid, request.getUserUuid());

        ProjectMember member;
        if (existingMemberOpt.isPresent()) {
            member = existingMemberOpt.get();
            member.setProjectRole(request.getProjectRole());
            member.setActive(true);
            member.setLastModifiedBy(username);
        } else {
            member = ProjectMember.builder()
                    .project(project)
                    .userUuid(request.getUserUuid())
                    .projectRole(request.getProjectRole())
                    .isActive(true)
                    .isDeleted(false)
                    .createdBy(username)
                    .build();
        }

        ProjectMember savedMember = projectMemberRepository.save(member);
        ProjectMemberResponseDTO responseDTO = projectMemberMapper.toDto(savedMember);

        // Enriquecer con información federada de Cerberos SSO
        if (tenantId != null && !tenantId.isBlank()) {
            List<AppUserSummaryDTO> tenantUsers = cerberosClientService.getTenantUsers(tenantId);
            tenantUsers.stream()
                    .filter(u -> request.getUserUuid().equalsIgnoreCase(u.getUuid()) || request.getUserUuid().equalsIgnoreCase(u.getUsername()))
                    .findFirst()
                    .ifPresent(responseDTO::setUserSummary);
        }

        return ResponseDTO.<ProjectMemberResponseDTO>builder()
                .message("Miembro asignado exitosamente")
                .data(responseDTO)
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseDTO<ArrayList<ProjectMemberResponseDTO>> getMembers(String projectUuid) {
        RequestContextData context = RequestContextHolder.getContext();
        String tenantId = (context != null && context.getTenant() != null) ? context.getTenant() : null;

        if (!projectRepository.existsById(
                projectRepository.findByUuidAndIsDeletedFalse(projectUuid)
                        .orElseThrow(() -> new RecordNotFoundException("Proyecto no encontrado con UUID: " + projectUuid))
                        .getId())) {
            throw new RecordNotFoundException("Proyecto no encontrado con UUID: " + projectUuid);
        }

        List<ProjectMember> members = projectMemberRepository.findAllByProjectUuidAndIsDeletedFalse(projectUuid);

        Map<String, AppUserSummaryDTO> userMap = Map.of();
        if (tenantId != null && !tenantId.isBlank()) {
            try {
                List<AppUserSummaryDTO> tenantUsers = cerberosClientService.getTenantUsers(tenantId);
                userMap = tenantUsers.stream()
                        .filter(u -> u.getUuid() != null)
                        .collect(Collectors.toMap(AppUserSummaryDTO::getUuid, u -> u, (a, b) -> a));
            } catch (Exception e) {
                log.warn("No se pudo obtener la lista de usuarios de Cerberos SSO: {}", e.getMessage());
            }
        }

        Map<String, AppUserSummaryDTO> finalUserMap = userMap;
        ArrayList<ProjectMemberResponseDTO> dtos = members.stream().map(m -> {
            ProjectMemberResponseDTO dto = projectMemberMapper.toDto(m);
            if (finalUserMap.containsKey(m.getUserUuid())) {
                dto.setUserSummary(finalUserMap.get(m.getUserUuid()));
            }
            return dto;
        }).collect(Collectors.toCollection(ArrayList::new));

        return ResponseDTO.<ArrayList<ProjectMemberResponseDTO>>builder()
                .message("Operación exitosa")
                .data(dtos)
                .build();
    }

    @Transactional
    public ResponseDTO<String> removeMember(String projectUuid, String userUuid) {
        RequestContextData context = RequestContextHolder.getContext();
        String username = (context != null && context.getSub() != null) ? context.getSub() : "system";

        ProjectMember member = projectMemberRepository.findByProjectUuidAndUserUuidAndIsDeletedFalse(projectUuid, userUuid)
                .orElseThrow(() -> new RecordNotFoundException("Miembro no encontrado con UUID: " + userUuid + " en el proyecto: " + projectUuid));

        member.setDeleted(true);
        member.setActive(false);
        member.setLastModifiedBy(username);
        projectMemberRepository.save(member);

        return ResponseDTO.<String>builder()
                .message("Miembro removido exitosamente")
                .data(userUuid)
                .build();
    }
}
