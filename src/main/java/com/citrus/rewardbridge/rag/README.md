# RAG Module - PRD

## Overview
RAG（Retrieval-Augmented Generation）模組在新架構中定位為 **Source 的補充資料提供者**。當 Source 的某個 prompt 片段標記 `needs_rag_supplement=true` 時，Builder 會透過 RAG 依 `sourceId` 撈取該片段的補充內容。

RAG 不再獨立按 scenario 運作，而是精確掛在特定 Source 片段下，提供額外的 prompt 內容、預設語句、執行步驟等補充資料。

初期採用 **Full-Context 模式**（從 DB 讀取全文直接回傳），未來可 per-source 粒度切換為向量檢索。

## Responsibilities
- 依 `sourceId` 回傳該 Source 片段的所有補充資料
- 管理補充資料的子分類（`rag_type`）、排序（`order_no`）、可覆蓋性（`overridable`）
- 管理檢索模式（`retrieval_mode`：full_context / 未來 vector_search）
- 儲存 prompt 補充內容（角色設定延伸、執行步驟、預設回應格式、欄位規則等）

## Data Model

### rb_rag_supplement

| 欄位 | 型別 | 說明 |
|------|------|------|
| rag_id | BIGINT (PK, auto) | 主鍵 |
| source_id | BIGINT (FK → rb_source) | 補充哪個 Source 片段 |
| rag_type | VARCHAR | 子分類（`"execution_steps"` / `"default_content"` / `"column_rules"` 等） |
| title | VARCHAR | 標題 |
| content | TEXT | 補充內容全文 |
| order_no | INTEGER | 在此 source 下的排序 |
| overridable | BOOLEAN | 是否可被前端使用者輸入覆蓋 |
| retrieval_mode | VARCHAR | 檢索模式：`"full_context"` / `"vector_search"`（預留） |

### 欄位說明

#### rag_type（子分類）
用途是讓同一個 Source 下的多筆補充資料有語意區分，方便 Builder Override Factory 辨識。常見值：
- `execution_steps`：執行步驟說明
- `default_content`：預設語句（可能被前端 text 覆蓋）
- `column_rules`：XLSX 欄位規則
- `structure_rule`：回應結構規則
- `response_contract`：回應格式契約

#### overridable
配合 Builder Override Factory 使用：
- `overridable=true`：Builder 組裝時若前端有傳 text，Override Factory 可決定是否用 text 取代此段內容
- `overridable=false`：此段內容固定不變，不受前端輸入影響

#### retrieval_mode
- `full_context`（初期）：直接回傳 `content` 全文
- `vector_search`（未來）：使用 embedding 去 pgvector 做 similarity search，回傳 top-K 相關段落

## 查詢邏輯

### RagQueryService.queryBySourceId(sourceId)

```sql
SELECT *
FROM rb_rag_supplement
WHERE source_id = :sourceId
ORDER BY order_no ASC
```

回傳 `List<RagSupplementDto>`，每個 dto 包含：

```java
record RagSupplementDto(
    Long ragId,
    Long sourceId,
    String ragType,
    String title,
    String content,
    Integer orderNo,
    boolean overridable,
    String retrievalMode
)
```

### 內部邏輯（依 retrieval_mode 分流）
```
if retrieval_mode == "full_context"
    → 直接回傳 content 全文（現行做法）

if retrieval_mode == "vector_search"
    → 用 embedding query 去 pgvector 做 similarity search
    → 回傳 top-K 段落
    → Builder 不需改動，照樣收到文字內容
```

## 向量檢索擴充（未來）

### 擴充方式
1. 在 `rb_rag_supplement` 新增 `embedding_query` 欄位（nullable）
2. 在 RagQueryService 內部加 `retrieval_mode` 分流判斷
3. 新增 VectorSearchReader 實作
4. 將需要向量檢索的 RAG 筆數改 `retrieval_mode = "vector_search"`

### 不需改動的部分
- **Builder 完全不用改**：它只認「拿到文字 + orderNo → 照順序組裝」
- **Source 完全不用改**：它只標記 `needs_rag_supplement`，不管 RAG 內部怎麼撈
- **Gatekeeper 完全不用改**

### 混合模式
同一個 builder 下，不同 source 的 RAG 可以各自選擇檢索策略：

```
builderId=2 (QA 冒煙測試)
├── Source: 置頂類 → needs_rag=false（直接用 prompt）
├── Source: 檢查類 → needs_rag=true, retrieval_mode=full_context
└── Source: 內文類 → needs_rag=true, retrieval_mode=vector_search ← 只有這個走向量
```

## Scope
- **In Scope**: 依 sourceId 查詢補充資料、retrieval_mode 分流、content 全文回傳
- **Out of Scope**: prompt 組裝（Builder 負責）、Source 排序（Source 負責）、覆蓋決策（Builder Override Factory 負責）

## Interface
- **Input**: sourceId，來自 Builder UseCase
- **Output**: `List<RagSupplementDto>`（已排序的補充資料清單）

## Dependencies
- PostgreSQL Database
- 未來：pgvector extension（向量檢索時）

## Design Change from Previous Architecture

| 面向 | 舊架構 | 新架構 |
|------|--------|--------|
| 定位 | 獨立文件管理模組 | Source 的補充資料提供者 |
| 查詢路由 | `group_code + type_code + document_category` | `sourceId` 精確查詢 |
| 資料表 | `rb_rag_document`（documentKey 唯一） | `rb_rag_supplement`（sourceId FK） |
| 與 Source 關係 | 透過 `rb_source_rag_mapping` 間接關聯 | 直接掛在 Source 下（FK） |
| 向量檢索切換 | 整個 scenario 一起切 | per-source 粒度可混合使用 |

## AI Role Setting（角色設定管理）

角色設定與安全規則現在存放在 **Source 的 PINNED 區域**，不再由 RAG 獨立管理。RAG 負責的是更細粒度的補充內容。

### 共通原則（仍適用）
- AI 以對應使用者看得懂的語言回答
- 不主動暴露工程細節
- 安全檢查（STEP1）規則由 Source PINNED 區域驅動

## Notes
- RAG 不再獨立按 scenario 運作，改為精確掛在 Source 下
- `overridable` flag 讓 Builder Override Factory 可識別哪些內容可被前端覆蓋
- `retrieval_mode` 預留向量檢索擴充，初期全部為 `full_context`
- PM 單次 consult 上傳的文件 / 圖片**不進 RAG**，直接交由模型處理
