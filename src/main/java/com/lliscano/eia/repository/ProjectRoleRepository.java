package com.lliscano.eia.repository;

import com.lliscano.eia.model.entity.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRoleRepository extends JpaRepository<ProjectRole, Long> {

    Optional<ProjectRole> findByCode(String code);

    Optional<ProjectRole> findByCodeAndIsActiveTrue(String code);

    List<ProjectRole> findAllByIsActiveTrueOrderByCreatedAtAsc();

    Optional<ProjectRole> findFirstByIsDefaultMemberTrueAndIsActiveTrue();

    Optional<ProjectRole> findFirstByIsLeadRoleTrueAndIsActiveTrue();
}
