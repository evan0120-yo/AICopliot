package com.citrus.rewardbridge.rag.repository;

import com.citrus.rewardbridge.rag.entity.RagSupplementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RagSupplementRepository extends JpaRepository<RagSupplementEntity, Long> {

    List<RagSupplementEntity> findBySourceIdOrderByOrderNoAscRagIdAsc(Long sourceId);
}
