package com.citrus.rewardbridge.builder.service.query;

import com.citrus.rewardbridge.builder.dto.BuilderSummaryDto;
import com.citrus.rewardbridge.common.repository.BuilderConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuilderQueryService {

    private final BuilderConfigRepository builderConfigRepository;

    public List<BuilderSummaryDto> listActiveBuilders() {
        return builderConfigRepository.findAllByActiveTrueOrderByBuilderIdAsc().stream()
                .map(builderConfig -> new BuilderSummaryDto(
                        builderConfig.getBuilderId(),
                        builderConfig.getBuilderCode(),
                        builderConfig.getGroupKey(),
                        builderConfig.getGroupLabel(),
                        builderConfig.getName(),
                        builderConfig.getDescription(),
                        builderConfig.isIncludeFile(),
                        builderConfig.getDefaultOutputFormat()
                ))
                .toList();
    }
}
