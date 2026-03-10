package com.citrus.rewardbridge.source.dto;

import java.util.List;

public record SourceLoadResult(
        Integer builderId,
        List<SourceEntryDto> entries
) {
}
