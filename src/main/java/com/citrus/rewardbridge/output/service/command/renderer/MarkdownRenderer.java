package com.citrus.rewardbridge.output.service.command.renderer;

import com.citrus.rewardbridge.output.dto.OutputFormat;
import com.citrus.rewardbridge.output.dto.OutputRenderCommand;
import com.citrus.rewardbridge.output.dto.RenderedFile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MarkdownRenderer implements OutputRenderer {

    @Override
    public boolean supports(OutputFormat outputFormat) {
        return outputFormat == OutputFormat.MARKDOWN;
    }

    @Override
    public RenderedFile render(OutputRenderCommand command) {
        String response = command.businessResponse().getResponse();
        String markdown = response == null ? "" : response;
        return new RenderedFile(
                buildFileName(command, "md"),
                OutputFormat.MARKDOWN.contentType(),
                markdown.getBytes(StandardCharsets.UTF_8)
        );
    }
}
