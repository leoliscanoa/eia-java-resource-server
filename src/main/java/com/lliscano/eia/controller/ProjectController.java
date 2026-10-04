package com.lliscano.eia.controller;

import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.eia.model.dto.request.ProjectCreateRequestDTO;
import com.lliscano.eia.model.dto.request.ProjectUpdateRequestDTO;
import com.lliscano.eia.model.dto.response.ProjectResponseDTO;
import com.lliscano.eia.service.ProjectService;
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
@RequestMapping({"/v1/projects", "/v1/admin/projects"})
@RequiredArgsConstructor
@Tag(name = "EIA Projects", description = "Endpoints de gestión de proyectos EIA")
@PreAuthorize("hasRole('ADMIN_PROJECTS')")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "Crear nuevo proyecto EIA con caracterización completa")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proyecto creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado para crear proyectos")
    })
    @PreAuthorize("hasAuthority('ADMIN_PROJECTS_CREATE')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDTO<ProjectResponseDTO>> createProject(
            @Valid @RequestBody ProjectCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @Operation(summary = "Listar proyectos del tenant según permisos del usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de proyectos recuperada exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PreAuthorize("hasAuthority('ADMIN_PROJECTS_READ')")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDTO<ArrayList<ProjectResponseDTO>>> listProjects() {
        return ResponseEntity.ok(projectService.listProjects());
    }

    @Operation(summary = "Obtener detalle de un proyecto por UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyecto encontrado"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    @PreAuthorize("hasAuthority('ADMIN_PROJECTS_READ')")
    @GetMapping(value = "/{projectUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDTO<ProjectResponseDTO>> getProjectByUuid(
            @PathVariable("projectUuid") String projectUuid) {
        return ResponseEntity.ok(projectService.getProjectByUuid(projectUuid));
    }

    @Operation(summary = "Actualizar información de un proyecto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyecto actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    @PreAuthorize("hasAuthority('ADMIN_PROJECTS_UPDATE') and @projectSecurity.isProjectLead(#projectUuid)")
    @PutMapping(value = "/{projectUuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDTO<ProjectResponseDTO>> updateProject(
            @PathVariable("projectUuid") String projectUuid,
            @Valid @RequestBody ProjectUpdateRequestDTO request) {
        return ResponseEntity.ok(projectService.updateProject(projectUuid, request));
    }

    @Operation(summary = "Eliminar (soft-delete) un proyecto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyecto eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    @PreAuthorize("hasAuthority('ADMIN_PROJECTS_DELETE') and @projectSecurity.isProjectLead(#projectUuid)")
    @DeleteMapping(value = "/{projectUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDTO<String>> deleteProject(
            @PathVariable("projectUuid") String projectUuid) {
        return ResponseEntity.ok(projectService.deleteProject(projectUuid));
    }
}
