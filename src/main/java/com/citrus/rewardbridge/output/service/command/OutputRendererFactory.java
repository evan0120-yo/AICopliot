package com.citrus.rewardbridge.output.service.command;

import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.output.dto.OutputFormat;
import com.citrus.rewardbridge.output.service.command.renderer.OutputRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutputRendererFactory {

    private final List<OutputRenderer> outputRenderers;

    public OutputRenderer get(OutputFormat outputFormat) {
        return outputRenderers.stream()
                .filter(renderer -> renderer.supports(outputFormat))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "OUTPUT_RENDERER_NOT_FOUND",
                        "No output renderer supports the requested output format.",
                        HttpStatus.INTERNAL_SERVER_ERROR
                ));
    }
}
