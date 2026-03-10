# RewardBridge - Project Plan

## 1. Project Motivation

### 背景問題
在公司內部，許多同事其實有明確的工作需求，但**不太會自己正確使用 AI**。常見情況不是「完全不知道要問什麼」，而是：
- 不知道要怎麼下 prompt 才能拿到穩定結果
- 不知道哪些附件可以一起丟給模型
- 不知道不同任務應該用什麼輸出格式
- 即使能用 ChatGPT，也很難把公司內部規則、歷史文件、團隊慣例一起帶進去

以 Rewards / 活動需求為例，原本就存在這類溝通成本：
- 現有 Rewards 系統採用的是**拋棄式架構**，每個活動都可能是一次性實作
- PM 很難用傳統固定模板理解「可以開什麼活動」
- 每次都需要工程師重複解釋架構差異、可行性與工時

### 解決方案
建立一個 **公司內部 AI Copilot 後台系統**，部署在內網，讓不同部門的同事都能透過同一個入口向 AI 發問與產生工作產物。

這個系統的核心不是「做一個通用聊天頁」，而是：
- 讓同事透過固定欄位與固定流程使用 AI
- 讓 AI 自動帶入對應部門 / 功能的規則、知識、提示詞、附件策略
- 讓同一支 API 能依任務需求輸出不同格式
- 把 prompt engineering、知識維護、輸出樣式控制，集中在後端系統內處理

典型使用情境包含：
- PM 詢問活動需求的工時估算、風險、可行性分析
- 測試人員快速生成冒煙測試文件
- 未來可延伸到營運、客服、RD、主管等內部場景

因此本專案的定位，應定義為：

> **Internal AI Copilot Platform**
>
> 一個以 `builderId + outputFormat` 為擴充核心的內部 AI 任務平台。

### 擴充模型
- **builderId**：前端傳入的路由 key，代表一組「prompt 組裝配方」
  - 每個 builderId 對應一套完整的 prompt 片段組合（Source entries + RAG supplements）
  - 例如：`builderId=1` = PM 工時估算、`builderId=2` = QA 冒煙測試
- **group**：歸屬標籤，標示這個 builder 屬於哪個團隊 / 角色（僅供顯示，不參與資料查詢）
  - 例如：`產品經理`、`測試團隊`
- **outputFormat**：希望輸出的結果形式
  - 例如：`markdown`、`xlsx`
  - 僅在 builder 設定為「需要附帶檔案」時生效

這代表未來要新增能力時，目標流程不是重寫整條主流程，而是：
- 在 `rb_builder_config` 新增一筆配置
- 在 `rb_source` 新增對應的 prompt 片段（按區域分類 + 排序）
- 在 `rb_rag_supplement` 新增需要的補充資料
- Output 模組補對應輸出 renderer 或模板（如需要新格式）

### 個人學習目標
本專案同時作為 **AI Engineer** 技能的練習項目，涵蓋業界對後端工程師新增要求的核心能力：

| Step | 技能 | 對應本專案 |
|------|------|-----------|
| Step 1 | Prompt 組裝 + API Orchestration | Builder 模組：依 Source 排序組裝 prompt、工廠模式處理覆蓋邏輯 |
| Step 2 | RAG / Memory Retrieval | RAG 模組：文件補充資料、未來可擴充向量檢索 |
| Step 3 | Agent / Tool Calling | Builder 編排 Source + RAG + AIClient 的資料驅動 agent 流程 |

> **目標做到 Step 3**。Step 4（Local LLM / self-hosted model）可額外嘗試但非主要目標。

---

## 2. Architecture Overview

