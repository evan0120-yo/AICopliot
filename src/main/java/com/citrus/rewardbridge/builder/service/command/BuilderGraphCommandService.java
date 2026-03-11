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
import com.citrus.rewardbridge.output.dto.OutputFormat;
import com.citrus.rewardbridge.rag.entity.RagSupplementEntity;
import com.citrus.rewardbridge.rag.repository.RagSupplementRepository;
import com.citrus.rewardbridge.source.entity.SourceEntity;
import com.citrus.rewardbridge.source.entity.SourceTypeEntity;
import com.citrus.rewardbridge.source.repository.SourceRepository;
import com.citrus.rewardbridge.source.repository.SourceTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BuilderGraphCommandService {

    private static final String DEFAULT_TYPE_CODE = "CONTENT";
    private static final String DEFAULT_RETRIEVAL_MODE = "full_context";
    private static final String DEFAULT_GROUP_LABEL = "未分類";

    private final BuilderConfigRepository builderConfigRepository;
    private final SourceRepository sourceRepository;
    private final SourceTypeRepository sourceTypeRepository;
    private final RagSupplementRepository ragSupplementRepository;

    @Transactional
    public BuilderGraphResponse saveGraph(Integer builderId, BuilderGraphRequest request) {
        if (builderId == null) {
            throw new BusinessException("BUILDER_ID_MISSING", "builderId is required.", HttpStatus.BAD_REQUEST);
        }

        BuilderConfigEntity existingBuilder = builderConfigRepository.findById(builderId)
                .orElseThrow(() -> new BusinessException(
                        "BUILDER_NOT_FOUND",
                        "Requested builder does not exist.",
                        HttpStatus.NOT_FOUND
                ));
        List<ResolvedSourceRequest> sourceRequests = resolveSourceRequests(normalizeSources(request));

        BuilderConfigEntity builderConfig = saveBuilderConfig(builderId, request == null ? null : request.builder(), existingBuilder);
        deleteExistingGraph(builderId);

        List<ResolvedSourceResponse> savedSources = new ArrayList<>();

        for (ResolvedSourceRequest sourceRequest : sourceRequests) {
            SourceEntity sourceEntity = sourceRepository.save(new SourceEntity(
                    builderId,
                    sourceRequest.sourceType().getTypeId(),
                    sourceRequest.prompts(),
                    sourceRequest.orderNo(),
                    !sourceRequest.ragRequests().isEmpty(),
                    sourceRequest.templateId()
            ));

            List<BuilderGraphRagResponse> savedRags = saveRags(sourceEntity, sourceRequest.ragRequests());
            savedSources.add(new ResolvedSourceResponse(
                    sourceRequest.typeCode(),
                    sourceRequest.sourceType().getSortPriority(),
                    new BuilderGraphSourceResponse(
                            sourceEntity.getSourceId(),
                            sourceRequest.templateId(),
                            sourceRequest.templateKey(),
                            sourceRequest.templateName(),
                            sourceRequest.templateDescription(),
                            sourceRequest.templateGroupKey(),
                            sourceRequest.typeCode(),
                            sourceEntity.getOrderNo(),
                            sourceEntity.getPrompts(),
                            savedRags
                    )
            ));
        }

        List<BuilderGraphSourceResponse> canonicalSources = savedSources.stream()
                .sorted(Comparator.comparingInt(ResolvedSourceResponse::sortPriority)
                        .thenComparingInt(response -> response.source().orderNo())
                        .thenComparing(response -> response.source().sourceId()))
                .map(ResolvedSourceResponse::source)
                .toList();

        return new BuilderGraphResponse(toBuilderResponse(builderConfig), canonicalSources);
    }

    private BuilderConfigEntity saveBuilderConfig(
            Integer builderId,
            BuilderGraphBuilderRequest builderRequest,
            BuilderConfigEntity existingBuilder
    ) {
        BuilderConfigEntity builderConfig = existingBuilder;
        builderConfig.setBuilderId(builderId);

        String resolvedBuilderCode = firstNonBlank(
                builderRequest == null ? null : builderRequest.builderCode(),
                existingBuilder.getBuilderCode(),
                "builder-" + builderId
        );
        validateBuilderCodeUniqueness(builderId, resolvedBuilderCode);
        String resolvedName = firstNonBlank(
                builderRequest == null ? null : builderRequest.name(),
                existingBuilder.getName(),
                "Builder " + builderId
        );
        String resolvedGroupLabel = firstNonBlank(
                builderRequest == null ? null : builderRequest.groupLabel(),
                existingBuilder.getGroupLabel(),
                DEFAULT_GROUP_LABEL
        );
        String resolvedGroupKey = firstNonBlank(
                builderRequest == null ? null : builderRequest.groupKey(),
                existingBuilder.getGroupKey(),
                toGroupKey(resolvedGroupLabel)
        );
        boolean includeFile = resolveBoolean(
                builderRequest == null ? null : builderRequest.includeFile(),
                existingBuilder.isIncludeFile(),
                false
        );
        String defaultOutputFormat = resolveDefaultOutputFormat(
                includeFile,
                builderRequest == null ? null : builderRequest.defaultOutputFormat(),
                existingBuilder.getDefaultOutputFormat()
        );
        String resolvedFilePrefix = firstNonBlank(
                builderRequest == null ? null : builderRequest.filePrefix(),
                existingBuilder.getFilePrefix(),
                resolvedBuilderCode
        );

        builderConfig.setBuilderCode(resolvedBuilderCode);
        builderConfig.setGroupKey(resolvedGroupKey);
        builderConfig.setGroupLabel(resolvedGroupLabel);
        builderConfig.setName(resolvedName);
        builderConfig.setDescription(resolveDescription(
                builderRequest == null ? null : builderRequest.description(),
                existingBuilder.getDescription()
        ));
        builderConfig.setIncludeFile(includeFile);
        builderConfig.setDefaultOutputFormat(defaultOutputFormat);
        builderConfig.setFilePrefix(resolvedFilePrefix);
        builderConfig.setActive(resolveBoolean(
                builderRequest == null ? null : builderRequest.active(),
                existingBuilder.isActive(),
                true
        ));

        return builderConfigRepository.save(builderConfig);
    }

    private void deleteExistingGraph(Integer builderId) {
        List<SourceEntity> existingSources = sourceRepository.findAllByBuilderIdOrdered(builderId);
        List<Long> sourceIds = existingSources.stream()
                .map(SourceEntity::getSourceId)
                .toList();

        if (!sourceIds.isEmpty()) {
            ragSupplementRepository.deleteAllBySourceIdIn(sourceIds);
        }
        sourceRepository.deleteAllByBuilderId(builderId);
    }

    private List<BuilderGraphRagResponse> saveRags(SourceEntity sourceEntity, List<ResolvedRagRequest> ragRequests) {
        List<BuilderGraphRagResponse> responses = new ArrayList<>();

        for (ResolvedRagRequest ragRequest : ragRequests) {
            RagSupplementEntity savedEntity = ragSupplementRepository.save(new RagSupplementEntity(
                    sourceEntity.getSourceId(),
                    ragRequest.ragType(),
                    ragRequest.title(),
                    ragRequest.content(),
                    ragRequest.orderNo(),
                    ragRequest.overridable(),
                    ragRequest.retrievalMode()
            ));
            responses.add(new BuilderGraphRagResponse(
                    savedEntity.getRagId(),
                    savedEntity.getRagType(),
                    savedEntity.getTitle(),
                    savedEntity.getContent(),
                    savedEntity.getOrderNo(),
                    savedEntity.isOverridable(),
                    savedEntity.getRetrievalMode()
            ));
        }

        return responses.stream()
                .sorted(Comparator.comparingInt(BuilderGraphRagResponse::orderNo)
                        .thenComparing(BuilderGraphRagResponse::ragId))
                .toList();
    }

    private List<BuilderGraphSourceRequest> normalizeSources(BuilderGraphRequest request) {
        if (request == null) {
            throw new BusinessException("GRAPH_REQUEST_MISSING", "Graph request body is required.", HttpStatus.BAD_REQUEST);
        }

        boolean hasSources = request.sources() != null && !request.sources().isEmpty();
        boolean hasAiAgent = request.aiagent() != null && !request.aiagent().isEmpty();
        if (hasSources && hasAiAgent) {
            throw new BusinessException(
                    "GRAPH_SOURCE_FORMAT_CONFLICT",
                    "Use either sources or aiagent in the graph payload, not both.",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (hasSources) {
            return request.sources();
        }

        if (hasAiAgent) {
            return request.aiagent().stream()
                    .map(BuilderGraphAiAgentItemRequest::source)
                    .filter(Objects::nonNull)
                    .toList();
        }

        throw new BusinessException(
                "GRAPH_SOURCES_REQUIRED",
                "At least one source is required in the graph payload.",
                HttpStatus.BAD_REQUEST
        );
    }

    private List<ResolvedSourceRequest> resolveSourceRequests(List<BuilderGraphSourceRequest> sourceRequests) {
        if (sourceRequests.isEmpty()) {
            throw new BusinessException(
                    "GRAPH_SOURCES_REQUIRED",
                    "At least one source is required in the graph payload.",
                    HttpStatus.BAD_REQUEST
            );
        }

        Map<String, SourceTypeEntity> sourceTypesByCode = new HashMap<>();
        Map<String, List<PendingSourceRequest>> pendingByType = new LinkedHashMap<>();

        for (BuilderGraphSourceRequest sourceRequest : sourceRequests) {
            if (sourceRequest == null) {
                throw new BusinessException("GRAPH_SOURCE_INVALID", "Source entry cannot be null.", HttpStatus.BAD_REQUEST);
            }
            if (!StringUtils.hasText(sourceRequest.prompts())) {
                throw new BusinessException("SOURCE_PROMPTS_REQUIRED", "Each source must contain prompts.", HttpStatus.BAD_REQUEST);
            }

            String typeCode = normalizeTypeCode(sourceRequest.typeCode());
            SourceTypeEntity sourceType = sourceTypesByCode.computeIfAbsent(typeCode, this::loadSourceType);
            pendingByType.computeIfAbsent(typeCode, ignored -> new ArrayList<>())
                    .add(new PendingSourceRequest(sourceRequest, sourceType, normalizeRagRequests(sourceRequest.rag())));
        }

        List<ResolvedSourceRequest> resolved = new ArrayList<>();
        for (Map.Entry<String, List<PendingSourceRequest>> entry : pendingByType.entrySet()) {
            resolved.addAll(resolveSourceOrders(entry.getKey(), entry.getValue()));
        }
        return resolved;
    }

    private List<BuilderGraphRagRequest> normalizeRagRequests(List<BuilderGraphRagRequest> ragRequests) {
        return ragRequests == null ? List.of() : ragRequests.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ResolvedSourceRequest> resolveSourceOrders(String typeCode, List<PendingSourceRequest> pendingRequests) {
        Set<Integer> usedOrders = new HashSet<>();
        for (PendingSourceRequest pendingRequest : pendingRequests) {
            Integer explicitOrder = pendingRequest.sourceRequest().orderNo();
            if (explicitOrder == null) {
                continue;
            }
            validatePositiveOrder(explicitOrder, "SOURCE_ORDER_INVALID", "Source orderNo must be a positive integer.");
            if (!usedOrders.add(explicitOrder)) {
                throw new BusinessException(
                        "DUPLICATE_SOURCE_ORDER",
                        "Duplicate source orderNo exists within the same typeCode.",
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        int nextAvailableOrder = 1;
        List<ResolvedSourceRequest> resolved = new ArrayList<>();
        for (PendingSourceRequest pendingRequest : pendingRequests) {
            int resolvedOrder = pendingRequest.sourceRequest().orderNo() != null
                    ? pendingRequest.sourceRequest().orderNo()
                    : nextUnusedOrder(usedOrders, nextAvailableOrder);
            usedOrders.add(resolvedOrder);
            nextAvailableOrder = resolvedOrder + 1;

            resolved.add(new ResolvedSourceRequest(
                    pendingRequest.sourceRequest().templateId(),
                    pendingRequest.sourceRequest().templateKey(),
                    pendingRequest.sourceRequest().templateName(),
                    pendingRequest.sourceRequest().templateDescription(),
                    pendingRequest.sourceRequest().templateGroupKey(),
                    typeCode,
                    pendingRequest.sourceType(),
                    resolvedOrder,
                    pendingRequest.sourceRequest().prompts().trim(),
                    resolveRagRequests(pendingRequest.ragRequests())
            ));
        }
        return resolved;
    }

    private List<ResolvedRagRequest> resolveRagRequests(List<BuilderGraphRagRequest> ragRequests) {
        if (ragRequests.isEmpty()) {
            return List.of();
        }

        Set<Integer> usedOrders = new HashSet<>();
        for (BuilderGraphRagRequest ragRequest : ragRequests) {
            if (ragRequest.orderNo() == null) {
                continue;
            }
            validatePositiveOrder(ragRequest.orderNo(), "RAG_ORDER_INVALID", "Rag orderNo must be a positive integer.");
            if (!usedOrders.add(ragRequest.orderNo())) {
                throw new BusinessException(
                        "DUPLICATE_RAG_ORDER",
                        "Duplicate rag orderNo exists within the same source.",
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        int nextAvailableOrder = 1;
        List<ResolvedRagRequest> resolved = new ArrayList<>();
        for (BuilderGraphRagRequest ragRequest : ragRequests) {
            int resolvedOrder = ragRequest.orderNo() != null
                    ? ragRequest.orderNo()
                    : nextUnusedOrder(usedOrders, nextAvailableOrder);
            usedOrders.add(resolvedOrder);
            nextAvailableOrder = resolvedOrder + 1;

            String ragType = normalizeRagType(ragRequest.ragType(), resolvedOrder);
            resolved.add(new ResolvedRagRequest(
                    ragType,
                    firstNonBlank(ragRequest.title(), ragType + "-" + resolvedOrder),
                    resolveRagContent(ragRequest),
                    resolvedOrder,
                    ragRequest.overridable() != null && ragRequest.overridable(),
                    normalizeRetrievalMode(ragRequest.retrievalMode())
            ));
        }
        return resolved;
    }

    private SourceTypeEntity loadSourceType(String typeCode) {
        return sourceTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new BusinessException(
                        "SOURCE_TYPE_NOT_FOUND",
                        "Unknown source typeCode: " + typeCode,
                        HttpStatus.BAD_REQUEST
                ));
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

    private String resolveDefaultOutputFormat(boolean includeFile, String requested, String existing) {
        if (!includeFile) {
            return null;
        }

        String rawValue = firstNonBlank(requested, existing, OutputFormat.MARKDOWN.value());
        return OutputFormat.from(rawValue)
                .orElseThrow(() -> new BusinessException(
                        "INVALID_DEFAULT_OUTPUT_FORMAT",
                        "Builder default output format must be markdown or xlsx.",
                        HttpStatus.BAD_REQUEST
                ))
                .value();
    }

    private String resolveDescription(String requested, String existing) {
        return StringUtils.hasText(requested) ? requested.trim() : existing;
    }

    private boolean resolveBoolean(Boolean requested, Boolean existing, boolean defaultValue) {
        if (requested != null) {
            return requested;
        }
        if (existing != null) {
            return existing;
        }
        return defaultValue;
    }

    private String resolveRagContent(BuilderGraphRagRequest ragRequest) {
        if (ragRequest == null || !StringUtils.hasText(ragRequest.content())) {
            throw new BusinessException("RAG_CONTENT_REQUIRED", "Each rag entry must contain content.", HttpStatus.BAD_REQUEST);
        }
        return ragRequest.content().trim();
    }

    private String normalizeTypeCode(String rawTypeCode) {
        return normalizeUpperCaseValue(rawTypeCode, DEFAULT_TYPE_CODE);
    }

    private String normalizeRagType(String rawRagType, int orderNo) {
        if (!StringUtils.hasText(rawRagType)) {
            return "supplement_" + orderNo;
        }
        return rawRagType.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRetrievalMode(String rawRetrievalMode) {
        if (!StringUtils.hasText(rawRetrievalMode)) {
            return DEFAULT_RETRIEVAL_MODE;
        }

        String normalized = rawRetrievalMode.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("vector_search")) {
            throw new BusinessException(
                    "RAG_RETRIEVAL_MODE_UNSUPPORTED",
                    "retrievalMode=vector_search is not implemented yet.",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (!normalized.equals("full_context")) {
            throw new BusinessException(
                    "INVALID_RETRIEVAL_MODE",
                    "retrievalMode must be full_context.",
                    HttpStatus.BAD_REQUEST
            );
        }
        return normalized;
    }

    private String normalizeUpperCaseValue(String rawValue, String defaultValue) {
        return StringUtils.hasText(rawValue) ? rawValue.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    private String toGroupKey(String rawGroupLabel) {
        if (!StringUtils.hasText(rawGroupLabel)) {
            return null;
        }

        String normalized = rawGroupLabel.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "-")
                .replaceAll("^-+|-+$", "");
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private int nextUnusedOrder(Set<Integer> usedOrders, int start) {
        int candidate = start;
        while (usedOrders.contains(candidate)) {
            candidate++;
        }
        return candidate;
    }

    private void validatePositiveOrder(Integer orderNo, String code, String message) {
        if (orderNo != null && orderNo <= 0) {
            throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
        }
    }

    private void validateBuilderCodeUniqueness(Integer builderId, String builderCode) {
        builderConfigRepository.findByBuilderCode(builderCode)
                .filter(existing -> !existing.getBuilderId().equals(builderId))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "BUILDER_CODE_DUPLICATE",
                            "builderCode is already used by another builder.",
                            HttpStatus.BAD_REQUEST
                    );
                });
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }

        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private record PendingSourceRequest(
            BuilderGraphSourceRequest sourceRequest,
            SourceTypeEntity sourceType,
            List<BuilderGraphRagRequest> ragRequests
    ) {
    }

    private record ResolvedSourceRequest(
            Long templateId,
            String templateKey,
            String templateName,
            String templateDescription,
            String templateGroupKey,
            String typeCode,
            SourceTypeEntity sourceType,
            Integer orderNo,
            String prompts,
            List<ResolvedRagRequest> ragRequests
    ) {
    }

    private record ResolvedRagRequest(
            String ragType,
            String title,
            String content,
            Integer orderNo,
            boolean overridable,
            String retrievalMode
    ) {
    }

    private record ResolvedSourceResponse(
            String typeCode,
            Integer sortPriority,
            BuilderGraphSourceResponse source
    ) {
    }
}
