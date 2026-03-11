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
import com.citrus.rewardbridge.source.entity.SourceTypeEntity;
import com.citrus.rewardbridge.source.repository.SourceTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BuilderTemplateCommandService {

    private static final String DEFAULT_TYPE_CODE = "CONTENT";
    private static final String DEFAULT_RETRIEVAL_MODE = RagRetrievalModeNormalizer.FULL_CONTEXT;

    private final SourceTemplateRepository sourceTemplateRepository;
    private final RagTemplateRepository ragTemplateRepository;
    private final SourceTypeRepository sourceTypeRepository;

    @Transactional
    public BuilderTemplateResponse createTemplate(BuilderTemplateRequest request) {
        if (request == null) {
            throw new BusinessException("TEMPLATE_REQUEST_MISSING", "Template request body is required.", HttpStatus.BAD_REQUEST);
        }

        SourceTypeEntity sourceType = loadSourceType(normalizeTypeCode(request.typeCode()));
        String resolvedName = requireText(request.name(), "TEMPLATE_NAME_REQUIRED", "Template name is required.");
        String resolvedTemplateKey = resolveTemplateKey(request.templateKey(), resolvedName);
        validateTemplateKeyUniqueness(resolvedTemplateKey, null);

        SourceTemplateEntity savedTemplate = sourceTemplateRepository.save(new SourceTemplateEntity(
                resolvedTemplateKey,
                resolvedName,
                normalizeNullableText(request.description()),
                normalizeNullableText(request.groupKey()),
                sourceType.getTypeCode(),
                normalizePrompts(request.prompts()),
                request.active() == null || request.active()
        ));
        List<BuilderTemplateRagResponse> savedRags = replaceRags(savedTemplate.getTemplateId(), request.rag());
        return toResponse(savedTemplate, savedRags);
    }

    @Transactional
    public BuilderTemplateResponse updateTemplate(Long templateId, BuilderTemplateRequest request) {
        if (templateId == null) {
            throw new BusinessException("TEMPLATE_ID_MISSING", "templateId is required.", HttpStatus.BAD_REQUEST);
        }
        if (request == null) {
            throw new BusinessException("TEMPLATE_REQUEST_MISSING", "Template request body is required.", HttpStatus.BAD_REQUEST);
        }

        SourceTemplateEntity existing = sourceTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "Requested template does not exist.", HttpStatus.NOT_FOUND));

        SourceTypeEntity sourceType = loadSourceType(normalizeTypeCode(request.typeCode()));
        String resolvedName = requireText(request.name(), "TEMPLATE_NAME_REQUIRED", "Template name is required.");
        String resolvedTemplateKey = resolveTemplateKey(request.templateKey(), resolvedName);
        validateTemplateKeyUniqueness(resolvedTemplateKey, templateId);

        existing.setTemplateKey(resolvedTemplateKey);
        existing.setName(resolvedName);
        existing.setDescription(normalizeNullableText(request.description()));
        existing.setGroupKey(normalizeNullableText(request.groupKey()));
        existing.setTypeCode(sourceType.getTypeCode());
        existing.setPrompts(normalizePrompts(request.prompts()));
        existing.setActive(request.active() == null || request.active());

        SourceTemplateEntity savedTemplate = sourceTemplateRepository.save(existing);
        List<BuilderTemplateRagResponse> savedRags = replaceRags(savedTemplate.getTemplateId(), request.rag());
        return toResponse(savedTemplate, savedRags);
    }

    @Transactional
    public void deleteTemplate(Long templateId) {
        if (templateId == null) {
            throw new BusinessException("TEMPLATE_ID_MISSING", "templateId is required.", HttpStatus.BAD_REQUEST);
        }

        SourceTemplateEntity existing = sourceTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "Requested template does not exist.", HttpStatus.NOT_FOUND));
        ragTemplateRepository.deleteAll(ragTemplateRepository.findByTemplateIdOrderByOrderNoAscTemplateRagIdAsc(templateId));
        sourceTemplateRepository.delete(existing);
    }

    private List<BuilderTemplateRagResponse> replaceRags(Long templateId, List<BuilderTemplateRagRequest> ragRequests) {
        List<RagTemplateEntity> existingRags = ragTemplateRepository.findByTemplateIdOrderByOrderNoAscTemplateRagIdAsc(templateId);
        if (!existingRags.isEmpty()) {
            ragTemplateRepository.deleteAll(existingRags);
        }

        List<ResolvedTemplateRagRequest> resolved = resolveRagRequests(ragRequests);
        List<BuilderTemplateRagResponse> responses = new ArrayList<>();
        for (ResolvedTemplateRagRequest rag : resolved) {
            RagTemplateEntity saved = ragTemplateRepository.save(new RagTemplateEntity(
                    templateId,
                    rag.ragType(),
                    rag.title(),
                    rag.content(),
                    rag.orderNo(),
                    rag.overridable(),
                    rag.retrievalMode()
            ));
            responses.add(new BuilderTemplateRagResponse(
                    saved.getTemplateRagId(),
                    saved.getRagType(),
                    saved.getTitle(),
                    saved.getContent(),
                    saved.getOrderNo(),
                    saved.isOverridable(),
                    saved.getRetrievalMode()
            ));
        }
        return responses;
    }

    private List<ResolvedTemplateRagRequest> resolveRagRequests(List<BuilderTemplateRagRequest> ragRequests) {
        if (ragRequests == null || ragRequests.isEmpty()) {
            return List.of();
        }

        Set<Integer> usedOrders = new HashSet<>();
        List<ResolvedTemplateRagRequest> resolved = new ArrayList<>();
        int nextOrder = 1;
        for (BuilderTemplateRagRequest ragRequest : ragRequests) {
            if (ragRequest == null) {
                continue;
            }
            int orderNo = ragRequest.orderNo() != null ? ragRequest.orderNo() : nextUnusedOrder(usedOrders, nextOrder);
            if (orderNo <= 0) {
                throw new BusinessException("TEMPLATE_RAG_ORDER_INVALID", "Template rag orderNo must be a positive integer.", HttpStatus.BAD_REQUEST);
            }
            if (!usedOrders.add(orderNo)) {
                throw new BusinessException("TEMPLATE_RAG_ORDER_DUPLICATE", "Duplicate template rag orderNo exists within the same template.", HttpStatus.BAD_REQUEST);
            }
            nextOrder = orderNo + 1;

            String ragType = StringUtils.hasText(ragRequest.ragType())
                    ? ragRequest.ragType().trim().toLowerCase(Locale.ROOT)
                    : "supplement_" + orderNo;
            String title = StringUtils.hasText(ragRequest.title())
                    ? ragRequest.title().trim()
                    : ragType + "-" + orderNo;
            String content = requireText(ragRequest.content(), "TEMPLATE_RAG_CONTENT_REQUIRED", "Each template rag entry must contain content.");
            String retrievalMode = normalizeRetrievalMode(ragRequest.retrievalMode());

            resolved.add(new ResolvedTemplateRagRequest(
                    ragType,
                    title,
                    content,
                    orderNo,
                    ragRequest.overridable() != null && ragRequest.overridable(),
                    retrievalMode
            ));
        }
        return resolved;
    }

    private BuilderTemplateResponse toResponse(SourceTemplateEntity template, List<BuilderTemplateRagResponse> ragResponses) {
        return new BuilderTemplateResponse(
                template.getTemplateId(),
                template.getTemplateKey(),
                template.getName(),
                template.getDescription(),
                template.getGroupKey(),
                template.getTypeCode(),
                template.getPrompts(),
                template.isActive(),
                ragResponses
        );
    }

    private SourceTypeEntity loadSourceType(String typeCode) {
        return sourceTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new BusinessException("SOURCE_TYPE_NOT_FOUND", "Unknown source typeCode: " + typeCode, HttpStatus.BAD_REQUEST));
    }

    private void validateTemplateKeyUniqueness(String templateKey, Long currentTemplateId) {
        sourceTemplateRepository.findByTemplateKey(templateKey)
                .filter(existing -> currentTemplateId == null || !existing.getTemplateId().equals(currentTemplateId))
                .ifPresent(existing -> {
                    throw new BusinessException("TEMPLATE_KEY_DUPLICATE", "templateKey is already used by another template.", HttpStatus.BAD_REQUEST);
                });
    }

    private String resolveTemplateKey(String rawTemplateKey, String fallbackName) {
        String normalized = StringUtils.hasText(rawTemplateKey) ? rawTemplateKey.trim() : toTemplateKey(fallbackName);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("TEMPLATE_KEY_REQUIRED", "Template key is required.", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String toTemplateKey(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "-")
                .replaceAll("^-+|-+$", "");
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private String normalizeTypeCode(String rawTypeCode) {
        return StringUtils.hasText(rawTypeCode) ? rawTypeCode.trim().toUpperCase(Locale.ROOT) : DEFAULT_TYPE_CODE;
    }

    private String normalizePrompts(String prompts) {
        return StringUtils.hasText(prompts) ? prompts.trim() : "";
    }

    private String normalizeNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String requireText(String value, String code, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
        }
        return value.trim();
    }

    private String normalizeRetrievalMode(String rawRetrievalMode) {
        if (!StringUtils.hasText(rawRetrievalMode)) {
            return DEFAULT_RETRIEVAL_MODE;
        }

        String normalized = rawRetrievalMode.trim().toLowerCase(Locale.ROOT);
        if ("vector_search".equals(normalized)) {
            throw new BusinessException("RAG_RETRIEVAL_MODE_UNSUPPORTED", "retrievalMode=vector_search is not implemented yet.", HttpStatus.BAD_REQUEST);
        }
        if (!DEFAULT_RETRIEVAL_MODE.equals(normalized)) {
            throw new BusinessException("INVALID_RETRIEVAL_MODE", "retrievalMode must be full_context.", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private int nextUnusedOrder(Set<Integer> usedOrders, int start) {
        int candidate = start;
        while (usedOrders.contains(candidate)) {
            candidate++;
        }
        return candidate;
    }

    private record ResolvedTemplateRagRequest(
            String ragType,
            String title,
            String content,
            Integer orderNo,
            boolean overridable,
            String retrievalMode
    ) {
    }
}
