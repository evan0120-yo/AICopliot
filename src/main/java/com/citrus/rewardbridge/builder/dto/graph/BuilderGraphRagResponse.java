package com.citrus.rewardbridge.builder.dto.graph;

public record BuilderGraphRagResponse(
        Long ragId,
        String ragType,
        String title,
        String content,
        Integer orderNo,
        boolean overridable,
        String retrievalMode
) {
}
