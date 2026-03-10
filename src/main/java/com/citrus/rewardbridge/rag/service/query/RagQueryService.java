package com.citrus.rewardbridge.rag.service.query;

import com.citrus.rewardbridge.rag.dto.RagDocumentDto;
import com.citrus.rewardbridge.rag.entity.RagDocumentEntity;
import com.citrus.rewardbridge.rag.repository.RagDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RagQueryService {

    private static final Logger log = LoggerFactory.getLogger(RagQueryService.class);
    private static final String TYPE_CATEGORY = "TYPE";

    private final RagDocumentRepository ragDocumentRepository;

    public List<RagDocumentDto> queryByKeys(List<String> keys) {
        List<String> normalizedKeys = keys == null ? List.of() : keys.stream()
                .filter(key -> key != null && !key.isBlank())
                .toList();

        if (normalizedKeys.isEmpty()) {
            log.info("RAG query skipped because no keys were provided.");
            return List.of();
        }

        Map<String, RagDocumentEntity> documentByKey = new LinkedHashMap<>();
        for (RagDocumentEntity entity : ragDocumentRepository.findByDocumentKeyIn(normalizedKeys)) {
            documentByKey.put(entity.getDocumentKey(), entity);
        }

        List<RagDocumentDto> documents = normalizedKeys.stream()
                .map(documentByKey::get)
                .filter(entity -> entity != null)
                .map(entity -> new RagDocumentDto(entity.getDocumentKey(), entity.getTitle(), entity.getContent()))
                .toList();

        log.info("Loaded RAG documents. requestedKeys={}, loadedCount={}", normalizedKeys, documents.size());
        return documents;
    }

    public List<RagDocumentDto> queryByScenario(Integer group, Integer type) {
        return queryByScenarioAndCategory(group, type, TYPE_CATEGORY);
    }

    public List<RagDocumentDto> queryByScenarioAndCategory(Integer group, Integer type, String category) {
        List<RagDocumentDto> documents = ragDocumentRepository
                .findByGroupCodeAndTypeCodeAndDocumentCategoryOrderByIdAsc(group, type, category)
                .stream()
                .map(entity -> new RagDocumentDto(entity.getDocumentKey(), entity.getTitle(), entity.getContent()))
                .toList();

        log.info(
                "Loaded scenario-compatible RAG documents. group={}, type={}, category={}, loadedCount={}",
                group,
                type,
                category,
                documents.size()
        );
        return documents;
    }
}
