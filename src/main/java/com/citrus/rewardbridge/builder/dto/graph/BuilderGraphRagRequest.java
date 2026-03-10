package com.citrus.rewardbridge.builder.dto.graph;

import com.fasterxml.jackson.annotation.JsonAlias;

public record BuilderGraphRagRequest(
        String ragType,
        String title,
        @JsonAlias("prompts") String content,
        Integer orderNo,
        Boolean overridable,
        String retrievalMode
) {
}
