package com.citrus.rewardbridge.builder.usecase.command;

import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateRequest;
import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateResponse;
import com.citrus.rewardbridge.builder.service.command.BuilderTemplateCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BuilderTemplateCommandUseCase {

    private final BuilderTemplateCommandService builderTemplateCommandService;

    public BuilderTemplateResponse createTemplate(BuilderTemplateRequest request) {
        return builderTemplateCommandService.createTemplate(request);
    }

    public BuilderTemplateResponse updateTemplate(Long templateId, BuilderTemplateRequest request) {
        return builderTemplateCommandService.updateTemplate(templateId, request);
    }

    public void deleteTemplate(Long templateId) {
        builderTemplateCommandService.deleteTemplate(templateId);
    }
}
