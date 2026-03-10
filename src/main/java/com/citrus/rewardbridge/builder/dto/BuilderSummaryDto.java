package com.citrus.rewardbridge.builder.dto;

public record BuilderSummaryDto(
        Integer builderId,
        String builderCode,
        String groupLabel,
        String name,
        String description,
        boolean includeFile,
        String defaultOutputFormat
) {
}
