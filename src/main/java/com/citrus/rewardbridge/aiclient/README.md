# AIClient Module - PRD

## Overview
AIClient 模組負責與所有外部 AI 模型的通訊，作為系統與 AI 服務之間的統一抽象層。目前主要對接 OpenAI Responses API，未來可擴充至其他模型。

初期主路線以 **Responses API** 為主，因為需要同時支援文字、圖片、與附件直送模型。

## Responsibilities
- 封裝 AI API 呼叫（目前為 OpenAI Responses API）
- 管理 API key、rate limiting、retry 策略
- 統一 request/response 格式，屏蔽不同 AI 供應商的差異
- 處理 API 錯誤
- 支援將 PM 傳入的文字、文件、圖片直接作為模型輸入材料送出

## Scope
- **In Scope**: Responses API 呼叫、錯誤處理、retry、structured response 解析
- **Out of Scope**: prompt 組裝（Builder 負責）、業務邏輯

## Interface
- **Input**:
  - 組裝完成的 prompt（來自 Builder）
  - PM 單次 consult 的原始附件（文件 / 圖片，直接 passthrough）
- **Output**: AI 模型的結構化回應內容（AiConsultResponse）

## Dependencies
- OpenAI Responses API（初期）
- HTTP Client（RestClient / WebClient）

## Factory Pattern
- 採用工廠模式抽象 AI 供應商
- 目前實作：OpenAI Responses API
- 未來可擴充：Anthropic Claude、Google Gemini、Local LLM 等

## Current Supported Inputs
- text input
- image input
- file input

## Future: Embedding API
- 初期不需要 Embedding API（RAG 採用 Full-Context 模式）
- 未來 RAG 的 `retrieval_mode` 切換為 `vector_search` 時，再新增 Embedding API 呼叫功能
- 工廠模式已預留擴充空間

## Failure Handling
若附件串入模型失敗、或模型 / Responses API 不接受附件格式：

```json
{
  "status": false,
  "statusAns": "串入檔案格式錯誤",
  "response": ""
}
```

- 不做 fallback 文字抽取
- 不自動切換成其他較低可信度路線

## Notes
- API key 透過環境變數或 application properties 管理，不寫死在程式碼中
- 初期主路線採用 GPT Responses API
- PM 上傳的附件不做落地保存，僅作為單次請求內容送給模型
- 目標體驗接近「直接把文字、文件、圖片貼給 GPT 對話」
- AIClient 不受 Source / RAG 架構重構影響，介面維持不變
