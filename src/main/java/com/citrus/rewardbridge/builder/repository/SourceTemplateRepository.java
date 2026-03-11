package com.citrus.rewardbridge.builder.repository;

import com.citrus.rewardbridge.builder.entity.SourceTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SourceTemplateRepository extends JpaRepository<SourceTemplateEntity, Long> {

    Optional<SourceTemplateEntity> findByTemplateKey(String templateKey);

    @Query("""
            select st
            from SourceTemplateEntity st
            join fetch st.sourceType sourceType
            where st.active = true
              and (st.groupKey is null or (:groupKey is not null and st.groupKey = :groupKey))
            order by
              case when st.groupKey is null then 1 else 0 end asc,
              sourceType.sortPriority asc,
              st.templateId asc
            """)
    List<SourceTemplateEntity> findActiveByBuilderGroup(String groupKey);

    @Query("""
            select st
            from SourceTemplateEntity st
            join fetch st.sourceType sourceType
            order by
              case when st.groupKey is null then 1 else 0 end asc,
              sourceType.sortPriority asc,
              st.templateId asc
            """)
    List<SourceTemplateEntity> findAllWithTypeOrdered();
}
