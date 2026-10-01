package com.lliscano.eia.repository;

import com.lliscano.eia.model.entity.Territory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TerritoryRepository extends JpaRepository<Territory, Long> {

    Optional<Territory> findByUuidAndTenantIdAndIsDeletedFalse(String uuid, String tenantId);

    Optional<Territory> findByUuidAndIsDeletedFalse(String uuid);

    List<Territory> findAllByUuidInAndTenantIdAndIsDeletedFalse(Collection<String> uuids, String tenantId);

    List<Territory> findAllByTenantIdAndIsDeletedFalse(String tenantId);
}
