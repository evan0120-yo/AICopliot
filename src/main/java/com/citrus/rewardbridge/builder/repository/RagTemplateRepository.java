package com.citrus.rewardbridge.builder.repository;

import com.citrus.rewardbridge.builder.entity.RagTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RagTemplateRepository extends JpaRepository<RagTemplateEntity, Long> {

    List<RagTemplateEntity> findByTemplateIdOrderByOrderNoAscTemplateRagIdAsc(Long templateId);

    List<RagTemplateEntity> findByTemplateIdInOrderByTemplateIdAscOrderNoAscTemplateRagIdAsc(Collection<Long> templateIds);
}
