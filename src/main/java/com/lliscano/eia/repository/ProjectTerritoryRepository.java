package com.lliscano.eia.repository;

import com.lliscano.eia.model.entity.ProjectTerritory;
import com.lliscano.eia.model.entity.ProjectTerritoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectTerritoryRepository extends JpaRepository<ProjectTerritory, ProjectTerritoryId> {

    List<ProjectTerritory> findAllByProjectId(Long projectId);

    long countByProjectIdAndInfluenceTypeIgnoreCase(Long projectId, String influenceType);
}
