# RewardBridge - Development Guide

## 1. Architecture：四層架構

```
Controller → UseCase → Service → Repository
```

| 層級 | 職責 | 規範 |
|------|------|------|
| **Controller** | 接收 HTTP 請求、參數驗證、回傳 Response | 不含業務邏輯，僅做 request/response 轉換 |
| **UseCase** | 業務流程編排、跨模組協作 | 唯一允許跨模組溝通的層級 |
| **Service** | 單一模組內的業務邏輯、Guard 驗證邏輯 | 不可跨模組呼叫，只服務自己的 UseCase |
| **Repository** | 資料存取（DB CRUD） | 不含業務邏輯，純資料操作 |

### 層級呼叫規則
```
Controller  →  UseCase  →  Service  →  Repository
                 ↕
          其他模組 UseCase
```

- Controller 只能呼叫 UseCase
- UseCase 可呼叫自己模組的 Service，也可呼叫其他模組的 UseCase
- Service 只能呼叫自己模組的 Repository（或自己模組內的其他 Service）
- Repository 只做資料存取

### 嚴格限制：跨模組通訊
```
⭕ builder.usecase  →  rag.usecase
⭕ builder.usecase  →  source.usecase
⭕ builder.usecase  →  aiclient.usecase
⭕ builder.usecase  →  output.usecase

❌ builder.service  →  rag.service
❌ builder.service  →  source.repository
❌ builder.controller → rag.service
```

**不同模組之間，只有 UseCase 層可以互相呼叫，其他層一律禁止。**

---

## 2. CQRS（Command Query Responsibility Segregation）

UseCase 和 Service 層採用 CQRS，分離讀取與寫入：

### UseCase 層
```
module/
├── usecase/
│   ├── command/          ← 寫入操作
│   │   └── XxxCommandUseCase.java
│   └── query/            ← 讀取操作
│       └── XxxQueryUseCase.java
```

### Service 層
```
module/
├── service/
│   ├── command/          ← 寫入邏輯
│   │   └── XxxCommandService.java
│   └── query/            ← 讀取邏輯
│       └── XxxQueryService.java
```

### CQRS 原則
- Command（寫入）：改變系統狀態的操作（新增、修改、刪除）
- Query（讀取）：不改變系統狀態的操作（查詢、檢索）
- 一個 UseCase / Service class 只做 Command 或 Query，不混用

---

## 3. Guard 層

Guard（驗證 / 權限檢查）放在 **Service 層**：

```
gatekeeper/
├── service/
│   ├── command/
│   └── query/
│   └── guard/
│       └── AuthGuardService.java    ← 驗證邏輯在這
```

- Guard 是 Service 層的一部分，由 UseCase 呼叫
- Gatekeeper 模組的 Guard 可被其他模組的 UseCase 呼叫（因為走 UseCase 層跨模組）
- 實作方式：可搭配 Spring Interceptor / Filter 在 Controller 前攔截，實際邏輯委派給 Guard Service

---

## 4. Package Structure

