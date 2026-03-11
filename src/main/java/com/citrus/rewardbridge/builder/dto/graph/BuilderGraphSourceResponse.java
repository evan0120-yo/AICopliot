package com.citrus.rewardbridge.builder.dto.graph;

import java.util.List;

public record BuilderGraphSourceResponse(
        Long sourceId,
        Long templateId,
        String templateKey,
        String templateName,
        String templateDescription,
        String templateGroupKey,
        String typeCode,
        Integer orderNo,
        String prompts,
        List<BuilderGraphRagResponse> rag
) {
    public BuilderGraphSourceResponse(
            Long sourceId,
            String typeCode,
            Integer orderNo,
            String prompts,
            List<BuilderGraphRagResponse> rag
    ) {
        this(sourceId, null, null, null, null, null, typeCode, orderNo, prompts, rag);
    }
}
