package com.citrus.rewardbridge.builder.dto;

public record BuilderSummaryDto(
        Integer builderId,
        String builderCode,
        String groupKey,
        String groupLabel,
        String name,
        String description,
        boolean includeFile,
        String defaultOutputFormat
) {
    public BuilderSummaryDto(
            Integer builderId,
            String builderCode,
            String groupLabel,
            String name,
            String description,
            boolean includeFile,
            String defaultOutputFormat
    ) {
        this(builderId, builderCode, null, groupLabel, name, description, includeFile, defaultOutputFormat);
    }
}
