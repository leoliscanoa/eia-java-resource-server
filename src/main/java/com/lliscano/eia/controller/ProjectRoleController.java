package com.lliscano.eia.controller;

import com.lliscano.commons.annotations.LoggableController;
import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.eia.model.dto.response.ProjectRoleResponseDTO;
import com.lliscano.eia.service.ProjectRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@Validated
@RestController
@RequestMapping(value = "/v1/projects/roles", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Roles de Proyecto - API", description = "Catálogo parametrizable de roles para asignación de miembros en EIA")
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "BAD REQUEST",
                content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))}),
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED",
                content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))}),
        @ApiResponse(responseCode = "403", description = "FORBIDDEN",
                content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))}),
        @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR",
                content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))})
})
@LoggableController
public class ProjectRoleController {

    private final ProjectRoleService projectRoleService;

    @Operation(summary = "Obtiene la lista completa de roles de proyecto activos")
    @ApiResponses(@ApiResponse(responseCode = "200", useReturnTypeSchema = true))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseDTO<ArrayList<ProjectRoleResponseDTO>>> getAllRoles() {
        return ResponseEntity.ok(this.projectRoleService.getAllActiveRoles());
    }
}
