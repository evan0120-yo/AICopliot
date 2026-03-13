# Output Module

## Overview
Output 模組負責把 AI 回傳的業務結果轉成最終對前端輸出的形狀。它同時處理純文字回應與附帶檔案的情境。

## Responsibilities
- 根據 builder 設定判斷是否需要附帶檔案
- 根據 `outputFormat` 選擇 renderer
- 產生 `ConsultFilePayload`
- 將檔案內容轉成 base64

## Current Flow

```text
ConsultBusinessResponse
  -> OutputScenarioPolicyResolver
  -> OutputRendererFactory
  -> MarkdownRenderer or XlsxRenderer
  -> RenderedOutput
```

## Policy Rules
- 若 `businessResponse.status=false`，直接回原始文字結果，不附帶檔案
- 若 `builder.include_file=false`，直接回原始文字結果
- 若 `builder.include_file=true`：
  - 先用 request `outputFormat`
  - 若未提供，再用 `builder.default_output_format`

## Supported Formats
- `markdown`
- `xlsx`

## HTTP Shape
controller 最終回傳的是：

```json
{
  "success": true,
  "data": {
    "status": true,
    "statusAns": "",
    "response": "AI response text",
    "file": {
      "fileName": "qa-functional-doc-consult.xlsx",
      "contentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "base64": "..."
    }
  },
  "error": null
}
```

## Renderer Details

### MarkdownRenderer
- 直接把 `response` 內容輸出成 `.md`

### XlsxRenderer
- 先嘗試從 `response` 解析 markdown table
- 若成功：
  - 建立 `cases` sheet
  - 視需要建立 `summary` sheet
- 若失敗：
  - 建立 `consult` sheet
  - 逐行輸出文字

## File Naming
- 優先使用 `builder.file_prefix`
- 若無 `file_prefix`，fallback 為 `builder-{builderId}`
- 檔名格式：`{prefix}-consult.{ext}`

## Notes
- Output 不負責 prompt 組裝
- Output 不直接接 HTTP request
- `include_file` 與 `default_output_format` 來自 `rb_builder_config`
