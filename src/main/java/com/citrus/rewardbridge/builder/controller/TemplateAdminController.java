package com.citrus.rewardbridge.builder.controller;

import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateRequest;
import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateResponse;
import com.citrus.rewardbridge.builder.usecase.command.BuilderTemplateCommandUseCase;
import com.citrus.rewardbridge.builder.usecase.query.BuilderTemplateQueryUseCase;
import com.citrus.rewardbridge.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/templates")
@RequiredArgsConstructor
public class TemplateAdminController {

    private final BuilderTemplateQueryUseCase builderTemplateQueryUseCase;
    private final BuilderTemplateCommandUseCase builderTemplateCommandUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BuilderTemplateResponse>>> listAllTemplates() {
        return ResponseEntity.ok(ApiResponse.success(builderTemplateQueryUseCase.listAllTemplates()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BuilderTemplateResponse>> createTemplate(@RequestBody BuilderTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(builderTemplateCommandUseCase.createTemplate(request)));
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<ApiResponse<BuilderTemplateResponse>> updateTemplate(
            @PathVariable Long templateId,
            @RequestBody BuilderTemplateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(builderTemplateCommandUseCase.updateTemplate(templateId, request)));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long templateId) {
        builderTemplateCommandUseCase.deleteTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
