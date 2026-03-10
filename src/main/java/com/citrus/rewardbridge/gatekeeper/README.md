# Gatekeeper Module - PRD

## Overview
Gatekeeper 是系統的 HTTP 入口與驗證預留層，負責接收 internal copilot 的 consult 請求，先做基礎檢查，再將合法請求轉發至 Builder。

初期不實作正式登入驗證，但必須先把未來驗證插點留好，尤其是 **client IP 驗證**。

## Responsibilities
- 提供 consult API 入口：`POST /api/consult`
- 接收 internal user 的文字提問與上傳的多個附件
- 從 HTTP request 解析 client IP，作為未來驗證依據
- 做基礎 guard 檢查：
  - builderId 是否存在且 active（查詢 `rb_builder_config`）
  - outputFormat 若有傳入，格式是否支援
  - 檔案格式是否允許
  - 多附件是否可正確接收
  - client IP 是否可取得
- 預留未來驗證插點：
  - IP allowlist / blocklist
  - token / API key
  - rate limiting
- 將驗證通過的請求（text + 多附件 + builderId + outputFormat + clientIp）傳遞給 Builder

## Input Format
前端送進來的請求包含：
- **builderId**: 選擇的 builder 配方代碼（必填）
- **text**: 使用者的文字提問（選填）
- **files**: 上傳的附件列表（可選，支援 PDF / Word / Image，初期直接原樣交給模型）
- **outputFormat**: 可選欄位。若該 builder 需要附帶檔案，表示希望的檔案格式，例如 `markdown` / `xlsx`

初期 API contract 為 `multipart/form-data`：

```text
POST /api/consult
Content-Type: multipart/form-data

builderId=1
text=<使用者問題文字>
outputFormat=markdown
files=<optional attachment 1>
files=<optional attachment 2>
```

支援格式：
- 文件：`pdf` / `doc` / `docx`
- 圖片：`jpg` / `jpeg` / `png` / `webp` / `gif` / `bmp`

### Server-side Derived Input
- **clientIp**: 不由前端直接提供，由後端從 request header / remote address 解析並注入後續流程

### Builder Config 驗證流程
1. Gatekeeper 收到 `builderId` 後，查詢 `rb_builder_config` 表
2. 若 `builderId` 不存在 → 回 400（Bad Request）
3. 若 `active = false` → 回 403（Forbidden）
4. 驗證通過後，將 builderId 與必要驗證結果傳給 Builder

### Current Supported Builders
- `builderId=1`：產品經理 / PM 工時估算與建議（不附帶檔案）
- `builderId=2`：測試團隊 / QA 冒煙測試文件產生（預設附帶 xlsx）
- `outputFormat = markdown | xlsx`

## Scope
- **In Scope**: API 入口、client IP 解析、builderId 驗證、檔案格式驗證、附件 passthrough
- **Out of Scope**: 業務邏輯、prompt 組裝、AI 呼叫、檔案內容解析

## Interface
- **Input**: User HTTP Request（text + files[] + builderId + outputFormat）
- **Output**:
  - 驗證通過 → 轉發至 Builder UseCase
  - 驗證失敗 → 回傳統一錯誤格式
  - Builder 成功處理後 → 永遠回傳 JSON；其中可能包含可下載檔案的 base64 payload

### Current Attachment Policy
- 多附件初期採 **直接原樣交給模型**
- 不做文字抽取 fallback、OCR fallback、附件落地保存
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
- Controller 不負責判斷輸出格式的業務邏輯，只負責把 Builder / Output 產出的結果包成統一 JSON response

## Dependencies
- BuilderConfigRepository（查詢 `rb_builder_config`，放在 common 模組）
- Builder UseCase（同程序內呼叫）

## Notes
- 因部署於公司內部地端，正式登入驗證目前先不做
- 但 client IP 解析與驗證插點要先保留
- Gatekeeper 目前只負責接收、驗證、轉交多附件；**不做附件內容解析，也不做附件落地保存**
- 初期附件採用 passthrough 模式，目標體驗接近直接把文件 / 圖片貼給 GPT 對話
