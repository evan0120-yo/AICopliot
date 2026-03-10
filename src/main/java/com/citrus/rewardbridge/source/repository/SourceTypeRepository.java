package com.citrus.rewardbridge.source.repository;

import com.citrus.rewardbridge.source.entity.SourceTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SourceTypeRepository extends JpaRepository<SourceTypeEntity, Integer> {

    Optional<SourceTypeEntity> findByTypeCode(String typeCode);
}
