package com.citrus.rewardbridge.builder.controller;

import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphBuilderRequest;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphBuilderResponse;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphRagResponse;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphRequest;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphResponse;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphSourceRequest;
import com.citrus.rewardbridge.builder.dto.graph.BuilderGraphSourceResponse;
import com.citrus.rewardbridge.builder.usecase.command.BuilderGraphCommandUseCase;
import com.citrus.rewardbridge.builder.usecase.query.BuilderGraphQueryUseCase;
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
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BuilderAdminControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private BuilderGraphCommandUseCase builderGraphCommandUseCase;
    private BuilderGraphQueryUseCase builderGraphQueryUseCase;

    @BeforeEach
    void setUp() {
        builderGraphCommandUseCase = mock(BuilderGraphCommandUseCase.class);
        builderGraphQueryUseCase = mock(BuilderGraphQueryUseCase.class);

        mockMvc = MockMvcBuilders.standaloneSetup(new BuilderAdminController(
                        builderGraphCommandUseCase,
                        builderGraphQueryUseCase
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldSaveBuilderGraph() throws Exception {
        BuilderGraphResponse response = sampleResponse();
        given(builderGraphCommandUseCase.saveGraph(eq(2), any(BuilderGraphRequest.class))).willReturn(response);

        BuilderGraphRequest request = new BuilderGraphRequest(
                new BuilderGraphBuilderRequest("qa-smoke-doc", "測試團隊", "QA 冒煙測試文件產生", null, true, "xlsx", null, true),
                List.of(
                        new BuilderGraphSourceRequest("PINNED", 1, "你現在負責安全檢查...", List.of()),
                        new BuilderGraphSourceRequest("CONTENT", 2, "請依照以下流程完成分析", List.of())
                ),
                null
        );

        mockMvc.perform(put("/api/admin/builders/2/graph")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.builder.builderId").value(2))
                .andExpect(jsonPath("$.data.sources[1].typeCode").value("CONTENT"));
    }

    @Test
    void shouldLoadBuilderGraph() throws Exception {
        given(builderGraphQueryUseCase.loadGraph(2)).willReturn(sampleResponse());

        mockMvc.perform(get("/api/admin/builders/2/graph").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.builder.builderCode").value("qa-smoke-doc"))
                .andExpect(jsonPath("$.data.sources[1].rag[0].ragType").value("default_content"))
                .andExpect(jsonPath("$.data.sources[1].rag[0].overridable").value(true));
    }

    private BuilderGraphResponse sampleResponse() {
        return new BuilderGraphResponse(
                new BuilderGraphBuilderResponse(
                        2,
                        "qa-smoke-doc",
                        "測試團隊",
                        "QA 冒煙測試文件產生",
                        "協助 QA 快速產出冒煙測試案例",
                        true,
                        "xlsx",
                        "qa-smoke-doc",
                        true
                ),
                List.of(
                        new BuilderGraphSourceResponse(100L, "PINNED", 1, "你現在負責安全檢查...", List.of()),
                        new BuilderGraphSourceResponse(
                                101L,
                                "CONTENT",
                                2,
                                "請依照以下流程完成分析",
                                List.of(new BuilderGraphRagResponse(
                                        200L,
                                        "default_content",
                                        "預設內容",
                                        "若前端沒給需求，請先產出 default draft",
                                        1,
                                        true,
                                        "full_context"
                                ))
                        )
                )
        );
    }
}
