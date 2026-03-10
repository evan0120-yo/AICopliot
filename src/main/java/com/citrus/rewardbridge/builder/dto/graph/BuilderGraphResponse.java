package com.citrus.rewardbridge.builder.dto.graph;

import java.util.List;

public record BuilderGraphResponse(
        BuilderGraphBuilderResponse builder,
        List<BuilderGraphSourceResponse> sources
) {
}
