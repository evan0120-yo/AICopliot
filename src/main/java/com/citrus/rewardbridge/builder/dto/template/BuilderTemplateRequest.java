package com.citrus.rewardbridge.builder.dto.template;

import java.util.List;

public record BuilderTemplateRequest(
        String templateKey,
        String name,
        String description,
        String groupKey,
        String typeCode,
        String prompts,
        Boolean active,
        List<BuilderTemplateRagRequest> rag
) {
}
