# Builder Module - PRD

## Overview
Builder 是系統的核心編排中心，負責接收使用者問題後，根據 `builderId` 載入對應的 Source prompt 片段與 RAG 補充資料，組裝完整的 prompt，交由 AIClient 發送至 AI 模型，最後將結果交給 Output 模組渲染。

Builder 的 prompt 組裝是**資料驅動**的：prompt 的內容、排序、結構完全由 `rb_source` + `rb_rag_supplement` 的 DB 資料決定，新增 scenario 不需要改 Builder code。

## Responsibilities
- 接收 Gatekeeper 轉發的請求（text + files + builderId + outputFormat + clientIp）
- 依 `builderId` 載入 Builder Config（含 group_label、name、include_file 等配方設定）
- 依 `builderId` 載入所有 Source prompt 片段（按 type sort_priority → order_no 排序）
- 對標記 `needs_rag_supplement=true` 的 Source，依 `sourceId` 撈取 RAG 補充資料
- 透過 **Override Factory** 處理前端覆蓋邏輯（前端 text 取代 default 語句等）
- 將所有 prompt 片段按排序拼接為完整 prompt
- 若有上傳附件，初期採用直接原樣交給模型的方式
- 將組裝完成的 prompt 交給 AIClient 發送
- 接收 AI 回應後，交給 Output 模組渲染
- 提供後台編輯器使用的 Builder Graph `save/load` 編排入口

## Orchestration Flow（編排主流程）

```
BuilderCommandUseCase.consult(builderId, text, outputFormat, files, clientIp)
│
├─ Step 1：載入 Builder Config
│   → BuilderConfigRepository.findById(builderId)
│   → 取得 name, group_label, include_file, default_output_format 等
│
├─ Step 2：載入所有 Source
│   → SourceQueryUseCase.loadByBuilderId(builderId)
│   → 回傳 List<SourceEntryDto>
│   → 已按 source_type.sort_priority → source.order_no 排序
│   → 結果範例：
│       [PINNED-1] 安全檢查規則
│       [PINNED-2] 角色設定
│       [CHECK-1]  附件處理規則
│       [CONTENT-1] 執行流程 (needs_rag=true)
│       [CONTENT-2] 回應契約 (needs_rag=true)
│
├─ Step 3：撈 RAG Supplement
│   → 篩選 needs_rag_supplement=true 的 source entries
│   → 對每個 sourceId：RagQueryUseCase.queryBySourceId(sourceId)
│   → 回傳 List<RagSupplementDto>，按 order_no 排序
│   → 每個 supplement 含 ragType, title, content, overridable
│
├─ Step 4：組裝完整 prompt
│   → BuilderCommandService.assemblePrompt(...)
│   → 依排序拼接所有 prompt 片段 + RAG 補充內容
│   → Override Factory 檢查是否有 overridable 內容需被前端 text 取代
│   → 最終產出完整 prompt 字串
│
├─ Step 5：送 AI
│   → AiClientCommandUseCase.analyze(model, text, prompt, files)
│
└─ Step 6：Output
    → OutputCommandUseCase.render(builderConfig, outputFormat, response)
    → 回傳 RenderedOutput
```

## Builder Graph Editor API

後台 prompt 編輯器的第一優先不是拖拉 UI，而是先定義一份**穩定的 graph JSON**，讓後端可以清楚知道 Builder / Source / RAG 要怎麼落庫。

官方 canonical shape 以 `sources[]` 為準。若前端早期實驗曾使用 `aiagent[] -> source` 形狀，後端可暫時相容接收，但 `GET /graph` 一律回 `sources[]`。

### Save Contract

```text
PUT /api/admin/builders/{builderId}/graph
Content-Type: application/json
```

這支 API 目前只負責更新既有 builder graph。若 `{builderId}` 不存在，後端應回 `404 BUILDER_NOT_FOUND`，不在 `PUT` 內隱式建立新 builder。

