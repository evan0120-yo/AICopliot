# Source Module - PRD

## Overview
Source 模組是 **prompt 組裝的主體**，管理所有 prompt 片段的內容、分類與排序。Builder 依 `builderId` 從 Source 載入所有 prompt 片段，按區域分類（type）與排序（orderNo）拼接成完整 prompt。

Source 取代了原本的三張表（scenario config + reference items + rag mapping），統一為一張 `rb_source` 表 + 一張可配置的 `rb_source_type` 分類表。

## Responsibilities
- 依 `builderId` 回傳該 builder 的所有 prompt 片段
- 管理 prompt 片段的區域分類（PINNED / CHECK / CONTENT 等）
- 管理 prompt 片段的排序（type sort_priority + source order_no）
- 標記每個 prompt 片段是否需要 RAG 補充（`needs_rag_supplement`）
- 管理區域分類的定義（`rb_source_type`，可配置新增）
- 接受 Builder Graph payload 中的 source 定義，並轉成 `rb_source` 可落庫的結構

## Data Model

### rb_source_type（區域分類定義表）

| 欄位 | 型別 | 說明 |
|------|------|------|
| type_id | INTEGER (PK) | 主鍵 |
| type_code | VARCHAR (UNIQUE) | 代碼，例如 `"PINNED"`、`"CHECK"`、`"CONTENT"` |
| type_name | VARCHAR | 顯示名稱 |
| description | VARCHAR | 說明 |
| sort_priority | INTEGER | 類別排序，越小越前 |

初期預設值：

| type_code | type_name | sort_priority | 用途 |
|-----------|-----------|---------------|------|
| PINNED | 置頂類 | 1 | 安全規則、角色設定等永遠置頂的 prompt |
| CHECK | 檢查類 | 2 | 附件處理規則、格式驗證等檢查項 |
| CONTENT | 內文類 | 3 | 主要業務邏輯 prompt（執行流程、回應格式等） |

未來可自由新增，例如：
- `FOOTER`（尾段類）→ sort_priority=4
- `EXAMPLE`（範例類）→ sort_priority=5

### rb_source（Prompt 片段主表）

| 欄位 | 型別 | 說明 |
|------|------|------|
| source_id | BIGINT (PK, auto) | 主鍵 |
| builder_id | INTEGER (FK → rb_builder_config) | 屬於哪個 builder |
| type_id | INTEGER (FK → rb_source_type) | 區域分類 |
| prompts | TEXT | prompt 內容 |
| order_no | INTEGER | 同 type 內排序 |
| needs_rag_supplement | BOOLEAN | 是否需要 RAG 補充 |

### 後台編輯器對應欄位

後台 graph JSON 中，每個 source 建議長這樣：

```json
{
  "typeCode": "CONTENT",
  "orderNo": 2,
  "prompts": "請依照以下流程完成分析",
  "rag": [
    {
      "ragType": "execution_steps",
      "title": "執行流程",
      "content": "...",
      "orderNo": 1,
      "overridable": false,
      "retrievalMode": "full_context"
    }
  ]
}
```

Mapping 規則：
- `typeCode` → `rb_source_type.type_code`
- `orderNo` → `rb_source.order_no`
- `prompts` → `rb_source.prompts`
- `rag.length > 0` → `rb_source.needs_rag_supplement = true`
- `rag.length = 0` → `rb_source.needs_rag_supplement = false`

`orderNo` 規則：
- `source.orderNo` 若有傳值，必須是正整數
- `rag.orderNo` 若有傳值，必須是正整數
- 若未傳值，後端才會依同 type / 同 source 內已使用號碼自動補位

`typeCode` 的作用不是顯示用標籤，而是決定 source 屬於哪一個 prompt 區塊，並參與最終排序：
- `PINNED`：固定最前面的規則，例如安全規則、角色設定
- `CHECK`：檢查類規則，例如附件處理與格式限制
- `CONTENT`：主要業務內容

