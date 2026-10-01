package com.lliscano.eia.mapper;

import com.lliscano.eia.model.dto.response.ProjectMemberResponseDTO;
import com.lliscano.eia.model.entity.ProjectMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {

    @Mapping(target = "uuid", source = "userUuid")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToLocalDateTime")
    @Mapping(target = "userSummary", ignore = true)
    ProjectMemberResponseDTO toDto(ProjectMember entity);

    @Named("instantToLocalDateTime")
    default LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneOffset.UTC) : null;
    }
}
