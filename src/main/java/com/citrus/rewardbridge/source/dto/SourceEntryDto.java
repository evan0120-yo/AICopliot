package com.citrus.rewardbridge.source.dto;

public record SourceEntryDto(
        Long sourceId,
        String typeCode,
        String prompts,
        Integer orderNo,
        boolean needsRagSupplement
) {
}
