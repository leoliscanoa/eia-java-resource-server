package com.lliscano.eia.service;

import com.lliscano.commons.dtos.ResponseDTO;
import com.lliscano.eia.mapper.ProjectRoleMapper;
import com.lliscano.eia.model.dto.response.ProjectRoleResponseDTO;
import com.lliscano.eia.model.entity.ProjectRole;
import com.lliscano.eia.repository.ProjectRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectRoleService {

    private final ProjectRoleRepository projectRoleRepository;
    private final ProjectRoleMapper projectRoleMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = "project_roles_all")
    public ResponseDTO<ArrayList<ProjectRoleResponseDTO>> getAllActiveRoles() {
        List<ProjectRole> roles = projectRoleRepository.findAllByIsActiveTrueOrderByCreatedAtAsc();
        ArrayList<ProjectRoleResponseDTO> dtos = projectRoleMapper.toDtoList(roles);
        return ResponseDTO.<ArrayList<ProjectRoleResponseDTO>>builder()
                .message("Operación exitosa")
                .data(dtos)
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "project_roles_by_code", key = "#code", unless = "#result == null")
    public Optional<ProjectRole> getRoleByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return projectRoleRepository.findByCode(code.trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "project_roles_is_lead", key = "#roleCode")
    public boolean isLeadRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return false;
        }
        return getRoleByCode(roleCode)
                .map(role -> role.isActive() && role.isLeadRole())
                .orElse(false);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "project_roles_lead_code")
    public String getLeadRoleCode() {
        return projectRoleRepository.findFirstByIsLeadRoleTrueAndIsActiveTrue()
                .map(ProjectRole::getCode)
                .orElse("PROJECT_LEAD");
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "project_roles_default_code")
    public String getDefaultMemberRoleCode() {
        return projectRoleRepository.findFirstByIsDefaultMemberTrueAndIsActiveTrue()
                .map(ProjectRole::getCode)
                .orElse("MEMBER");
    }

    @CacheEvict(value = {
            "project_roles_all",
            "project_roles_by_code",
            "project_roles_is_lead",
            "project_roles_lead_code",
            "project_roles_default_code"
    }, allEntries = true)
    public void evictAllCaches() {
        log.info("Invalidando caché local de roles de proyecto EIA");
    }
}
