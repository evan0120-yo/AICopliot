package com.citrus.rewardbridge.output.usecase.command;

import com.citrus.rewardbridge.output.dto.OutputRenderCommand;
import com.citrus.rewardbridge.output.dto.RenderedOutput;
import com.citrus.rewardbridge.output.service.command.OutputCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutputCommandUseCase {

    private final OutputCommandService outputCommandService;

    public RenderedOutput render(OutputRenderCommand command) {
        return outputCommandService.render(command);
    }
}
