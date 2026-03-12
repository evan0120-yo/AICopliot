package com.citrus.rewardbridge.source.repository;

import com.citrus.rewardbridge.source.entity.SourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SourceRepository extends JpaRepository<SourceEntity, Long> {

    @Query("""
            select s
            from SourceEntity s
            left join fetch s.copiedFromTemplate template
            where s.builderId = :builderId
            order by s.orderNo asc, s.sourceId asc
            """)
    List<SourceEntity> findAllByBuilderIdOrdered(Integer builderId);

    void deleteAllByBuilderId(Integer builderId);

    List<SourceEntity> findAllByCopiedFromTemplateId(Long copiedFromTemplateId);
}
