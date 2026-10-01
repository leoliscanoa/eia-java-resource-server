package com.lliscano.eia.repository;

import com.lliscano.eia.model.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByUuidAndTenantIdAndIsDeletedFalse(String uuid, String tenantId);

    Optional<Project> findByUuidAndIsDeletedFalse(String uuid);

    Optional<Project> findByTenantIdAndCodeAndIsDeletedFalse(String tenantId, String code);

    boolean existsByTenantIdAndCodeAndIsDeletedFalse(String tenantId, String code);

    List<Project> findAllByTenantIdAndIsDeletedFalse(String tenantId);

    @Query("SELECT DISTINCT p FROM Project p " +
           "JOIN p.members m " +
           "WHERE p.tenantId = :tenantId " +
           "AND p.isDeleted = false " +
           "AND (m.userUuid = :userUuid OR m.userUuid = :username) " +
           "AND m.isActive = true " +
           "AND m.isDeleted = false")
    List<Project> findAllByTenantIdAndMemberUserUuid(
            @Param("tenantId") String tenantId,
            @Param("userUuid") String userUuid,
            @Param("username") String username);
}