```
Internal User (PM / QA / Ops / ...)
  │  [text + files[] + builderId + outputFormat]
  ▼
Gatekeeper（驗證層）
  │  驗證 builderId 是否存在且啟用
  │  驗證檔案限制
  │  解析 clientIp
  ▼
Builder（編排中心）
  │
  ├─ Step 1：載入 Builder Config（by builderId）
  │
  ├─ Step 2：載入所有 Source（by builderId）
  │   │  按 source_type.sort_priority → source.order_no 排序
  │   │
  │   │  對每個 source：
  │   ├─ needs_rag_supplement = false → 直接用 source.prompts
  │   └─ needs_rag_supplement = true  → 用 sourceId 撈 RAG supplement
  │       └─ RAG supplement 按 order_no 排序
  │
  ├─ Step 3：組裝完整 prompt
  │   └─ Builder Override Factory 處理前端覆蓋邏輯
  │
  ├─ Step 4：送 AIClient
  │   └─ AIClient.send(prompt + attachments)
  │
  └─ Step 5：Output
      └─ 依 builder config 決定是否附帶檔案
      └─ 回傳統一 JSON 給使用者
```

### 設計原則
- **Builder 是唯一的編排者**：所有資料流都經過 Builder 協調，其他模組不互相呼叫
- **Gatekeeper 單一職責**：只做驗證，不參與業務邏輯
- **Source 是 prompt 的主體**：prompt 的組成內容與排序由 Source 資料驅動，不硬寫在程式碼中
- **RAG 是 Source 的補充**：RAG 不再獨立運作，而是作為 Source 需要額外補充時的資料來源
- **工廠模式用於抽象替換**：AIClient（AI 供應商可換）、Output（renderer 可換）、Builder Override（覆蓋策略可換）
- **輸出渲染與內容生成分離**：AI 先產出標準化內容，再由 Output 模組決定如何渲染成文字或檔案

### Current MVP vs Target Architecture
- **目前已落地 builder**：
  - `builderId=1`：產品經理 / 工時估算與建議
  - `builderId=2`：測試團隊 / 生成冒煙測試
- **目標架構**：持續擴展為 `builderId + outputFormat` 的 internal copilot platform
- 新增 scenario = 新增 DB 資料，不需要改 code

---

## 3. Data Model

### 3.1 rb_builder_config（Builder 配方定義）

前端路由入口，定義一組「prompt 組裝配方」。取代原本的 `ConsultScenario` enum。

| 欄位 | 型別 | 說明 |
|------|------|------|
| builder_id | INTEGER (PK) | 前端傳入的路由 key |
| builder_code | VARCHAR (UNIQUE) | 唯一識別碼，例如 `"pm-estimate"`、`"qa-smoke-doc"` |
| group_label | VARCHAR | 歸屬標籤，僅供顯示（例如 `"產品經理"`、`"測試團隊"`） |
| name | VARCHAR | 名稱（例如 `"PM 工時估算與建議"`） |
| description | TEXT | 說明 / scenario summary |
| include_file | BOOLEAN | 是否附帶檔案輸出 |
| default_output_format | VARCHAR (nullable) | 預設輸出格式（例如 `"xlsx"`），不附帶檔案時為 null |
| file_prefix | VARCHAR | 輸出檔名前綴（例如 `"qa-smoke-doc"`) |
| active | BOOLEAN | 是否啟用，Gatekeeper 驗證時使用 |

### 3.2 rb_source_type（可配置的區域分類）

定義 Source 的區域分類，存 DB 可自由新增。

| 欄位 | 型別 | 說明 |
|------|------|------|
| type_id | INTEGER (PK) | 主鍵 |
| type_code | VARCHAR (UNIQUE) | 代碼，例如 `"PINNED"`、`"CHECK"`、`"CONTENT"` |
| type_name | VARCHAR | 顯示名稱，例如 `"置頂類"`、`"檢查類"`、`"內文類"` |
| description | VARCHAR | 說明 |
| sort_priority | INTEGER | 類別間的排序優先級，越小越前 |

初期預設三種區域分類：

| type_code | type_name | sort_priority | 說明 |
|-----------|-----------|---------------|------|
| PINNED | 置頂類 | 1 | 永遠最先出現的 prompt 片段（如安全規則、角色設定） |
| CHECK | 檢查類 | 2 | 驗證 / 檢查相關 prompt（如附件處理規則） |
| CONTENT | 內文類 | 3 | 主要業務內容 prompt（如執行流程、回應格式） |

### 3.3 rb_source（Prompt 組裝主體）

取代原本的三張 Source 表（scenario config + reference items + rag mapping）。每一筆就是一個 prompt 片段。