```json
{
  "builder": {
    "builderCode": "qa-smoke-doc",
    "groupLabel": "測試團隊",
    "name": "QA 冒煙測試文件產生",
    "description": "協助 QA 快速產出冒煙測試案例",
    "includeFile": true,
    "defaultOutputFormat": "xlsx",
    "filePrefix": "qa-smoke-doc",
    "active": true
  },
  "sources": [
    {
      "typeCode": "PINNED",
      "orderNo": 1,
      "prompts": "你現在負責安全檢查...",
      "rag": []
    },
    {
      "typeCode": "CONTENT",
      "orderNo": 2,
      "prompts": "請依照以下流程完成分析",
      "rag": [
        {
          "ragType": "execution_steps",
          "title": "執行流程",
          "content": "1. 先做安全檢查...",
          "orderNo": 1,
          "overridable": false,
          "retrievalMode": "full_context"
        },
        {
          "ragType": "default_content",
          "title": "預設內容",
          "content": "若前端沒給需求，請先產出 default draft",
          "orderNo": 2,
          "overridable": true,
          "retrievalMode": "full_context"
        }
      ]
    }
  ]
}
```

### Load Contract

```text
GET /api/admin/builders/{builderId}/graph
Accept: application/json
```

回傳與 save 相同結構的 graph JSON，作為前端編輯器的唯一真實資料來源。

### Save Strategy
1. 先更新既有 `rb_builder_config`
2. 再刪除該 builder 既有 RAG
3. 再刪除該 builder 既有 Source
4. 最後依 payload 順序重建 Source 與 RAG

### Field Rules
- `typeCode` 不是 prompt 內容，而是 source 的區塊分類鍵；它會決定 consult 組 prompt 時的排序層級
- `GET /graph` 回傳的 source 順序應以 consult 真正使用的 canonical 順序為準：先看 `typeCode` 對應的 sort priority，再看 `orderNo`
- `source.orderNo` 與 `rag.orderNo` 若有提供，必須是正整數；未提供時才由後端自動補不衝突的順序
- `builder` 區塊採 merge 語意：
  - 未提供欄位時，優先保留既有 builder 值
  - 只有既有值本身也沒有時，才會回落到系統預設值
- `sources[]` / `source.rag[]` 區塊採 replace 語意：
  - payload 內沒出現的 source / rag 會在重存時被刪除
  - `source.typeCode` 未提供時預設為 `CONTENT`
  - `rag.overridable` 未提供時預設為 `false`
  - `rag.retrievalMode` 未提供時預設為 `full_context`
- 空字串 `""` 目前不視為清空欄位：
  - `builder.description=""` 會保留舊值
  - `builder.filePrefix=""` 會保留舊值
  - 若要支援真正清空，需另外定義明確 payload 規則

### Why Builder Owns This
- graph JSON 雖然最終分散落在 `builder/source/rag` 三張表，但它在語意上是一個完整的「builder 配方」
- transaction、排序一致性、預設值補齊與 graph 驗證，應由 Builder 作為協調者負責
- 前端未來不論是表單、JSON editor 或拖拉畫布，都不應直接理解資料表細節

## Override Factory（覆蓋工廠）

Builder 新增 Override Factory，用於處理「前端使用者輸入取代 default prompt 語句」的邏輯。

### 設計
```
BuilderOverrideFactory
├── SimpleOverrideStrategy     ← 簡單模式：有 text 就整段替換 overridable 內容
├── TemplateOverrideStrategy   ← 模板模式：將 text 嵌入模板變數中
└── [未來可擴充]
```

### 觸發條件
- RAG supplement 的 `overridable = true`
- 前端有傳入 `text`

### 判斷流程
1. Builder 組裝 prompt 時，遇到 `overridable=true` 的 RAG supplement
2. 檢查前端是否有提供 text
3. 若有，交給 Override Factory 決定如何處理（簡單替換 / 模板合併 / 自訂邏輯）
4. 若無，使用 RAG supplement 的原始 content 作為 default