因此 source 的最終 canonical 排序不是只看 `orderNo`，而是：
1. 先看 `typeCode` 對應的 `sort_priority`
2. 再看同 type 內的 `orderNo`

若前端送的是精簡 payload，後端可補預設值：
- `typeCode` 未傳 → `CONTENT`
- `rag` 未傳 → `[]`

## 查詢邏輯

### SourceQueryService.loadByBuilderId(builderId)

```sql
SELECT s.*, st.type_code, st.sort_priority
FROM rb_source s
JOIN rb_source_type st ON s.type_id = st.type_id
WHERE s.builder_id = :builderId
ORDER BY st.sort_priority ASC, s.order_no ASC
```

回傳 `List<SourceEntryDto>`，每個 entry 包含：

```java
record SourceEntryDto(
    Long sourceId,
    String typeCode,
    String prompts,
    Integer orderNo,
    boolean needsRagSupplement
)
```

### 排序規則
1. 先按 `source_type.sort_priority` 排（PINNED=1 → CHECK=2 → CONTENT=3）
2. 再按 `source.order_no` 排（同 type 內的順序）

### 範例：builderId=1（PM 工時估算）的 Source 排列

```
[PINNED, order=1] 安全檢查規則（needs_rag=false）
[PINNED, order=2] 角色設定（needs_rag=false）
[CHECK,  order=1] 附件處理規則（needs_rag=false）
[CONTENT,order=1] 執行流程 prompts（needs_rag=true）
[CONTENT,order=2] 回應契約 prompts（needs_rag=true）
[CONTENT,order=3] 功能拆解參考資料（needs_rag=true）
```

### 範例：builderId=2（QA 冒煙測試）的 Source 排列

```
[PINNED, order=1] 安全檢查規則（needs_rag=false）
[PINNED, order=2] 角色設定（needs_rag=false）
[CHECK,  order=1] 附件處理規則（needs_rag=false）
[CONTENT,order=1] 執行流程 prompts（needs_rag=true）
[CONTENT,order=2] 回應契約 prompts（needs_rag=true）
[CONTENT,order=3] 結構規則 prompts（needs_rag=true）
[CONTENT,order=4] XLSX 欄位規則（needs_rag=true）
```

## Scope
- **In Scope**: prompt 片段管理、區域分類管理、排序查詢、needs_rag_supplement 標記
- **Out of Scope**: RAG 補充資料查詢（RAG 模組負責）、prompt 拼接組裝（Builder 負責）

## Interface
- **Input**: builderId，來自 Builder UseCase
- **Output**: `List<SourceEntryDto>`（已排序的 prompt 片段清單）

## Dependencies
- PostgreSQL Database

## Design Change from Previous Architecture

| 面向 | 舊架構 | 新架構 |
|------|--------|--------|
| 資料表 | 三張表（scenario config + reference items + rag mapping） | 一張主表 `rb_source` + 一張分類表 `rb_source_type` |
| 查詢路由 | `group_code + type_code` + Strategy Pattern | `builderId` 單一查詢，不需 Strategy |
| 擴充方式 | 每新增 scenario 需寫一個 Strategy class | 只需新增 DB 資料 |
| 與 RAG 的關係 | 透過 `rb_source_rag_mapping` 間接關聯 | 透過 `needs_rag_supplement` flag，RAG 直接掛在 Source 下 |

## Notes
- Source 不再需要 Strategy Pattern，所有 builder 用同一套查詢邏輯
- 新增 scenario 只需在 DB 新增 Source 資料，不需改 code
- Source 提供的 prompt 是給 AI 消化用的，AI 會轉譯為使用者看得懂的語言回覆
- 區域分類（rb_source_type）存 DB，可自由配置新增，不是寫死的 enum
- 後台畫布中的每個大區塊，本質上都是一筆 Source；drag-and-drop 只是編輯方式，不是新的後端資料模型
