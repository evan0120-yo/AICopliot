# Next Optimization Notes

這份文件只記錄目前程式碼仍存在、但尚未完成的後續項目。

## Security / Validation
- `ConsultGuardService` 仍未實作 IP allowlist / blocklist
- `ConsultGuardService` 目前只檢查副檔名與大小，尚未做 MIME validation

## Persistence
- 專案目前沒有正式 migration SQL
- local/test 依賴 Hibernate `create-drop`
- 若要進入穩定部署，需補 migration 與 seed strategy

## AI / RAG
- `retrieval_mode` 目前只正式支援 `full_context`
- 舊資料若殘留其他 mode，讀取時只會 fallback，不會真正做向量檢索
- `BuilderOverrideFactory` 目前有 `TemplateOverrideStrategy`（佔位符替換）與 `SimpleOverrideStrategy`（全文覆蓋），但尚無更進階的策略（如部分替換、條件式覆蓋等）

## Output
- `XlsxRenderer` 目前只支援：
  - markdown table -> cases/summary sheets
  - plain text -> consult sheet
- 若未來要支援更嚴格的格式化樣式，需再擴充 renderer

## Operations
- OpenAI client 目前沒有額外的 retry、backoff、circuit breaker
- 專案目前沒有觀察到專門的 audit / metrics / tracing 邏輯

## Documentation Baseline
- 文件目前以 Java code 為準
- schema 以 JPA entity 為準，不以舊版設計文件或歷史 migration 假設為準
