package com.citrus.rewardbridge.rag.repository;

import com.citrus.rewardbridge.rag.entity.RagDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RagDocumentRepository extends JpaRepository<RagDocumentEntity, Long> {

    List<RagDocumentEntity> findByDocumentKeyIn(List<String> documentKeys);

    List<RagDocumentEntity> findByGroupCodeAndTypeCodeAndDocumentCategoryOrderByIdAsc(
            Integer groupCode,
            Integer typeCode,
            String documentCategory
    );

    boolean existsByDocumentKey(String documentKey);
}
