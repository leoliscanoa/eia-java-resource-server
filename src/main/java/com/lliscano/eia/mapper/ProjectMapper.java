package com.lliscano.eia.mapper;

import com.lliscano.eia.model.dto.request.ProjectCreateRequestDTO;
import com.lliscano.eia.model.dto.response.ProjectResponseDTO;
import com.lliscano.eia.model.entity.Project;
import com.lliscano.eia.model.entity.ProjectTerritory;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "isDeleted", constant = "false")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "territories", ignore = true)
    Project toEntity(ProjectCreateRequestDTO dto);

    @Mapping(target = "directInfluenceCount", source = "territories", qualifiedByName = "countDirectInfluence")
    @Mapping(target = "indirectInfluenceCount", source = "territories", qualifiedByName = "countIndirectInfluence")
    @Mapping(target = "userRole", ignore = true)
    @Mapping(target = "description", ignore = true)
    ProjectResponseDTO toDto(Project entity);

    @Named("countDirectInfluence")
    default int countDirectInfluence(List<ProjectTerritory> territories) {
        if (territories == null) return 0;
        return (int) territories.stream()
                .filter(pt -> pt.getInfluenceType() != null &&
                        (pt.getInfluenceType().equalsIgnoreCase("DIRECT") || pt.getInfluenceType().equalsIgnoreCase("DIRECTA")))
                .count();
    }

    @Named("countIndirectInfluence")
    default int countIndirectInfluence(List<ProjectTerritory> territories) {
        if (territories == null) return 0;
        return (int) territories.stream()
                .filter(pt -> pt.getInfluenceType() != null &&
                        (pt.getInfluenceType().equalsIgnoreCase("INDIRECT") || pt.getInfluenceType().equalsIgnoreCase("INDIRECTA")))
                .count();
    }
}
