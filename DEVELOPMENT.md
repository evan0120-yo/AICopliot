# RewardBridge - Backend Development Guide

## 1. 架構

```text
Controller -> UseCase -> Service -> Repository
```

規則不變：
- Controller 只呼叫 UseCase
- UseCase 可跨模組協作
- Service 不跨模組
- Repository 只做資料存取

## 2. 最新資料模型基準

### Builder
- BuilderConfigEntity

### Source
- SourceEntity 不再有 `typeId`
- Source 只保留：
  - `builderId`
  - `prompts`
  - `orderNo`
  - `systemBlock`
  - `needsRagSupplement`
  - `copiedFromTemplateId`

### RAG
- RagSupplementEntity 維持
- 只依 `sourceId + orderNo` 工作

### Template
- SourceTemplateEntity 不再有 `typeCode`
- 新增 `orderNo`
- RagTemplateEntity 維持 `orderNo`

## 3. 排序規則

### Source
- `systemBlock=true` 的 Source 固定排最前
- 建議保留 `orderNo = 0`
- 非系統 Source 於同 builder 內唯一
- 非系統 Source 儲存前正規化為 `1..n`
- 查詢時 `ORDER BY source.order_no, source_id`

### RAG
- 同 source 內唯一
- 後端儲存前正規化為 `1..n`
- 查詢時 `ORDER BY rag.order_no, rag_id`

### Template
- Template 自己有 `orderNo`
- 同 library 顯示只看 template.orderNo

## 4. Prompt Assembly

Builder 組 prompt 的固定規則：

```text
for source in sources(orderNo asc):
  append source.prompts
  for rag in source.rags(orderNo asc):
    append rag.content
```

說明：
- 第一筆通常是 `systemBlock=true` 的系統安全區塊
- 後端仍保留 code-level framework tail 作為最後保底
- `systemBlock` 讓 Graph UI 與實際 prompt 順序更接近

## 5. Graph / Template API 開發準則

### Graph API
- `GET /api/admin/builders/{builderId}/graph`
- `PUT /api/admin/builders/{builderId}/graph`

request / response 都不再有 `typeCode`

Graph response 需帶：
- `systemBlock`
- `orderNo`
- `prompts`
- `rag[]`
- template metadata

saveGraph 準則：
- 保留 `systemBlock=true` 的 Source
- 只刪除並重建 `systemBlock=false` 的 Source / RAG
- 不信任前端對系統區塊的增刪改排序

### Template API
- `GET /api/admin/builders/{builderId}/templates`
- `GET /api/admin/templates`
- `POST /api/admin/templates`
- `PUT /api/admin/templates/{templateId}`
- `DELETE /api/admin/templates/{templateId}`

request / response 要有：
- `orderNo`
- `prompts`
- `rag[]`
- `groupKey`
- template metadata

## 6. Migration 準則

### Source 舊資料
1. 先依舊排序邏輯查出 canonical order
2. 重編非系統 Source 的 `orderNo = 1..n`
3. 補一筆 `systemBlock=true` 的系統安全區塊
4. 移除 `type_id`

### Template 舊資料
1. 先依舊邏輯查出 canonical order
2. 重編 `orderNo = 1..n`
3. 移除 `type_code`

## 7. 文件與程式碼一致性要求

任何新程式碼都必須符合：
- 不新增 `typeCode`
- 不依賴 `rb_source_type`
- 不在 query 中用 type 排序
- 不在前端或後端用 `PINNED/CHECK/CONTENT`
- `systemBlock` 目前只代表系統安全區塊
- 權限行為不要先存成多個布林欄位，先由角色 + `systemBlock` 推導

## 8. 未來但現在不做
- admin 編輯系統安全區塊
- 角色權限控制
- `tags`
- tag 搜尋
- tag-based 團隊規範

等 `systemBlock` 模型穩定後，再擴充權限控制。
