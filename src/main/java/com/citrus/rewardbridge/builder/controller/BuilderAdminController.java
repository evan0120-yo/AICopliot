package com.citrus.rewardbridge.builder.controller;

import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphRequest;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphResponse;
import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateResponse;
import com.citrus.rewardbridge.builder.usecase.command.BuilderGraphCommandUseCase;
import com.citrus.rewardbridge.builder.usecase.query.BuilderGraphQueryUseCase;
import com.citrus.rewardbridge.builder.usecase.query.BuilderTemplateQueryUseCase;
import com.citrus.rewardbridge.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/builders")
@RequiredArgsConstructor
public class BuilderAdminController {

    private final BuilderGraphCommandUseCase builderGraphCommandUseCase;
    private final BuilderGraphQueryUseCase builderGraphQueryUseCase;
    private final BuilderTemplateQueryUseCase builderTemplateQueryUseCase;

    @PutMapping("/{builderId}/graph")
    public ResponseEntity<ApiResponse<BuilderGraphResponse>> saveGraph(
            @PathVariable Integer builderId,
            @RequestBody BuilderGraphRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(builderGraphCommandUseCase.saveGraph(builderId, request)));
    }

    @GetMapping("/{builderId}/graph")
    public ResponseEntity<ApiResponse<BuilderGraphResponse>> loadGraph(@PathVariable Integer builderId) {
        return ResponseEntity.ok(ApiResponse.success(builderGraphQueryUseCase.loadGraph(builderId)));
    }

    @GetMapping("/{builderId}/templates")
    public ResponseEntity<ApiResponse<List<BuilderTemplateResponse>>> listTemplates(@PathVariable Integer builderId) {
        return ResponseEntity.ok(ApiResponse.success(builderTemplateQueryUseCase.listTemplates(builderId)));
    }
}
