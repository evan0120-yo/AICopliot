package com.citrus.rewardbridge.builder.repository;

import com.citrus.rewardbridge.builder.entity.SourceTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SourceTemplateRepository extends JpaRepository<SourceTemplateEntity, Long> {

    Optional<SourceTemplateEntity> findByTemplateKey(String templateKey);

    List<SourceTemplateEntity> findAllByOrderByOrderNoAscTemplateIdAsc();

    @Query("""
            select st
            from SourceTemplateEntity st
            where st.active = true
              and (st.groupKey is null or (:groupKey is not null and st.groupKey = :groupKey))
            order by
              case when st.groupKey is null then 1 else 0 end asc,
              st.orderNo asc,
              st.templateId asc
            """)
    List<SourceTemplateEntity> findActiveByBuilderGroup(String groupKey);

    @Query("""
            select st
            from SourceTemplateEntity st
            order by
              case when st.groupKey is null then 1 else 0 end asc,
              st.orderNo asc,
              st.templateId asc
            """)
    List<SourceTemplateEntity> findAllForAdminOrder();
}
