package com.citrus.rewardbridge.builder.dto.graph;

public record BuilderGraphBuilderResponse(
        Integer builderId,
        String builderCode,
        String groupKey,
        String groupLabel,
        String name,
        String description,
        boolean includeFile,
        String defaultOutputFormat,
        String filePrefix,
        boolean active
) {
    public BuilderGraphBuilderResponse(
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
        this(builderId, builderCode, null, groupLabel, name, description, includeFile, defaultOutputFormat, filePrefix, active);
    }
}
