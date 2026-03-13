# Source Module

## Overview
Source 模組管理 builder 內實際存在的 prompt blocks。每筆 source 對應一段 `prompts`，並可選擇性掛多筆 RAG。

## Current Data Model

### `rb_source`
- `source_id`
- `builder_id`
- `prompts`
- `order_no`
- `system_block`
- `needs_rag_supplement`
- `copied_from_template_id`

## Responsibilities
- 依 `builderId` 載入 source blocks
- 維護 source 順序
- 區分 `systemBlock` 與一般 source
- 保存 source 與 template 的來源關聯

## Query Rule
目前查詢規則為：

```sql
SELECT *
FROM rb_source
WHERE builder_id = :builderId
ORDER BY order_no ASC, source_id ASC
```

程式中會額外 `left join fetch copiedFromTemplate`，讓 graph query 可以帶出 template metadata。

## DTO Shape
consult 流程使用的 `SourceEntryDto` 目前包含：
- `sourceId`
- `prompts`
- `orderNo`
- `systemBlock`
- `needsRagSupplement`

## Save Rule In Graph API
- 保留既有 `systemBlock=true` source
- 只刪除並重建 `systemBlock=false` source
- 非系統 source 的 `orderNo` 會被重編為 `1..n`
- `needs_rag_supplement` 由 graph payload 內的 rag 是否為空決定

## Template Link
`copied_from_template_id` 的用途：
- graph load 時帶出 template metadata
- template delete 時可清理既有 source 與 template 的關聯

## Notes
- Source 已不再有 `typeCode`
- Source 已不再依賴 `rb_source_type`
- Source 排序依 `order_no ASC, source_id ASC`
