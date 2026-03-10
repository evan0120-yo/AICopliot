package com.citrus.rewardbridge.gatekeeper.dto;

import com.citrus.rewardbridge.output.dto.OutputFormat;

public record ConsultGuardResult(
        Integer group,
        Integer type,
        OutputFormat outputFormat
) {
}
