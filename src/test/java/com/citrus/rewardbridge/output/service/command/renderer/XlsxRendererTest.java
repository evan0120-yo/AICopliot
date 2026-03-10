package com.citrus.rewardbridge.output.service.command.renderer;

import com.citrus.rewardbridge.common.dto.ConsultBusinessResponse;
import com.citrus.rewardbridge.output.dto.OutputFormat;
import com.citrus.rewardbridge.output.dto.OutputRenderCommand;
import com.citrus.rewardbridge.output.dto.RenderedFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XlsxRendererTest {

    private final XlsxRenderer renderer = new XlsxRenderer();

    @Test
    void renderParsesMarkdownTableIntoStructuredSheet() throws Exception {
        String response = """
                冒煙測試摘要
                - 覆蓋首頁入口與 Rewards 主流程
                - 補充重複點擊與跨頁返回場景

                | 用例編號 | 需求 | 功能域 | 模塊細分 | 二級模塊細分 | 用例名稱 | 測試類型 | 前提條件 | 操作步驟 | 期望結果 | 用例級別 | 研发自测结果 |
                | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
                | TC-001 | 會員二期 | APP | 首頁 | 浮動按鈕 | 點擊首頁浮動按鈕入口 | 功能測試 | 已登入 | 1、點擊首頁浮動按鈕入口 | 1、跳轉 Rewards 積分牆頁面 | S |  |
                | TC-002 | 會員二期 | APP | 積分牆 | 簽到區 | 重複點擊簽到按鈕 | 功能測試 | 已登入 | 1、點擊按鈕多次簽到 | 1、僅觸發一次領取，coin 不會重複發送 | S |  |
                """;

        RenderedFile file = renderer.render(new OutputRenderCommand(
                2,
                2,
                OutputFormat.XLSX,
                new ConsultBusinessResponse(true, "", response, null)
        ));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.fileBytes()))) {
            assertEquals("cases", workbook.getSheetAt(0).getSheetName());
            assertEquals("summary", workbook.getSheetAt(1).getSheetName());
            assertEquals("用例編號", workbook.getSheet("cases").getRow(1).getCell(0).getStringCellValue());
            assertEquals("TC-001", workbook.getSheet("cases").getRow(2).getCell(0).getStringCellValue());
            assertEquals("點擊首頁浮動按鈕入口", workbook.getSheet("cases").getRow(2).getCell(5).getStringCellValue());
            assertEquals("冒煙測試摘要", workbook.getSheet("summary").getRow(1).getCell(0).getStringCellValue());
        }
    }
}
