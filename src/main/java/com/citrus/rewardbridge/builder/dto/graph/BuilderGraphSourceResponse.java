package com.citrus.rewardbridge.builder.dto.graph;

import java.util.List;

public record BuilderGraphSourceResponse(
        Long sourceId,
        String typeCode,
        Integer orderNo,
        String prompts,
        List<BuilderGraphRagResponse> rag
) {
}
