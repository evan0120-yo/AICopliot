package com.citrus.rewardbridge.output.service.command;

import com.citrus.rewardbridge.common.dto.ConsultBusinessResponse;
import com.citrus.rewardbridge.common.dto.ConsultFilePayload;
import com.citrus.rewardbridge.output.dto.OutputFormat;
import com.citrus.rewardbridge.output.dto.OutputRenderCommand;
import com.citrus.rewardbridge.output.dto.RenderedFile;
import com.citrus.rewardbridge.output.dto.RenderedOutput;
import com.citrus.rewardbridge.output.dto.ScenarioOutputPolicy;
import com.citrus.rewardbridge.output.service.command.renderer.OutputRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class OutputCommandService {

    private final OutputRendererFactory outputRendererFactory;
    private final OutputScenarioPolicyResolver outputScenarioPolicyResolver;

    public RenderedOutput render(OutputRenderCommand command) {
        if (!command.businessResponse().isStatus()) {
            return RenderedOutput.of(withFile(command.businessResponse(), null));
        }

        ScenarioOutputPolicy policy = outputScenarioPolicyResolver.resolve(command.group(), command.type());
        if (!policy.includeFile()) {
            return RenderedOutput.of(withFile(command.businessResponse(), null));
        }

        OutputFormat resolvedOutputFormat = resolveOutputFormat(command, policy);
        OutputRenderer renderer = outputRendererFactory.get(resolvedOutputFormat);
        RenderedFile renderedFile = renderer.render(new OutputRenderCommand(
                command.group(),
                command.type(),
                resolvedOutputFormat,
                command.businessResponse()
        ));

        return RenderedOutput.of(withFile(
                command.businessResponse(),
                new ConsultFilePayload(
                        renderedFile.fileName(),
                        renderedFile.contentType(),
                        Base64.getEncoder().encodeToString(renderedFile.fileBytes())
                )
        ));
    }

    private OutputFormat resolveOutputFormat(OutputRenderCommand command, ScenarioOutputPolicy policy) {
        if (command.outputFormat() != null) {
            return command.outputFormat();
        }

        return policy.defaultOutputFormat();
    }

    private ConsultBusinessResponse withFile(ConsultBusinessResponse response, ConsultFilePayload file) {
        return new ConsultBusinessResponse(
                response.isStatus(),
                response.getStatusAns(),
                response.getResponse(),
                file
        );
    }
}
