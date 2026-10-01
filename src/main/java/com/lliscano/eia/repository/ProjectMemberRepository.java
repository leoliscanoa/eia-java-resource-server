package com.lliscano.eia.repository;

import com.lliscano.eia.model.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findAllByProjectUuidAndIsDeletedFalse(String projectUuid);

    Optional<ProjectMember> findByProjectIdAndUserUuidAndIsDeletedFalse(Long projectId, String userUuid);

    Optional<ProjectMember> findByProjectUuidAndUserUuidAndIsDeletedFalse(String projectUuid, String userUuid);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM ProjectMember m " +
           "WHERE m.project.uuid = :projectUuid " +
           "AND (m.userUuid = :userUuid OR m.userUuid = :username) " +
           "AND m.projectRole = :projectRole " +
           "AND m.isActive = true " +
           "AND m.isDeleted = false")
    boolean existsByProjectUuidAndUserIdentifierAndRole(
            @Param("projectUuid") String projectUuid,
            @Param("userUuid") String userUuid,
            @Param("username") String username,
            @Param("projectRole") String projectRole);
}