| 欄位 | 型別 | 說明 |
|------|------|------|
| source_id | BIGINT (PK, auto) | 主鍵 |
| builder_id | INTEGER (FK → rb_builder_config) | 屬於哪個 builder |
| type_id | INTEGER (FK → rb_source_type) | 區域分類 |
| prompts | TEXT | 這個區域的 prompt 內容 |
| order_no | INTEGER | 同 type 內的排序 |
| needs_rag_supplement | BOOLEAN | 是否需要去 RAG 撈補充資料 |

### 3.4 rb_rag_supplement（Source 的補充資料）

取代原本的 `rb_rag_document`。RAG 不再獨立運作，而是掛在特定 Source 下作為補充。

| 欄位 | 型別 | 說明 |
|------|------|------|
| rag_id | BIGINT (PK, auto) | 主鍵 |
| source_id | BIGINT (FK → rb_source) | 補充哪個 Source 片段 |
| rag_type | VARCHAR | 子分類（例如 `"execution_steps"`、`"default_content"`、`"column_rules"` 等） |
| title | VARCHAR | 標題 |
| content | TEXT | 補充內容 |
| order_no | INTEGER | 在這個 source 下的排序 |
| overridable | BOOLEAN | 是否可被前端輸入覆蓋（配合 Builder Override Factory 使用） |
| retrieval_mode | VARCHAR | 檢索模式：`"full_context"`（初期）/ `"vector_search"`（未來） |

#### 向量檢索擴充預留
- 初期 `retrieval_mode` 全部為 `"full_context"`，RAG 直接回傳 `content` 全文
- 未來某些 Source 的補充資料若文件量超過 context window，可將 `retrieval_mode` 改為 `"vector_search"`
- 向量檢索時，RAG 內部用 embedding query 去 pgvector 做 similarity search，回傳 top-K 段落
- **Builder 完全不用改**，因為它只認「拿到文字內容 + orderNo → 照順序組裝」
- 同一個 builder 下，不同 source 可以各自選擇 full_context 或 vector_search，混合模式天然支持

---

## 4. Module Summary

### 4.1 Gatekeeper
- **職責**: 入口驗證層
- **設計**: Filter / Interceptor
- **接收**: text + files[] + builderId + outputFormat
- **驗證**: builderId 是否存在且 active、檔案限制
- **輸出**: 驗證通過 → Builder；失敗 → 400/401/403
- **詳細 PRD**: [gatekeeper/README.md](src/main/java/com/citrus/rewardbridge/gatekeeper/README.md)

### 4.2 Builder
- **職責**: 核心編排中心，依 builderId 載入 Source + RAG 組裝 prompt
- **設計**: 資料驅動 prompt 組裝 + Override Factory 處理覆蓋邏輯 + Builder Graph JSON 儲存
- **依賴**: Source、RAG、AIClient、Output
- **關鍵流程**: 載入 builder config → 載入 Source（排序）→ 撈 RAG supplement → Override Factory → 送 AI → Output 渲染
- **後台編輯方向**: 先做後端 `save/load graph` API，讓前端未來不論是表單或拖拉畫布，都只需要送同一份 JSON
- **詳細 PRD**: [builder/README.md](src/main/java/com/citrus/rewardbridge/builder/README.md)

### 4.3 Source
- **職責**: Prompt 組裝主體，管理所有 prompt 片段
- **設計**: 依 builderId 查詢 + 按 type sort_priority → order_no 排序
- **儲存內容**: 安全規則、角色設定、附件處理規則、執行流程、回應契約等 prompt 片段
- **關鍵**: 每個 source 可標記 `needs_rag_supplement`，決定是否需要補充資料
- **擴充**: 新增 builder = 新增 DB 資料，不需改 code
- **詳細 PRD**: [source/README.md](src/main/java/com/citrus/rewardbridge/source/README.md)

### 4.4 RAG
- **職責**: Source 的補充資料提供者
- **設計**: 依 sourceId 查詢，回傳補充內容
- **初期**: Full-Context 模式（從 DB 讀全文直接回傳）
- **未來**: 工廠模式預留向量檢索擴充（per-source 粒度可混合使用）
- **詳細 PRD**: [rag/README.md](src/main/java/com/citrus/rewardbridge/rag/README.md)

