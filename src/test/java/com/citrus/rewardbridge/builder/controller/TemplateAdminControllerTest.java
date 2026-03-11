package com.citrus.rewardbridge.builder.controller;

import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateRequest;
import com.citrus.rewardbridge.builder.dto.template.BuilderTemplateResponse;
import com.citrus.rewardbridge.builder.usecase.command.BuilderTemplateCommandUseCase;
import com.citrus.rewardbridge.builder.usecase.query.BuilderTemplateQueryUseCase;
import com.citrus.rewardbridge.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TemplateAdminControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private BuilderTemplateQueryUseCase builderTemplateQueryUseCase;
    private BuilderTemplateCommandUseCase builderTemplateCommandUseCase;

    @BeforeEach
    void setUp() {
        builderTemplateQueryUseCase = mock(BuilderTemplateQueryUseCase.class);
        builderTemplateCommandUseCase = mock(BuilderTemplateCommandUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TemplateAdminController(
                        builderTemplateQueryUseCase,
                        builderTemplateCommandUseCase
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldListAllTemplates() throws Exception {
        given(builderTemplateQueryUseCase.listAllTemplates()).willReturn(List.of(
                new BuilderTemplateResponse(1L, "system-guard", "系統安全防護", "desc", null, "PINNED", "prompts", true, List.of())
        ));

        mockMvc.perform(get("/api/admin/templates").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data[0].templateKey").value("system-guard"));
    }

    @Test
    void shouldCreateTemplate() throws Exception {
        var response = new BuilderTemplateResponse(2L, "qa-template", "QA 範本", "desc", "qa", "CONTENT", "prompts", true, List.of());
        given(builderTemplateCommandUseCase.createTemplate(any(BuilderTemplateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/admin/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new BuilderTemplateRequest(
                                "qa-template", "QA 範本", "desc", "qa", "CONTENT", "prompts", true, List.of()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value(2))
                .andExpect(jsonPath("$.data.groupKey").value("qa"));
    }

    @Test
    void shouldUpdateTemplate() throws Exception {
        var response = new BuilderTemplateResponse(2L, "qa-template", "QA 範本", "desc", "qa", "CONTENT", "prompts", true, List.of());
        given(builderTemplateCommandUseCase.updateTemplate(eq(2L), any(BuilderTemplateRequest.class))).willReturn(response);

        mockMvc.perform(put("/api/admin/templates/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new BuilderTemplateRequest(
                                "qa-template", "QA 範本", "desc", "qa", "CONTENT", "prompts", true, List.of()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value(2));
    }

    @Test
    void shouldDeleteTemplate() throws Exception {
        doNothing().when(builderTemplateCommandUseCase).deleteTemplate(2L);

        mockMvc.perform(delete("/api/admin/templates/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
