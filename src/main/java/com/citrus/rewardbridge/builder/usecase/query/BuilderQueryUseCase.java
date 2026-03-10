package com.citrus.rewardbridge.builder.usecase.query;

import com.citrus.rewardbridge.builder.dto.BuilderSummaryDto;
import com.citrus.rewardbridge.builder.service.query.BuilderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuilderQueryUseCase {

    private final BuilderQueryService builderQueryService;

    public List<BuilderSummaryDto> listActiveBuilders() {
        return builderQueryService.listActiveBuilders();
    }
}