### 4.5 AIClient
- **職責**: AI API 通訊層
- **設計**: Factory Pattern（抽象 AI 供應商）
- **功能**: Responses API（初期）；Embedding API 未來 RAG 切換向量檢索時再加
- **初期**: OpenAI GPT Responses API
- **詳細 PRD**: [aiclient/README.md](src/main/java/com/citrus/rewardbridge/aiclient/README.md)

### 4.6 Output
- **職責**: 將 AI / Builder 產出的標準化內容渲染為指定輸出格式
- **設計**: Factory Pattern（依 `outputFormat` 選擇 renderer）
- **功能**: 產出 Markdown / Excel
- **原則**: 不把格式判斷寫在 Controller、不讓 AIClient 承擔檔案格式渲染責任
- **詳細 PRD**: [output/README.md](src/main/java/com/citrus/rewardbridge/output/README.md)

---

## 5. Tech Stack

| 類別 | 技術 | 說明 |
|------|------|------|
| Framework | Spring Boot 4.0.3 | 主框架 |
| Language | Java 21 | Virtual Threads 啟用 |
| Database | PostgreSQL | 結構化資料 + prompt 片段 + 補充資料儲存 |
| AI API | OpenAI Responses API | 多模態輸入主路線 |
| Build | Maven | |
| Deployment | 公司內部地端主機 | 部署於公司閒置電腦 |
| Document Output | Markdown text + Apache POI | Markdown / Excel 由 Output 模組統一抽象 |
| 未來擴充 | pgvector / Embedding API | RAG 補充資料量超過 context window 時，per-source 切換向量檢索 |

---

## 6. Deployment Plan（地端架設）

### 部署環境
- **目標機器**: 公司內部閒置電腦
- **網路**: 公司內網，僅需能連外呼叫 OpenAI API
- **存取方式**: 內網 IP + Port，PM 透過瀏覽器存取前端頁面

### 部署組件
```
公司內部主機
├── RewardBridge Backend (Spring Boot JAR)
│     ├── Port: 8080 (或自訂)
│     └── 連外: OpenAI API (api.openai.com)
├── PostgreSQL
│     └── Port: 5432
└── 前端靜態頁面（或由 Spring Boot 一起 serve）
```

### 環境變數管理
- `OPENAI_API_KEY`: OpenAI API 金鑰
- `SPRING_DATASOURCE_URL`: PostgreSQL 連線字串
- `SPRING_DATASOURCE_USERNAME` / `PASSWORD`: DB 帳密

### 安全性考量
- 部署於內網，不對外開放
- Gatekeeper 驗證層可依需求簡化（內網信任度高）
- API key 透過環境變數注入，不進版控
- PM 上傳的文件僅存於公司內部，不外傳

---

## 7. Internal User Interface（前端需求）

### 畫面元素
1. **Builder 下拉選單**: 選擇要使用的 prompt 配方（例如「PM 工時估算」「QA 冒煙測試」）
2. **輸出格式下拉選單**: 選擇 `markdown / xlsx`
3. **文字輸入框**: 使用者直接輸入問題或需求
4. **檔案上傳區**: 上傳附件作為補充上下文
5. **送出按鈕**: 送出請求
6. **回應顯示區 / 檔案下載區**: 顯示 AI 回覆或提供檔案下載

### Example Builder Matrix

| builderId | group_label | 說明 | 預設輸出 |
|-----------|-------------|------|----------|
| 1 | 產品經理 | PM 工時估算與建議 | 無附檔 |
| 2 | 測試團隊 | QA 冒煙測試文件產生 | xlsx |
| ... | ... | 未來擴充 | 依設定決定 |

### API Contract

```text
GET /api/builders
Accept: application/json
```

回傳前端 builder 下拉與導覽用的 active 清單，欄位包含：
- `builderId`
- `builderCode`
- `groupLabel`
- `name`
- `description`
- `includeFile`
- `defaultOutputFormat`

