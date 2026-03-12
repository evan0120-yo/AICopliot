package com.citrus.rewardbridge.builder.service.command;

import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateRagRequest;
import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateRagResponse;
import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateRequest;
import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateResponse;
import com.citrus.rewardbridge.builder.entity.RagTemplateEntity;
import com.citrus.rewardbridge.builder.entity.SourceTemplateEntity;
import com.citrus.rewardbridge.builder.repository.RagTemplateRepository;
import com.citrus.rewardbridge.builder.repository.SourceTemplateRepository;
import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.rag.RagRetrievalModeNormalizer;
import com.citrus.rewardbridge.source.entity.SourceEntity;
import com.citrus.rewardbridge.source.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BuilderTemplateCommandService {

    private static final String FULL_CONTEXT = RagRetrievalModeNormalizer.FULL_CONTEXT;

    private final SourceTemplateRepository sourceTemplateRepository;
    private final RagTemplateRepository ragTemplateRepository;
    private final SourceRepository sourceRepository;

    @Transactional
    public BuilderTemplateResponse createTemplate(BuilderTemplateRequest request) {
        validateRequest(request);
        validateTemplateKeyUniqueness(null, request.templateKey());

        SourceTemplateEntity saved = sourceTemplateRepository.save(new SourceTemplateEntity(
                request.templateKey().trim(),
                request.name().trim(),
                normalizeNullableText(request.description()),
                trimToNull(request.groupKey()),
                1,
                normalizeText(request.prompts()),
                request.active() == null || request.active()
        ));

        reorderTemplates(saved.getTemplateId(), request.orderNo());
        List<BuilderTemplateRagResponse> ragResponses = replaceTemplateRags(saved.getTemplateId(), request.rag());
        SourceTemplateEntity refreshed = sourceTemplateRepository.findById(saved.getTemplateId())
                .orElse(saved);
        return toResponse(refreshed, ragResponses);
    }

    @Transactional
    public BuilderTemplateResponse updateTemplate(Long templateId, BuilderTemplateRequest request) {
        validateRequest(request);
        SourceTemplateEntity existing = sourceTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(
                        "TEMPLATE_NOT_FOUND",
                        "Requested template does not exist.",
                        HttpStatus.NOT_FOUND
                ));

        validateTemplateKeyUniqueness(templateId, request.templateKey());
        existing.setTemplateKey(request.templateKey().trim());
        existing.setName(request.name().trim());
        existing.setDescription(normalizeNullableText(request.description()));
        existing.setGroupKey(trimToNull(request.groupKey()));
        existing.setPrompts(normalizeText(request.prompts()));
        existing.setActive(request.active() == null || request.active());
        sourceTemplateRepository.save(existing);

        reorderTemplates(templateId, request.orderNo() == null ? existing.getOrderNo() : request.orderNo());
        List<BuilderTemplateRagResponse> ragResponses = replaceTemplateRags(templateId, request.rag());
        SourceTemplateEntity refreshed = sourceTemplateRepository.findById(templateId)
                .orElse(existing);
        return toResponse(refreshed, ragResponses);
    }

    @Transactional
    public void deleteTemplate(Long templateId) {
        SourceTemplateEntity existing = sourceTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(
                        "TEMPLATE_NOT_FOUND",
                        "Requested template does not exist.",
                        HttpStatus.NOT_FOUND
                ));

        List<SourceEntity> copiedSources = sourceRepository.findAllByCopiedFromTemplateId(templateId);
        if (!copiedSources.isEmpty()) {
            copiedSources.forEach(source -> source.setCopiedFromTemplateId(null));
            sourceRepository.saveAll(copiedSources);
        }

        ragTemplateRepository.deleteAllByTemplateId(templateId);
        sourceTemplateRepository.delete(existing);
        renumberTemplates(sourceTemplateRepository.findAllByOrderByOrderNoAscTemplateIdAsc());
    }

    private void validateRequest(BuilderTemplateRequest request) {
        if (request == null) {
            throw new BusinessException("TEMPLATE_REQUEST_MISSING", "Template request is required.", HttpStatus.BAD_REQUEST);
        }
        requireText("TEMPLATE_KEY_MISSING", request.templateKey(), "Template key is required.");
        requireText("TEMPLATE_NAME_MISSING", request.name(), "Template name is required.");
        validatePositiveOrder("TEMPLATE_ORDER_INVALID", request.orderNo(), "Template orderNo must be positive when provided.");
    }

    private void validateTemplateKeyUniqueness(Long currentTemplateId, String templateKey) {
        sourceTemplateRepository.findByTemplateKey(templateKey.trim())
                .filter(existing -> !existing.getTemplateId().equals(currentTemplateId))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "TEMPLATE_KEY_DUPLICATE",
                            "Template key already exists.",
                            HttpStatus.BAD_REQUEST
                    );
                });
    }

    private void reorderTemplates(Long templateId, Integer requestedOrderNo) {
        List<SourceTemplateEntity> orderedTemplates = new ArrayList<>(sourceTemplateRepository.findAllByOrderByOrderNoAscTemplateIdAsc());
        SourceTemplateEntity target = orderedTemplates.stream()
                .filter(template -> template.getTemplateId().equals(templateId))
                .findFirst()
                .orElseGet(() -> sourceTemplateRepository.findById(templateId)
                        .orElseThrow(() -> new BusinessException(
                                "TEMPLATE_NOT_FOUND",
                                "Requested template does not exist.",
                                HttpStatus.NOT_FOUND
                        )));

        orderedTemplates.removeIf(template -> template.getTemplateId().equals(templateId));
        int insertIndex = requestedOrderNo == null ? orderedTemplates.size() : Math.max(0, Math.min(requestedOrderNo - 1, orderedTemplates.size()));
        orderedTemplates.add(insertIndex, target);
        renumberTemplates(orderedTemplates);
    }

    private void renumberTemplates(List<SourceTemplateEntity> templates) {
        for (int index = 0; index < templates.size(); index++) {
            templates.get(index).setOrderNo(index + 1);
        }
        sourceTemplateRepository.saveAll(templates);
    }

    private List<BuilderTemplateRagResponse> replaceTemplateRags(Long templateId, List<BuilderTemplateRagRequest> requests) {
        ragTemplateRepository.deleteAllByTemplateId(templateId);
        List<NormalizedTemplateRag> normalizedRags = normalizeRags(requests);

        List<BuilderTemplateRagResponse> responses = new ArrayList<>();
        for (NormalizedTemplateRag normalizedRag : normalizedRags) {
            RagTemplateEntity savedRag = ragTemplateRepository.save(new RagTemplateEntity(
                    templateId,
                    normalizedRag.ragType(),
                    normalizedRag.title(),
                    normalizedRag.content(),
                    normalizedRag.orderNo(),
                    normalizedRag.overridable(),
                    normalizedRag.retrievalMode()
            ));
            responses.add(new BuilderTemplateRagResponse(
                    savedRag.getTemplateRagId(),
                    savedRag.getRagType(),
                    savedRag.getTitle(),
                    savedRag.getContent(),
                    savedRag.getOrderNo(),
                    savedRag.isOverridable(),
                    RagRetrievalModeNormalizer.normalizeForRead(
                            savedRag.getRetrievalMode(),
                            savedRag.getTemplateId(),
                            savedRag.getTemplateRagId(),
                            savedRag.getRagType()
                    )
            ));
        }
        return responses;
    }

    private List<NormalizedTemplateRag> normalizeRags(List<BuilderTemplateRagRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        List<IndexedTemplateRag> indexed = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            BuilderTemplateRagRequest request = requests.get(index);
            if (request != null) {
                indexed.add(new IndexedTemplateRag(index, request));
            }
        }

        indexed.sort(Comparator
                .comparing((IndexedTemplateRag rag) -> rag.request().orderNo() == null ? Integer.MAX_VALUE : rag.request().orderNo())
                .thenComparing(IndexedTemplateRag::index));

        List<NormalizedTemplateRag> normalized = new ArrayList<>();
        for (int index = 0; index < indexed.size(); index++) {
            BuilderTemplateRagRequest request = indexed.get(index).request();
            validatePositiveOrder("TEMPLATE_RAG_ORDER_INVALID", request.orderNo(), "Template RAG orderNo must be positive when provided.");
            normalized.add(new NormalizedTemplateRag(
                    requireText("TEMPLATE_RAG_TYPE_MISSING", request.ragType(), "Template RAG type is required."),
                    trimToNull(request.title()) == null ? request.ragType().trim() : request.title().trim(),
                    normalizeText(request.content()),
                    index + 1,
                    Boolean.TRUE.equals(request.overridable()),
                    normalizeRetrievalMode(request.retrievalMode())
            ));
        }
        return normalized;
    }

    private BuilderTemplateResponse toResponse(SourceTemplateEntity template, List<BuilderTemplateRagResponse> ragResponses) {
        return new BuilderTemplateResponse(
                template.getTemplateId(),
                template.getTemplateKey(),
                template.getName(),
                template.getDescription(),
                template.getGroupKey(),
                template.getOrderNo(),
                template.getPrompts(),
                template.isActive(),
                ragResponses
        );
    }

    private String normalizeRetrievalMode(String rawMode) {
        if (!StringUtils.hasText(rawMode)) {
            return FULL_CONTEXT;
        }

        String normalized = rawMode.trim().toLowerCase(Locale.ROOT);
        if (FULL_CONTEXT.equals(normalized)) {
            return FULL_CONTEXT;
        }

        throw new BusinessException(
                "RAG_RETRIEVAL_MODE_UNSUPPORTED",
                "Only full_context retrieval mode is currently supported.",
                HttpStatus.BAD_REQUEST
        );
    }

    private void validatePositiveOrder(String code, Integer orderNo, String message) {
        if (orderNo != null && orderNo <= 0) {
            throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
        }
    }

    private String requireText(String code, String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
        }
        return trimmed;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullableText(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record IndexedTemplateRag(int index, BuilderTemplateRagRequest request) {
    }

    private record NormalizedTemplateRag(
            String ragType,
            String title,
            String content,
            Integer orderNo,
            boolean overridable,
            String retrievalMode
    ) {
    }
}
