# Output Module - PRD

## Overview
Output 模組負責將 Builder / AI 產出的標準化結果，轉換成使用者真正需要的輸出形式。

這個模組存在的原因是：
- 同一個 consult API 需要支援多種輸出格式
- 輸出格式判斷不能寫在 controller
- AIClient 只應負責模型通訊，不應順便承擔文件產生責任

因此，Output 是整個 internal copilot platform 的**最終渲染層**。

## Responsibilities
- 接收 Builder 傳來的標準化結果內容
- 當 scenario 需要附帶檔案時，根據 `outputFormat` 選擇對應 renderer
- 必要時依 `group + type` 套用不同輸出模板
- 支援以下輸出形式：
  - `markdown`
  - `xlsx`
- 統一管理附帶檔案的檔名、content type 與 base64 payload

## Why Separate Module
- **Controller 不應知道輸出格式細節**
- **Builder 應專注在內容編排，不應專注檔案渲染**
- **AIClient 應專注在和模型溝通，不應負責 Markdown / Excel 產生**
- **同一份內容未來可能需要用不同格式輸出**

## Scope
- **In Scope**:
  - output renderer factory
  - markdown / xlsx 渲染
  - scenario template 套用
  - response metadata 組裝
- **Out of Scope**:
  - prompt 組裝
  - RAG 檢索
  - Source 查詢
  - AI API 呼叫

## Input
Output 模組不直接接 HTTP request，而是接收 Builder 傳來的結果物件。

建議輸入概念如下：

```text
group=2
type=2
outputFormat=markdown
resultContent=<標準化內容>
```

- `outputFormat` 的語意是：**如果此 scenario 需要附帶檔案，要用什麼格式**
- 若 scenario 本身不附帶檔案，`outputFormat` 會被忽略
- 若 scenario 需要附帶檔案但前端未傳 `outputFormat`，則使用該 scenario 的預設格式
- 目前已實作的代碼如下：
  - `group=1`, `type=1`：產品經理 / 工時估算
  - `group=2`, `type=2`：測試團隊 / 生成冒煙測試

### Current Internal Model
目前 Builder 透過 `OutputRenderCommand` 將 AI 回應傳入 Output 模組：

```java
OutputRenderCommand {
  Integer group;
  Integer type;
  OutputFormat outputFormat;
  ConsultBusinessResponse businessResponse;
}
```

Output 依 `ConsultBusinessResponse.response` 做渲染。若未來不同 scenario 的文件結構差異很大，可考慮加入更結構化的中介 payload（如 title、sections[]），但目前以 AI 回應原文直接渲染即可。

## Output
Output 模組應回傳可供 HTTP 層包裝的結果，例如：

```text
ConsultBusinessResponse {
  status
  statusAns
  response
  file? {
    fileName
    contentType
    base64
  }
}
```

Controller 永遠回 JSON。若 scenario 需要附帶檔案，Output 會把檔案內容以 base64 形式掛到 `file` 欄位；若不需要，則不輸出 `file` 欄位。

## Design Pattern

### Factory Pattern

```text
OutputScenarioPolicyResolver
  └─ decide whether current group + type should attach a file

OutputRendererFactory
├── MarkdownRenderer
├── XlsxRenderer
```

- scenario policy 先決定「要不要附帶檔案」
- 主要 renderer 分流鍵為 `outputFormat`
- renderer 內部可再依 `group + type` 決定模板
- 這樣可以維持：
  - 新增格式時不影響既有 controller
  - 新增場景時不必複製整條輸出流程

### Optional Template Layer
若未來 `group=2,type=2` 與 `group=1,type=1` 的文件長相差異很大，可再加入 template abstraction：

```text
OutputRenderer
  └─ uses ScenarioTemplateProvider(group, type)
```

例如：
- PM 工時估算：以段落式摘要 + 功能拆解
- QA 冒煙文件：以測試項目表格、前置條件、預期結果輸出

## Example Scenarios

### 1. PM 工時估算
- `group=1`
- `type=1`

預期：
- 目前先回純文字 JSON
- 不強制附帶下載檔案

### 2. QA 冒煙文件
- `group=2`
- `type=2`
- `outputFormat=markdown`

預期：
- 永遠有文字回應
- 可選擇附帶 Markdown 文件
- 內容可包含：
  - 測試目的
  - 前置條件
  - 冒煙測試案例
  - 預期結果

### 3. QA 測試案例清單
- `group=2`
- `type=2`
- `outputFormat=xlsx`

預期：
- 永遠有文字回應
- 可附帶 Excel 表格
- 每列可對應：
  - case id
  - 測試步驟
  - 預期結果
  - 優先級

## Module Boundaries
- `Gatekeeper`
  - 接 request，驗證欄位
- `Builder`
  - 組上下文、呼叫 AI、整理標準化結果
- `Output`
  - 將結果渲染成最終格式

正式流程：

```text
Gatekeeper -> Builder -> AIClient -> Output -> HTTP response
```

## Notes
- 同一支 consult API 應可重用，不因 `outputFormat` 不同而複製多支 API
- 輸出格式判斷不能下沉到 controller
- `group + type` 先決定此 scenario 是否需要附帶檔案
- `outputFormat` 決定「如果要附帶檔案，採用哪一種格式」
- Output 模組應盡量建立在「標準化中介 payload」之上，避免 renderer 直接耦合 AI 原始字串
- 目前先正式支援 `markdown` / `xlsx`，若未來需要其他格式，再以 renderer 方式擴充
- 目前 `group=1,type=1` 仍先採文字回應，不附帶檔案；`group=2,type=2` 預設附帶 `xlsx`
- Output 目前已直接吃數字化 scenario code，不再依賴舊的字串 group / 語意化 type
