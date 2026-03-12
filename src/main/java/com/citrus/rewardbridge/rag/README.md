# RAG Module - PRD

## Overview
RAG 模組是 Source block 的補充資料提供者。它不負責 Source 排序，也不再假設 Source 有任何 `type` 概念。

Builder 使用方式固定：

1. 先拿到 Source
2. 先使用 `source.prompts`
3. 再把此 Source 底下的 RAG 依 `rag.orderNo` 接上去

這個規則同樣適用於 `systemBlock=true` 的系統安全區塊。

## Responsibilities
- 依 `sourceId` 回傳該 Source 的所有補充資料
- 維護 `rag.orderNo`
- 維護 `overridable`
- 維護 `retrieval_mode`
- 接受 graph payload 中的 rag 定義並落庫
- 支援系統安全區塊底下的唯讀安全補充內容

## Data Model

### rb_rag_supplement

| 欄位 | 型別 | 說明 |
|------|------|------|
| rag_id | BIGINT (PK, auto) | 主鍵 |
| source_id | BIGINT (FK → rb_source) | 掛在哪個 Source |
| rag_type | VARCHAR | 補充資料子分類 |
| title | VARCHAR | 顯示標題 |
| content | TEXT | 補充 prompts |
| order_no | INTEGER | 在此 source 下的順序 |
| overridable | BOOLEAN | 是否可被使用者 text 覆蓋 |
| retrieval_mode | VARCHAR | `full_context` / `vector_search` |

## Ordering Rules
- `rag.orderNo` 只在同一個 source 內比較
- 同一個 source 內不可重複
- save graph 時若送跳號，後端正規化成 `1..n`

## Prompt Assembly Position

對單一 Source：

```text
source.prompts
rag(orderNo=1).content
rag(orderNo=2).content
rag(orderNo=3).content
```

RAG 永遠接在自己的 Source 後面，不跨 Source 移動。

## Graph JSON Shape

```json
{
  "ragType": "default_content",
  "title": "預設內容",
  "content": "若前端沒給需求，請先產出 default draft",
  "orderNo": 2,
  "overridable": true,
  "retrievalMode": "full_context"
}
```

## 與 systemBlock 的關係
- `systemBlock=true` 的 Source 可以掛 RAG
- 系統安全區塊底下的 RAG 主要用來補充安全檢查、停止規則與放行準則
- 目前 graph UI 會把這些 RAG 顯示為唯讀內容
- 未來若有權限系統，可考慮只開放 admin 編輯系統區塊底下的 RAG

## retrieval_mode
- `full_context`：直接回傳全文
- `vector_search`：未來再擴充

### 相容規則
- 新資料只接受 `full_context`
- 若舊資料殘留 `vector_search`，查詢時 fallback 成 `full_context`

## Notes
- RAG 與 `typeCode` 無關
- RAG 不參與全域 prompt block 排序
- 若未來要做 tag / metadata，應掛在 Source 或 Template，不應讓 RAG 承擔 block 分類職責