```text
POST /api/consult
Content-Type: multipart/form-data

builderId=1
outputFormat=markdown
text=請協助評估這個活動需求的工時與風險
files=<optional attachment 1>
files=<optional attachment 2>
```

```text
PUT /api/admin/builders/{builderId}/graph
Content-Type: application/json
```

提供後台 prompt 編輯器使用的儲存入口。初期後端只需收到一份能清楚描述 Builder / Source / RAG 結構的 JSON，再由後端負責轉成既有資料表。

官方 canonical shape 以 `sources[]` 為準。若前端早期實驗曾使用 `aiagent[] -> source` 形狀，後端可暫時相容接收，但 `GET /graph` 一律回 `sources[]`。

`PUT /graph` 目前只代表「更新既有 builder graph」。若 `{builderId}` 不存在，後端應回 `404 BUILDER_NOT_FOUND`，不在這支 API 內隱式建立新 builder。

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

```text
GET /api/admin/builders/{builderId}/graph
Accept: application/json
```

提供後台 prompt 編輯器使用的載入入口，回傳同一份 graph JSON，作為前端畫布或表單的 source of truth。

### Graph Save Rules
- graph JSON 是後台編輯器的唯一儲存格式；前端之後不論是表單編輯還是拖拉畫布，最終都只送這一份結構
- 官方 canonical shape 是 `sources[]`
- `builder` 對應 `rb_builder_config`
- `sources[]` 對應 `rb_source`
- `source.rag[]` 對應 `rb_rag_supplement`
- `typeCode` 對應 `rb_source_type.type_code`；後端依 `typeCode` 反查 `typeId`
- `typeCode` 的作用是決定 prompt 區塊屬於哪一類，並參與最終排序：
  - `PINNED`: 最前面的固定規則，例如安全規則、角色設定
  - `CHECK`: 檢查類規則，例如附件處理與格式限制
  - `CONTENT`: 主要業務內容
- graph 的最終 canonical 排序應與 consult 組 prompt 一致，也就是先依 `typeCode` 對應的 `sort_priority`，再依 `orderNo`
- `source.orderNo` 與 `rag.orderNo` 若有傳值，必須是正整數；未傳時才由後端自動補不衝突的順序
- 初期紅框 `text` 不另外建立資料表節點；沿用現行 `overridable=true` 的 RAG 表示「這段預設內容可被前端 text 覆蓋」
- `PUT /graph` 的實作策略以「整個 builder graph 交易式重存」為主：
  1. 更新既有 `rb_builder_config`
  2. 刪除該 builder 既有 `rb_rag_supplement`
  3. 刪除該 builder 既有 `rb_source`
  4. 依 payload 順序重建 source 與 rag
- 後端可接受精簡 payload，但 `builder` 與 `sources/rag` 的處理語意不同：
  - `builder` 區塊採 merge 語意：
    - 未提供欄位時，優先保留既有 builder 值
    - 只有既有值本身為空時，才會回落到系統預設值
    - 例如：
      - `builder.description` 未提供 → 保留既有值
      - `builder.includeFile` 未提供 → 保留既有值，若既有也沒有才預設 `false`
      - `builder.active` 未提供 → 保留既有值，若既有也沒有才預設 `true`
  - `sources[]` / `source.rag[]` 區塊採 replace 語意：
    - payload 內沒出現的 source / rag 會在重存時被刪除
    - `source.typeCode` 未傳 → `CONTENT`
    - `rag.overridable` 未傳 → `false`
    - `rag.retrievalMode` 未傳 → `"full_context"`
- 文字欄位若傳空字串 `""`，目前視為「未提供新值」：
  - 不會主動清空既有 `description`
  - 不會主動清空既有 `filePrefix`
  - 若要支援清空欄位，需另外定義明確規則，不以空字串隱式表示
- `GET/PUT /api/admin/builders/{builderId}/graph` 屬於高風險後台寫入介面，正式環境應補管理權限保護
- 目前階段先以內網開發與快速驗證為優先，暫不處理 admin graph API 的資安 / 權限控管
- 上線前需回頭補齊：
  - admin API 身分驗證
  - 角色或 allowlist 控制
  - 操作審計 / 修改紀錄

