# Next Optimization Notes

這份文件記錄目前已確認、但尚未完全程式化完成的新資料模型收斂方向。

## 1. 最新已確認決策

### 1.1 typeCode 全面移除
- `typeCode` 不再是 Builder / Source / Template 的欄位
- `rb_source_type` 整張表移除
- `rb_source.type_id` 移除
- `rb_source_template.type_code` 移除

### 1.2 Source 新排序模型
- 同一個 builder 內，Source 只看單一 `orderNo`
- 每個 builder 固定保留一筆 `systemBlock=true` 的系統安全區塊
- `systemBlock=true` 固定排最前面
- 一般 Source 的 `orderNo` 越小越前面
- save graph 時後端只正規化一般 Source 成 `1..n`

### 1.3 Source 內部組裝規則
- 先 `source.prompts`
- 再依 `rag.orderNo` 把該 Source 旗下的 RAG 接上去

### 1.4 Graph Save 保護規則
- save graph 時保留 `systemBlock=true` 的 Source
- delete/reinsert 只作用在 `systemBlock=false` 的 Source / RAG
- 前端即使回傳了系統區塊，後端也不信任其增刪改排序

### 1.5 Template 新排序模型
- Template 自己新增 `orderNo`
- 只看 template 的單一 `orderNo`
- Template 也不再有 `typeCode`

### 1.6 舊資料處理方式
- 不砍資料
- 先用舊邏輯算出舊 canonical order
- 再重編成新的單一 `orderNo`
- 為每個 builder 補上系統安全區塊

## 2. 仍需完成的程式碼調整

### Phase A：Schema
- 移除 `rb_source_type`
- 移除 `rb_source.type_id`
- `rb_source` 新增 `system_block`
- `rb_source_template` 新增 `order_no`
- 移除 `rb_source_template.type_code`

### Phase B：Entity / Repository
- 刪除 `SourceTypeEntity`
- 刪除 `SourceTypeRepository`
- `SourceRepository` 改成純 `order_no` 排序
- graph save 的刪除流程排除 `system_block=true`
- `SourceTemplateRepository` 改成純 `order_no` 排序

### Phase C：Builder Graph Command / Query
- request / response DTO 移除 `typeCode`
- graph DTO 新增 `systemBlock`
- save graph 時對一般 source / rag 做 `1..n` 正規化
- query graph 時回傳完整排序模型，包含系統安全區塊

### Phase D：Consult 主流程
- Builder consult 改成只看 `source.orderNo`
- prompt 組裝改成：
  - system safety source
  - source.prompts
  - source.rag by orderNo
- 保留 framework tail 作為 code-level guard

### Phase E：Init Data / Migration
- 將舊 Source 與 Template 依舊 canonical order 重編新 `orderNo`
- 清除 seed 中所有 `PINNED/CHECK/CONTENT` 概念
- 每個 builder seed 一筆系統安全區塊

### Phase F：Frontend 對齊
- Graph UI 顯示 `systemBlock=true` 的唯讀卡片
- 隱藏系統區塊的刪除 / 排序 / 另存範本操作
- graph JSON 帶出 `systemBlock`
- Template library 不處理系統區塊

## 3. 後續但尚未現在做
- admin 編輯系統安全區塊
- 角色權限控制
- `tags`
- Template / Source tag 查詢
- tag-based 團隊規範
