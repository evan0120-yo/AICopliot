package com.citrus.rewardbridge.builder.service.command;

import com.citrus.rewardbridge.builder.dto.PromptAssemblyResult;
import com.citrus.rewardbridge.builder.service.command.override.BuilderOverrideFactory;
import com.citrus.rewardbridge.builder.service.command.override.SimpleOverrideStrategy;
import com.citrus.rewardbridge.builder.service.command.override.TemplateOverrideStrategy;
import com.citrus.rewardbridge.common.entity.BuilderConfigEntity;
import com.citrus.rewardbridge.rag.dto.RagSupplementDto;
import com.citrus.rewardbridge.source.dto.SourceEntryDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BuilderCommandServiceTest {

    private final BuilderCommandService service = new BuilderCommandService(
            new BuilderOverrideFactory(List.of(new TemplateOverrideStrategy(), new SimpleOverrideStrategy()))
    );

    @Test
    void assemblePromptUsesOverrideContentWhenUserTextExists() {
        PromptAssemblyResult promptAssemblyResult = service.assemblePrompt(
                new BuilderConfigEntity(2, "qa-smoke-doc", "qa", "測試團隊", "QA 冒煙測試", "生成冒煙測試用例", true, "xlsx", "qa-smoke-doc", true),
                List.of(new SourceEntryDto(10L, "請依照以下規則產出案例", 1, false, true)),
                Map.of(10L, List.of(
                        new RagSupplementDto(20L, 10L, "default_content", "Default", "這是預設內容", 1, true, "full_context"),
                        new RagSupplementDto(21L, 10L, "column_rules", "Columns", "請輸出 markdown table", 2, false, "full_context")
                )),
                "請根據會員二期需求產出冒煙測試"
        );

        String prompt = promptAssemblyResult.instructions();
        assertTrue(prompt.contains("請根據會員二期需求產出冒煙測試"));
        assertTrue(prompt.contains("請輸出 markdown table"));
        assertTrue(prompt.contains("[RAW_USER_TEXT]"));
        assertTrue(prompt.contains("FRAMEWORK_TAIL"));
        assertTrue(promptAssemblyResult.userMessageText().contains("請依 instructions 執行本次 consult"));
    }
}
