package com.citrus.rewardbridge.builder.service.command;

import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateRagRequest;
import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateRequest;
import com.citrus.rewardbridge.builder.entity.RagTemplateEntity;
import com.citrus.rewardbridge.builder.entity.SourceTemplateEntity;
import com.citrus.rewardbridge.builder.repository.RagTemplateRepository;
import com.citrus.rewardbridge.builder.repository.SourceTemplateRepository;
import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.source.entity.SourceEntity;
import com.citrus.rewardbridge.source.repository.SourceRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class BuilderTemplateCommandServiceTest {

    private final SourceTemplateRepository sourceTemplateRepository = mock(SourceTemplateRepository.class);
    private final RagTemplateRepository ragTemplateRepository = mock(RagTemplateRepository.class);
    private final SourceRepository sourceRepository = mock(SourceRepository.class);

    private final BuilderTemplateCommandService service = new BuilderTemplateCommandService(
            sourceTemplateRepository,
            ragTemplateRepository,
            sourceRepository
    );

    @Test
    void createTemplateShouldPersistTemplateAndNormalizeOrders() {
        SourceTemplateEntity existing = new SourceTemplateEntity("system-guard", "System", "desc", null, 1, "prompts", true);
        existing.setTemplateId(1L);

        given(sourceTemplateRepository.findByTemplateKey("qa-template")).willReturn(Optional.empty());
        given(sourceTemplateRepository.findAllByOrderByOrderNoAscTemplateIdAsc()).willReturn(List.of(existing));
        given(sourceTemplateRepository.save(any(SourceTemplateEntity.class))).willAnswer(invocation -> {
            SourceTemplateEntity entity = invocation.getArgument(0);
            if (entity.getTemplateId() == null) {
                entity.setTemplateId(10L);
            }
            return entity;
        });
        given(sourceTemplateRepository.findById(10L)).willReturn(Optional.of(new SourceTemplateEntity("qa-template", "QA 測試範本", "測試用", "qa", 1, "主要 prompts", true) {{
            setTemplateId(10L);
        }}));
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
                1,
                "主要 prompts",
                true,
                List.of(
                        new BuilderTemplateRagRequest("default_content", "Default", "內容一", 2, true, null),
                        new BuilderTemplateRagRequest("execution_steps", "Steps", "內容二", 1, false, null)
                )
        ));

        assertEquals("qa-template", response.templateKey());
        assertEquals(1, response.orderNo());
        assertEquals(2, response.rag().size());
        assertEquals("execution_steps", response.rag().get(0).ragType());
        assertEquals(1, response.rag().get(0).orderNo());
        assertEquals("full_context", response.rag().get(0).retrievalMode());
    }

    @Test
    void updateTemplateShouldRejectDuplicateKey() {
        SourceTemplateEntity existing = new SourceTemplateEntity("qa-template", "QA", "desc", "qa", 2, "prompts", true);
        existing.setTemplateId(10L);
        given(sourceTemplateRepository.findById(10L)).willReturn(Optional.of(existing));

        SourceTemplateEntity other = new SourceTemplateEntity("duplicate", "Other", "desc", null, 1, "prompts", true);
        other.setTemplateId(11L);
        given(sourceTemplateRepository.findByTemplateKey("duplicate")).willReturn(Optional.of(other));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.updateTemplate(
                10L,
                new BuilderTemplateRequest("duplicate", "QA", "desc", "qa", 2, "prompts", true, List.of())
        ));

        assertEquals("TEMPLATE_KEY_DUPLICATE", exception.getCode());
    }

    @Test
    void deleteTemplateShouldClearCopiedTemplateReferences() {
        SourceTemplateEntity existing = new SourceTemplateEntity("qa-template", "QA", "desc", "qa", 2, "prompts", true);
        existing.setTemplateId(10L);
        SourceTemplateEntity publicTemplate = new SourceTemplateEntity("system-guard", "System", "desc", null, 1, "prompts", true);
        publicTemplate.setTemplateId(1L);
        SourceEntity copiedSource = new SourceEntity(2, "copied", 1, false, 10L);
        copiedSource.setSourceId(99L);

        given(sourceTemplateRepository.findById(10L)).willReturn(Optional.of(existing));
        given(sourceRepository.findAllByCopiedFromTemplateId(10L)).willReturn(List.of(copiedSource));
        given(sourceTemplateRepository.findAllByOrderByOrderNoAscTemplateIdAsc()).willReturn(List.of(publicTemplate));

        service.deleteTemplate(10L);

        assertNull(copiedSource.getCopiedFromTemplateId());
    }
}