```
com.citrus.rewardbridge
├── initData/
│   └── Local.java
│
├── gatekeeper/
│   ├── controller/
│   ├── usecase/
│   │   ├── command/
│   │   └── query/
│   ├── service/
│   │   ├── command/
│   │   ├── query/
│   │   └── guard/
│   ├── repository/
│   └── dto/
│
├── builder/
│   ├── controller/
│   ├── usecase/
│   │   ├── command/
│   │   └── query/
│   ├── service/
│   │   ├── command/
│   │   └── query/
│   ├── repository/
│   └── dto/
│
├── rag/
│   ├── controller/
│   ├── usecase/
│   │   ├── command/
│   │   └── query/
│   ├── service/
│   │   ├── command/
│   │   └── query/
│   ├── repository/
│   ├── factory/               ← 檢索策略工廠（FullContextReader / VectorSearchReader）
│   └── dto/
│
├── source/
│   ├── controller/
│   ├── usecase/
│   │   ├── command/
│   │   └── query/
│   ├── service/
│   │   ├── command/
│   │   └── query/
│   │       └── strategy/      ← group + type 對應策略（BaseScenarioSourceStrategy）
│   ├── repository/
│   └── dto/
│
├── aiclient/
│   ├── usecase/
│   │   ├── command/
│   │   └── query/
│   ├── service/
│   │   ├── command/
│   │   └── query/
│   ├── factory/               ← AI 供應商工廠
│   └── dto/
│
├── output/
│   ├── usecase/
│   │   └── command/
│   ├── service/
│   │   └── command/
│   │       └── renderer/      ← output renderer / template 分派
│   └── dto/
│
└── common/                    ← 共用工具、例外定義、Response 格式
    ├── dto/                   ← ConsultBusinessResponse、ConsultFilePayload
    ├── scenario/              ← ConsultScenario、ConsultGroupCode、ConsultTypeCode
    ├── exception/
    ├── response/
    └── config/
```

