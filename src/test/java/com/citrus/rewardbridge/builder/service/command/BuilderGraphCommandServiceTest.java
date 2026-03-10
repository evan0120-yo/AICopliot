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
import com.citrus.rewardbridge.source.entity.SourceTypeEntity;
import com.citrus.rewardbridge.source.repository.SourceRepository;
import com.citrus.rewardbridge.source.repository.SourceTypeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class BuilderGraphCommandServiceTest {

    private final BuilderConfigRepository builderConfigRepository = mock(BuilderConfigRepository.class);
    private final SourceRepository sourceRepository = mock(SourceRepository.class);
    private final SourceTypeRepository sourceTypeRepository = mock(SourceTypeRepository.class);
    private final RagSupplementRepository ragSupplementRepository = mock(RagSupplementRepository.class);

    private final BuilderGraphCommandService service = new BuilderGraphCommandService(
            builderConfigRepository,
            sourceRepository,
            sourceTypeRepository,
            ragSupplementRepository
    );

    private BuilderConfigEntity existingBuilder(Integer builderId) {
        return new BuilderConfigEntity(
                builderId,
                "builder-" + builderId,
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
    void saveGraphShouldUpdateExistingBuilderAndPersistNormalizedValues() {
        BuilderConfigEntity existingBuilder = existingBuilder(9);
        given(builderConfigRepository.findById(9)).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.findByBuilderCode("builder-9")).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.save(any(BuilderConfigEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(sourceRepository.findAllByBuilderIdOrdered(9)).willReturn(List.of());
        given(sourceTypeRepository.findByTypeCode("CONTENT")).willReturn(Optional.of(
                new SourceTypeEntity(3, "CONTENT", "內文類", "主要業務流程與回應格式", 3)
        ));
        given(sourceRepository.save(any(SourceEntity.class))).willAnswer(invocation -> {
            SourceEntity entity = invocation.getArgument(0);
            entity.setSourceId(100L);
            return entity;
        });

        var response = service.saveGraph(9, new BuilderGraphRequest(
                new BuilderGraphBuilderRequest(null, null, "新 Builder", null, null, null, null, null),
                List.of(new BuilderGraphSourceRequest(null, null, "主要 prompt", List.of())),
                null
        ));

        assertEquals(9, response.builder().builderId());
        assertEquals("builder-9", response.builder().builderCode());
        assertEquals("既有群組", response.builder().groupLabel());
        assertEquals("新 Builder", response.builder().name());
        assertNull(response.builder().defaultOutputFormat());
        assertTrue(response.builder().active());
        assertEquals("CONTENT", response.sources().get(0).typeCode());
        assertEquals(1, response.sources().get(0).orderNo());
    }

    @Test
    void saveGraphShouldAcceptAiAgentShapeAndPromptsAliasForRagContent() {
        BuilderConfigEntity existingBuilder = new BuilderConfigEntity(
                2,
                "qa-smoke-doc",
                "測試團隊",
                "QA 冒煙測試文件產生",
                "desc",
                true,
                "xlsx",
                "qa-smoke-doc",
                true
        );
        given(builderConfigRepository.findById(2)).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.findByBuilderCode("qa-smoke-doc")).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.save(any(BuilderConfigEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(sourceRepository.findAllByBuilderIdOrdered(2)).willReturn(List.of());
        given(sourceTypeRepository.findByTypeCode("CONTENT")).willReturn(Optional.of(
                new SourceTypeEntity(3, "CONTENT", "內文類", "主要業務流程與回應格式", 3)
        ));
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
                                null,
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

        assertEquals(1, response.sources().size());
        assertEquals(1, response.sources().get(0).rag().size());
        assertEquals(promptsValue, response.sources().get(0).rag().get(0).content());
        assertEquals("full_context", response.sources().get(0).rag().get(0).retrievalMode());
        assertTrue(response.sources().get(0).rag().get(0).overridable());
        assertEquals(3, response.sources().get(0).orderNo());
    }

    @Test
    void saveGraphShouldAssignMissingSourceOrderWithoutCollidingWithExplicitOrder() {
        BuilderConfigEntity existingBuilder = existingBuilder(3);
        given(builderConfigRepository.findById(3)).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.findByBuilderCode("builder-3")).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.save(any(BuilderConfigEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(sourceRepository.findAllByBuilderIdOrdered(3)).willReturn(List.of());
        given(sourceTypeRepository.findByTypeCode("CONTENT")).willReturn(Optional.of(
                new SourceTypeEntity(3, "CONTENT", "內文類", "主要業務流程與回應格式", 3)
        ));
        given(sourceRepository.save(any(SourceEntity.class))).willAnswer(new org.mockito.stubbing.Answer<SourceEntity>() {
            private long nextId = 100;

            @Override
            public SourceEntity answer(org.mockito.invocation.InvocationOnMock invocation) {
                SourceEntity entity = invocation.getArgument(0);
                entity.setSourceId(nextId++);
                return entity;
            }
        });

        var response = service.saveGraph(3, new BuilderGraphRequest(
                new BuilderGraphBuilderRequest(null, null, "Builder 3", null, null, null, null, null),
                List.of(
                        new BuilderGraphSourceRequest("CONTENT", null, "未指定順序", List.of()),
                        new BuilderGraphSourceRequest("CONTENT", 1, "明確指定順序", List.of())
                ),
                null
        ));

        assertEquals(2, response.sources().size());
        assertEquals(1, response.sources().get(0).orderNo());
        assertEquals("明確指定順序", response.sources().get(0).prompts());
        assertEquals(2, response.sources().get(1).orderNo());
        assertEquals("未指定順序", response.sources().get(1).prompts());
    }

    @Test
    void saveGraphShouldRejectDuplicateBuilderCode() {
        BuilderConfigEntity existingBuilder = existingBuilder(7);
        given(builderConfigRepository.findById(7)).willReturn(Optional.of(existingBuilder));
        given(builderConfigRepository.findByBuilderCode("qa-smoke-doc")).willReturn(Optional.of(
                new BuilderConfigEntity(2, "qa-smoke-doc", "測試團隊", "QA 冒煙測試文件產生", "desc", true, "xlsx", "qa-smoke-doc", true)
        ));
        given(sourceTypeRepository.findByTypeCode("CONTENT")).willReturn(Optional.of(
                new SourceTypeEntity(3, "CONTENT", "內文類", "主要業務流程與回應格式", 3)
        ));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.saveGraph(7, new BuilderGraphRequest(
                new BuilderGraphBuilderRequest("qa-smoke-doc", null, "Builder 7", null, null, null, null, null),
                List.of(new BuilderGraphSourceRequest(null, null, "主要 prompt", List.of())),
                null
        )));

        assertEquals("BUILDER_CODE_DUPLICATE", exception.getCode());
    }

    @Test
    void saveGraphShouldRejectMissingBuilderWhenUsingPutContract() {
        given(builderConfigRepository.findById(11)).willReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.saveGraph(11, new BuilderGraphRequest(
                new BuilderGraphBuilderRequest(null, null, "Builder 11", null, null, null, null, null),
                List.of(new BuilderGraphSourceRequest(null, null, "主要 prompt", List.of())),
                null
        )));

        assertEquals("BUILDER_NOT_FOUND", exception.getCode());
    }

    @Test
    void saveGraphShouldRejectNonPositiveSourceOrder() {
        BuilderConfigEntity existingBuilder = existingBuilder(12);
        given(builderConfigRepository.findById(12)).willReturn(Optional.of(existingBuilder));
        given(sourceTypeRepository.findByTypeCode("CONTENT")).willReturn(Optional.of(
                new SourceTypeEntity(3, "CONTENT", "內文類", "主要業務流程與回應格式", 3)
        ));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.saveGraph(12, new BuilderGraphRequest(
                null,
                List.of(new BuilderGraphSourceRequest("CONTENT", 0, "主要 prompt", List.of())),
                null
        )));

        assertEquals("SOURCE_ORDER_INVALID", exception.getCode());
    }

    @Test
    void saveGraphShouldRejectNonPositiveRagOrder() {
        BuilderConfigEntity existingBuilder = existingBuilder(13);
        given(builderConfigRepository.findById(13)).willReturn(Optional.of(existingBuilder));
        given(sourceTypeRepository.findByTypeCode("CONTENT")).willReturn(Optional.of(
                new SourceTypeEntity(3, "CONTENT", "內文類", "主要業務流程與回應格式", 3)
        ));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.saveGraph(13, new BuilderGraphRequest(
                null,
                List.of(new BuilderGraphSourceRequest(
                        "CONTENT",
                        1,
                        "主要 prompt",
                        List.of(new BuilderGraphRagRequest("default_content", null, "內容", -1, null, null))
                )),
                null
        )));

        assertEquals("RAG_ORDER_INVALID", exception.getCode());
    }
}
