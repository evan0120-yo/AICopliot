package com.citrus.rewardbridge.rag.service.query;

import com.citrus.rewardbridge.rag.entity.RagSupplementEntity;
import com.citrus.rewardbridge.rag.repository.RagSupplementRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RagQueryServiceTest {

    private final RagSupplementRepository ragSupplementRepository = mock(RagSupplementRepository.class);

    private final RagQueryService service = new RagQueryService(ragSupplementRepository);

    @Test
    void queryBySourceIdShouldFallbackUnsupportedVectorSearchToFullContext() {
        RagSupplementEntity entity = new RagSupplementEntity(
                10L,
                "default_content",
                "預設內容",
                "這是預設內容",
                1,
                true,
                "vector_search"
        );
        entity.setRagId(20L);

        given(ragSupplementRepository.findBySourceIdOrderByOrderNoAscRagIdAsc(10L))
                .willReturn(List.of(entity));

        var result = service.queryBySourceId(10L);

        assertEquals(1, result.size());
        assertEquals("full_context", result.getFirst().retrievalMode());
    }
}
