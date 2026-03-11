package com.citrus.rewardbridge.builder.service.command;

import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateRagRequest;
import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateRequest;
import com.citrus.rewardbridge.builder.entity.RagTemplateEntity;
import com.citrus.rewardbridge.builder.entity.SourceTemplateEntity;
import com.citrus.rewardbridge.builder.repository.RagTemplateRepository;
import com.citrus.rewardbridge.builder.repository.SourceTemplateRepository;
import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.source.entity.SourceTypeEntity;
import com.citrus.rewardbridge.source.repository.SourceTypeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class BuilderTemplateCommandServiceTest {

    private final SourceTemplateRepository sourceTemplateRepository = mock(SourceTemplateRepository.class);
    private final RagTemplateRepository ragTemplateRepository = mock(RagTemplateRepository.class);
    private final SourceTypeRepository sourceTypeRepository = mock(SourceTypeRepository.class);

    private final BuilderTemplateCommandService service = new BuilderTemplateCommandService(
            sourceTemplateRepository,
            ragTemplateRepository,
            sourceTypeRepository
    );

    @Test
    void createTemplateShouldPersistTemplateAndRags() {
        given(sourceTypeRepository.findByTypeCode("CONTENT")).willReturn(Optional.of(
                new SourceTypeEntity(3, "CONTENT", "內文類", "主要業務流程與回應格式", 3)
        ));
        given(sourceTemplateRepository.findByTemplateKey("qa-template")).willReturn(Optional.empty());
        given(sourceTemplateRepository.save(any(SourceTemplateEntity.class))).willAnswer(invocation -> {
            SourceTemplateEntity entity = invocation.getArgument(0);
            entity.setTemplateId(10L);
            return entity;
        });
        given(ragTemplateRepository.findByTemplateIdOrderByOrderNoAscTemplateRagIdAsc(10L)).willReturn(List.of());
        given(ragTemplateRepository.save(any(RagTemplateEntity.class))).willAnswer(invocation -> {
            RagTemplateEntity entity = invocation.getArgument(0);
            entity.setTemplateRagId(100L + entity.getOrderNo());
            return entity;
        });

        var response = service.createTemplate(new BuilderTemplateRequest(
                "qa-template",
                "QA 測試範本",
                "測試用",
                "qa",
                "CONTENT",
                "主要 prompts",
                true,
                List.of(
                        new BuilderTemplateRagRequest("default_content", "Default", "內容一", 1, true, null),
                        new BuilderTemplateRagRequest("execution_steps", "Steps", "內容二", 2, false, null)
                )
        ));

        assertEquals("qa-template", response.templateKey());
        assertEquals(2, response.rag().size());
        assertEquals("full_context", response.rag().get(0).retrievalMode());
    }

    @Test
    void updateTemplateShouldRejectDuplicateKey() {
        SourceTemplateEntity existing = new SourceTemplateEntity("qa-template", "QA", "desc", "qa", "CONTENT", "prompts", true);
        existing.setTemplateId(10L);
        given(sourceTemplateRepository.findById(10L)).willReturn(Optional.of(existing));
        given(sourceTypeRepository.findByTypeCode("CONTENT")).willReturn(Optional.of(
                new SourceTypeEntity(3, "CONTENT", "內文類", "主要業務流程與回應格式", 3)
        ));

        SourceTemplateEntity other = new SourceTemplateEntity("duplicate", "Other", "desc", null, "CONTENT", "prompts", true);
        other.setTemplateId(11L);
        given(sourceTemplateRepository.findByTemplateKey("duplicate")).willReturn(Optional.of(other));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.updateTemplate(
                10L,
                new BuilderTemplateRequest("duplicate", "QA", "desc", "qa", "CONTENT", "prompts", true, List.of())
        ));

        assertEquals("TEMPLATE_KEY_DUPLICATE", exception.getCode());
    }
}
