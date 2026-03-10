package com.citrus.rewardbridge.builder.usecase.command;

import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphRequest;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphResponse;
import com.citrus.rewardbridge.builder.service.command.BuilderGraphCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BuilderGraphCommandUseCase {

    private final BuilderGraphCommandService builderGraphCommandService;

    public BuilderGraphResponse saveGraph(Integer builderId, BuilderGraphRequest request) {
        return builderGraphCommandService.saveGraph(builderId, request);
    }
}
