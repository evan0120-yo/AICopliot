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
│       └── ConsultGuardService.java    ← 驗證邏輯在這
```

- Guard 是 Service 層的一部分，由 UseCase 呼叫
- Gatekeeper 模組的 Guard 可被其他模組的 UseCase 呼叫（因為走 UseCase 層跨模組）
- 實作方式：可搭配 Spring Interceptor / Filter 在 Controller 前攔截，實際邏輯委派給 Guard Service

---

## 4. Package Structure

```
com.citrus.rewardbridge
├── initData/
│   └── Local.java                  ← local profile seed data
│
├── common/
│   ├── dto/                        ← ConsultBusinessResponse、ConsultFilePayload
│   ├── entity/                     ← BuilderConfigEntity（跨模組共用）
│   ├── repository/                 ← BuilderConfigRepository
│   ├── exception/
│   ├── response/                   ← ApiResponse
│   └── config/                     ← RewardBridgeConfiguration、RewardBridgeProperties
│
├── gatekeeper/
│   ├── controller/                 ← GatekeeperController
│   ├── usecase/
│   │   └── command/                ← GatekeeperCommandUseCase
│   ├── service/
│   │   └── guard/                  ← ConsultGuardService、ClientIpResolver
│   └── dto/                        ← ConsultRequest、ConsultGuardResult
│
├── builder/
│   ├── usecase/
│   │   └── command/                ← BuilderCommandUseCase
│   ├── service/
│   │   └── command/                ← BuilderCommandService
│   │       └── override/           ← BuilderOverrideFactory + 覆蓋策略
│   └── dto/                        ← BuilderConsultCommand
│
├── source/
│   ├── usecase/
│   │   └── query/                  ← SourceQueryUseCase
│   ├── service/
│   │   └── query/                  ← SourceQueryService
│   ├── entity/                     ← SourceTypeEntity、SourceEntity
│   ├── repository/                 ← SourceTypeRepository、SourceRepository
│   └── dto/                        ← SourceEntryDto、SourceLoadResult
│
├── rag/
│   ├── usecase/
│   │   └── query/                  ← RagQueryUseCase
│   ├── service/
│   │   └── query/                  ← RagQueryService
│   ├── entity/                     ← RagSupplementEntity
│   ├── repository/                 ← RagSupplementRepository
│   └── dto/                        ← RagSupplementDto
│
├── aiclient/
│   ├── usecase/
│   │   └── command/                ← AiClientCommandUseCase
│   ├── service/
│   │   └── command/                ← AiClientCommandService
│   └── dto/                        ← AiConsultResponse
│
├── output/
│   ├── usecase/
│   │   └── command/                ← OutputCommandUseCase
│   ├── service/
│   │   └── command/                ← OutputCommandService
│   │       └── renderer/           ← OutputRenderer、MarkdownRenderer、XlsxRenderer
│   └── dto/                        ← OutputFormat、OutputRenderCommand、RenderedOutput、
│                                      RenderedFile、ScenarioOutputPolicy
│
```

### 備註
- **BuilderConfigEntity** 放在 `common/entity`，因為它被 Gatekeeper（驗證）、Builder（讀配置）、Output（讀 include_file）跨模組使用
- **source** 不再有 strategy/ 目錄，因為不再需要 per-scenario strategy，改為依 builderId 統一查詢
- **rag** 不再有 factory/ 目錄，檢索模式判斷在 RagQueryService 內部處理
- **builder** 新增 `service/command/override/` 目錄，放 Override Factory 與覆蓋策略
- **aiclient** 仍然沒有 controller 和 repository
- **output** 仍然沒有 repository
- **common/** 放跨模組共用的東西（exception、response wrapper、config、BuilderConfig entity）

---

## 5. Entity Schema

### 5.1 BuilderConfigEntity（common/entity）

```java
@Entity
@Table(name = "rb_builder_config")
public class BuilderConfigEntity {
    @Id
    private Integer builderId;

    @Column(nullable = false, unique = true)
    private String builderCode;        // "pm-estimate", "qa-smoke-doc"

