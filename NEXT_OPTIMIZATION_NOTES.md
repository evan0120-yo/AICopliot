# Next Optimization Notes

這份文件用來記錄目前專案中「已知但尚未處理」的剩餘優化點。
2026-03-09 已完成一輪大範圍文件同步與主流程擴充，因此舊版文件不同步項目已大多收斂。

## 1. 目前已完成的收斂

- `PLAN.md` 已改成 internal copilot platform 敘事
- `group + type + outputFormat` 已進入主流程 request contract
- `output` module 已建立，並接入 `Gatekeeper -> Builder -> Output`
- consult 成功回應已收斂為「永遠回 JSON + optional base64 file」
- `AIClient` README 已統一改為 `Responses API`
- `Gatekeeper` README 已移除「Builder 尚未實作」舊描述
- `RAG` README 已移除「PM 單次附件會進 RAG」的矛盾描述
- `DEVELOPMENT.md` 已補上：
  - `output/`
  - `common/dto`
  - `initData/Local.java`
  - 顯式 virtual-thread executor 的現況
  - 新版 `group + type + outputFormat` data flow

## 2. 目前仍值得處理的項目

### 2.1 Output renderer 目前是通用版，還不是場景模板版

目前正式支援的輸出已收斂為：
- `markdown`
- `xlsx`

但目前仍有兩個限制：
- `group=1,type=1` 仍只回文字，不附帶檔案
- `group=2,type=2` 雖已正式附帶 `xlsx`，但 renderer 目前仍偏通用 line-based 輸出，不是測試案例模板版

這足夠作為平台骨架，但還不是最終體驗。

後續建議：
- 針對不同 `group + type` 建 template provider
- 例如：
  - `group=1,type=1`：段落 + 功能拆解
  - `group=2,type=2`：測試案例表格、前置條件、預期結果
### 2.2 缺一份「目前真實系統狀態」摘要文件

目前真實狀態主要分散在：
- `PLAN.md`
- `NEXT_OPTIMIZATION_NOTES.md`
- 各 module README

後續建議：
- 在 root 補一份簡短狀態文件，例如 `CURRENT_STATUS.md`
- 建議內容：
- 目前已支援的 scenario code
  - 目前已支援的 output formats
  - 啟動前必要條件
  - 真實 request contract
  - 尚未完成的能力

### 2.3 prompt 規則仍可再精煉

目前 `initData/Local.java` 已塞入：
- PM estimate 規則
- QA smoke test 規則
- source reference items
- Source→RAG mapping

這已足夠跑 MVP，但之後若實際打 OpenAI API，仍可能需要再優化：
- STEP1 誤殺率
- 小功能拆解粒度
- 工時原因表達方式
- 縮工建議的穩定性
- PM 可讀性

後續建議：
- 實打 API 後，把真實回覆案例回灌進 prompt 調整流程

### 2.4 Source 的示意資料仍偏 MVP baseline

目前 Source 的資料是 MVP seed：
- 需求拆解與確認
- 主流程功能開發
- 串接與驗證
- 風險處理與補充調整

後續建議：
- 改成更接近 Rewards 真實情境的 feature estimate baseline
- 例如：
  - 活動規則設定
  - 任務判斷
  - 即時驗證
  - 獎勵發放
  - 後台設定

### 2.5 尚未實打完整 API 驗證與測試

目前 `.\mvnw.cmd -DskipTests package` 已可成功，但仍缺：
- 真實 OpenAI API 流程驗證
- `group=2,type=2` 的實際回覆品質確認
- 自動化測試

## 3. 處理優先順序建議

若下一步要繼續推進，建議順序：

1. 先把 `group=2,type=2` 的 Output template 做成真正可用的測試案例格式
2. 再補 `CURRENT_STATUS.md`
3. 再實打 OpenAI API 看 `group=1,type=1` / `group=2,type=2` 回覆品質
4. 再補自動化測試
5. 最後才調整更深的 prompt 與 Source baseline
