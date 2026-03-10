package com.citrus.rewardbridge.output.service.command.renderer;

import com.citrus.rewardbridge.common.scenario.ConsultScenario;
import com.citrus.rewardbridge.output.dto.OutputFormat;
import com.citrus.rewardbridge.output.dto.OutputRenderCommand;
import com.citrus.rewardbridge.output.dto.RenderedFile;

import java.util.Arrays;
import java.util.List;

public interface OutputRenderer {

    boolean supports(OutputFormat outputFormat);

    RenderedFile render(OutputRenderCommand command);

    default String buildFileName(OutputRenderCommand command, String extension) {
        return ConsultScenario.fromCodes(command.group(), command.type())
                .map(scenario -> "%s-consult.%s".formatted(scenario.filePrefix(), extension))
                .orElse("group%d-type%d-consult.%s".formatted(command.group(), command.type(), extension));
    }

    default String buildTitle(OutputRenderCommand command) {
        return ConsultScenario.fromCodes(command.group(), command.type())
                .map(scenario -> "RewardBridge %s".formatted(scenario.displayName()))
                .orElse("RewardBridge group=%d type=%d".formatted(command.group(), command.type()));
    }

    default List<String> splitLines(OutputRenderCommand command) {
        String response = command.businessResponse().getResponse();
        if (response == null || response.isBlank()) {
            return List.of("(empty response)");
        }

        return Arrays.stream(response.split("\\R"))
                .toList();
    }
}
