package com.citrus.rewardbridge.builder.service.command;

import com.citrus.rewardbridge.builder.dto.BuilderConsultCommand;
import com.citrus.rewardbridge.output.dto.OutputFormat;
import com.citrus.rewardbridge.rag.dto.RagDocumentDto;
import com.citrus.rewardbridge.source.dto.SourceReferenceItemDto;
import com.citrus.rewardbridge.source.dto.SourceResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BuilderCommandServiceTest {

    private final BuilderCommandService service = new BuilderCommandService();

    @Test
    void buildAnalysisInstructionsUsesScenarioFallbackWhenTextIsBlank() {
        String instructions = service.buildAnalysisInstructions(
                new BuilderConsultCommand("   ", 2, 2, OutputFormat.XLSX, List.of(), "127.0.0.1"),
                new SourceResult(
                        2,
                        2,
                        "生成冒煙測試用例",
                        List.of("qa-smoke-response-contract"),
                        List.of(new SourceReferenceItemDto("覆蓋焦點", "核心流程", "優先覆蓋主流程"))
                ),
                List.of(new RagDocumentDto("qa-smoke-safety", "QA Smoke Safety", "先檢查惡意注入")),
                List.of(new RagDocumentDto("qa-smoke-role", "QA Smoke Role", "請輸出用例表"))
        );

        assertTrue(instructions.contains("== 用戶額外需求區開始 =="));
        assertTrue(instructions.contains("用戶沒有額外需求"));
        assertTrue(instructions.contains("若 text 為空且沒有附件"));
        assertTrue(instructions.contains("通用型冒煙測試初版"));
        assertTrue(instructions.contains("STEP1"));
        assertTrue(instructions.contains("STEP2"));
    }
}
