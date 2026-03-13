# Gatekeeper Module

## Overview
Gatekeeper 是目前系統的 HTTP 入口。它負責接收 consult 請求、做基礎驗證、解析 client IP，然後把請求交給 Builder。

## Current Endpoints
- `GET /api/builders`
- `POST /api/consult`

## Responsibilities
- 回傳 active builders 給前端下拉選單
- 接收 `multipart/form-data` consult 請求
- 驗證 `builderId`
- 驗證 `outputFormat`
- 驗證檔案數量、單檔大小、總大小與副檔名
- 解析 client IP
- 將合法請求轉交給 Builder use case

## Request Contract

### `GET /api/builders`
回傳 active builders，依 `builderId ASC` 排序。

每筆資料目前包含：
- `builderId`
- `builderCode`
- `groupKey`
- `groupLabel`
- `name`
- `description`
- `includeFile`
- `defaultOutputFormat`

### `POST /api/consult`
`Content-Type: multipart/form-data`

欄位：
- `builderId` required
- `text` optional
- `outputFormat` optional
- `files` optional, multiple

支援副檔名：
- document: `pdf`, `doc`, `docx`
- image: `jpg`, `jpeg`, `png`, `webp`, `gif`, `bmp`

## Validation Rules
- `builderId` 必填
- builder 必須存在於 `rb_builder_config`
- builder 必須為 `active=true`
- `outputFormat` 若提供，必須是 `markdown` 或 `xlsx`
- client IP 必須可解析
- 實際上傳檔案數不可超過 `rewardbridge.consult.max-files`
- 單檔大小不可超過 `rewardbridge.consult.max-file-size-bytes`
- 總大小不可超過 `rewardbridge.consult.max-total-size-bytes`

## Client IP Resolution
解析順序：
1. `X-Forwarded-For` 第一個 IP
2. `X-Real-IP`
3. `HttpServletRequest#getRemoteAddr()`

## Response Contract
Gatekeeper controller 最終回傳：

```json
{
  "success": true,
  "data": {
    "status": true,
    "statusAns": "",
    "response": "AI response text",
    "file": null
  },
  "error": null
}
```

錯誤時回：

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "BUILDER_NOT_FOUND",
    "message": "Requested builder does not exist."
  }
}
```

## Notes
- Gatekeeper 不做附件內容解析
- Gatekeeper 不做附件落地保存
- 目前仍未實作 IP allowlist / blocklist
- 目前仍未實作 MIME validation
