package com.citrus.rewardbridge.builder.dto.graph;

import java.util.List;

public record BuilderGraphSourceResponse(
        Long sourceId,
        Long templateId,
        String templateKey,
        String templateName,
        String templateDescription,
        String templateGroupKey,
        Integer orderNo,
        boolean systemBlock,
        String prompts,
        List<BuilderGraphRagResponse> rag
) {
    public BuilderGraphSourceResponse(
            Long sourceId,
            Integer orderNo,
            boolean systemBlock,
            String prompts,
            List<BuilderGraphRagResponse> rag
    ) {
        this(sourceId, null, null, null, null, null, orderNo, systemBlock, prompts, rag);
    }
}
