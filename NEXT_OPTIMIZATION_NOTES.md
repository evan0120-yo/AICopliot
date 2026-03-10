# Next Optimization Notes

這份文件用來記錄目前專案中「已知但尚未處理」的剩餘優化點。
2026-03-10 已完成 Source / RAG 架構重構的文件設計，程式碼尚未改動。

## 1. 目前已完成的收斂

### 文件層
- `PLAN.md` 已改為 `builderId` 驅動的 internal copilot platform 敘事
- `DEVELOPMENT.md` 已更新 package structure、entity schema、data flow
- 所有 module README 已更新為新架構（builderId + Source 主體 + RAG 補充）
- 新資料模型已定義完整（rb_builder_config、rb_source_type、rb_source、rb_rag_supplement）

### 架構設計層
- 前端路由 key 從 `group + type` 改為 `builderId`
- `group` 降級為 Builder Config 上的歸屬標籤
- Source 升格為 prompt 組裝主體
- RAG 降為 Source 的補充資料
- Source Strategy Pattern 移除，改為依 builderId 統一查詢
- Builder Override Factory 設計完成
- 向量檢索擴充路徑已設計（per-source 粒度，retrieval_mode 欄位）

## 2. 目前仍需處理的項目

### 2.1 程式碼重構（最高優先）

程式碼仍是舊架構，需依新文件進行重構：

- **Phase 1：資料層重建**
  - 新增 BuilderConfigEntity + Repository（放 common）
  - 新增 SourceTypeEntity + Repository
  - 新增 SourceEntity + Repository
  - 新增 RagSupplementEntity + Repository
  - 重寫 initData/Local.java

- **Phase 2：Source / RAG 模組重寫**
  - SourceQueryService 改為依 builderId 查詢 + join source_type 排序
  - RagQueryService 改為依 sourceId 查詢
  - 刪除舊 entity / repository / strategy

- **Phase 3：Builder 模組重寫**
  - BuilderCommandUseCase 主流程重寫
  - BuilderCommandService prompt 組裝改為 source 排序拼接
  - 新增 BuilderOverrideFactory

- **Phase 4：Gatekeeper / Output 適配**
  - ConsultRequest 改 builderId
  - ConsultGuardService 改驗 builderId（查 rb_builder_config）
  - OutputScenarioPolicyResolver 改讀 BuilderConfigEntity

- **Phase 5：清理**
  - 刪除 ConsultScenario / ConsultGroupCode / ConsultTypeCode enum
  - 刪除舊 Source 三張表 entity / repository
  - 刪除舊 RagDocumentEntity / RagDocumentRepository
  - 刪除 Source strategy 相關類別

- **Phase 6：驗證**
  - `mvnw -DskipTests package` 確認編譯通過

### 2.2 Output renderer 目前是通用版

目前正式支援 markdown / xlsx，但 renderer 仍偏通用。

後續建議：
- 針對不同 builder 建 template provider
- 例如 builderId=2 的 XLSX 做成正式的測試案例表格格式

### 2.3 prompt 規則仍可再精煉

目前 initData/Local.java 的 prompt 內容是 MVP 版。實打 OpenAI API 後，仍可能需要調整：
- STEP1 誤殺率
- 小功能拆解粒度
- 工時原因表達方式
- PM 可讀性

### 2.4 尚未實打完整 API 驗證

目前 `.\mvnw.cmd -DskipTests package` 可成功，但仍缺：
- 真實 OpenAI API 流程驗證
- 各 builder 的實際回覆品質確認
- 自動化測試

## 3. 處理優先順序建議

1. 先完成 2.1 程式碼重構（Phase 1-6）
2. 再把 builderId=2 的 Output template 做成真正可用的測試案例格式
3. 再實打 OpenAI API 驗證回覆品質
4. 再補自動化測試
5. 最後調整 prompt 與 Source baseline

## 4. 已刪除或取代的舊設計

以下舊設計已在本次重構中被取代，後續 AI / 工程師接手時不應再使用：

| 舊設計 | 取代為 |
|--------|--------|
| `ConsultScenario` enum | `rb_builder_config` DB 表 |
| `ConsultGroupCode` enum | `BuilderConfigEntity.groupLabel` 字串 |
| `ConsultTypeCode` enum | 不再需要，由 builderId 取代 |
| `SourceScenarioConfigEntity` | `rb_builder_config.description` |
| `SourceReferenceItemEntity` | `rb_source` 的 prompt 片段 |
| `SourceRagMappingEntity` | `rb_source.needs_rag_supplement` + `rb_rag_supplement.source_id` |
| `RagDocumentEntity` | `rb_rag_supplement` |
| Source Strategy Pattern | 依 builderId 統一查詢 |
| `group + type` 路由 | `builderId` 路由 |
