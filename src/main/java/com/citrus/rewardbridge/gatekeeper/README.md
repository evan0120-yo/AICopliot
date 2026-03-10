# Gatekeeper Module - PRD

## Overview
Gatekeeper 是系統的 HTTP 入口與驗證預留層，負責接收 internal copilot 的 consult 請求，先做基礎檢查，再將合法請求轉發至 Builder。

初期不實作正式登入驗證，但必須先把未來驗證插點留好，尤其是 **client IP 驗證**。

## Responsibilities
- 提供 consult API 入口：`POST /api/consult`
- 接收 internal user 的文字提問與上傳的多個附件
- 從 HTTP request 解析 client IP，作為未來驗證依據
- 做基礎 guard 檢查：
  - group 是否支援
  - type 是否支援
  - outputFormat 若有傳入，格式是否支援
  - 檔案格式是否允許
  - 多附件是否可正確接收
  - client IP 是否可取得
- 預留未來驗證插點：
  - IP allowlist / blocklist
  - token / API key
  - rate limiting
- 將驗證通過的請求（文字 + 多附件 + group + type + outputFormat + clientIp）傳遞給 Builder

## Input Format
前端送進來的請求包含：
- **text**: 使用者的文字提問（直接問 AI 的內容）
- **group**: 使用者所屬團隊代碼
- **files**: 上傳的附件列表（可選，用於補充上下文，支援 PDF / Word / Image，初期直接原樣交給模型）
- **type**: 下拉選單選擇的任務代碼
- **outputFormat**: 可選欄位。若該 scenario 需要附帶檔案，表示希望的檔案格式，例如 `markdown` / `xlsx`

初期 API contract 建議為 `multipart/form-data`：

```text
POST /api/consult
Content-Type: multipart/form-data

text=<使用者問題文字>
group=1
type=1
files=<optional attachment 1>
files=<optional attachment 2>
files=<optional attachment N>
```

支援格式：
- 文件：`pdf` / `doc` / `docx`
- 圖片：`jpg` / `jpeg` / `png` / `webp` / `gif` / `bmp`

### Server-side Derived Input
- **clientIp**: 不由前端直接提供，由後端從 request header / remote address 解析並注入後續流程

### Current Supported Type
- `group = 1`, `type = 1`: 產品經理 / 工時估算及建議
- `group = 2`, `type = 2`: 測試團隊 / 生成冒煙測試
- `outputFormat = markdown | xlsx`
  - `group=1,type=1` 不附帶檔案，若有傳入只做格式驗證
  - `group=2,type=2` 預設附帶 `xlsx`，若前端有傳 `outputFormat` 則依指定格式渲染

### Scenario Code Registry
- `group=1` = `產品經理`
- `group=2` = `測試團隊`
- `type=1` = `工時估算`
- `type=2` = `生成冒煙測試`
- 代碼與中文描述由 Builder / common scenario enum 維護，避免前後端散落 hardcode

## Scope
- **In Scope**: API 入口、client IP 解析、基礎 guard 檢查、未來驗證插點、檔案格式驗證、附件 passthrough
- **Out of Scope**: 業務邏輯、prompt 組裝、AI 呼叫、檔案內容解析

## Interface
- **Input**: User HTTP Request（text + files[] + group + type + outputFormat）
- **Output**:
  - 驗證通過 → 轉發至 Builder UseCase
  - 驗證失敗 → 回傳統一錯誤格式
  - Builder 成功處理後 → 永遠回傳 JSON；其中可能包含可下載檔案的 base64 payload

### Current Attachment Policy
- 多附件初期採 **直接原樣交給模型**
- 不做：
  - 文字抽取 fallback
  - OCR fallback
  - 附件落地保存
- 若模型端或 API 端不接受附件格式，consult 流程應回固定業務 payload：

```json
{
  "status": false,
  "statusAns": "串入檔案格式錯誤",
  "response": ""
}
```

### Builder Handoff
- Gatekeeper 轉交給 Builder 的方式是 **同一個 Spring Boot 內的 UseCase method call**
- 不會在模組之間再打一層 HTTP
- Controller 不負責判斷輸出格式的業務邏輯，只負責把 Builder / Output 產出的結果包成統一 JSON response

## Dependencies
- Builder UseCase（同程序內呼叫）

## Notes
- 因部署於公司內部地端，正式登入驗證目前先不做
- 但 client IP 解析與驗證插點要先保留，未來若需要做內網 IP 白名單限制可直接接上
- Gatekeeper 目前只負責接收、驗證、轉交多附件；**不做附件內容解析，也不做附件落地保存**
- 初期附件採用 passthrough 模式，直接交由後續 AI 流程使用，目標體驗接近直接把文件 / 圖片貼給 GPT 對話
- 未來可擴充為 OAuth2 / LDAP 等企業級驗證
- 目前已正式放行兩個 scenario：
  - `group=1`, `type=1`
  - `group=2`, `type=2`
