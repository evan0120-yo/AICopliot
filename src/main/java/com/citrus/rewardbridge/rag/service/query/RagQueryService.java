package com.citrus.rewardbridge.rag.service.query;

import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.rag.dto.RagSupplementDto;
import com.citrus.rewardbridge.rag.entity.RagSupplementEntity;
import com.citrus.rewardbridge.rag.repository.RagSupplementRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RagQueryService {

    private static final Logger log = LoggerFactory.getLogger(RagQueryService.class);

    private final RagSupplementRepository ragSupplementRepository;

    public List<RagSupplementDto> queryBySourceId(Long sourceId) {
        List<RagSupplementDto> supplements = ragSupplementRepository.findBySourceIdOrderByOrderNoAscRagIdAsc(sourceId)
                .stream()
                .map(this::toDto)
                .toList();

        log.info("Loaded RAG supplements for source. sourceId={}, supplementCount={}", sourceId, supplements.size());
        return supplements;
    }

    private RagSupplementDto toDto(RagSupplementEntity entity) {
        String retrievalMode = entity.getRetrievalMode() == null ? "full_context" : entity.getRetrievalMode().trim().toLowerCase(Locale.ROOT);
        if ("vector_search".equals(retrievalMode)) {
            throw new BusinessException(
                    "RAG_RETRIEVAL_MODE_UNSUPPORTED",
                    "retrieval_mode=vector_search is configured but not implemented yet.",
                    HttpStatus.NOT_IMPLEMENTED
            );
        }

        return new RagSupplementDto(
                entity.getRagId(),
                entity.getSourceId(),
                entity.getRagType(),
                entity.getTitle(),
                entity.getContent(),
                entity.getOrderNo(),
                entity.isOverridable(),
                retrievalMode
        );
    }
}
