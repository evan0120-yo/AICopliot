package com.citrus.rewardbridge.builder.service.query;

import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateRagResponse;
import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateResponse;
import com.citrus.rewardbridge.builder.entity.RagTemplateEntity;
import com.citrus.rewardbridge.builder.entity.SourceTemplateEntity;
import com.citrus.rewardbridge.builder.repository.RagTemplateRepository;
import com.citrus.rewardbridge.builder.repository.SourceTemplateRepository;
import com.citrus.rewardbridge.common.entity.BuilderConfigEntity;
import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.common.repository.BuilderConfigRepository;
import com.citrus.rewardbridge.rag.RagRetrievalModeNormalizer;
import jakarta.transaction.Transactional;
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
@Transactional(Transactional.TxType.SUPPORTS)
public class BuilderTemplateQueryService {

    private final BuilderConfigRepository builderConfigRepository;
    private final SourceTemplateRepository sourceTemplateRepository;
    private final RagTemplateRepository ragTemplateRepository;

    public List<BuilderTemplateResponse> listTemplates(Integer builderId) {
        if (builderId == null) {
            throw new BusinessException("BUILDER_ID_MISSING", "builderId is required.", HttpStatus.BAD_REQUEST);
        }

        BuilderConfigEntity builderConfig = builderConfigRepository.findById(builderId)
                .orElseThrow(() -> new BusinessException(
                        "BUILDER_NOT_FOUND",
                        "Requested builder does not exist.",
                        HttpStatus.NOT_FOUND
                ));

        List<SourceTemplateEntity> templates = sourceTemplateRepository.findActiveByBuilderGroup(builderConfig.getGroupKey());
        Map<Long, List<RagTemplateEntity>> ragsByTemplateId = loadRagsByTemplateIds(templates.stream()
                .map(SourceTemplateEntity::getTemplateId)
                .toList());

        return templates.stream()
                .map(template -> toResponse(template, ragsByTemplateId.getOrDefault(template.getTemplateId(), List.of())))
                .toList();
    }

    public List<BuilderTemplateResponse> listAllTemplates() {
        List<SourceTemplateEntity> templates = sourceTemplateRepository.findAllForAdminOrder();
        Map<Long, List<RagTemplateEntity>> ragsByTemplateId = loadRagsByTemplateIds(templates.stream()
                .map(SourceTemplateEntity::getTemplateId)
                .toList());

        return templates.stream()
                .map(template -> toResponse(template, ragsByTemplateId.getOrDefault(template.getTemplateId(), List.of())))
                .toList();
    }

    private Map<Long, List<RagTemplateEntity>> loadRagsByTemplateIds(Collection<Long> templateIds) {
        Map<Long, List<RagTemplateEntity>> result = new LinkedHashMap<>();
        if (templateIds == null || templateIds.isEmpty()) {
            return result;
        }

        for (RagTemplateEntity ragTemplate : ragTemplateRepository.findByTemplateIdInOrderByTemplateIdAscOrderNoAscTemplateRagIdAsc(templateIds)) {
            result.computeIfAbsent(ragTemplate.getTemplateId(), ignored -> new ArrayList<>())
                    .add(ragTemplate);
        }
        return result;
    }

    private BuilderTemplateRagResponse toRagResponse(RagTemplateEntity ragTemplate) {
        return new BuilderTemplateRagResponse(
                ragTemplate.getTemplateRagId(),
                ragTemplate.getRagType(),
                ragTemplate.getTitle(),
                ragTemplate.getContent(),
                ragTemplate.getOrderNo(),
                ragTemplate.isOverridable(),
                RagRetrievalModeNormalizer.normalizeForRead(
                        ragTemplate.getRetrievalMode(),
                        ragTemplate.getTemplateId(),
                        ragTemplate.getTemplateRagId(),
                        ragTemplate.getRagType()
                )
        );
    }

    private BuilderTemplateResponse toResponse(SourceTemplateEntity template, List<RagTemplateEntity> ragTemplates) {
        return new BuilderTemplateResponse(
                template.getTemplateId(),
                template.getTemplateKey(),
                template.getName(),
                template.getDescription(),
                template.getGroupKey(),
                template.getOrderNo(),
                template.getPrompts(),
                template.isActive(),
                ragTemplates.stream()
                        .map(this::toRagResponse)
                        .toList()
        );
    }
}