    @Column(nullable = false)
    private String groupLabel;          // "產品經理", "測試團隊"（僅顯示）

    @Column(nullable = false)
    private String name;                // "PM 工時估算與建議"

    @Column(columnDefinition = "TEXT")
    private String description;         // scenario summary

    @Column(nullable = false)
    private boolean includeFile;        // 是否附帶檔案輸出

    private String defaultOutputFormat; // "xlsx", nullable

    private String filePrefix;          // "qa-smoke-doc"

    @Column(nullable = false)
    private boolean active;             // 是否啟用
}
```

### 5.2 SourceTypeEntity（source/entity）

```java
@Entity
@Table(name = "rb_source_type")
public class SourceTypeEntity {
    @Id
    private Integer typeId;

    @Column(nullable = false, unique = true)
    private String typeCode;            // "PINNED", "CHECK", "CONTENT"

    @Column(nullable = false)
    private String typeName;            // "置頂類", "檢查類", "內文類"

    private String description;

    @Column(nullable = false)
    private Integer sortPriority;       // 類別排序，越小越前
}
```

### 5.3 SourceEntity（source/entity）

```java
@Entity
@Table(name = "rb_source")
public class SourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sourceId;

    @Column(nullable = false)
    private Integer builderId;          // FK → rb_builder_config

    @Column(nullable = false)
    private Integer typeId;             // FK → rb_source_type

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompts;             // prompt 內容

    @Column(nullable = false)
    private Integer orderNo;            // 同 type 內排序

    @Column(nullable = false)
    private boolean needsRagSupplement; // 是否需要 RAG 補充
}
```

### 5.4 RagSupplementEntity（rag/entity）

```java
@Entity
@Table(name = "rb_rag_supplement")
public class RagSupplementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ragId;

    @Column(nullable = false)
    private Long sourceId;              // FK → rb_source

    @Column(nullable = false)
    private String ragType;             // "execution_steps", "default_content", "column_rules"

    @Column(nullable = false)
    private String title;               // 標題

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;             // 補充內容

    @Column(nullable = false)
    private Integer orderNo;            // 在此 source 下排序

    @Column(nullable = false)
    private boolean overridable;        // 是否可被前端覆蓋

    @Column(nullable = false)
    private String retrievalMode;       // "full_context" | "vector_search"
}
```

---

## 6. Virtual Threads（Java 21）

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
- Builder 載入 Source + RAG 的並行查詢
- DB 查詢
- 未來 RAG 切換向量檢索時的 embedding API 呼叫

### 注意事項
- Virtual Threads 適合 I/O bound 任務，本專案大量外部 API 呼叫正好適用
- 目前程式有顯式建立 `rewardBridgeExecutorService = Executors.newVirtualThreadPerTaskExecutor()`，用於 Builder 的並行資料收集
- synchronized 改用 ReentrantLock（如有需要），避免 pinning

---

## 7. Data Flow（完整資料流）

以 `builderId=1`（產品經理詢問開發工時和意見）為例：

```
1. PM 送出請求
   POST /api/consult
   { builderId: 1, text: "...", outputFormat?: "markdown", files: [...] }

2. Gatekeeper (Controller → UseCase → Guard Service)
   → 解析 client IP 並保留未來驗證插點
   → 驗證 builderId 是否存在且 active
   → 驗證檔案限制（數量、大小、副檔名）
   → 若有傳入 outputFormat，再驗證格式是否合法
   → 通過 → 轉發至 Builder

