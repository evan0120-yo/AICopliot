package com.citrus.rewardbridge.source.repository;

import com.citrus.rewardbridge.source.entity.SourceScenarioConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SourceScenarioConfigRepository extends JpaRepository<SourceScenarioConfigEntity, Long> {

    Optional<SourceScenarioConfigEntity> findByGroupCodeAndTypeCode(Integer groupCode, Integer typeCode);
}
