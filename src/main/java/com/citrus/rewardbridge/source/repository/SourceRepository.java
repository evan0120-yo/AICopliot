package com.citrus.rewardbridge.source.repository;

import com.citrus.rewardbridge.source.entity.SourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SourceRepository extends JpaRepository<SourceEntity, Long> {

    @Query("""
            select s
            from SourceEntity s
            join fetch s.sourceType st
            where s.builderId = :builderId
            order by st.sortPriority asc, s.orderNo asc, s.sourceId asc
            """)
    List<SourceEntity> findAllByBuilderIdOrdered(Integer builderId);

    void deleteAllByBuilderId(Integer builderId);
}
