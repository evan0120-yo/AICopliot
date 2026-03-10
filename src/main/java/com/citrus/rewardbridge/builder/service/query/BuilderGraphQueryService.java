package com.citrus.rewardbridge.builder.service.query;

import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphBuilderResponse;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphRagResponse;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphResponse;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphSourceResponse;
import com.citrus.rewardbridge.common.entity.BuilderConfigEntity;
import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.common.repository.BuilderConfigRepository;
import com.citrus.rewardbridge.rag.entity.RagSupplementEntity;
import com.citrus.rewardbridge.rag.repository.RagSupplementRepository;
import com.citrus.rewardbridge.source.entity.SourceEntity;
import com.citrus.rewardbridge.source.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BuilderGraphQueryService {

    private final BuilderConfigRepository builderConfigRepository;
    private final SourceRepository sourceRepository;
    private final RagSupplementRepository ragSupplementRepository;

    public BuilderGraphResponse loadGraph(Integer builderId) {
        BuilderConfigEntity builderConfig = builderConfigRepository.findById(builderId)
                .orElseThrow(() -> new BusinessException(
                        "BUILDER_NOT_FOUND",
                        "Requested builder does not exist.",
                        HttpStatus.NOT_FOUND
                ));

        List<SourceEntity> sources = sourceRepository.findAllByBuilderIdOrdered(builderId);
        Map<Long, List<RagSupplementEntity>> ragsBySourceId = loadRagsBySourceIds(sources.stream()
                .map(SourceEntity::getSourceId)
                .toList());

        List<BuilderGraphSourceResponse> sourceResponses = sources.stream()
                .map(source -> new BuilderGraphSourceResponse(
                        source.getSourceId(),
                        source.getSourceType().getTypeCode(),
                        source.getOrderNo(),
                        source.getPrompts(),
                        ragsBySourceId.getOrDefault(source.getSourceId(), List.of()).stream()
                                .map(this::toRagResponse)
                                .toList()
                ))
                .toList();

        return new BuilderGraphResponse(
                new BuilderGraphBuilderResponse(
                        builderConfig.getBuilderId(),
                        builderConfig.getBuilderCode(),
                        builderConfig.getGroupLabel(),
                        builderConfig.getName(),
                        builderConfig.getDescription(),
                        builderConfig.isIncludeFile(),
                        builderConfig.getDefaultOutputFormat(),
                        builderConfig.getFilePrefix(),
                        builderConfig.isActive()
                ),
                sourceResponses
        );
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
                ragSupplement.getRetrievalMode()
        );
    }
}
