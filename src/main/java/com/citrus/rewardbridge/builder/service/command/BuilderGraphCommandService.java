package com.citrus.rewardbridge.builder.service.command;

import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphAiAgentItemRequest;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphBuilderRequest;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphBuilderResponse;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphRagRequest;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphRagResponse;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphRequest;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphResponse;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphSourceRequest;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphSourceResponse;
import com.citrus.rewardbridge.common.entity.BuilderConfigEntity;
import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.common.repository.BuilderConfigRepository;
import com.citrus.rewardbridge.rag.RagRetrievalModeNormalizer;
import com.citrus.rewardbridge.rag.entity.RagSupplementEntity;
import com.citrus.rewardbridge.rag.repository.RagSupplementRepository;
import com.citrus.rewardbridge.source.entity.SourceEntity;
import com.citrus.rewardbridge.source.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BuilderGraphCommandService {

    private static final String FULL_CONTEXT = RagRetrievalModeNormalizer.FULL_CONTEXT;

    private final BuilderConfigRepository builderConfigRepository;
    private final SourceRepository sourceRepository;
    private final RagSupplementRepository ragSupplementRepository;

    @Transactional
    public BuilderGraphResponse saveGraph(Integer builderId, BuilderGraphRequest request) {
        BuilderConfigEntity existingBuilder = builderConfigRepository.findById(builderId)
                .orElseThrow(() -> new BusinessException(
                        "BUILDER_NOT_FOUND",
                        "Requested builder does not exist.",
                        HttpStatus.NOT_FOUND
                ));

        BuilderConfigEntity savedBuilder = builderConfigRepository.save(mergeBuilder(existingBuilder, request == null ? null : request.builder()));

        List<SourceEntity> existingSources = sourceRepository.findAllByBuilderIdOrdered(builderId);
        List<SourceEntity> systemSources = existingSources.stream()
                .filter(SourceEntity::isSystemBlock)
                .toList();
        List<NormalizedSource> normalizedSources = normalizeSources(extractSourceRequests(request));
        deleteExistingGraph(existingSources);

        List<BuilderGraphSourceResponse> sourceResponses = new ArrayList<>(buildSystemSourceResponses(systemSources));
        for (NormalizedSource normalizedSource : normalizedSources) {
            SourceEntity savedSource = sourceRepository.save(new SourceEntity(
                    builderId,
                    normalizedSource.prompts(),
                    normalizedSource.orderNo(),
                    false,
                    !normalizedSource.rags().isEmpty(),
                    normalizedSource.templateId()
            ));

            List<BuilderGraphRagResponse> ragResponses = new ArrayList<>();
            for (NormalizedRag normalizedRag : normalizedSource.rags()) {
                RagSupplementEntity savedRag = ragSupplementRepository.save(new RagSupplementEntity(
                        savedSource.getSourceId(),
                        normalizedRag.ragType(),
                        normalizedRag.title(),
                        normalizedRag.content(),
                        normalizedRag.orderNo(),
                        normalizedRag.overridable(),
                        normalizedRag.retrievalMode()
                ));
                ragResponses.add(new BuilderGraphRagResponse(
                        savedRag.getRagId(),
                        savedRag.getRagType(),
                        savedRag.getTitle(),
                        savedRag.getContent(),
                        savedRag.getOrderNo(),
                        savedRag.isOverridable(),
                        RagRetrievalModeNormalizer.normalizeForRead(
                                savedRag.getRetrievalMode(),
                                savedSource.getSourceId(),
                                savedRag.getRagId(),
                                savedRag.getRagType()
                        )
                ));
            }

            sourceResponses.add(new BuilderGraphSourceResponse(
                    savedSource.getSourceId(),
                    normalizedSource.templateId(),
                    normalizedSource.templateKey(),
                    normalizedSource.templateName(),
                    normalizedSource.templateDescription(),
                    normalizedSource.templateGroupKey(),
                    savedSource.getOrderNo(),
                    savedSource.isSystemBlock(),
                    savedSource.getPrompts(),
                    ragResponses
            ));
        }

        return new BuilderGraphResponse(toBuilderResponse(savedBuilder), sourceResponses);
    }

    private BuilderConfigEntity mergeBuilder(BuilderConfigEntity existingBuilder, BuilderGraphBuilderRequest request) {
        if (request == null) {
            return existingBuilder;
        }

        String builderCode = trimToNull(request.builderCode());
        if (!StringUtils.hasText(builderCode)) {
            builderCode = existingBuilder.getBuilderCode();
        }
        validateBuilderCodeUniqueness(existingBuilder.getBuilderId(), builderCode);

        existingBuilder.setBuilderCode(builderCode);
        existingBuilder.setGroupLabel(resolveGroupLabel(existingBuilder, request));
        existingBuilder.setGroupKey(resolveGroupKey(existingBuilder, request));
        existingBuilder.setName(resolveString(request.name(), existingBuilder.getName(), true));
        existingBuilder.setDescription(resolveString(request.description(), existingBuilder.getDescription(), false));
        existingBuilder.setIncludeFile(resolveBoolean(request.includeFile(), existingBuilder.isIncludeFile()));
        existingBuilder.setDefaultOutputFormat(resolveString(request.defaultOutputFormat(), existingBuilder.getDefaultOutputFormat(), false));
        existingBuilder.setFilePrefix(resolveString(request.filePrefix(), existingBuilder.getFilePrefix(), false));
        existingBuilder.setActive(resolveBoolean(request.active(), existingBuilder.isActive()));
        return existingBuilder;
    }

    private BuilderGraphBuilderResponse toBuilderResponse(BuilderConfigEntity builderConfig) {
        return new BuilderGraphBuilderResponse(
                builderConfig.getBuilderId(),
                builderConfig.getBuilderCode(),
                builderConfig.getGroupKey(),
                builderConfig.getGroupLabel(),
                builderConfig.getName(),
                builderConfig.getDescription(),
                builderConfig.isIncludeFile(),
                builderConfig.getDefaultOutputFormat(),
                builderConfig.getFilePrefix(),
                builderConfig.isActive()
        );
    }

    private void validateBuilderCodeUniqueness(Integer builderId, String builderCode) {
        builderConfigRepository.findByBuilderCode(builderCode)
                .filter(existing -> !existing.getBuilderId().equals(builderId))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "BUILDER_CODE_DUPLICATE",
                            "Builder code already exists.",
                            HttpStatus.BAD_REQUEST
                    );
                });
    }

    private List<BuilderGraphSourceRequest> extractSourceRequests(BuilderGraphRequest request) {
        if (request == null) {
            return List.of();
        }
        if (request.sources() != null && !request.sources().isEmpty()) {
            return request.sources();
        }
        if (request.aiagent() == null || request.aiagent().isEmpty()) {
            return List.of();
        }
        return request.aiagent().stream()
                .map(BuilderGraphAiAgentItemRequest::source)
                .filter(source -> source != null && !Boolean.TRUE.equals(source.systemBlock()))
                .toList();
    }

    private List<NormalizedSource> normalizeSources(List<BuilderGraphSourceRequest> requests) {
        List<IndexedSource> indexedSources = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            BuilderGraphSourceRequest request = requests.get(index);
            if (request == null) {
                continue;
            }
            if (Boolean.TRUE.equals(request.systemBlock())) {
                continue;
            }
            indexedSources.add(new IndexedSource(index, request));
        }

        indexedSources.sort(Comparator
                .comparing((IndexedSource source) -> source.request().orderNo() == null ? Integer.MAX_VALUE : source.request().orderNo())
                .thenComparing(IndexedSource::index));

        List<NormalizedSource> normalized = new ArrayList<>();
        for (int index = 0; index < indexedSources.size(); index++) {
            BuilderGraphSourceRequest request = indexedSources.get(index).request();
            validatePositiveOrder("SOURCE_ORDER_INVALID", request.orderNo(), "Source orderNo must be positive when provided.");
            normalized.add(new NormalizedSource(
                    request.templateId(),
                    trimToNull(request.templateKey()),
                    trimToNull(request.templateName()),
                    normalizeNullableText(request.templateDescription()),
                    trimToNull(request.templateGroupKey()),
                    index + 1,
                    normalizeText(request.prompts()),
                    normalizeRags(request.rag())
            ));
        }
        return normalized;
    }

    private List<NormalizedRag> normalizeRags(List<BuilderGraphRagRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        List<IndexedRag> indexedRags = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            BuilderGraphRagRequest request = requests.get(index);
            if (request == null) {
                continue;
            }
            indexedRags.add(new IndexedRag(index, request));
        }

        indexedRags.sort(Comparator
                .comparing((IndexedRag rag) -> rag.request().orderNo() == null ? Integer.MAX_VALUE : rag.request().orderNo())
                .thenComparing(IndexedRag::index));

        List<NormalizedRag> normalized = new ArrayList<>();
        for (int index = 0; index < indexedRags.size(); index++) {
            BuilderGraphRagRequest request = indexedRags.get(index).request();
            validatePositiveOrder("RAG_ORDER_INVALID", request.orderNo(), "RAG orderNo must be positive when provided.");
            normalized.add(new NormalizedRag(
                    normalizeRequiredText("RAG_TYPE_MISSING", request.ragType(), "RAG type is required."),
                    resolveRagTitle(request),
                    normalizeText(request.content()),
                    index + 1,
                    Boolean.TRUE.equals(request.overridable()),
                    normalizeRetrievalMode(request.retrievalMode())
            ));
        }
        return normalized;
    }

    private void validatePositiveOrder(String code, Integer orderNo, String message) {
        if (orderNo != null && orderNo <= 0) {
            throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
        }
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

    private String resolveRagTitle(BuilderGraphRagRequest request) {
        String title = trimToNull(request.title());
        return title != null ? title : request.ragType();
    }

    private List<BuilderGraphSourceResponse> buildSystemSourceResponses(List<SourceEntity> systemSources) {
        if (systemSources.isEmpty()) {
            return List.of();
        }

        Map<Long, List<RagSupplementEntity>> ragsBySourceId = loadRagsBySourceIds(systemSources.stream()
                .map(SourceEntity::getSourceId)
                .toList());

        return systemSources.stream()
                .map(source -> new BuilderGraphSourceResponse(
                        source.getSourceId(),
                        source.getCopiedFromTemplateId(),
                        source.getCopiedFromTemplate() == null ? null : source.getCopiedFromTemplate().getTemplateKey(),
                        source.getCopiedFromTemplate() == null ? null : source.getCopiedFromTemplate().getName(),
                        source.getCopiedFromTemplate() == null ? null : source.getCopiedFromTemplate().getDescription(),
                        source.getCopiedFromTemplate() == null ? null : source.getCopiedFromTemplate().getGroupKey(),
                        source.getOrderNo(),
                        true,
                        source.getPrompts(),
                        ragsBySourceId.getOrDefault(source.getSourceId(), List.of()).stream()
                                .map(this::toRagResponse)
                                .toList()
                ))
                .toList();
    }

    private void deleteExistingGraph(List<SourceEntity> existingSources) {
        if (existingSources.isEmpty()) {
            return;
        }

        List<Long> sourceIds = existingSources.stream()
                .filter(source -> !source.isSystemBlock())
                .map(SourceEntity::getSourceId)
                .toList();
        if (sourceIds.isEmpty()) {
            return;
        }

        ragSupplementRepository.deleteAllBySourceIdIn(sourceIds);
        sourceRepository.deleteAll(existingSources.stream()
                .filter(source -> !source.isSystemBlock())
                .toList());
    }

    private Map<Long, List<RagSupplementEntity>> loadRagsBySourceIds(Collection<Long> sourceIds) {
        Map<Long, List<RagSupplementEntity>> result = new LinkedHashMap<>();
        if (sourceIds == null || sourceIds.isEmpty()) {
            return result;
        }

        for (RagSupplementEntity ragSupplement : ragSupplementRepository.findBySourceIdInOrderBySourceIdAscOrderNoAscRagIdAsc(sourceIds)) {
            result.computeIfAbsent(ragSupplement.getSourceId(), ignored -> new ArrayList<>())
                    .add(ragSupplement);
        }
        return result;
    }

    private BuilderGraphRagResponse toRagResponse(RagSupplementEntity ragSupplement) {
        return new BuilderGraphRagResponse(
                ragSupplement.getRagId(),
                ragSupplement.getRagType(),
                ragSupplement.getTitle(),
                ragSupplement.getContent(),
                ragSupplement.getOrderNo(),
                ragSupplement.isOverridable(),
                RagRetrievalModeNormalizer.normalizeForRead(
                        ragSupplement.getRetrievalMode(),
                        ragSupplement.getSourceId(),
                        ragSupplement.getRagId(),
                        ragSupplement.getRagType()
                )
        );
    }

    private String resolveGroupLabel(BuilderConfigEntity existingBuilder, BuilderGraphBuilderRequest request) {
        String requestedGroupLabel = trimToNull(request.groupLabel());
        if (requestedGroupLabel != null) {
            return requestedGroupLabel;
        }
        return existingBuilder.getGroupLabel();
    }

    private String resolveGroupKey(BuilderConfigEntity existingBuilder, BuilderGraphBuilderRequest request) {
        String explicitGroupKey = trimToNull(request.groupKey());
        if (explicitGroupKey != null) {
            return explicitGroupKey;
        }

        String existingGroupKey = trimToNull(existingBuilder.getGroupKey());
        if (existingGroupKey != null) {
            return existingGroupKey;
        }

        return toGroupKey(resolveGroupLabel(existingBuilder, request));
    }

    private String toGroupKey(String rawGroupLabel) {
        String normalized = trimToNull(rawGroupLabel);
        if (normalized == null) {
            return null;
        }

        String collapsed = normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return collapsed.isBlank() ? normalized : collapsed;
    }

    private String resolveString(String requestedValue, String existingValue, boolean required) {
        String normalized = requestedValue == null ? existingValue : normalizeNullableText(requestedValue);
        if (required && !StringUtils.hasText(normalized)) {
            throw new BusinessException("BUILDER_FIELD_MISSING", "Required builder field is missing.", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private boolean resolveBoolean(Boolean requestedValue, boolean existingValue) {
        return requestedValue == null ? existingValue : requestedValue;
    }

    private String normalizeRequiredText(String code, String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
        }
        return normalized;
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

    private record IndexedSource(int index, BuilderGraphSourceRequest request) {
    }

    private record IndexedRag(int index, BuilderGraphRagRequest request) {
    }

    private record NormalizedSource(
            Long templateId,
            String templateKey,
            String templateName,
            String templateDescription,
            String templateGroupKey,
            Integer orderNo,
            String prompts,
            List<NormalizedRag> rags
    ) {
    }

    private record NormalizedRag(
            String ragType,
            String title,
            String content,
            Integer orderNo,
            boolean overridable,
            String retrievalMode
    ) {
    }
}
