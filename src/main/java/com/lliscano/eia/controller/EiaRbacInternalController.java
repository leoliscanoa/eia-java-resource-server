package com.lliscano.eia.controller;

import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.eia.model.dto.response.EiaRbacProfileDTO;
import com.lliscano.eia.repository.ProjectMemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "EIA ReBAC Internal", description = "Endpoints internos para resolución de membresías y permisos ReBAC")
public class EiaRbacInternalController {

    private final ProjectMemberRepository projectMemberRepository;

    @GetMapping("/{uuid}/eia-rbac")
    @Operation(summary = "Consultar perfil y asignaciones de proyectos ReBAC para un usuario")
    public ResponseEntity<ResponseDTO<EiaRbacProfileDTO>> getEiaRbacProfile(@PathVariable("uuid") String uuid) {
        log.debug("Resolviendo perfil ReBAC interno para usuario [{}]", uuid);

        boolean isEiaAdmin = projectMemberRepository.isUserEiaAdmin(uuid, uuid);
        List<String> assignedProjectIds = projectMemberRepository.findAssignedProjectUuidsByUserIdentifier(uuid, uuid);

        EiaRbacProfileDTO profile = EiaRbacProfileDTO.builder()
                .isEiaAdmin(isEiaAdmin)
                .assignedProjectIds(assignedProjectIds)
                .build();

        return ResponseEntity.ok(ResponseDTO.<EiaRbacProfileDTO>builder()
                .message("Perfil ReBAC resuelto exitosamente")
                .data(profile)
                .build());
    }
}
