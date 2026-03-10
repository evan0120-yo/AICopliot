package com.citrus.rewardbridge.gatekeeper.dto;

import com.citrus.rewardbridge.output.dto.OutputFormat;

public record ConsultGuardResult(
        Integer builderId,
        OutputFormat outputFormat
) {
}
