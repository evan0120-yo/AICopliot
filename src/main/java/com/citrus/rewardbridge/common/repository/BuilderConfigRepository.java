package com.citrus.rewardbridge.common.repository;

import com.citrus.rewardbridge.common.entity.BuilderConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuilderConfigRepository extends JpaRepository<BuilderConfigEntity, Integer> {

    List<BuilderConfigEntity> findAllByActiveTrueOrderByBuilderIdAsc();

    Optional<BuilderConfigEntity> findByBuilderCode(String builderCode);

}
