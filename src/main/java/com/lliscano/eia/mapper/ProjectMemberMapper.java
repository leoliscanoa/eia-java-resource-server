package com.lliscano.eia.mapper;

import com.lliscano.eia.model.dto.response.ProjectMemberResponseDTO;
import com.lliscano.eia.model.entity.ProjectMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {

    @Mapping(target = "uuid", source = "userUuid")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "userSummary", ignore = true)
    ProjectMemberResponseDTO toDto(ProjectMember entity);
}
