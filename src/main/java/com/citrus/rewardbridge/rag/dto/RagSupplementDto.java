package com.citrus.rewardbridge.rag.dto;

public record RagSupplementDto(
        Long ragId,
        Long sourceId,
        String ragType,
        String title,
        String content,
        Integer orderNo,
        boolean overridable,
        String retrievalMode
) {
}
