package com.citrus.rewardbridge.builder.dto.template;

public record BuilderTemplateRagRequest(
        String ragType,
        String title,
        String content,
        Integer orderNo,
        Boolean overridable,
        String retrievalMode
) {
}
