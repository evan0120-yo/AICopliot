package com.citrus.rewardbridge.output.dto;

import com.citrus.rewardbridge.common.dto.ConsultBusinessResponse;

public record RenderedOutput(
        ConsultBusinessResponse body
) {

    public static RenderedOutput of(ConsultBusinessResponse response) {
        return new RenderedOutput(response);
    }
}
