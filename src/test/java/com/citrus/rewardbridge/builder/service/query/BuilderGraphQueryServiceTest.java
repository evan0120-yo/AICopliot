package com.citrus.rewardbridge.builder.service.query;

import com.citrus.rewardbridge.common.entity.BuilderConfigEntity;
import com.citrus.rewardbridge.common.repository.BuilderConfigRepository;
import com.citrus.rewardbridge.rag.entity.RagSupplementEntity;
import com.citrus.rewardbridge.rag.repository.RagSupplementRepository;
import com.citrus.rewardbridge.source.entity.SourceEntity;
import com.citrus.rewardbridge.source.repository.SourceRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class BuilderGraphQueryServiceTest {

    private final BuilderConfigRepository builderConfigRepository = mock(BuilderConfigRepository.class);
    private final SourceRepository sourceRepository = mock(SourceRepository.class);
    private final RagSupplementRepository ragSupplementRepository = mock(RagSupplementRepository.class);

    private final BuilderGraphQueryService service = new BuilderGraphQueryService(
            builderConfigRepository,
            sourceRepository,
            ragSupplementRepository
    );

    @Test
    void loadGraphShouldReturnSortedSourceAndRagStructure() {
        BuilderConfigEntity builderConfig = new BuilderConfigEntity(
                2,
                "qa-functional-doc",
                "qa",
                "測試團隊",
                "QA 功能測試文件產生",
                "協助 QA 快速產出功能測試案例",
                true,
                "xlsx",
                "qa-functional-doc",
                true
        );

        SourceEntity sourceEntity = new SourceEntity(2, "請依照以下流程完成分析", 2, false, true);
        sourceEntity.setSourceId(10L);

        RagSupplementEntity ragSupplementEntity = new RagSupplementEntity(
                10L,
                "default_content",
                "預設內容",
                "若前端沒給需求，請先產出 default draft",
                1,
                true,
                "full_context"
        );
        ragSupplementEntity.setRagId(20L);

        given(builderConfigRepository.findById(2)).willReturn(Optional.of(builderConfig));
        given(sourceRepository.findAllByBuilderIdOrdered(2)).willReturn(List.of(sourceEntity));
        given(ragSupplementRepository.findBySourceIdInOrderBySourceIdAscOrderNoAscRagIdAsc(List.of(10L)))
                .willReturn(List.of(ragSupplementEntity));

        var response = service.loadGraph(2);

        assertEquals("qa-functional-doc", response.builder().builderCode());
        assertEquals("qa", response.builder().groupKey());
        assertEquals(1, response.sources().size());
        assertEquals(2, response.sources().get(0).orderNo());
        assertEquals(false, response.sources().get(0).systemBlock());
        assertEquals(1, response.sources().get(0).rag().size());
        assertEquals("default_content", response.sources().get(0).rag().get(0).ragType());
    }

    @Test
    void loadGraphShouldNormalizeUnsupportedRetrievalModeToFullContext() {
        BuilderConfigEntity builderConfig = new BuilderConfigEntity(
                3,
                "pm-estimate",
                "pm",
                "產品經理",
                "PM 工時估算與建議",
                "協助 PM 針對需求做工時估算、拆解與風險說明。",
                false,
                null,
                "pm-estimate",
                true
        );

        SourceEntity sourceEntity = new SourceEntity(3, "請依照以下流程完成分析", 1, false, true);
        sourceEntity.setSourceId(30L);

        RagSupplementEntity ragSupplementEntity = new RagSupplementEntity(
                30L,
                "default_content",
                "預設內容",
                "若前端沒給需求，請先產出 default draft",
                1,
                true,
                "vector_search"
        );
        ragSupplementEntity.setRagId(40L);

        given(builderConfigRepository.findById(3)).willReturn(Optional.of(builderConfig));
        given(sourceRepository.findAllByBuilderIdOrdered(3)).willReturn(List.of(sourceEntity));
        given(ragSupplementRepository.findBySourceIdInOrderBySourceIdAscOrderNoAscRagIdAsc(List.of(30L)))
                .willReturn(List.of(ragSupplementEntity));

        var response = service.loadGraph(3);

        assertEquals("full_context", response.sources().getFirst().rag().getFirst().retrievalMode());
    }

    @Test
    void loadGraphShouldExposeSystemBlockFlag() {
        BuilderConfigEntity builderConfig = new BuilderConfigEntity(
                4,
                "qa-functional-doc",
                "qa",
                "測試團隊",
                "QA 功能測試文件產生",
                "desc",
                true,
                "xlsx",
                "qa-functional-doc",
                true
        );

        SourceEntity systemSource = new SourceEntity(4, "系統安全區塊", 0, true, false);
        systemSource.setSourceId(50L);

        given(builderConfigRepository.findById(4)).willReturn(Optional.of(builderConfig));
        given(sourceRepository.findAllByBuilderIdOrdered(4)).willReturn(List.of(systemSource));
        given(ragSupplementRepository.findBySourceIdInOrderBySourceIdAscOrderNoAscRagIdAsc(List.of(50L)))
                .willReturn(List.of());

        var response = service.loadGraph(4);

        assertEquals(true, response.sources().getFirst().systemBlock());
        assertEquals(0, response.sources().getFirst().orderNo());
    }
}