### 備註
- **aiclient** 沒有 controller 和 repository：它不直接接收 HTTP 請求，也不存取 DB，純粹對外呼叫 AI API
- **output** 沒有 repository：它不直接查 DB，主要做最終格式渲染
- **factory / renderer factory** 只出現在有工廠模式的模組（rag、aiclient、output）
- **strategy/** 只出現在 source 模組（位於 `service/query/strategy/`）
- **common/** 放跨模組共用的東西（exception、response wrapper、config、scenario enum registry）

---

## 5. Virtual Threads（Java 21）

本專案啟用 Java 21 Virtual Threads：

### 設定
```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true
```

### 適用場景
- AIClient 呼叫 OpenAI API（I/O bound，等待外部回應）
- Builder 的 Phase 1 並行資料收集（同時打 RAG + Source）
- DB 查詢
- 未來 RAG 切換向量檢索時的 embedding API 呼叫

### 注意事項
- Virtual Threads 適合 I/O bound 任務，本專案大量外部 API 呼叫正好適用
- 目前程式有顯式建立 `rewardBridgeExecutorService = Executors.newVirtualThreadPerTaskExecutor()`，用於 Builder 的並行資料收集
- synchronized 改用 ReentrantLock（如有需要），避免 pinning

---

## 6. Data Flow（完整資料流）

以目前已落地的 `group=1`, `type=1`（產品經理詢問開發工時和意見）為例：

```
1. PM 送出請求
   POST /api/consult
   { text: "...", group: 1, type: 1, outputFormat?: "markdown", files: [...] }

2. Gatekeeper (Controller → UseCase → Guard Service)
   → 基礎 guard 檢查
   → 解析 client IP 並保留未來驗證插點
   → 驗證 group / type / 檔案限制
   → 若有傳入 outputFormat，再驗證格式是否合法
   → 通過 → 轉發至 Builder

3. Builder (UseCase)
   → BuilderCommandUseCase.consult(text, group, type, outputFormat, files)

   3a. Phase 1（並行）
       → RagQueryUseCase.getByScenario(group, type)  → scenario 關聯的文件全文
       → SourceQueryUseCase.getData(group, type)     → 結構化資料 + ragKeys[]

   3a-1. Attachment Handling
       → PM 上傳的文件 / 圖片不做中間加工
       → 直接原樣交由 AIClient / Responses API 作為模型輸入
       → 初期不落地保存
       → 若模型端不接受附件格式或附件串入失敗，直接回固定失敗格式，不做 fallback

   3b. Phase 2
       → RagQueryUseCase.getByKeys(ragKeys)          → 指定文件全文

   3c. Assemble prompt
       → system prompt + all context + user text

   3d. Send to AI
       → AiClientCommandUseCase.analyze(...)         → AI 回應

   3e. Output
        → OutputCommandUseCase.render(...)
        → 永遠保留文字回應
        → 依 scenario 決定是否附帶檔案
        → 若附帶檔案，再依 outputFormat 或 scenario default 渲染 markdown / xlsx

4. 回傳 AI 回應給 PM
   → HTTP 層固定回 JSON
   → `data.response` 給前端直接顯示
   → `data.file` 若存在，前端可提供下載按鈕
```

### ragKeys 資料來源
- ragKeys 存在 **DB** 中，由 Source 的 Strategy 透過 `rb_source_rag_mapping` 查詢取得
- mapping table 含 `group_code + type_code + document_key + sort_order`
- 這讓同一個 scenario 可以精確指定要額外補哪些 RAG 文件，以及補入順序

---

## 7. Error Handling

### 統一回應格式
```java
{
  "success": boolean,
  "data": T,
  "error": {
    "code": "ERROR_CODE",
    "message": "error description"
  }
}
```

### 例外處理策略
- Controller 層：GlobalExceptionHandler（@RestControllerAdvice）統一捕捉
- UseCase / Service 層：拋出自定義 BusinessException
- AIClient：API 呼叫失敗時包裝為 AiClientException，含 retry 資訊
- 不做過度防禦性程式設計，內部模組間信任呼叫

### consult 業務回應格式
consult 的業務 payload 採固定 JSON 結構：

```json
{
  "status": true,
  "statusAns": "",
  "response": "AI 回應內容"
}
```

若某個 scenario 需要附帶檔案，response 會額外帶出 `file` 欄位：

```json
{
  "fileName": "qa-type2-consult.xlsx",
  "contentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "base64": "UEsDB..."
}
```

#### STEP1: Injection 檢查
- 只檢查傳入 `text`
- 不檢查圖片內容
- 不檢查附件內容
- 檢查重點不是「是否有程式碼」，而是是否包含：
  - 明顯 prompt injection
  - 規則覆寫意圖
  - 越權要求
  - 明顯以 code / command 形式操控模型行為的內容
- 若判定有問題，直接回：

```json
{
  "status": false,
  "statusAns": "prompts有違法注入內容",
  "response": "取消回應"
}
```

#### STEP2: 正常分析
- 若 STEP1 通過，再進行正常分析並回覆

#### 附件串入失敗
- 若附件格式不被模型接受、或 Responses API 拒收附件，直接回：

```json
{
  "status": false,
  "statusAns": "串入檔案格式錯誤",
  "response": ""
}
```

#### 備註
- 失敗時不做 fallback 文字抽取，避免回答品質漂移造成 PM 信任下降
- `status` 會作為後續流程判斷依據
- 一般技術內容、JSON、SQL、API 規格、或需求中的程式碼片段，不應因為「看起來像 code」就直接被拒絕

---

## 8. Naming Convention

| 類型 | 命名規則 | 範例 |
|------|---------|------|
| Controller | `XxxController` | `BuilderController` |
| Command UseCase | `XxxCommandUseCase` | `BuilderCommandUseCase` |
| Query UseCase | `XxxQueryUseCase` | `RagQueryUseCase` |
| Command Service | `XxxCommandService` | `SourceCommandService` |
| Query Service | `XxxQueryService` | `RagQueryService` |
| Guard Service | `XxxGuardService` | `AuthGuardService` |
| Repository | `XxxRepository` | `SourceRepository` |
| Factory | `XxxFactory` | `AiClientFactory` |
| Strategy | `XxxStrategy` | `DevEstimateStrategy` |
| DTO | `XxxRequest` / `XxxResponse` / `XxxDto` | `ConsultRequest` |
| Entity | `XxxEntity` | `SourceScenarioConfigEntity` |

---

## 9. 不使用的技術（刻意排除）

| 技術 | 原因 |
|------|------|
| DDD | 公司不使用，保持四層架構 |
| Message Queue | 小專案，不需要非同步訊息傳遞，有需求再議 |
| 高流量設計 | 公司內部使用，不需要 cache、load balancer 等 |
| 微服務 | 單體應用即可，所有模組在同一個 Spring Boot 內 |
