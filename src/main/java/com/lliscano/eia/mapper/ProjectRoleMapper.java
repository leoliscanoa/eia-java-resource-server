package com.lliscano.eia.mapper;

import com.lliscano.eia.model.dto.response.ProjectRoleResponseDTO;
import com.lliscano.eia.model.entity.ProjectRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectRoleMapper {

    @Mapping(target = "isLeadRole", source = "leadRole")
    @Mapping(target = "isDefaultMember", source = "defaultMember")
    @Mapping(target = "isActive", source = "active")
    ProjectRoleResponseDTO toDto(ProjectRole entity);

    ArrayList<ProjectRoleResponseDTO> toDtoList(List<ProjectRole> entities);
}
