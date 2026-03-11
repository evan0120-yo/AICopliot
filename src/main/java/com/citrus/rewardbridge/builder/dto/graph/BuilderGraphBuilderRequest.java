package com.citrus.rewardbridge.builder.dto.graph;

public record BuilderGraphBuilderRequest(
        String builderCode,
        String groupKey,
        String groupLabel,
        String name,
        String description,
        Boolean includeFile,
        String defaultOutputFormat,
        String filePrefix,
        Boolean active
) {
    public BuilderGraphBuilderRequest(
            String builderCode,
            String groupLabel,
            String name,
            String description,
            Boolean includeFile,
            String defaultOutputFormat,
            String filePrefix,
            Boolean active
    ) {
        this(builderCode, null, groupLabel, name, description, includeFile, defaultOutputFormat, filePrefix, active);
    }
}
