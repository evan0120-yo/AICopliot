# Output Module - PRD

## Overview
Output 模組負責將 Builder / AI 產出的標準化結果，轉換成使用者真正需要的輸出形式。

這個模組存在的原因是：
- 同一個 consult API 需要支援多種輸出格式
- 輸出格式判斷不能寫在 controller
- AIClient 只應負責模型通訊，不應順便承擔文件產生責任

因此，Output 是整個 internal copilot platform 的**最終渲染層**。

## Responsibilities
- 接收 Builder 傳來的標準化結果內容
- 依 `BuilderConfigEntity` 的 `include_file` 決定是否附帶檔案
- 若需要附帶檔案，依 `outputFormat` 或 `default_output_format` 選擇對應 renderer
- 支援以下輸出形式：
  - `markdown`
  - `xlsx`
- 統一管理附帶檔案的檔名、content type 與 base64 payload

## Why Separate Module
- **Controller 不應知道輸出格式細節**
- **Builder 應專注在內容編排，不應專注檔案渲染**
- **AIClient 應專注在和模型溝通，不應負責 Markdown / Excel 產生**
- **同一份內容未來可能需要用不同格式輸出**

## Scope
- **In Scope**: output renderer factory、markdown / xlsx 渲染、response metadata 組裝
- **Out of Scope**: prompt 組裝、RAG 檢索、Source 查詢、AI API 呼叫

## Input
Output 模組不直接接 HTTP request，而是接收 Builder 傳來的結果物件。

```java
OutputRenderCommand {
    BuilderConfigEntity builderConfig;  // 含 include_file, default_output_format, file_prefix
    OutputFormat outputFormat;          // 前端指定的格式（nullable）
    ConsultBusinessResponse businessResponse;
}
```

### 決策邏輯
1. 若 `builderConfig.includeFile = false` → 不附帶檔案，只回文字
2. 若 `builderConfig.includeFile = true`：
   - 若前端有傳 `outputFormat` → 用前端指定的格式
   - 若前端沒傳 → 用 `builderConfig.defaultOutputFormat`
3. 選擇對應 renderer 渲染

## Output
Output 模組回傳 `RenderedOutput`，包含：

```json
{
  "status": true,
  "statusAns": "",
  "response": "AI 回應內容",
  "file": {
    "fileName": "qa-smoke-doc-consult.xlsx",
    "contentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "base64": "UEsDB..."
  }
}
```

- Controller 永遠回 JSON
- `file` 欄位只有在 builder 設定為需要附帶檔案時才存在
- 檔名前綴來自 `builderConfig.filePrefix`

## Design Pattern

### Factory Pattern

```text
OutputScenarioPolicyResolver
  └─ 從 BuilderConfigEntity 讀取 include_file + default_output_format

OutputRendererFactory
├── MarkdownRenderer
├── XlsxRenderer
└── [未來可擴充]
```

- policy 來自 `rb_builder_config` 表（取代原本的 ConsultScenario enum）
- renderer 分流鍵為 `outputFormat`
- 新增格式時不影響既有 controller

### Optional Template Layer
若未來不同 builder 的文件長相差異很大，可再加入 template abstraction：

```text
OutputRenderer
  └─ uses BuilderTemplateProvider(builderConfig)
```

## Example Builders

### 1. PM 工時估算（builderId=1）
- `include_file = false`
- 只回純文字 JSON，不附帶下載檔案

### 2. QA 冒煙測試（builderId=2）
- `include_file = true`
- `default_output_format = "xlsx"`
- 永遠有文字回應
- 可附帶 Excel 表格或 Markdown 文件

## Module Boundaries

正式流程：
```text
Gatekeeper -> Builder -> AIClient -> Output -> HTTP response
```

## Notes
- 同一支 consult API 應可重用，不因 `outputFormat` 不同而複製多支 API
- 輸出格式判斷不能下沉到 controller
- `include_file` + `default_output_format` 來自 `rb_builder_config` 表
- `outputFormat` 決定「如果要附帶檔案，採用哪一種格式」
- 目前先正式支援 `markdown` / `xlsx`，若未來需要其他格式，再以 renderer 方式擴充
