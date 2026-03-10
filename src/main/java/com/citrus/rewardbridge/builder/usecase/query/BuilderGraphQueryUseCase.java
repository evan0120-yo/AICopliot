package com.citrus.rewardbridge.builder.usecase.query;

import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphResponse;
import com.citrus.rewardbridge.builder.service.query.BuilderGraphQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BuilderGraphQueryUseCase {

    private final BuilderGraphQueryService builderGraphQueryService;

    public BuilderGraphResponse loadGraph(Integer builderId) {
        return builderGraphQueryService.loadGraph(builderId);
    }
}
