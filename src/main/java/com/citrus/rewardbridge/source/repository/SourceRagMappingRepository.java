package com.citrus.rewardbridge.source.repository;

import com.citrus.rewardbridge.source.entity.SourceRagMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceRagMappingRepository extends JpaRepository<SourceRagMappingEntity, Long> {

    List<SourceRagMappingEntity> findByGroupCodeAndTypeCodeOrderBySortOrderAscIdAsc(Integer groupCode, Integer typeCode);
}
