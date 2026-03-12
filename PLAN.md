# RewardBridge - Backend Plan

## 1. 產品定位

RewardBridge Backend 是公司內部的 AI Copilot 編排平台。核心擴充單位是：

- `builderId`
- `source blocks`
- `rag supplements`
- `template library`

## 2. 最新核心決策

### 2.1 typeCode 完全移除
- `typeCode` 不再存在於 Builder / Source / Template
- `rb_source_type` 移除
- `rb_source.type_id` 移除
- `rb_source_template.type_code` 移除

### 2.2 Source 排序新規則
- 同一 builder 內，Source 只看單一 `orderNo`
- 每個 builder 固定保留一筆 `systemBlock=true` 的系統安全區塊
- `systemBlock=true` 固定排最前面
- 一般 Source 的 `orderNo` 越小越前
- save graph 時後端只正規化一般 Source 成 `1..n`

### 2.3 Prompt 組裝規則
對每個 Source：
1. 先 `source.prompts`
2. 再依 `rag.orderNo` 接上該 source 底下所有 RAG prompts

對整個 Builder：
1. `systemBlock=true`
2. `source(order=1)`
3. `source(order=2)`
4. `source(order=3)`

### 2.4 Template 新規則
- Template 也不再有 `typeCode`
- Template 自己新增 `orderNo`
- Template 的 RAG 一樣只看 `rag.orderNo`

### 2.5 舊資料處理
- 不砍舊資料
- 依舊邏輯先算出目前實際順序
- 再重編成新的單一 `orderNo`
- 補一筆系統安全區塊

## 3. Data Model

### 3.1 rb_builder_config

| 欄位 | 說明 |
|------|------|
| builder_id | PK |
| builder_code | 唯一識別 |
| group_key | 穩定群組鍵 |
| group_label | 顯示名稱 |
| name | Builder 名稱 |
| description | 對外說明 |
| include_file | 是否附帶檔案輸出 |
| default_output_format | 預設輸出格式 |
| file_prefix | 檔名前綴 |
| active | 是否啟用 |

### 3.2 rb_source

| 欄位 | 說明 |
|------|------|
| source_id | PK |
| builder_id | FK → rb_builder_config |
| prompts | Source block 的 prompts |
| order_no | 同 builder 內全域排序 |
| system_block | 是否為系統安全區塊 |
| needs_rag_supplement | 是否需要 RAG |
| copied_from_template_id | 來源 template（nullable） |

### 3.3 rb_rag_supplement

| 欄位 | 說明 |
|------|------|
| rag_id | PK |
| source_id | FK → rb_source |
| rag_type | 子分類 |
| title | 顯示名稱 |
| content | 補充 prompts |
| order_no | 同 source 內排序 |
| overridable | 是否可覆蓋 |
| retrieval_mode | full_context / vector_search |

### 3.4 rb_source_template

| 欄位 | 說明 |
|------|------|
| template_id | PK |
| template_key | 唯一識別 |
| name | 範本名稱 |
| description | 範本說明 |
| group_key | nullable，null=公版 |
| order_no | template library 排序 |
| prompts | 範本 prompts |
| active | 是否啟用 |

### 3.5 rb_rag_template

| 欄位 | 說明 |
|------|------|
| template_rag_id | PK |
| template_id | FK → rb_source_template |
| rag_type | 子分類 |
| title | 顯示名稱 |
| content | 補充 prompts |
| order_no | 同 template 內排序 |
| overridable | 是否可覆蓋 |
| retrieval_mode | full_context / vector_search |

## 4. Consult 主流程

```text
Gatekeeper
  -> Builder
      -> load builder config
      -> load sources by builderId order by source.order_no
      -> for each source load rag by sourceId order by rag.order_no
      -> assemble prompt
      -> AIClient
      -> Output
```

補充：
- 第一筆 Source 預期是 `systemBlock=true` 的系統安全區塊
- `BuilderCommandService` 仍保留 framework tail 等 code-level guard

## 5. Graph JSON 契約

### Save / Load Shape

