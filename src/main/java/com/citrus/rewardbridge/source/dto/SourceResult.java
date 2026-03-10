package com.citrus.rewardbridge.source.dto;

import java.util.List;

public record SourceResult(
        Integer group,
        Integer type,
        String summary,
        List<String> ragKeys,
        List<SourceReferenceItemDto> referenceItems
) {
}
