package com.citrus.rewardbridge.builder.dto.graph;

import java.util.List;

public record BuilderGraphSourceRequest(
        String typeCode,
        Integer orderNo,
        String prompts,
        List<BuilderGraphRagRequest> rag
) {
}
