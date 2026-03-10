package com.citrus.rewardbridge.builder.dto.graph;

public record BuilderGraphBuilderResponse(
        Integer builderId,
        String builderCode,
        String groupLabel,
        String name,
        String description,
        boolean includeFile,
        String defaultOutputFormat,
        String filePrefix,
        boolean active
) {
}
