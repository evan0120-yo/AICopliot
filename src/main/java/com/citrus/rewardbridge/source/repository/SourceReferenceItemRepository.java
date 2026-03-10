package com.citrus.rewardbridge.source.repository;

import com.citrus.rewardbridge.source.entity.SourceReferenceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceReferenceItemRepository extends JpaRepository<SourceReferenceItemEntity, Long> {

    List<SourceReferenceItemEntity> findByGroupCodeAndTypeCodeOrderBySortOrderAscIdAsc(Integer groupCode, Integer typeCode);
}