### Output Rules
- consult API 一律回 JSON，前端永遠拿得到 `response` 文字內容
- 只有當前 builder 被設定為「需要附帶檔案」時，才會另外產出 `file`
- `outputFormat=markdown`
  - 表示附帶檔案時使用 `.md`
- `outputFormat=xlsx`
  - 表示附帶檔案時使用 `.xlsx`
- 若 builder 不需要附帶檔案，`outputFormat` 會被忽略
- 若 builder 需要附帶檔案但未傳 `outputFormat`，使用該 builder 的預設格式

### Existing Builders
目前已落地的 builder：
- `builderId=1`：產品經理 / 工時估算
- `builderId=2`：測試團隊 / 生成冒煙測試

### Builder List Source of Truth
- builder 下拉資料來自 `rb_builder_config`
- `description` 存在 `rb_builder_config.description`
- local seed 由 `initData/Local.java` 維護，需保證為繁體中文可讀描述

### AI 回覆原則
- AI 仍應以對應使用者看得懂的語言回答
- PM 場景聚焦在工時預估、可行性、建議方向、風險提醒
- QA 場景則可聚焦在測試案例、冒煙測試結構、覆蓋範圍
- 不同 builder 可有不同 system rule 與輸出模板（由 Source + RAG 資料驅動）

---

## 8. Development Priority

依照依賴關係由底層往上開發：

```
Phase 1 — Builder Graph 儲存 API
  ├─ 定義 `PUT /api/admin/builders/{builderId}/graph`
  ├─ 定義 `GET /api/admin/builders/{builderId}/graph`
  ├─ 定義 graph request/response DTO
  └─ 明確規範 builder/source/rag 的預設值補齊邏輯

Phase 2 — Graph 落庫實作
  ├─ upsert `rb_builder_config`
  ├─ 依 typeCode 解析 `rb_source_type`
  ├─ transaction 內重建 `rb_source`
  └─ transaction 內重建 `rb_rag_supplement`

Phase 3 — Consult 主流程對齊
  ├─ 保持 consult 仍走既有 builder/source/rag 查詢
  ├─ 維持 Override Factory 規則
  └─ 紅框 text 初期沿用 `overridable=true` 的 RAG 語意，不先新增新表

Phase 4 — 前端編輯器接入
  ├─ 前端先用表單或簡單 JSON editor 驗證 save/load graph
  └─ drag-and-drop 畫布等 UI 後做，不先反推資料模型

Phase 5 — 驗證
  └─ mvnw -DskipTests package 確認編譯通過
```

---

## 9. Learning Roadmap Mapping

本專案涵蓋的 AI Engineer 技能與實作對應：

### Step 1: Prompt 組裝 + API Orchestration
- Builder 模組的資料驅動 prompt 組裝
- Source 區域分類 + 排序拼接設計
- Override Factory 處理前端覆蓋邏輯
- AIClient 的 API 呼叫封裝

### Step 2: RAG / Memory Retrieval
- Full-Context vs Vector Search 的設計決策（理解 trade-off）
- RAG 作為 Source 補充資料的架構設計
- per-source 粒度的檢索策略選擇
- 未來切換向量檢索時 Builder 不需改動

### Step 3: Agent / Tool Calling
- Builder 作為 Agent 的資料驅動編排邏輯
- 根據 builderId 動態決定要載入哪些 Source + RAG
- Source `needs_rag_supplement` 驅動的條件式補充查詢
- Override Factory 根據前端輸入動態決定覆蓋策略

### Step 3.5: Structured Output Rendering
- 將 AI 產出的標準化內容與最終輸出格式拆開
- 建立 Output Factory 與多 renderer
- 讓同一任務可依需求輸出文字或文件

### Step 4: Local LLM（optional，非主要目標）
- AIClient 工廠模式已預留介面
- 未來可接入 Ollama / vLLM 等 local model
- 實現資料完全不出公司內網

---

## 10. Collaboration Decision Log

### 10.1 已確認的取捨與邊界（2026-03-07）

