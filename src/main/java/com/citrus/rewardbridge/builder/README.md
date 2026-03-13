# Builder Module

## Overview
Builder 是目前後端的編排中心。它接收 Gatekeeper 傳來的 consult 請求，載入 builder/source/rag 資料，組裝 prompt，呼叫 AIClient，再把結果交給 Output。

## Responsibilities
- 載入 `rb_builder_config`
- 載入 `rb_source`
- 對 `needs_rag_supplement=true` 的 source 載入 `rb_rag_supplement`
- 組裝 prompt instructions
- 套用 overridable RAG 覆蓋邏輯
- 呼叫 AIClient
- 呼叫 Output
- 提供 graph save/load API
- 提供 template list/save/update/delete API

## Consult Orchestration

```text
Gatekeeper -> BuilderCommandUseCase
  -> load builder config and sources in parallel
  -> load required RAG entries in parallel
  -> assemble prompt
  -> call AIClient
  -> call Output
```

## Prompt Assembly
目前 `BuilderCommandService` 的 prompt 組裝順序為：

1. framework header
2. `[RAW_USER_TEXT]`
3. each source by `source.orderNo`
4. each source's RAG by `rag.orderNo`
5. optional `[USER_INPUT]`
6. `[FRAMEWORK_TAIL]`

### Important Current Behavior
- `systemBlock=true` 的 source 只是資料層的區塊標記
- code 並沒有單獨把 system block 抽成另一條流程
- 最後一段安全與 JSON 回應契約仍由 `FRAMEWORK_TAIL` 保底
- 若 overridable RAG 已經吃掉 user text，就不再額外附加 `[USER_INPUT]`

## Source / RAG Ordering
- source query: `order_no ASC, source_id ASC`
- rag query: `order_no ASC, rag_id ASC`
- graph save 時，非系統 source 會重編 `1..n`
- 每個 source 內的 rag 也會重編 `1..n`

## Override Logic
目前有兩個 override strategy，依 Spring `@Order` 優先度排列：

### `TemplateOverrideStrategy` (Order 0)
- 當 `rag.overridable=true`、user text 有值、且 rag content 包含 `{{userText}}` 佔位符時
- 將 `{{userText}}` 替換為 user text，保留其餘 content

### `SimpleOverrideStrategy` (Order 1)
- 當 `rag.overridable=true` 且 user text 有值時
- 直接以 user text 完整覆蓋該 rag content

兩個策略透過 `BuilderOverrideStrategy` 介面統一管理，`BuilderOverrideFactory` 會依序嘗試，第一個 `supports()` 為 true 的策略生效。

## Graph API
- `GET /api/admin/builders/{builderId}/graph`
- `PUT /api/admin/builders/{builderId}/graph`

### Current Save Semantics
- `builder` 採 merge
- `sources[]` 採 replace，但只作用於 `systemBlock=false`
- 既有 `systemBlock=true` source 與其 RAG 會被保留
- payload 中若夾帶 `systemBlock=true` source，後端會忽略
- request 目前仍兼容舊的 `aiagent[]` 輸入形狀

## Template API
- `GET /api/admin/builders/{builderId}/templates`
- `GET /api/admin/templates`
- `POST /api/admin/templates`
- `PUT /api/admin/templates/{templateId}`
- `DELETE /api/admin/templates/{templateId}`

### Current Template Rules
- `templateKey` 必須唯一
- `orderNo` 會被重新排成 canonical order
- `groupKey=null` 表示公版 template
- 刪除 template 前，會先把已複製 source 的 `copied_from_template_id` 清空

## Current Data Model
Builder 模組直接使用：
- `rb_builder_config`
- `rb_source`
- `rb_rag_supplement`
- `rb_source_template`
- `rb_rag_template`

已不再使用：
- `typeCode`
- `rb_source_type`
- `rb_source.type_id`

## Response Contract
Builder 對 AI 的要求仍是固定 JSON：

```json
{
  "status": true,
  "statusAns": "",
  "response": "..."
}
```

之後再由 Output 視 builder 設定決定是否附帶 `file`。