```json
{
  "builder": {
    "builderCode": "qa-smoke-doc",
    "groupKey": "qa",
    "groupLabel": "測試團隊",
    "name": "QA 冒煙測試文件產生",
    "description": "協助 QA 快速產出冒煙測試案例",
    "includeFile": true,
    "defaultOutputFormat": "xlsx",
    "filePrefix": "qa-smoke-doc",
    "active": true
  },
  "sources": [
    {
      "orderNo": 0,
      "systemBlock": true,
      "prompts": "你現在負責 RewardBridge consult flow 的系統安全檢查。",
      "rag": []
    },
    {
      "templateId": 1001,
      "templateKey": "opening-check",
      "templateName": "開場驗證範本",
      "templateDescription": "開場驗證範本",
      "templateGroupKey": null,
      "orderNo": 1,
      "systemBlock": false,
      "prompts": "先做驗證",
      "rag": [
        {
          "ragType": "review_focus",
          "title": "檢查重點",
          "content": "...",
          "orderNo": 1,
          "overridable": false,
          "retrievalMode": "full_context"
        }
      ]
    }
  ]
}
```

### Save Rules
- `builder` 採 merge 語意
- 一般 `sources[]` / `rag[]` 採 replace 語意
- 後端以陣列順序正規化一般 `source.orderNo = 1..n`
- 後端以陣列順序正規化每個 source 內的 `rag.orderNo = 1..n`
- `systemBlock=true` 的 Source 由後端保留與保護

## 6. Migration Plan

### 6.1 Source Migration
舊順序：

```text
type.sort_priority -> source.order_no -> source_id
```

新順序：

```text
systemBlock(order=0) -> source.order_no only
```

做法：
1. 先查出舊 canonical order
2. 依該順序重編一般 Source 成 `1..n`
3. 寫回 `rb_source.order_no`
4. 新增 `rb_source.system_block`
5. 為每個 builder 補一筆系統安全區塊
6. 移除 `type_id`

### 6.2 Template Migration
做法：
1. 先查出舊 template canonical order
2. 寫成新的 `template.order_no = 1..n`
3. 移除 `type_code`

## 7. Module Summary

### Gatekeeper
- 驗 builder 是否存在且 active
- 驗檔案規則

### Builder
- 載 builder config
- 載 source
- 載 rag
- 組 prompt
- save/load graph
- save/load/list template

### Source
- 以 block 模型管理 prompts
- 不再有 type 模型
- 維護 `systemBlock`

### RAG
- 提供 source 的補充 prompts
- 只參與 source 內部排序

### AIClient
- 發送 prompt 給模型

### Output
- 根據 builder config 與 outputFormat 渲染輸出

## 8. Backend 開發順序

### Phase 1：Schema
- 移除 `rb_source_type`
- 移除 `rb_source.type_id`
- `rb_source` 新增 `system_block`
- `rb_source_template` 新增 `order_no`
- 移除 `rb_source_template.type_code`

### Phase 2：Entity / Repository
- 刪除 `SourceTypeEntity`
- 刪除 `SourceTypeRepository`
- `SourceRepository` 改用 `order_no`
- graph save 刪除時排除 `system_block=true`
- `SourceTemplateRepository` 改用 `order_no`

### Phase 3：DTO / API
- graph DTO 移除 `typeCode`
- graph DTO 新增 `systemBlock`
- template DTO 移除 `typeCode`
- template DTO 新增 `orderNo`

### Phase 4：Service
- graph save 正規化一般 source / rag order
- consult 改成只看 source.orderNo
- template save/load/list 改成只看 template.orderNo

### Phase 5：InitData
- seed 不再建立 `PINNED/CHECK/CONTENT`
- seed 直接建立單純的 source blocks 與 templates
- 每個 builder 額外 seed 一筆系統安全區塊

### Phase 6：Verification
- `.\mvnw.cmd clean test`
- 自我 code review

## 9. 注意事項
- `systemBlock` 先只代表系統安全區塊
- 之後若要做搜尋或管理語意，應新增 `tags`
- 不要再回頭把 `typeCode` 當分類或排序欄位使用
- 權限控制未完成前，graph UI 一律把 `systemBlock=true` 視為唯讀
