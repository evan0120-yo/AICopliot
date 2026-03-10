package com.citrus.rewardbridge.builder.dto.graph;

import java.util.List;

public record BuilderGraphRequest(
        BuilderGraphBuilderRequest builder,
        List<BuilderGraphSourceRequest> sources,
        List<BuilderGraphAiAgentItemRequest> aiagent
) {
}
