# Source Module - PRD

## Overview
Source 模組負責管理與提供結構化資料，作為 Builder 取得系統資料的統一介面。根據 `group_code + type_code` 回傳對應的 scenario baseline，並透過 mapping table 指定需要額外從 RAG 取得的文件。

## Responsibilities
- 根據 `group_code + type_code` 回傳對應的結構化資料
- 管理 scenario summary / reference items / Source→RAG mapping
- 透過 mapping table 告知 Builder 需要額外取得哪些 RAG 文件
- 管理活動相關的結構化資料（活動設定、玩法參數、歷史工時紀錄等）
- 管理開發相關的結構化資料（工時紀錄、功能模組清單等）

## Data Structure
每個 scenario 對應的回傳資料包含：
```
{
  "group": 1,
  "type": 1,
  "summary": "工時估算及建議",
  "referenceItems": [
    {
      "itemName": "需求拆解與確認",
      "referenceContent": "...",
      "suggestion": "..."
    }
  ],
  "ragKeys": ["pm-estimate-response-contract", "pm-estimate-feature-breakdown"]
}
```

目前資料表切分為：
- `rb_source_scenario_config`
  - 每個 scenario 一筆 summary
- `rb_source_reference_item`
  - 每個 scenario 多筆結構化參考項，含 `sort_order`
- `rb_source_rag_mapping`
  - 每個 scenario 對多份 RAG 文件的 mapping，含 `sort_order`

## Scope
- **In Scope**: 結構化資料查詢、scenario summary、reference items、RAG mapping、資料 CRUD
- **Out of Scope**: 非結構化文件檢索（RAG 負責）、prompt 組裝（Builder 負責）

## Interface
- **Input**: group_code + type_code，來自 Builder
- **Output**: 結構化資料 + ragKeys[]

## Dependencies
- PostgreSQL Database

## Design Pattern: Strategy
採用 Strategy Pattern 而非 Factory Pattern：
```
Source
├── SourceStrategy (interface)
│     ├ supports(group, type)
│     └ query(group, type) → SourceResult
├── DevEstimateStrategy    (group=1, type=1)
├── QaSmokeDocStrategy     (group=2, type=2)
├── [未來新增 Strategy]     (group=?, type=?, ...)
└── SourceService          (根據 scenario 分派對應 strategy)
```
- 每新增一個團隊任務，只需新增一個 Strategy class
- SourceService 統一對外，Builder 不需知道內部有幾種 strategy

## Notes
- 與 RAG 的區分：RAG 處理非結構化文件（SA 文件、開發文件），Source 處理結構化資料（DB records、設定檔）
- ragKeys 欄位不是寫死在 config csv，而是來自 `rb_source_rag_mapping`，讓 Source 可以精確指定需要哪些 RAG 文件來補充上下文
- Source 提供的資料是給 AI 消化用的，AI 會轉譯為產品語言回覆 PM，PM 不會直接看到 Source 的原始資料
- 目前正式支援：
  - `group=1`, `type=1`：產品經理 / 工時估算
  - `group=2`, `type=2`：測試團隊 / 生成冒煙測試
