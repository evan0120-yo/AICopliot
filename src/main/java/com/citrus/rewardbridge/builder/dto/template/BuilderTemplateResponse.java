package com.citrus.rewardbridge.builder.dto.template;

import java.util.List;

public record BuilderTemplateResponse(
        Long templateId,
        String templateKey,
        String name,
        String description,
        String groupKey,
        String typeCode,
        String prompts,
        boolean active,
        List<BuilderTemplateRagResponse> rag
) {
}
