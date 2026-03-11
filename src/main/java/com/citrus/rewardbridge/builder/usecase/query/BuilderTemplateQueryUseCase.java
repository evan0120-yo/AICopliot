package com.citrus.rewardbridge.builder.usecase.query;

import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateResponse;
import com.citrus.rewardbridge.builder.service.query.BuilderTemplateQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuilderTemplateQueryUseCase {

    private final BuilderTemplateQueryService builderTemplateQueryService;

    public List<BuilderTemplateResponse> listTemplates(Integer builderId) {
        return builderTemplateQueryService.listTemplates(builderId);
    }

    public List<BuilderTemplateResponse> listAllTemplates() {
        return builderTemplateQueryService.listAllTemplates();
    }
}