3. Builder (UseCase)
   → BuilderCommandUseCase.consult(builderId, text, outputFormat, files, clientIp)

   3a. Step 1：載入 Builder Config
       → BuilderConfigRepository.findById(builderId)
       → 取得 name, group_label, include_file, default_output_format 等

   3b. Step 2：載入所有 Source
       → SourceQueryUseCase.loadByBuilderId(builderId)
       → 回傳 List<SourceEntryDto>，已按 source_type.sort_priority → source.order_no 排序
       → 每個 entry 包含：sourceId, typeCode, prompts, orderNo, needsRagSupplement

   3c. Step 3：撈 RAG Supplement
       → 對 needsRagSupplement=true 的 source entry：
         → RagQueryUseCase.queryBySourceId(sourceId)
         → 回傳 List<RagSupplementDto>，按 order_no 排序
         → 每個 supplement 包含：ragType, title, content, orderNo, overridable

   3d. Step 4：組裝完整 prompt
       → BuilderCommandService.assemblePrompt(builderConfig, sourceEntries, ragSupplements, userText)
       → Builder Override Factory 檢查：
         → 若前端有傳 text，且某個 RAG supplement 的 overridable=true
         → 由 Override Factory 決定覆蓋策略（簡單替換 / 模板合併 / etc.）
       → 最終產出完整 prompt 字串

   3d-1. Attachment Handling
       → PM 上傳的文件 / 圖片不做中間加工
       → 直接原樣交由 AIClient / Responses API 作為模型輸入
       → 初期不落地保存
       → 若模型端不接受附件格式或附件串入失敗，直接回固定失敗格式，不做 fallback

   3e. Step 5：送 AI
       → AiClientCommandUseCase.analyze(model, text, prompt, files)
       → AI 回應

   3f. Step 6：Output
       → OutputCommandUseCase.render(builderConfig, outputFormat, response)
       → 永遠保留文字回應
       → 依 builder config 的 include_file 決定是否附帶檔案
       → 若附帶檔案，再依 outputFormat 或 builder default 渲染 markdown / xlsx

4. 回傳 AI 回應給 PM
   → HTTP 層固定回 JSON
   → `data.response` 給前端直接顯示
   → `data.file` 若存在，前端可提供下載按鈕
```

### Source 排序邏輯
Source 排序分兩層：
1. **type sort_priority**：置頂類(1) → 檢查類(2) → 內文類(3)
2. **source order_no**：同 type 內的排序

最終 prompt 片段的排列順序就是：PINNED 的 prompt 片段 → CHECK 的 prompt 片段 → CONTENT 的 prompt 片段，每組內部再依 order_no 排列。

### RAG Supplement 查詢邏輯
- 只有 `needs_rag_supplement = true` 的 Source 才會觸發 RAG 查詢
- RAG 查詢依 `sourceId` 精確撈取，不會撈到其他 Source 的補充資料
- 撈回來的 supplement 按 `order_no` 排序，插入對應 source 的 prompt 後方

---

## 8. Error Handling

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

若某個 builder 需要附帶檔案，response 會額外帶出 `file` 欄位：

```json
{
  "fileName": "qa-smoke-doc-consult.xlsx",
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

## 9. Naming Convention

| 類型 | 命名規則 | 範例 |
|------|---------|------|
| Controller | `XxxController` | `GatekeeperController` |
| Command UseCase | `XxxCommandUseCase` | `BuilderCommandUseCase` |
| Query UseCase | `XxxQueryUseCase` | `SourceQueryUseCase` |
| Command Service | `XxxCommandService` | `BuilderCommandService` |
| Query Service | `XxxQueryService` | `SourceQueryService` |
| Guard Service | `XxxGuardService` | `ConsultGuardService` |
| Repository | `XxxRepository` | `SourceRepository` |
| Factory | `XxxFactory` | `BuilderOverrideFactory` |
| DTO | `XxxRequest` / `XxxResponse` / `XxxDto` | `ConsultRequest` |
| Entity | `XxxEntity` | `SourceEntity` |
| Override Strategy | `XxxOverrideStrategy` | `SimpleOverrideStrategy` |

---

## 10. 不使用的技術（刻意排除）

| 技術 | 原因 |
|------|------|
| DDD | 公司不使用，保持四層架構 |
| Message Queue | 小專案，不需要非同步訊息傳遞，有需求再議 |
| 高流量設計 | 公司內部使用，不需要 cache、load balancer 等 |
| 微服務 | 單體應用即可，所有模組在同一個 Spring Boot 內 |
| Source Strategy Pattern | 已移除，改為依 builderId 統一查詢，新增 scenario 只需加 DB 資料 |
