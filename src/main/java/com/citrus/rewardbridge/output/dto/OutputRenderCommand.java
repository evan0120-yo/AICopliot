package com.citrus.rewardbridge.output.dto;

import com.citrus.rewardbridge.common.dto.ConsultBusinessResponse;

public record OutputRenderCommand(
        Integer group,
        Integer type,
        OutputFormat outputFormat,
        ConsultBusinessResponse businessResponse
) {
}
