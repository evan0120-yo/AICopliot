package com.citrus.rewardbridge.builder.dto.graph;

import java.util.List;

public record BuilderGraphSourceRequest(
        Long templateId,
        String templateKey,
        String templateName,
        String templateDescription,
        String templateGroupKey,
        String typeCode,
        Integer orderNo,
        String prompts,
        List<BuilderGraphRagRequest> rag
) {
    public BuilderGraphSourceRequest(
            String typeCode,
            Integer orderNo,
            String prompts,
            List<BuilderGraphRagRequest> rag
    ) {
        this(null, null, null, null, null, typeCode, orderNo, prompts, rag);
    }
}
