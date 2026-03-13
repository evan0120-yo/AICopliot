# RAG Module

## Overview
RAG 模組提供 source block 的補充 prompt。它不參與全域 source 排序，只負責 source 內部的補充內容順序。

## Current Data Model

### `rb_rag_supplement`
- `rag_id`
- `source_id`
- `rag_type`
- `title`
- `content`
- `order_no`
- `overridable`
- `retrieval_mode`

## Responsibilities
- 依 `sourceId` 查詢 RAG supplements
- 依 `orderNo` 排序回傳
- 提供 `overridable` 與 `retrievalMode` 給 Builder 使用
- 在 graph save 時接收並儲存 rag 定義

## Query Rule

```sql
SELECT *
FROM rb_rag_supplement
WHERE source_id = :sourceId
ORDER BY order_no ASC, rag_id ASC
```

## Retrieval Mode
目前正式支援的 mode 只有：
- `full_context`

目前程式行為：
- 寫入 graph/template 時若不是 `full_context`，直接拒絕
- 讀取既有資料時若 mode 非法，fallback 為 `full_context`

## Override Behavior
`overridable=true` 的 RAG 會交由 Builder override factory 處理。

目前唯一策略是：
- user text 存在時，直接以 user text 覆蓋 supplement content

## Notes
- RAG 不參與全域 prompt block 排序
- RAG 與 `typeCode` 無關
- `vector_search` 目前尚未實作
