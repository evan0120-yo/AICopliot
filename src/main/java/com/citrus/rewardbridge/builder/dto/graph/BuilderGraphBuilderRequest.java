package com.citrus.rewardbridge.builder.dto.graph;

public record BuilderGraphBuilderRequest(
        String builderCode,
        String groupLabel,
        String name,
        String description,
        Boolean includeFile,
        String defaultOutputFormat,
        String filePrefix,
        Boolean active
) {
}
