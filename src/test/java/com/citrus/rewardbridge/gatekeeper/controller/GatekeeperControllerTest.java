package com.citrus.rewardbridge.gatekeeper.controller;

import com.citrus.rewardbridge.builder.dto.BuilderSummaryDto;
import com.citrus.rewardbridge.builder.usecase.query.BuilderQueryUseCase;
import com.citrus.rewardbridge.gatekeeper.service.guard.ClientIpResolver;
import com.citrus.rewardbridge.gatekeeper.usecase.command.GatekeeperCommandUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GatekeeperControllerTest {

    private MockMvc mockMvc;
    private BuilderQueryUseCase builderQueryUseCase;

    @BeforeEach
    void setUp() {
        GatekeeperCommandUseCase gatekeeperCommandUseCase = mock(GatekeeperCommandUseCase.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        builderQueryUseCase = mock(BuilderQueryUseCase.class);
        GatekeeperController controller = new GatekeeperController(
                gatekeeperCommandUseCase,
                clientIpResolver,
                builderQueryUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldReturnActiveBuildersForDropdown() throws Exception {
        given(builderQueryUseCase.listActiveBuilders()).willReturn(List.of(
                new BuilderSummaryDto(
                        1,
                        "pm-estimate",
                        "pm",
                        "產品經理",
                        "PM 工時估算與建議",
                        "協助 PM 針對需求做工時估算、拆解與風險說明。",
                        false,
                        null
                ),
                new BuilderSummaryDto(
                        2,
                        "qa-smoke-doc",
                        "qa",
                        "測試團隊",
                        "QA 冒煙測試文件產生",
                        "協助 QA 依需求快速產出可轉成 xlsx 的冒煙測試案例。",
                        true,
                        "xlsx"
                )
        ));

        mockMvc.perform(get("/api/builders").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].builderId").value(1))
                .andExpect(jsonPath("$.data[0].name").value("PM 工時估算與建議"))
                .andExpect(jsonPath("$.data[0].description").value("協助 PM 針對需求做工時估算、拆解與風險說明。"))
                .andExpect(jsonPath("$.data[1].includeFile").value(true))
                .andExpect(jsonPath("$.data[1].defaultOutputFormat").value("xlsx"));
    }
}
