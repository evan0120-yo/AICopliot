# AIClient Module

## Overview
AIClient 模組目前負責與 OpenAI Responses API 溝通。它接收 Builder 組好的 instructions、user text 與附件，並回傳結構化的 `AiConsultResponse`。

## Current Responsibilities
- 取得 `OpenAIClient`
- 將 user text 正規化
- 將附件上傳到 OpenAI files API
- 根據副檔名把附件轉成 file input 或 image input
- 呼叫 Responses API
- 將 structured output 解析成 `AiConsultResponse`
- 將 OpenAI / attachment 失敗轉成 `BusinessException`

## Current Input
- `model`
- `text`
- `instructions`
- `attachments`

## Current Output

```json
{
  "status": true,
  "statusAns": "",
  "response": "..."
}
```

之後會被包成 `ConsultBusinessResponse` 傳回 Builder。

## Attachment Handling
目前行為：
- 空附件會被略過
- 圖片副檔名會走 `VISION`
- 其他支援文件會走 `USER_DATA`
- 每個附件都會先寫入 temp file，再上傳到 OpenAI
- 上傳完成後會刪除 temp file

## Supported Attachment Types
- image: `jpg`, `jpeg`, `png`, `webp`, `gif`, `bmp`
- file: `pdf`, `doc`, `docx`

## Error Mapping
目前已處理：
- OpenAI client 未配置
- OpenAI auth failure
- attachment transfer failure
- attachment upload rejected
- OpenAI empty structured output
- generic OpenAI analysis failure

## Current Non-Goals
目前 code 尚未實作：
- provider factory
- retry / backoff
- rate limiting
- multi-provider routing
- embedding / vector retrieval

## Notes
- 附件不做文字抽取 fallback
- 附件不落地保存
- 目前唯一對接供應商是 OpenAI
