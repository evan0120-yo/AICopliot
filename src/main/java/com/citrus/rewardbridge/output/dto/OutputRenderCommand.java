package com.citrus.rewardbridge.output.dto;

import com.citrus.rewardbridge.common.entity.BuilderConfigEntity;
import com.citrus.rewardbridge.common.dto.ConsultBusinessResponse;

public record OutputRenderCommand(
        BuilderConfigEntity builderConfig,
        OutputFormat outputFormat,
        ConsultBusinessResponse businessResponse
) {
}
