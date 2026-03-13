# RewardBridge Backend Development Guide

## Layering
目前 Java 專案的分層如下：

```text
Controller -> UseCase -> Service -> Repository
```

規則：
- Controller 只處理 HTTP 與 `ApiResponse`
- UseCase 可以跨模組協作
- Service 只處理模組內部邏輯
- Repository 只負責資料存取

## Configuration
- `application.yml` 定義共用設定結構
- `application-local.properties` 提供 local profile 覆蓋值
- `application-test.properties` 提供測試環境覆蓋值
- `RewardBridgeProperties` 目前只管理：
  - `rewardbridge.consult`
  - `rewardbridge.ai.models.consult`

## Database Source Of Truth
目前沒有 migration SQL，schema 以 JPA entity 為準。

核心 entity：
- `BuilderConfigEntity`
- `SourceEntity`
- `RagSupplementEntity`
- `SourceTemplateEntity`
- `RagTemplateEntity`

已不再使用：
- `rb_source_type`
- `rb_source.type_id`
- `typeCode`

## Public API Conventions
- 所有 controller 回傳 `ApiResponse`
- 業務錯誤使用 `BusinessException`
- `GlobalExceptionHandler` 會統一轉成 HTTP status + `ApiResponse.error(...)`

## Consult Flow

```text
GatekeeperController
  -> GatekeeperCommandUseCase
  -> ConsultGuardService
  -> BuilderCommandUseCase
  -> BuilderCommandService
  -> AiClientCommandUseCase
  -> OutputCommandUseCase
```

### Guard Rules
目前已實作：
- `builderId` required
- builder must exist
- builder must be active
- `outputFormat` must be `markdown` or `xlsx` when provided
- file extension allowlist
- file count limit
- file size limit
- total upload size limit
- client IP must be resolvable

目前未實作：
- IP allowlist / blocklist
- MIME validation

## Builder Rules
- source 依 `order_no ASC, source_id ASC`
- rag 依 `order_no ASC, rag_id ASC`
- `systemBlock=true` 的 source 會被保留在 graph save 之外
- 非系統 source 與 rag 在 save graph 時會重編為 `1..n`
- `BuilderOverrideFactory` 目前有兩個策略，依 Spring `@Order` 優先度排列：
  1. `TemplateOverrideStrategy` (Order 0)：當 RAG 可覆蓋、有 user text、且 content 包含 `{{userText}}` 佔位符時，將佔位符替換為 user text
  2. `SimpleOverrideStrategy` (Order 1)：當 RAG 可覆蓋且有 user text 時，直接用 user text 完整覆蓋該 RAG content
- 兩個策略透過 `BuilderOverrideStrategy` 介面統一管理，factory 會依序嘗試，第一個 `supports()` 為 true 的策略生效

## Graph API Rules
- `GET /api/admin/builders/{builderId}/graph`
- `PUT /api/admin/builders/{builderId}/graph`

目前 request 主要使用 `sources[]`，但程式仍兼容舊的 `aiagent[]` 載入形狀。

saveGraph 行為：
- merge builder fields
- 保留現有 `system_block=true` source
- 刪除並重建所有 `system_block=false` source/rag
- 忽略 payload 中 `systemBlock=true` 的 source

## Template API Rules
- `GET /api/admin/builders/{builderId}/templates`
- `GET /api/admin/templates`
- `POST /api/admin/templates`
- `PUT /api/admin/templates/{templateId}`
- `DELETE /api/admin/templates/{templateId}`

目前 template 規則：
- `templateKey` 必須唯一
- `orderNo` 會被重排成 canonical order
- delete template 前，會先把 `rb_source.copied_from_template_id` 置空

## Output Rules
- `include_file=false` 時，只回文字結果
- `include_file=true` 時，使用 `outputFormat` 或 `default_output_format`
- `MarkdownRenderer` 直接輸出 `.md`
- `XlsxRenderer` 會先嘗試解析 markdown table
- 若沒有偵測到 markdown table，則輸出單純逐行內容

## Local Seed Data
`local` profile 會執行 `initData.Local`：
- upsert builder config
- upsert templates
- upsert source blocks
- 僅在不存在時補 rag supplements

因此 local seed 並不是「全部清掉重建」，而是偏同步式初始化。

## Testing
目前主要是單元測試與 controller 層測試。

測試檔案覆蓋重點：
- graph command/query
- template command/query
- builder command service
- rag query service
- xlsx renderer
- controllers

## Current Development Notes
- 若要改資料模型，先改 entity，再檢查 local/test profile 是否仍能靠 `ddl-auto=create-drop` 啟動
- 若要改 API 契約，需同時檢查 controller、DTO、exception handler 與 seed data 是否一起對齊
- 若要改 consult 結果格式，需同時檢查 Output 與前端相容性
