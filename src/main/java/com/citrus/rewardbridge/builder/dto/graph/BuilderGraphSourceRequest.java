package com.citrus.rewardbridge.builder.dto.graph;

import java.util.List;

public record BuilderGraphSourceRequest(
        Long templateId,
        String templateKey,
        String templateName,
        String templateDescription,
        String templateGroupKey,
        Integer orderNo,
        Boolean systemBlock,
        String prompts,
        List<BuilderGraphRagRequest> rag
) {
    public BuilderGraphSourceRequest(
            Integer orderNo,
            String prompts,
            List<BuilderGraphRagRequest> rag
    ) {
        this(null, null, null, null, null, orderNo, false, prompts, rag);
    }
}