- `src/main/resources/config/application-local.properties` 中直接保留 local PostgreSQL 帳密，且 `spring.jpa.hibernate.ddl-auto=create-drop` 暫時接受。
- 使用者明確表示這是 local / 公司內部用途，現階段不把「密碼在版控內」與「create-drop 有誤刪風險」視為優先處理事項。
- 針對「四層架構 + CQRS + 五模組 + Factory + Strategy 可能過度設計」的疑慮，使用者明確決定維持現有完整規劃。
- 模組說明文件暫時繼續放在 `src/main/java/.../README.md` 下。
- 測試暫時不做。

### 10.2 設定變更（2026-03-07）

- 已啟用 Java 21 Virtual Threads（`spring.threads.virtual.enabled=true`）
- 已追加 `spring-boot-starter-validation`、`spring-boot-starter-actuator`、`flyway-core`、`openai-java-spring-boot-starter`

### 10.3 RAG 策略決策（2026-03-07）

- 初期 RAG 不使用向量資料庫，採 Full-Context 模式
- 未來文件量超過 context window 再以 per-source 粒度切換向量檢索
- AIClient 初期只需 Responses API，Embedding API 延後

### 10.4 附件與安全決策（2026-03-07）

- PM 上傳的文件 / 圖片初期直接原樣交給模型，不做中間加工
- 附件不落地保存
- 若附件串入失敗，直接回固定失敗 payload，不做 fallback
- STEP1 防注入只檢查 text，不檢查附件
- IP 驗證策略未來以白名單模式為主，但目前不實作

### 10.5 Output 簡化決策（2026-03-09）

- 正式支援 `markdown` / `xlsx`，不先做 Word / PDF
- 保留 Output 模組工廠式擴充結構

### 10.6 JSON-First 輸出決策（2026-03-09）

- consult API 回傳固定為 JSON
- 前端永遠拿到 `response` 文字內容
- 檔案以 `file.base64` 掛在同一份 JSON payload
- 不建立後端檔案保存機制，不額外開 download API

### 10.7 Source / RAG 架構重構決策（2026-03-10）

- 前端路由 key 從 `group + type` 改為 **`builderId`**
- `group` 降級為 Builder Config 上的歸屬標籤，不參與資料查詢
- `type` 語意從「任務類型」改為 Source 上的「**區域分類**」（置頂類 / 檢查類 / 內文類），存 DB 可配置
- **Source 升格為 prompt 組裝主體**，取代原本的三張表（scenario config + reference items + rag mapping）
- **RAG 降為 Source 的補充資料**，靠 `sourceId` 精確撈取，不再獨立按 scenario 運作
- Source 的 strategy pattern 移除，改為依 builderId 統一查詢 + 排序（新增 scenario 只需加 DB 資料）
- Builder 的 prompt 組裝從硬寫結構改為**資料驅動**（依 Source 排序拼接）
- Builder 新增 **Override Factory** 工廠模式處理前端覆蓋 default 語句的邏輯
- RAG 的 `overridable` flag 配合 Override Factory 使用
- RAG 預留 `retrieval_mode` 欄位，未來可 per-source 切換 full_context / vector_search
- 向量檢索切換時 Builder 不需改動，只改 RAG 內部邏輯

### 10.8 Builder 下拉 API 決策（2026-03-10）

- 前端 builder 下拉不再寫死，改由 `GET /api/builders` 取得
- builder 下拉的顯示說明來自 `rb_builder_config.description`
- local seed 啟動時需同步 builder metadata，避免舊資料殘留造成 description 缺失

### 10.9 Builder Graph Editor 決策（2026-03-10）

- 先做後端 graph JSON 儲存 / 載入 API，再決定前端畫布長相
- graph JSON 的核心結構是 `builder + sources[] + source.rag[]`
- 後台編輯器的 source of truth 是 graph JSON，不是拖拉 UI 本身
- 初期不為紅框 `text` 額外新增資料表節點；沿用 `overridable=true` 的 RAG 表示「可被前端 text 覆蓋的位置」
- graph 儲存策略採 transaction 內整個 builder graph 重存，避免前端送 patch 帶來一致性問題
