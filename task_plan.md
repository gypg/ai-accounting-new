# Task Plan: AI 管家借鉴点落地任务清单

## Goal
把 EasyAccounts 中最值得借鉴的 AI 管家思路，转成当前 Android 项目的分阶段、可执行开发任务清单，并明确优先级、依赖关系与完成标准。

## Current Phase
Phase 5

## Phases

### Phase 1: Requirements & Discovery
- [x] 理解用户诉求：把借鉴点转成具体开发任务清单
- [x] 识别当前代码中的映射位置与现状
- [x] 将研究结论写入 findings.md
- **Status:** complete

### Phase 2: Planning & Prioritization
- [x] 定义任务分组与优先级
- [x] 标注每项任务对应代码位置
- [x] 给出依赖关系与验收标准
- **Status:** complete

### Phase 3: Delivery Structure
- [x] 产出按优先级排序的任务结构
- [x] 输出面向用户的具体任务清单
- [x] 区分近期可做与中期优化项
- **Status:** complete

### Phase 4: Verification
- [x] 校对任务清单与已有代码现状一致
- [x] 校对与 findings.md / docs 的结论一致
- [x] 确保没有把外围能力误排进高优先级
- **Status:** complete

### Phase 5: Delivery
- [x] 向用户交付任务清单
- [x] 明确推荐执行顺序
- [x] 若用户需要，继续转成模块计划
- **Status:** complete

## Prioritized Delivery Checklist

### 近期优先（直接服务当前 AI 管家主线）
1. **AI 留痕与可追溯入口**
   - 目标：让 AI 创建 / 修改 / 删除 / 自动补建动作都可被筛选、查看与回溯。
   - 当前映射位置：`AIAssistantActionExecutor.kt`、`AILocalProcessor.kt`、交易/账户/分类持久化链路。
   - 最小任务：
     - 给 AI 生成交易补统一来源标记；
     - 给一次 AI 执行链生成可串联的 trace id；
     - 在明细页或编辑页给出最小可见来源入口。
   - 验收标准：用户能看出哪些记录来自 AI，且出现误记账时能顺着 trace 回看执行链。

2. **工具语义显式化（typed action / typed entity reference）**
   - 目标：减少执行层对远程 JSON 的二次猜测，把动作、查询目标、实体引用提升成显式合同。
   - 当前映射位置：`Butler.kt`、`AIAssistantRemoteResponseInterpreter.kt`、`AIAssistantActionExecutor.kt`。
   - 最小任务：
     - 统一 action envelope；
     - 把 query / create / add_transaction 收口为 typed action；
     - 把 account/category 引用收口为 typed entity reference。
   - 验收标准：解释器输出的执行语义可直接被 executor 消费，旧 payload 仍兼容，不再依赖多轮字段猜测。

3. **普通聊天与执行链语义边界收口**
   - 目标：让 AI 助手稳定表现为“可聊天的管家”，同时避免 reminder / 备忘 / 说明性文本误执行。
   - 当前映射位置：`AIReasoningEngine.kt`、`AILocalProcessor.kt`、`AIAssistantRemoteResponseInterpreter.kt`。
   - 最小任务：
     - 收紧非即时记账表达的误判边界；
     - 保持聊天 fallback 的人格化主路径；
     - 保持 explanatory JSON 不直接执行。
   - 验收标准：聊天、提醒、说明性文本与可执行动作边界稳定，回归测试能覆盖典型误判输入。

4. **共享实体解析与补建编排**
   - 目标：统一本地 / 远程交易执行时的账户、分类解析与缺失补建逻辑。
   - 当前映射位置：`AILocalProcessor.kt`、`AIAssistantActionExecutor.kt`、共享 resolver 层。
   - 最小任务：
     - 抽共享 resolver；
     - 统一按 ID 优先命中、名称匹配、缺失补建、失败反馈；
     - 保持本地 fallback 与远程显式动作的安全边界差异。
   - 验收标准：本地 / 远程记账在实体解析上的行为一致，失败时能明确告诉用户哪个实体出了问题。

### 中期优化（主链稳定后再做）
5. **人格配置统一收口**
   - 目标：把 Butler 人设、身份确认、普通聊天风格收口到单一配置源。
   - 当前映射位置：`Butler.kt`、`IdentityConfirmationDetector.kt`、`AIReasoningEngine.kt`、`AILocalProcessor.kt`。
   - 验收标准：新增/调整人格时，不需要同时改多处 prompt / fallback / detector 逻辑。

6. **先查再执行的产品化编排**
   - 目标：把“查询上下文 → 决定动作 → 执行动作 → 汇总反馈”做成更显式的产品链路。
   - 当前映射位置：`AIAssistantActionExecutor.kt`、查询型 typed action、后续 coordinator / orchestrator。
   - 验收标准：复杂动作前能显式查询上下文，减少误补建、误入账与错误归因。

### 当前不应排入高优先级
- MCP/外围平台能力接入
- 只做展示层的 AI 设置页扩展
- 直接照搬 EasyAccounts OCR 方案

## Recommended Execution Order
1. AI 留痕与可追溯入口
2. 工具语义显式化
3. 普通聊天与执行链语义边界收口
4. 共享实体解析与补建编排
5. 人格配置统一收口
6. 先查再执行的产品化编排

## Key Questions
1. 哪些借鉴点最适合当前 AI 管家主线，而不是偏外围能力？
2. 每个借鉴点映射到当前代码后，最小可执行任务是什么？
3. 哪些任务应该先做，才能最大化提升聊天稳定性、执行稳定性与可追溯性？

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| 第一梯队先做 AI 留痕与工具语义显式化 | 直接提升可信度、可追溯性与执行稳定性，改动相对可控 |
| 第二梯队再做人格统一收口与先查再执行编排 | 价值高，但涉及链路更广，适合在当前主链稳定后推进 |
| 本轮只做任务清单规划，不直接进入实现 | 当前用户要的是优先级任务清单 |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
| `ls` 在 Bash 环境不可用导致第一次 catchup 并行失败 | 1 | 改为单独运行 `session-catchup.py` |
| 发现项目根目录已有旧 planning 文件，与当前任务不一致 | 1 | 读取后按当前任务覆盖为新的规划上下文 |

## Notes
- 当前任务的文件式规划已收尾，剩余输出可直接基于本文件继续拆模块。
- 外部灵感结论已经写入 `findings.md` 与开发文档，不要重复做同一轮研究。
- 若继续推进实现，应按“AI 留痕 → 工具语义 → 语义边界 → 共享编排”的顺序进入新模块。