### 備註
- Override Factory 放在 `builder/service/command/override/` 目錄下
- 工廠模式讓不同 builder 可以有不同的覆蓋策略，不需要硬寫 if-else
- 初期不額外建立 `USER_TEXT_SLOT` 類型的資料表節點；若畫布需要表達「這裡可插入使用者 text」，仍沿用 `overridable=true` 的 RAG 語意

## Response Contract
Builder 要求 AI 依照固定 JSON 結構回應：

```json
{
  "status": true,
  "statusAns": "",
  "response": "放入 AI 回應內容"
}
```

### STEP1: 安全檢查
- 由 Source 的 PINNED 區域 prompt 驅動（安全檢查規則存在 Source 中）
- 只檢查傳入 `text` 是否包含 prompt injection / 規則覆寫 / 越權要求
- 圖片與附件不做注入審核
- 一般技術內容不應誤殺
- 若判定有問題：

```json
{
  "status": false,
  "statusAns": "prompts有違法注入內容",
  "response": "取消回應"
}
```

### STEP2: 正常分析
- 若 STEP1 通過，根據 Source + RAG 規則完成業務分析

### Attachment Failure Contract
若附件串入模型失敗：

```json
{
  "status": false,
  "statusAns": "串入檔案格式錯誤",
  "response": ""
}
```

- 不做 fallback 文字抽取，維持回答品質與 PM 信任

## Prompt Assembly（prompt 組裝邏輯）

### 資料驅動組裝
prompt 組裝不再硬寫 STEP1/STEP2 結構，而是依 Source 排序拼接：

```
最終 prompt 結構（依 Source 排序自動生成）：
├── [PINNED-1] 安全檢查規則（STEP1 邏輯）
├── [PINNED-2] 角色設定（你是 xxx 顧問）
├── [CHECK-1]  附件處理規則
├── [CONTENT-1] 執行流程 prompts
│   └── RAG supplement: 功能拆解步驟
│   └── RAG supplement: 預設回應格式 (overridable → 被前端 text 覆蓋)
├── [CONTENT-2] 回應契約 prompts
│   └── RAG supplement: 輸出結構規則
├── 使用者原始 text（經 Override Factory 處理後的位置）
└── Builder 自動附加的固定尾段（JSON 格式要求、執行順序等）
```

### 固定尾段
Builder 仍會附加一段固定的系統指令（如 JSON 格式要求、STEP 執行順序），這段不由 Source 管理，而是 Builder 內建的框架邏輯。

## Scope
- **In Scope**: builder config 載入、source 載入與排序、RAG supplement 撈取、Override Factory、prompt 組裝、回應編排、Output handoff
- **Out of Scope**: 驗證（Gatekeeper 負責）、RAG 檢索實作（RAG 模組負責）、AI API 呼叫實作（AIClient 負責）

## Interface
- **Input**: 驗證通過的使用者請求（text + files? + builderId + outputFormat + clientIp）
- **Output**: RenderedOutput，永遠包含文字回應，並可選擇附帶檔案 payload

## Dependencies
- BuilderConfigRepository（common 模組，載入 builder config）
- Source Module（UseCase 層，載入 prompt 片段）
- RAG Module（UseCase 層，載入補充資料）
- AIClient Module（UseCase 層，發送 prompt 至 AI 模型）
- Output Module（UseCase 層，渲染輸出格式）

## Notes
- Builder 是唯一的編排者，所有資料流都經過 Builder 協調
- Prompt 組裝改為資料驅動，不再硬寫 per-scenario 的 prompt 結構
- 新增 scenario = 新增 DB 資料（Source + RAG），不需改 Builder code
- Override Factory 提供擴充彈性，支援簡單替換與複雜模板合併
- PM 上傳的附件初期不進 DB、不進知識庫，僅作為單次 consult 對模型的輸入材料
