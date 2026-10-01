package com.lliscano.eia.controller;

import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.eia.model.dto.request.ProjectMemberAssignDTO;
import com.lliscano.eia.model.dto.response.ProjectMemberResponseDTO;
import com.lliscano.eia.service.ProjectMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/v1/projects/{projectUuid}/members")
@RequiredArgsConstructor
@Tag(name = "EIA Project Members", description = "Endpoints de gestión de miembros de proyectos EIA")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @Operation(summary = "Asignar o actualizar rol de un miembro en el proyecto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Miembro asignado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado para gestionar miembros del proyecto"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    @PreAuthorize("hasRole('EIA_ADMIN') or hasAuthority('EIA_ADMIN') or @projectSecurity.isProjectLead(#projectUuid)")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDTO<ProjectMemberResponseDTO>> assignMember(
            @PathVariable("projectUuid") String projectUuid,
            @Valid @RequestBody ProjectMemberAssignDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectMemberService.assignMember(projectUuid, request));
    }

    @Operation(summary = "Listar miembros activos asignados al proyecto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de miembros obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDTO<ArrayList<ProjectMemberResponseDTO>>> getMembers(
            @PathVariable("projectUuid") String projectUuid) {
        return ResponseEntity.ok(projectMemberService.getMembers(projectUuid));
    }

    @Operation(summary = "Remover un miembro del proyecto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Miembro removido exitosamente"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Proyecto o miembro no encontrado")
    })
    @PreAuthorize("hasRole('EIA_ADMIN') or hasAuthority('EIA_ADMIN') or @projectSecurity.isProjectLead(#projectUuid)")
    @DeleteMapping(value = "/{userUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDTO<String>> removeMember(
            @PathVariable("projectUuid") String projectUuid,
            @PathVariable("userUuid") String userUuid) {
        return ResponseEntity.ok(projectMemberService.removeMember(projectUuid, userUuid));
    }
}
