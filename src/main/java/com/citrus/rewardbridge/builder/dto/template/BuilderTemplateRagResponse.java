package com.citrus.rewardbridge.builder.dto.template;

public record BuilderTemplateRagResponse(
        Long templateRagId,
        String ragType,
        String title,
        String content,
        Integer orderNo,
        boolean overridable,
        String retrievalMode
) {
}
