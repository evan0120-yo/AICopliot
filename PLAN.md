# RewardBridge Backend Snapshot

## Overview
目前 `Java/` 目錄下的 Backend 是一個可運作的 Spring Boot 4.0.3 專案，負責接收 consult 請求、組裝 prompt、呼叫 OpenAI Responses API，並依 builder 設定回傳純文字或附帶檔案的 JSON。

主流程：

```text
HTTP Controller
  -> Gatekeeper UseCase
  -> Builder UseCase
  -> AIClient UseCase
  -> Output UseCase
  -> ApiResponse JSON
```

## Runtime
- Java 21
- Spring Boot 4.0.3
- Spring Web MVC
- Spring Data JPA
- PostgreSQL runtime driver
- OpenAI Java Spring Boot Starter
- Apache POI for XLSX rendering

## Current Modules

### Gatekeeper
- `GET /api/builders`
- `POST /api/consult`
- 驗證 `builderId`
- 驗證 `outputFormat`
- 驗證附件副檔名、檔案數量、單檔大小、總大小
- 從 `X-Forwarded-For` / `X-Real-IP` / remote address 解析 client IP

### Builder
- 載入 builder config
- 載入 source blocks
- 對 `needs_rag_supplement=true` 的 source 載入 RAG
- 組裝 prompt instructions
- 套用 overridable RAG override
- 呼叫 AIClient
- 呼叫 Output
- 提供 builder graph save/load API
- 提供 template list/save/update/delete API

### Source
- 管理 builder 內的 prompt blocks
- `rb_source.order_no` 為 block 順序
- `system_block` 標記系統安全區塊
- `copied_from_template_id` 保留 template 來源

### RAG
- 管理 `rb_rag_supplement`
- 依 `source_id` 與 `order_no` 提供補充 prompt
- 讀取時會把不支援的 `retrieval_mode` fallback 成 `full_context`

### AIClient
- 使用 OpenAI Responses API
- 支援 text + file + image inputs
- 附件先上傳到 OpenAI files API，再組成 response input
- 以 structured output 解析成 `AiConsultResponse`

### Output
- 根據 `builder.include_file` 決定是否附帶檔案
- 支援 `markdown` 與 `xlsx`
- 回傳 `ConsultBusinessResponse`
- 若附帶檔案，內容會轉成 base64 放入 JSON

## Current Database Model
此專案目前沒有 migration SQL；以 JPA entity 為準，實際使用的核心表只有以下 5 張：

### `rb_builder_config`
- `builder_id`
- `builder_code`
- `group_key`
- `group_label`
- `name`
- `description`
- `include_file`
- `default_output_format`
- `file_prefix`
- `active`

### `rb_source`
- `source_id`
- `builder_id`
- `prompts`
- `order_no`
- `system_block`
- `needs_rag_supplement`
- `copied_from_template_id`

### `rb_rag_supplement`
- `rag_id`
- `source_id`
- `rag_type`
- `title`
- `content`
- `order_no`
- `overridable`
- `retrieval_mode`

### `rb_source_template`
- `template_id`
- `template_key`
- `name`
- `description`
- `group_key`
- `order_no`
- `prompts`
- `active`

### `rb_rag_template`
- `template_rag_id`
- `template_id`
- `rag_type`
- `title`
- `content`
- `order_no`
- `overridable`
- `retrieval_mode`

## Current API Surface

### Public
- `GET /api/builders`
- `POST /api/consult`

### Admin
- `GET /api/admin/builders/{builderId}/graph`
- `PUT /api/admin/builders/{builderId}/graph`
- `GET /api/admin/builders/{builderId}/templates`
- `GET /api/admin/templates`
- `POST /api/admin/templates`
- `PUT /api/admin/templates/{templateId}`
- `DELETE /api/admin/templates/{templateId}`

所有 controller 都回傳：

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

失敗時由 `GlobalExceptionHandler` 回：

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "..."
  }
}
```

## Current Consult Contract
`POST /api/consult` 使用 `multipart/form-data`。

欄位：
- `builderId` required
- `text` optional
- `outputFormat` optional
- `files` optional, multiple

Builder / AI / Output 完成後，HTTP response 的 `data` 會是：

```json
{
  "status": true,
  "statusAns": "",
  "response": "AI response text",
  "file": {
    "fileName": "qa-smoke-doc-consult.xlsx",
    "contentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "base64": "..."
  }
}
```

`file` 只有在 builder 需要附帶檔案時才存在。

## Prompt Assembly Rules
目前 Builder 實作的 prompt 組裝順序為：

1. framework header
2. raw user text section
3. sources by `orderNo`
4. each source's RAG by `orderNo`
5. optional `[USER_INPUT]` section when no overridable RAG already consumed user text
6. fixed framework tail

其中 `framework tail` 仍保留最終 JSON 契約與安全檢查說明。

## Concurrency
`BuilderCommandUseCase` 目前會：
- 並行載入 builder config 與 sources
- 並行載入多個 source 的 RAG

執行器來自 `Executors.newVirtualThreadPerTaskExecutor()`

## Local/Test Environment
- `application-local.properties` 使用 PostgreSQL
- `application-test.properties` 使用 H2 in-memory
- local/test 都是 `spring.jpa.hibernate.ddl-auto=create-drop`
- local profile 會執行 `initData.Local` 自動灌 seed data

## Current Known Gaps
- 尚未實作 IP allowlist / blocklist
- 尚未做 MIME validation
- `vector_search` 尚未實作，讀取時只做 fallback
- 專案尚未提供正式 migration 腳本
