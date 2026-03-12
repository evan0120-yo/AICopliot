package com.citrus.rewardbridge.source.dto;

public record SourceEntryDto(
        Long sourceId,
        String prompts,
        Integer orderNo,
        boolean systemBlock,
        boolean needsRagSupplement
) {
}
