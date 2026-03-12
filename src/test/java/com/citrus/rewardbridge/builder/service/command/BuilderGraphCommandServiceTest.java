package com.citrus.rewardbridge.builder.service.command;

import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphAiAgentItemRequest;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphBuilderRequest;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphRagRequest;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphRequest;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphSourceRequest;
import com.citrus.rewardbridge.common.entity.BuilderConfigEntity;
import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.common.repository.BuilderConfigRepository;
import com.citrus.rewardbridge.rag.entity.RagSupplementEntity;
import com.citrus.rewardbridge.rag.repository.RagSupplementRepository;
import com.citrus.rewardbridge.source.entity.SourceEntity;
import com.citrus.rewardbridge.source.repository.SourceRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BuilderGraphCommandServiceTest {

    private final BuilderConfigRepository builderConfigRepository = mock(BuilderConfigRepository.class);
    private final SourceRepository sourceRepository = mock(SourceRepository.class);
    private final RagSupplementRepository ragSupplementRepository = mock(RagSupplementRepository.class);

    private final BuilderGraphCommandService service = new BuilderGraphCommandService(
            builderConfigRepository,
            sourceRepository,
            ragSupplementRepository
    );

    private BuilderConfigEntity existingBuilder(Integer builderId) {
        return new BuilderConfigEntity(
                builderId,
                "builder-" + builderId,
                "legacy-group",
                "既有群組",
                "既有 Builder " + builderId,
                "既有說明",
                false,
                null,
                "builder-" + builderId,
                true
        );
    }

    @Test
    void saveGraphShouldNormalizeSourceAndRagOrdersGlobally() {
        BuilderConfigEntity existingBuilder = existingBuilder(9);
        given(builderConfigRepository.findById(9)).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.findByBuilderCode("builder-9")).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.save(any(BuilderConfigEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        SourceEntity systemSource = new SourceEntity(9, "系統安全區塊", 0, true, false);
        systemSource.setSourceId(99L);
        given(sourceRepository.findAllByBuilderIdOrdered(9)).willReturn(List.of(systemSource));
        given(ragSupplementRepository.findBySourceIdInOrderBySourceIdAscOrderNoAscRagIdAsc(List.of(99L))).willReturn(List.of());
        given(sourceRepository.save(any(SourceEntity.class))).willAnswer(new org.mockito.stubbing.Answer<SourceEntity>() {
            private long nextId = 100;

            @Override
            public SourceEntity answer(org.mockito.invocation.InvocationOnMock invocation) {
                SourceEntity entity = invocation.getArgument(0);
                entity.setSourceId(nextId++);
                return entity;
            }
        });
        given(ragSupplementRepository.save(any(RagSupplementEntity.class))).willAnswer(new org.mockito.stubbing.Answer<RagSupplementEntity>() {
            private long nextId = 200;

            @Override
            public RagSupplementEntity answer(org.mockito.invocation.InvocationOnMock invocation) {
                RagSupplementEntity entity = invocation.getArgument(0);
                entity.setRagId(nextId++);
                return entity;
            }
        });

        var response = service.saveGraph(9, new BuilderGraphRequest(
                new BuilderGraphBuilderRequest(null, null, "新 Builder", null, null, null, null, null),
                List.of(
                        new BuilderGraphSourceRequest(
                                5,
                                "第二個 source",
                                List.of(
                                        new BuilderGraphRagRequest("rag-b", null, "內容 B", 3, false, null),
                                        new BuilderGraphRagRequest("rag-a", null, "內容 A", 1, true, null)
                                )
                        ),
                        new BuilderGraphSourceRequest(1, "第一個 source", List.of())
                ),
                null
        ));

        assertEquals(9, response.builder().builderId());
        assertEquals("新 Builder", response.builder().name());
        assertEquals(3, response.sources().size());
        assertTrue(response.sources().get(0).systemBlock());
        assertEquals("系統安全區塊", response.sources().get(0).prompts());
        assertEquals("第一個 source", response.sources().get(1).prompts());
        assertEquals(1, response.sources().get(1).orderNo());
        assertEquals("第二個 source", response.sources().get(2).prompts());
        assertEquals(2, response.sources().get(2).orderNo());
        assertEquals(1, response.sources().get(2).rag().get(0).orderNo());
        assertEquals("rag-a", response.sources().get(2).rag().get(0).ragType());
        assertEquals(2, response.sources().get(2).rag().get(1).orderNo());
        assertEquals("rag-b", response.sources().get(2).rag().get(1).ragType());
        verify(sourceRepository, never()).deleteAll(List.of(systemSource));
    }

    @Test
    void saveGraphShouldAcceptAiAgentShapeAndPromptsAliasForRagContent() {
        BuilderConfigEntity existingBuilder = existingBuilder(2);
        given(builderConfigRepository.findById(2)).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.findByBuilderCode("builder-2")).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.save(any(BuilderConfigEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        SourceEntity systemSource = new SourceEntity(2, "系統安全區塊", 0, true, false);
        systemSource.setSourceId(2L);
        given(sourceRepository.findAllByBuilderIdOrdered(2)).willReturn(List.of(systemSource));
        given(ragSupplementRepository.findBySourceIdInOrderBySourceIdAscOrderNoAscRagIdAsc(List.of(2L))).willReturn(List.of());
        given(sourceRepository.save(any(SourceEntity.class))).willAnswer(invocation -> {
            SourceEntity entity = invocation.getArgument(0);
            entity.setSourceId(10L);
            return entity;
        });
        given(ragSupplementRepository.save(any(RagSupplementEntity.class))).willAnswer(invocation -> {
            RagSupplementEntity entity = invocation.getArgument(0);
            entity.setRagId(20L);
            return entity;
        });

        String promptsValue = "這是可覆蓋的預設內容";
        var response = service.saveGraph(2, new BuilderGraphRequest(
                null,
                null,
                List.of(new BuilderGraphAiAgentItemRequest(
                        new BuilderGraphSourceRequest(
                                3,
                                "請依照以下流程完成分析",
                                List.of(new BuilderGraphRagRequest(
                                        "default_content",
                                        null,
                                        promptsValue,
                                        1,
                                        true,
                                        null
                                ))
                        )
                ))
        ));

        assertEquals(2, response.sources().size());
        assertTrue(response.sources().get(0).systemBlock());
        assertEquals(1, response.sources().get(1).orderNo());
        assertEquals(1, response.sources().get(1).rag().size());
        assertEquals(promptsValue, response.sources().get(1).rag().get(0).content());
        assertEquals("full_context", response.sources().get(1).rag().get(0).retrievalMode());
        assertTrue(response.sources().get(1).rag().get(0).overridable());
    }

    @Test
    void saveGraphShouldRejectDuplicateBuilderCode() {
        BuilderConfigEntity existingBuilder = existingBuilder(7);
        given(builderConfigRepository.findById(7)).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.findByBuilderCode("qa-smoke-doc")).willReturn(Optional.of(
                new BuilderConfigEntity(2, "qa-smoke-doc", "qa", "測試團隊", "QA 冒煙測試文件產生", "desc", true, "xlsx", "qa-smoke-doc", true)
        ));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.saveGraph(7, new BuilderGraphRequest(
                new BuilderGraphBuilderRequest("qa-smoke-doc", null, "Builder 7", null, null, null, null, null),
                List.of(new BuilderGraphSourceRequest(1, "主要 prompt", List.of())),
                null
        )));

        assertEquals("BUILDER_CODE_DUPLICATE", exception.getCode());
    }

    @Test
    void saveGraphShouldRejectMissingBuilderWhenUsingPutContract() {
        given(builderConfigRepository.findById(11)).willReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.saveGraph(11, new BuilderGraphRequest(
                new BuilderGraphBuilderRequest(null, null, "Builder 11", null, null, null, null, null),
                List.of(new BuilderGraphSourceRequest(1, "主要 prompt", List.of())),
                null
        )));

        assertEquals("BUILDER_NOT_FOUND", exception.getCode());
    }

    @Test
    void saveGraphShouldRejectNonPositiveSourceOrder() {
        BuilderConfigEntity existingBuilder = existingBuilder(12);
        given(builderConfigRepository.findById(12)).willReturn(Optional.of(existingBuilder));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.saveGraph(12, new BuilderGraphRequest(
                null,
                List.of(new BuilderGraphSourceRequest(0, "主要 prompt", List.of())),
                null
        )));

        assertEquals("SOURCE_ORDER_INVALID", exception.getCode());
    }

    @Test
    void saveGraphShouldRejectNonPositiveRagOrder() {
        BuilderConfigEntity existingBuilder = existingBuilder(13);
        given(builderConfigRepository.findById(13)).willReturn(Optional.of(existingBuilder));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.saveGraph(13, new BuilderGraphRequest(
                null,
                List.of(new BuilderGraphSourceRequest(
                        1,
                        "主要 prompt",
                        List.of(new BuilderGraphRagRequest("default_content", null, "內容", -1, null, null))
                )),
                null
        )));

        assertEquals("RAG_ORDER_INVALID", exception.getCode());
    }

    @Test
    void saveGraphShouldRejectUnsupportedVectorSearchRetrievalMode() {
        BuilderConfigEntity existingBuilder = existingBuilder(14);
        given(builderConfigRepository.findById(14)).willReturn(Optional.of(existingBuilder));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.saveGraph(14, new BuilderGraphRequest(
                null,
                List.of(new BuilderGraphSourceRequest(
                        1,
                        "主要 prompt",
                        List.of(new BuilderGraphRagRequest("default_content", null, "內容", 1, null, "vector_search"))
                )),
                null
        )));

        assertEquals("RAG_RETRIEVAL_MODE_UNSUPPORTED", exception.getCode());
    }

    @Test
    void saveGraphShouldAcceptExplicitGroupKeyUpdate() {
        BuilderConfigEntity existingBuilder = existingBuilder(15);
        given(builderConfigRepository.findById(15)).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.findByBuilderCode("builder-15")).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.save(any(BuilderConfigEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        SourceEntity systemSource = new SourceEntity(15, "系統安全區塊", 0, true, false);
        systemSource.setSourceId(150L);
        given(sourceRepository.findAllByBuilderIdOrdered(15)).willReturn(List.of(systemSource));
        given(ragSupplementRepository.findBySourceIdInOrderBySourceIdAscOrderNoAscRagIdAsc(List.of(150L))).willReturn(List.of());
        given(sourceRepository.save(any(SourceEntity.class))).willAnswer(invocation -> {
            SourceEntity entity = invocation.getArgument(0);
            entity.setSourceId(151L);
            return entity;
        });

        var response = service.saveGraph(15, new BuilderGraphRequest(
                new BuilderGraphBuilderRequest(null, "qa", "測試團隊", "Builder 15", null, null, null, null, null),
                List.of(new BuilderGraphSourceRequest(1, "主要 prompt", List.of())),
                null
        ));

        assertEquals("qa", response.builder().groupKey());
        assertEquals("測試團隊", response.builder().groupLabel());
    }

    @Test
    void saveGraphShouldDeriveUnicodeGroupKeyFromGroupLabelWhenMissing() {
        BuilderConfigEntity existingBuilder = new BuilderConfigEntity(
                16,
                "builder-16",
                null,
                "未分類",
                "Builder 16",
                "desc",
                false,
                null,
                "builder-16",
                true
        );
        given(builderConfigRepository.findById(16)).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.findByBuilderCode("builder-16")).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.save(any(BuilderConfigEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        SourceEntity systemSource = new SourceEntity(16, "系統安全區塊", 0, true, false);
        systemSource.setSourceId(160L);
        given(sourceRepository.findAllByBuilderIdOrdered(16)).willReturn(List.of(systemSource));
        given(ragSupplementRepository.findBySourceIdInOrderBySourceIdAscOrderNoAscRagIdAsc(List.of(160L))).willReturn(List.of());
        given(sourceRepository.save(any(SourceEntity.class))).willAnswer(invocation -> {
            SourceEntity entity = invocation.getArgument(0);
            entity.setSourceId(161L);
            return entity;
        });

        var response = service.saveGraph(16, new BuilderGraphRequest(
                new BuilderGraphBuilderRequest(null, null, "測試團隊", "Builder 16", null, null, null, null, null),
                List.of(new BuilderGraphSourceRequest(1, "主要 prompt", List.of())),
                null
        ));

        assertEquals("測試團隊", response.builder().groupKey());
        assertEquals("測試團隊", response.builder().groupLabel());
        assertNull(response.builder().defaultOutputFormat());
    }

    @Test
    void saveGraphShouldIgnoreSystemBlockPayloadAndKeepExistingOne() {
        BuilderConfigEntity existingBuilder = existingBuilder(17);
        SourceEntity systemSource = new SourceEntity(17, "既有系統安全區塊", 0, true, false);
        systemSource.setSourceId(170L);

        given(builderConfigRepository.findById(17)).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.findByBuilderCode("builder-17")).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.save(any(BuilderConfigEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(sourceRepository.findAllByBuilderIdOrdered(17)).willReturn(List.of(systemSource));
        given(ragSupplementRepository.findBySourceIdInOrderBySourceIdAscOrderNoAscRagIdAsc(List.of(170L))).willReturn(List.of());
        given(sourceRepository.save(any(SourceEntity.class))).willAnswer(invocation -> {
            SourceEntity entity = invocation.getArgument(0);
            entity.setSourceId(171L);
            return entity;
        });

        var response = service.saveGraph(17, new BuilderGraphRequest(
                null,
                List.of(
                        new BuilderGraphSourceRequest(null, null, null, null, null, 0, true, "不要被覆蓋", List.of()),
                        new BuilderGraphSourceRequest(1, "一般 source", List.of())
                ),
                null
        ));

        assertEquals(2, response.sources().size());
        assertEquals("既有系統安全區塊", response.sources().get(0).prompts());
        assertTrue(response.sources().get(0).systemBlock());
    }
}
