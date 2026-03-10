package com.citrus.rewardbridge.source.service.query.strategy;

import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.common.scenario.ConsultScenario;
import com.citrus.rewardbridge.source.dto.SourceReferenceItemDto;
import com.citrus.rewardbridge.source.dto.SourceResult;
import com.citrus.rewardbridge.source.entity.SourceReferenceItemEntity;
import com.citrus.rewardbridge.source.entity.SourceRagMappingEntity;
import com.citrus.rewardbridge.source.entity.SourceScenarioConfigEntity;
import com.citrus.rewardbridge.source.repository.SourceRagMappingRepository;
import com.citrus.rewardbridge.source.repository.SourceReferenceItemRepository;
import com.citrus.rewardbridge.source.repository.SourceScenarioConfigRepository;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;

import java.util.List;

public abstract class BaseScenarioSourceStrategy implements SourceStrategy {

    private final SourceScenarioConfigRepository sourceScenarioConfigRepository;
    private final SourceReferenceItemRepository sourceReferenceItemRepository;
    private final SourceRagMappingRepository sourceRagMappingRepository;

    protected BaseScenarioSourceStrategy(
            SourceScenarioConfigRepository sourceScenarioConfigRepository,
            SourceReferenceItemRepository sourceReferenceItemRepository,
            SourceRagMappingRepository sourceRagMappingRepository
    ) {
        this.sourceScenarioConfigRepository = sourceScenarioConfigRepository;
        this.sourceReferenceItemRepository = sourceReferenceItemRepository;
        this.sourceRagMappingRepository = sourceRagMappingRepository;
    }

    protected abstract ConsultScenario scenario();

    protected abstract Logger log();

    @Override
    public boolean supports(Integer group, Integer type) {
        return scenario().matches(group, type);
    }

    @Override
    public SourceResult query(Integer group, Integer type) {
        SourceScenarioConfigEntity config = sourceScenarioConfigRepository.findByGroupCodeAndTypeCode(group, type)
                .orElseThrow(() -> new BusinessException(
                        "SOURCE_SCENARIO_NOT_FOUND",
                        "No source configuration was found for the requested consult scenario.",
                        HttpStatus.INTERNAL_SERVER_ERROR
                ));

        List<SourceReferenceItemEntity> referenceItems = sourceReferenceItemRepository
                .findByGroupCodeAndTypeCodeOrderBySortOrderAscIdAsc(group, type);
        List<String> ragKeys = sourceRagMappingRepository
                .findByGroupCodeAndTypeCodeOrderBySortOrderAscIdAsc(group, type)
                .stream()
                .map(SourceRagMappingEntity::getDocumentKey)
                .toList();

        log().info(
                "Loaded source data for consult scenario. group={}, type={}, referenceCount={}, ragKeyCount={}",
                group,
                type,
                referenceItems.size(),
                ragKeys.size()
        );

        return new SourceResult(
                group,
                type,
                config.getSummary(),
                ragKeys,
                referenceItems.stream()
                        .map(this::toDto)
                        .toList()
        );
    }

    private SourceReferenceItemDto toDto(SourceReferenceItemEntity entity) {
        return new SourceReferenceItemDto(
                entity.getItemName(),
                entity.getReferenceContent(),
                entity.getSuggestion()
        );
    }
}
