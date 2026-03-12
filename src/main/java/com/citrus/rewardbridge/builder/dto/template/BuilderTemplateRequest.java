package com.citrus.rewardbridge.builder.dto.template;

import java.util.List;

public record BuilderTemplateRequest(
        String templateKey,
        String name,
        String description,
        String groupKey,
        Integer orderNo,
        String prompts,
        Boolean active,
        List<BuilderTemplateRagRequest> rag
) {
}
