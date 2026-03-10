package com.citrus.rewardbridge.output.service.command.renderer;

import com.citrus.rewardbridge.output.dto.OutputFormat;
import com.citrus.rewardbridge.output.dto.OutputRenderCommand;
import com.citrus.rewardbridge.output.dto.RenderedFile;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

public interface OutputRenderer {

    boolean supports(OutputFormat outputFormat);

    RenderedFile render(OutputRenderCommand command);

    default String buildFileName(OutputRenderCommand command, String extension) {
        String filePrefix = command.builderConfig() == null ? null : command.builderConfig().getFilePrefix();
        if (!StringUtils.hasText(filePrefix)) {
            Integer builderId = command.builderConfig() == null ? null : command.builderConfig().getBuilderId();
            filePrefix = builderId == null ? "rewardbridge" : "builder-%d".formatted(builderId);
        }
        return "%s-consult.%s".formatted(filePrefix, extension);
    }

    default String buildTitle(OutputRenderCommand command) {
        if (command.builderConfig() == null || !StringUtils.hasText(command.builderConfig().getName())) {
            return "RewardBridge Consult Output";
        }
        return "RewardBridge %s".formatted(command.builderConfig().getName());
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
