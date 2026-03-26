# AI记账 — 产品开发文档

> 版本：v1.8.3 | 文档日期：2026-03-22 | 文档状态：发布前同步版

---

## 一、项目概述

### 1.1 产品定位

AI记账是一款面向中国大陆个人用户的智能记账 Android 应用。核心差异化在于通过自然语言、语音和图片等方式辅助记账，并逐步将 AI 配置、模型切换、邀请码绑定与网关代理整合为可发布产品能力。

### 1.2 当前工程状态

| 指标 | 当前值 |
|------|--------|
| 应用版本 | v1.8.3 |
| versionCode | 19 |
| applicationId | `com.moneytalk.ai` |
| namespace | `com.example.aiaccounting` |
| minSdk / targetSdk | 26 / 34 |
| JDK | 17 |
| CI 主流程 | Lint → Unit Tests → Build APK → Release |

---

## 二、最近功能与技术变更

### 2.1 v1.8.3 发布准备完成项

1. **应用标识确认**
   - `applicationId` 已确认为正式包名 `com.moneytalk.ai`
   - `namespace` 保持为 `com.example.aiaccounting`

2. **Release 构建链路确认**
   - ProGuard / 资源压缩已启用
   - `assembleRelease` 验证通过
   - Release 签名由环境变量驱动，缺省时回退 debug 签名以保障 CI 可执行

3. **发布策略**
   - 当前采取“先发布 v1.8.3，再基于真实反馈优化”的策略
   - P0 发布准备已完成，P1/P2 优化后置到反馈阶段决策

### 2.2 AI 助手文本消息主链路最新状态

#### 2026-03-26 模块 5 完成：typed entity reference + transfer 语义封口
- 已在模块 5 第一轮 typed action 收口基础上，完成 **typed entity reference + transfer 语义封口** 的第二轮最小收口，目标是把交易动作里仍然并存的 `accountName/accountId/categoryName/categoryId` 进一步提升为统一实体引用模型，并让 `TRANSFER` 不再停留在“能识别但未真正定义执行语义”的半完成状态。
- 本轮改动：
  - `AIAssistantTypedAction.AddTransaction` 已从并列的 `account/category` 名称与 ID 字段，收口为统一 `AIAssistantEntityReference`：当前显式建模了 `accountRef`、`categoryRef` 与可选 `transferAccountRef`。
  - `AIAssistantRemoteResponseInterpreter` 现在会优先解析 `accountRef / categoryRef / transferAccountRef`，同时继续兼容历史 `account/accountId/category/categoryId` 字段，保证旧 payload 不回退。
  - 执行层现在会优先按 typed ref 中的显式 ID 解析实体；即使原始名称字段与 ref 冲突，也以 typed ref 为准，避免 executor 在边界层重新猜语义。
  - 远程 `TRANSFER` 语义已明确封口：当前 `type:"transfer"` 或 `transactionType:"transfer"` 的 payload 会继续进入 typed action 主链，但执行器会返回显式“暂不支持执行转账语义”的失败反馈，而不是假装按普通单账户收支继续执行。
  - 本轮没有仓促补完整 transfer persistence 模型；现阶段选择的是**显式可解析、显式拒绝执行**，以避免写入半成品 transfer 记录并污染余额/trace 语义。
- 本轮新增回归测试：
  - `AIAssistantRemoteResponseInterpreterTest`
    - 覆盖 `accountRef / categoryRef` typed entity reference 解析；
    - 覆盖 `type:"transfer"` 与 `transferAccountRef` 的 typed transfer envelope 解析。
  - `AIAssistantActionExecutorTest`
    - 覆盖 typed ref 与原始名称冲突时优先按 typed ref 的 ID 命中已有实体；
    - 覆盖 transfer action 返回显式拒绝执行文案。
  - `AIAssistantRemoteExecutionHandlerTest`
    - 同步适配新的 `AIAssistantTypedAction.AddTransaction` 引用模型构造。
  - `AIReasoningEngineTest`
    - 复验本地 reasoning 路径对 transfer 文本仍保持 `TransactionType.TRANSFER` 识别，不影响现有识别语义。
- 本轮验证：
  - `./gradlew testDebugUnitTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantRemoteResponseInterpreterTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantActionExecutorTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantRemoteExecutionHandlerTest --tests com.example.aiaccounting.ai.AIReasoningEngineTest` ✅
- 当前状态：模块 5 已完成第二轮收口，**远程动作实体引用已显式 typed 化，TRANSFER 也已从半支持状态收口为“可解析但显式拒绝执行”**；若后续继续这个主模块，下一个自然方向应是补完整 transfer operation / persistence 语义，而不是再回到 executor 内扩散名称+ID 并存字段。

#### 2026-03-26 模块 5 完成：tool registry / typed action 语义显式化第一轮收口
- 已完成当前轮 **模块 5：tool registry / typed action 语义显式化** 的第一轮最小收口，目标是把远程可执行动作从“解释器识别出一段 JSON 字符串，执行器再二次猜字段”升级成更显式的 typed action 边界，同时保持现有 wrapped payload、安全判定与实体解析行为不回退。
- 本轮改动：
  - 新增 `AIAssistantActionEnvelope` / `AIAssistantTypedAction`，把远程可执行语义显式建模为 `AddTransaction / CreateAccount / CreateCategory / Query / Unknown` 五类 typed action；远程 reply 也统一挂到 envelope 上，而不是继续散落在原始 JSON 字符串中。
  - `AIAssistantRemoteResponseInterpreter` 现在会直接把可执行响应解析成 typed envelope，并保留现有 `data / result / payload` wrapper 解包、fenced JSON 提取、脏 `null...null` 清洗和交易请求 fallback 语义。
  - `query_accounts / query_categories / query_transactions` 这类历史 typed action alias 现在会在解释器层统一映射成 `Query(target=...)`，避免 alias 语义继续分散在执行器里做二次归一化。
  - `AIAssistantRemoteExecutionResult.ActionExecutionRequested` 不再携带裸 `rawResponse`，而是直接透传 typed envelope；`AIAssistantRemoteExecutionHandler` 与 `AIAssistantViewModel` 已同步接到新合同。
  - `AIAssistantActionExecutor` 的原始字符串入口现在只负责调用解释器解析 envelope；执行分发则只消费 typed action，不再重复持有一套 JSON 提取 / wrapper 解包 / action type 修补逻辑，执行边界比上一轮更清晰。
  - 当前 `accountId` / `categoryId` 数值 ID 仍会通过 `AITransactionEntityResolver` 优先命中已有实体，typed action 收口没有回退成把数值 ID 当成名称再解析。
  - 同轮顺手修掉了 `AIReasoningEngine` 的一个真实高风险兜底：当同类型默认分类创建失败时，旧链路不再静默回退到任意类型分类，避免本地 reasoning 路径把支出记到收入分类或反之。
- 本轮新增回归测试：
  - `AIAssistantRemoteResponseInterpreterTest`
    - 覆盖 alias 型 query action 会映射到 typed query target；
    - 覆盖 wrapped payload 内部的 `reply + actions` 能同时保留 reply 与 typed action。
  - `AIAssistantActionExecutorTest`
    - 覆盖 raw type `query_transactions` 仍能正确执行；
    - 覆盖 unsupported typed action 会返回友好未知动作提示。
  - `AIAssistantRemoteExecutionHandlerTest`
    - 同步更新为断言 `ActionExecutionRequested(envelope=...)` 新合同，而不是旧 `rawResponse`。
  - `AIReasoningEngineTest`
    - 与 typed action 回归一起复验，确保同轮修复未破坏现有 reasoning 主线。
- 本轮验证：
  - `./gradlew testDebugUnitTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantRemoteResponseInterpreterTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantActionExecutorTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantRemoteExecutionHandlerTest --tests com.example.aiaccounting.ai.AIReasoningEngineTest` ✅
- 当前状态：当前轮模块 5 已完成第一轮 typed action 收口，**远程动作主链已从“传 raw JSON”升级到“传 typed envelope”**；下一轮若继续这个主模块，更适合把账户/分类引用再显式升级成 typed entity reference，并明确处理 `TRANSFER` 这种尚未真正支持的动作语义，而不是回头扩更多 JSON 兼容分支。

#### 2026-03-26 模块 4 第二段完成：AI 留痕展示入口与安全实体解析收口
- 已完成 **模块 4 第二段：AI 留痕展示入口** 的最小收口，目标是在不新增复杂导航的前提下，把上一段已落库的 AI 来源字段真正接到用户可见界面，同时修正编辑流和显式实体解析中的可追溯性风险。
- 本轮改动：
  - `TransactionListViewModel` 新增 `sourceFilter`，并把来源筛选与现有月份 / 日期 / 收支类型 / 排序规则组合执行；当前明细页已支持 `全部 / AI / 手动` 三档最小来源筛选。
  - `TransactionListScreen` 的交易行现在会为 AI 生成记录显示来源 badge：当前已区分 `AI云端` 与 `AI本地`；这样用户在明细页能快速识别哪些账是 AI 直接生成的。
  - `EditTransactionScreen` 新增只读“来源信息”卡片，会展示当前交易的来源类型，并在有值时展示截断后的 `Trace ID`，作为最小可见审计入口。
  - `TransactionViewModel` 的编辑保存链不再直接 new 一个全新 `Transaction` 覆盖旧记录，而是先回读原交易再 `copy(...)` 更新业务字段，从而保留 `aiSourceType` / `aiTraceId` 等留痕元数据，避免用户手动编辑后把 AI 来源洗掉。
  - `AITransactionEntityResolver` 新增“显式实体引用缺失默认失败”语义：当远程动作明确给出账户 ID / 账户名 / 分类 ID / 分类名，但数据库里不存在时，现在默认直接返回失败，而不是偷偷落到默认账户或别的分类上；这样能避免 traceability 上出现“用户点名 A，系统却悄悄记到 B”的错误归因。
  - 本地离线记账流仍保留显式允许 fallback creation 的旧产品语义：只有本地自然语言兜底链会 opt-in 自动补默认账户/分类；远程显式动作链则按更安全的“显式缺失即失败”处理。
  - 同轮顺手修复了 `AIAssistantViewModel` 中重复 cancellation catch 的结构问题，并把远程动作默认补建账户的类型统一收口为稳定的 `CASH`，避免同名默认账户在不同链路上落成不一致类型。
- 本轮新增回归测试：
  - `TransactionListViewModelTest`
    - 覆盖 AI 来源筛选；
    - 覆盖来源筛选与收支类型筛选可组合工作。
  - `AIAssistantActionExecutorTest`
    - 更新显式账户/分类缺失时的安全失败语义断言，要求返回请求实体名与“未找到指定账户/分类”而不是静默补建到别处。
- 本轮验证：
  - `./gradlew testDebugUnitTest --tests com.example.aiaccounting.ui.viewmodel.TransactionListViewModelTest --tests com.example.aiaccounting.ui.viewmodel.TransactionViewModelTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantActionExecutorTest --tests com.example.aiaccounting.ai.AILocalProcessorTest` ✅
- 当前状态：模块 4 现在不仅有底层 trace 数据，也已有最小用户可见入口；**AI 来源可在明细列表筛选、在交易详情查看，并且手动编辑不会再丢失 AI provenance**。下一轮若继续沿 EasyAccounts 借鉴主线，更适合进入 `tool registry / typed action` 语义显式化，而不是继续扩更多 UI 面。

#### 2026-03-26 模块 4 完成：AI 留痕与可追溯标记统一入口
- 已完成当前轮 **模块 4：AI 留痕** 的最小收口，目标是把 AI 创建 / 修改 / 删除 / 记账以及记账过程中的自动补建动作，统一打上可追溯来源标记，后续可用于筛选、审计与问题回溯。
- 本轮改动：
  - `Transaction` 新增 `aiSourceType` 与 `aiTraceId` 两个轻量字段：前者标记来源通道（当前已落 `AI_LOCAL` / `AI_REMOTE` / `MANUAL`），后者用于把同一轮 AI 动作链路串成统一 trace。
  - 新增独立 Room 实体 `AIOperationTrace`，并配套 `AIOperationTraceDao` / `AIOperationTraceRepository`，统一记录 AI 在交易、账户、分类上的 create / update / delete / auto-create-supporting-entity 类动作。
  - `AIOperation` 新增共享 `AITraceContext`，现在远程 action 执行、本地离线记账与共享实体解析器都会显式透传同一个 `traceId`，从而把“缺账户 -> 自动建账户 -> 缺分类 -> 自动建分类 -> 最终入账”串成同一条可追溯链。
  - `AIAssistantActionExecutor` 现会给远程动作标记 `AI_REMOTE`；`AILocalProcessor` 与 `AITransactionEntityResolver` 会给本地记账及自动补建链标记 `AI_LOCAL`；最终入库交易会同时携带轻量来源字段与明细 trace 记录。
  - `AIOperationExecutor` 当前已为交易、账户、分类的新增/更新/删除关键动作统一落 trace；其中交易新增/更新还会回写 `Transaction.aiSourceType` / `Transaction.aiTraceId` 以支持后续 UI 过滤。
  - `AppDatabase` 已升级到 schema version 8，并补齐 `transactions` 的 AI 来源列、`ai_operation_traces` 表、索引，以及升级路径上的 migration；同时顺手修复历史 `MIGRATION_6_7` 中 `ai_permission_logs` 缺失建表的旧隐患，避免旧用户升级时发生 Room schema mismatch。
- 本轮验证：
  - `./gradlew testDebugUnitTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantActionExecutorTest --tests com.example.aiaccounting.ai.AILocalProcessorTest --tests com.example.aiaccounting.data.repository.TransactionRepositoryTest` ✅
  - code reviewer 先后发现 2 个 migration / DI 高风险问题，均已修复后复验通过 ✅
- 当前状态：模块 4 已完成最小可用收口，**AI 交易执行链现在既有交易级轻量来源字段，也有独立审计 trace 表作为统一回溯入口**；下一轮若继续沿 EasyAccounts 借鉴主线，优先适合做“基于这些 trace 的筛选 / 展示入口”或进一步把权限审计与执行留痕对齐，而不是继续扩写更多 trace 字段。

#### 2026-03-26 模块 3 当前轮第二段收口：细粒度失败反馈与 wrapper JSON 变体
- 已完成当前轮 **模块 3 第二段** 收口，目标是继续把远程 action 执行体验拉齐到真实模型输出：一方面让失败反馈能告诉用户“到底哪个账户 / 分类失败了”，另一方面补齐模型常见的 wrapper JSON 返回形式，而不是只认顶层 object / array。
- 本轮改动：
  - `AITransactionEntityResolver` 现在会在解析失败时保留请求实体标识（账户名 / 分类名 / ID），供上层拼装更细粒度失败反馈。
  - `AIAssistantActionExecutor` 在远程记账执行失败时，现在会优先输出 `失败实体 + 失败原因`，例如创建账户失败时带出请求账户名，创建分类失败时带出请求分类名。
  - `AIAssistantRemoteResponseInterpreter` 与 `AIAssistantActionExecutor` 已同步支持对 `data` / `result` / `payload` 三类已知 wrapper key 做保守解包；只有解包后内部仍命中 `action` / `actions` / typed action 标记时，才继续走执行路径。
  - 本轮没有把 wrapper 解包泛化到任意未知字段，仍保持当前安全面：只扩具体已知变体，不扩成任意嵌套 JSON 自动执行。
- 本轮新增回归测试：
  - `AIAssistantRemoteResponseInterpreterTest`
    - 覆盖 `{"data": {...}}` 包裹单个 action；
    - 覆盖 `{"payload": {"actions": [...]}}` 包裹批量 actions。
  - `AIAssistantActionExecutorTest`
    - 覆盖 `{"result": {...}}` 包裹单个 query action；
    - 覆盖 `{"payload": {"actions": [...]}}` 包裹批量 query actions；
    - 覆盖账户/分类解析失败时反馈会带出失败实体与底层错误原因。
- 本轮验证：
  - `./gradlew testDebugUnitTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantRemoteResponseInterpreterTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantActionExecutorTest` ✅
  - `./gradlew testDebugUnitTest --tests com.example.aiaccounting.ai.AILocalProcessorTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantActionExecutorTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantRemoteResponseInterpreterTest` ✅
- 当前状态：模块 3 当前轮两步补强已全部完成，**失败反馈已更细粒度，远程 wrapper JSON 变体也已补齐到解释器与执行器双层**；下一轮不建议继续横向扩 JSON 形态，优先应回到 EasyAccounts 借鉴路线中的下一个主模块。

#### 2026-03-26 模块 3 当前轮收口：本地 / 远程补实体共享编排层
- 已完成当前轮 **模块 3：本地 / 远程补实体共享编排层** 的最小收口，目标是把交易执行链里重复的“查账户 / 查分类 / 缺失补建 / 回查兜底”逻辑统一到一个共享解析层，同时不把交易解析、JSON 解释或用户文案也一起过度抽象。
- 本轮改动：
  - 新增 `AITransactionEntityResolver`，统一承接交易执行时的账户/分类解析、按 ID 优先命中、名称匹配、缺失补建、回查与兜底逻辑。
  - `AIAssistantActionExecutor` 现已改为通过共享解析层处理 `add_transaction` 的账户/分类决策，继续保留远程 JSON 解析、类型归一化、成功文案与 dirty reply 安全面在各自原层。
  - `AILocalProcessor` 的本地记账主链也已改走共享解析层，仍保留本地语义：先 `ensureBasicCategoriesExist()`，账户默认补 `默认账户`，分类先补 `其他收入/其他支出`，必要时再走 `收入/支出` 应急分类。
  - 本轮同时补了两处 review follow-up：
    - 本地记账意图识别恢复为“交易动词 + 金额”的宽松启发式，避免 `买咖啡20` 这类离线记账短句回退成普通聊天；同时仍保留 reminder 类误判保护。
    - `AIAssistantActionExecutor` 的多笔无 `reply` 汇总结果不再出现 `1. 1. ... / 2. 2. ...` 双编号。
- 本轮新增回归测试：
  - `AILocalProcessorTest`
    - 覆盖空账户 + 空分类时会自动补建默认实体后完成本地记账；
    - 覆盖 `买咖啡20` 这类“动词 + 金额”短句仍会进入本地记账链。
  - `AIAssistantActionExecutorTest`
    - 保持远程 numeric `accountId` / `categoryId` 优先命中现有实体；
    - 覆盖 array root / trailing prose JSON 变体；
    - 覆盖多笔 query 汇总结果只保留单层编号。
  - `AIAssistantRemoteResponseInterpreterTest`
    - 保持前后带文案、fenced JSON、array root 等远程 action 变体可稳定识别。
- 本轮验证：
  - `./gradlew testDebugUnitTest --tests com.example.aiaccounting.ai.AILocalProcessorTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantActionExecutorTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantRemoteResponseInterpreterTest` ✅
- 当前状态：当前轮模块 3 已完成最小共享编排收口，**本地 / 远程交易执行的实体解析与补建逻辑已统一到共享层**；下一轮若继续问题3，更适合进入更细粒度失败反馈与真实远程 wrapper JSON 变体扩展，而不是继续扩大共享抽象面。

#### 2026-03-25 产品目标重新对齐（高优先级）
- AI 助手的正式产品目标重新明确为：**可聊天的 AI 管家助手 + 可执行完整记账操作的事务助手**。
- 体验优先级应为：先按当前管家人格稳定承接普通对话、身份询问、闲聊与陪伴式回复，再在同一链路中无缝完成记账、查账、改账、删账、创建账户/分类等操作。
- 因此，当前实现不能退化成只会输出“您可以尝试：查看账户余额 / 分析本月支出 / 记一笔100元的餐饮消费”的命令提示器；普通聊天误判或落入通用 fallback 视为 P0 体验问题。
- 已有代码与既有设计痕迹表明该目标并非新增需求：当前链路已具备 `currentButler.systemPrompt` 注入、人格欢迎语、身份确认回复等基础能力，但这些能力尚未稳定成为默认聊天主路径。

#### 2026-03-25 本轮新暴露问题（P0 待修）
- **问题A：普通聊天/身份询问未稳定按人设回复**
  - 现象：用户输入“你好”“你是谁”“你的底层是什么模型啊”等普通聊天内容时，AI 助手频繁退化为通用 fallback：
    - “抱歉，我不太理解您的意思。您可以尝试说：查看账户余额 / 分析本月支出 / 记一笔100元的餐饮消费”
  - 结论：当前执行路由里，“人格化聊天”没有成为默认兜底主路径，部分消息落入 `UNKNOWN -> RequestClarification` 或本地通用 fallback，导致 AI 助手更像命令解析器，而不是可聊天的 AI 管家。
- **问题B：远程回复存在脏输出污染**
  - 现象：远程 AI 返回过 `nullnull...{json}null` 形式的污染文本，最终被用户直接看到。
  - 结论：当前远程完整性校验与解释器仍主要围绕“纯 JSON / 纯 fenced JSON”设计；动作执行器虽会截取 JSON，但 reply 提取与 UI 展示层没有把脏前后缀彻底隔离，导致污染文本可直接进入会话消息。
- **问题C：多笔记账执行稳定性不足**
  - 现象：远程返回的多笔 `add_transaction` JSON 结构已基本有效，但最终没有稳定完成入账。
  - 结论：空账户场景下当前动作执行依赖按 AI 生成的账户名（如微信 / 支付宝 / 现金 / 信用卡）即时建账并回查，失败路径仍可能导致整笔或多笔记账失败；需要把空账户兜底、逐笔执行反馈与错误可见性补齐。
- **问题D：产品语义与权限链路尚未统一收口**
  - 现状：AI 助手在远程 JSON 动作链路中已拥有接近手动操作的直接执行能力，但该链路未统一走 `AIPermissionExecutor`，说明“可执行全部记账操作”的产品语义已经存在，而“如何统一权限/确认策略”仍待后续收口。

#### 当前阶段新的默认接手点
- 问题3 不应再仅被理解为“结构化 continuation / stage model”技术收尾，后续主线应切回 **AI 助手产品体验收口**：
  1. 普通聊天与身份询问的人格化回复主路径
  2. 远程脏响应清洗与 reply 提取健壮性
  3. 多笔记账 / 空账户场景动作执行稳定性
  4. 远程动作执行与权限控制入口统一（次优先级）
- 下个窗口默认从以上 4 点继续，不回退到已完成的模块2B/2C/2D、4A~4E 或旧的 3D-9/3D-10。

#### 2026-03-25 P0 第一段修复完成：普通聊天 fallback 与脏回复清洗
- 已完成 **问题A + 问题B 的第一轮收口**：
  - `AIReasoningEngine` 现将“你好 / 谢谢 / 再见 / 你是什么模型 / 你的底层是什么模型啊”等典型聊天消息优先识别为 `GENERAL_CONVERSATION`，不再落入 `UNKNOWN -> RequestClarification("抱歉，我不太理解...")`。
  - 本地 `AILocalProcessor` 的普通对话兜底已从命令式帮助模板改为可继续聊天的 AI 管家口吻，避免离线/本地 fallback 时退化成“只会教命令”的机器人。
  - `AIAssistantRemoteResponseInterpreter` 新增对 `nullnull ... null` 这类远程脏包裹的清洗；现在普通文本回复会先清洗再展示，纯 JSON 动作回复也会先清洗再进入 action 判定。
  - 保持现有安全边界不变：**只有纯 JSON / 纯 fenced JSON** 仍会命中 action 执行；像“好的，下面是执行结果：{...}”这类说明性文本仍不会被直接执行，继续走文本/本地 fallback 分支，避免 explanatory JSON 误执行回归。
  - `AIAssistantActionExecutor` 也补了 `reply` 字段的 `null...null` 文本清洗，避免动作成功后把脏 reply 拼回用户可见消息。
- 新增回归测试：
  - `AIAssistantRemoteResponseInterpreterTest`：覆盖 `nullnull{"action":...}null` 仍可识别动作为执行请求，覆盖 `nullnull你好呀，我在呢null` 会清洗为干净文本回复。
  - `AIReasoningEngineTest`：覆盖“你的底层是什么模型啊”会被识别为 `GENERAL_CONVERSATION`，不再触发 rigid clarification。
- 当前状态：**聊天主路径与脏文本展示问题已得到首轮修复**；下一段继续处理问题C（空账户 / 多笔动作 / 逐笔失败可见性）。

#### 2026-03-25 P0 第二段修复完成：空账户与多笔动作执行稳定性
- 已完成 **问题C 的第一轮收口**：
  - `AIAssistantActionExecutor` 现在在执行远程 `actions` 数组时，会为每一项补齐序号结果，统一返回 `1. ... / 2. ...` 形式的逐笔反馈，避免多笔记账时只看到一条模糊总结。
  - 空账户场景下，动作执行现在会优先过滤归档账户；若没有可用账户，会创建 `默认账户` 后再次回查，并在找不到同名账户时继续回落到默认账户/首个可用账户，减少“账户刚创建但回查落空”导致的整笔失败。
  - `AIAssistantActionExecutor` 的日志调用改为 `runCatching` 包装，避免 JVM 单测环境因 `android.util.Log` 未 mock 而掩盖真正执行逻辑。
- 新增回归测试：
  - `AIAssistantActionExecutorTest`
    - 覆盖多笔 action 中“部分成功 + 部分失败”时会保留逐条编号反馈；
    - 覆盖空账户场景会先建默认账户再完成入账。
- 当前状态：**聊天主路径、脏回复展示、空账户建账与多笔逐条反馈** 已完成第一轮收口；后续若继续问题C，应优先补“逐笔失败原因更细粒度展示”与更多真实远程 JSON 变体回归。

#### 2026-03-25 review 修复补充：wrapped JSON / local failure / `记` 误判
- 已根据 code review 再补 3 个回归修复：
  - `AIAssistantRemoteResponseInterpreter` 现在允许“短前置说明 + JSON action”命中 action 执行，只要文本中可提取出合法 action JSON，就会把提取出的 JSON 片段传给执行器；不再因为前面多一句“好的，下面是执行结果：”而丢失结构化执行信息。
  - `combineRemoteAndLocalReply()` 不再只靠 `❌` emoji 判断本地失败；现在 `错误:`、`记账失败:`、`创建账户失败:`、`创建分类失败:`、`执行操作时出错:` 等本地失败文案也会压制远程“已收到”类话术，避免成功口吻和失败结果并存。
  - `AILocalProcessor` 不再把裸 `记` 当成记账触发词；已收紧为 `记账 / 记一笔 / 记一下 / 记下 / 记录一笔 / 花了 / 收入 / 支出 / 消费 / 转账 / 工资 / 奖金 / 报销` 等更明确的交易短语，避免“你记得我吗？”这类普通聊天误进记账分支。
- **补充产品约束（2026-03-25）**：多笔动作执行时，若账户不存在，AI 管家应直接创建账户；若分类不存在，AI 管家应直接创建分类，并在语义足够时支持直接创建子分类；AI 管家对所有记账相关操作默认拥有直接执行能力，不应退化为只能建议或只做半自动确认。
- 新增回归测试：
  - `AIAssistantRemoteResponseInterpreterTest`：覆盖短前置说明包裹的 action JSON 仍会执行；覆盖无 emoji 的本地失败结果也会压制远程回复。
  - `AILocalProcessorTest`：覆盖“你记得我吗？”保持普通聊天，不再误进本地记账。
- 当前状态：本轮 code review 提出的 1 个 HIGH + 2 个 MEDIUM 问题已全部修复并通过回归验证。

#### 2026-03-25 模块 1 完成：本地 / 远程语义合同统一（当前轮）
- 已完成当前轮 **模块 1：本地 / 远程语义合同统一** 的最小收口，目标是先解决“提醒 / 备忘 / 普通聊天里带一个 `记` 字，就被当成立即记账”的误判风险。
- 本轮改动：
  - `AILocalProcessor` 在进入本地记账命令前，新增 reminder / 备忘 / 会议 / 笔记类普通对话保护；像“记得明天提醒我开会”这类表达会优先走普通聊天，而不会误入本地记账。
  - `AIReasoningEngine` 补齐了非记账 reminder 边界；像“提醒我晚上记账 25 元”这类“以后再做”的提醒表达，当前会优先视作普通对话，而不是立即触发 `RECORD_TRANSACTION`。
  - 当前模块仍保持既有安全边界不变：远程解释器的 action JSON 判定、dirty reply 清洗和 wrapped JSON 提取逻辑未在本轮扩口，避免引入新的误执行风险。
- 本轮新增回归测试：
  - `AILocalProcessorTest`：覆盖“记得明天提醒我开会”保持普通聊天。
  - `AIReasoningEngineTest`：覆盖“记得明天提醒我开会”“提醒我晚上记账 25 元”保持 `GENERAL_CONVERSATION`。
- 本轮验证：
  - `./gradlew testDebugUnitTest --tests com.example.aiaccounting.ai.AILocalProcessorTest --tests com.example.aiaccounting.ai.AIReasoningEngineTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantRemoteResponseInterpreterTest` ✅
- 当前状态：模块 1 已完成当前轮最小收口，**普通 reminder / 备忘 / 非即时记账表达** 的本地与推理层误判边界已补齐；下一模块进入“补实体与直接执行主链统一”。

#### 2026-03-25 模块 2 完成：补实体与直接执行主链统一
- 已完成当前轮 **模块 2：补实体与直接执行主链统一** 的最小收口，目标是统一远程 action 执行时的“补账户 / 补分类 / 执行 / 反馈”主链，同时不扩 remote dirty reply / action JSON 安全边界。
- 本轮改动：
  - `AIAssistantActionExecutor` 现在在远程 `add_transaction` 执行成功后，会补齐与本地主链一致的已解析反馈，返回 `账户: ... / 分类: ...`，避免远程链路成功了但用户看不到最终落到哪个实体。
  - 当远程 payload 显式提供数值 `accountId` / `categoryId` 时，执行层现在会优先按 ID 命中已有账户/分类；只有 ID 缺失或无效时，才退回名称匹配、默认兜底和缺失补建，避免把 `12` / `34` 这类真实 ID 当成名称误补建占位实体。
  - 账户/分类缺失时，当前仍保持 AI 管家直接补建的既有产品语义；但本轮没有放宽 `AIAssistantRemoteResponseInterpreter` 的 action 判定边界，仍保持现有安全面不变。
- 本轮新增回归测试：
  - `AIAssistantActionExecutorTest`
    - 覆盖远程 action 在缺失账户/分类时补建后，会返回包含最终账户/分类的执行反馈；
    - 覆盖远程 action 提供数值 `accountId` / `categoryId` 时，会优先命中现有实体而不是误建占位账户/分类。
- 本轮验证：
  - `./gradlew testDebugUnitTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantActionExecutorTest --tests com.example.aiaccounting.ai.AILocalProcessorTest --tests com.example.aiaccounting.ai.AIReasoningEngineTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantRemoteResponseInterpreterTest` ✅
- 当前状态：模块 2 已完成当前轮最小收口，**远程动作的实体补全、ID 命中与执行反馈** 已和主链预期对齐；下一模块可继续看更细粒度失败反馈、真实远程 JSON 变体，或进一步把本地/远程补实体逻辑抽成共享编排层。

#### 2026-03-25 P0 第一段补充收口：普通聊天 fallback 第二轮
- 已继续收口 **问题A（普通聊天/能力询问人格化主路径）**：
  - `AIReasoningEngine` 补齐了“你能做什么 / 你会什么 / 能帮我做什么 / 可以帮我做什么”等能力询问的普通对话识别与 AI 管家式回复，避免这类高频聊天问题重新落回 `UNKNOWN` 或刚性澄清。
  - `AILocalProcessor` 的本地普通对话兜底同步补齐了能力说明回复；即使在离线/未配置 AI 的情况下，用户也会收到 AI 管家口吻的自然说明，而不是只给命令列表。
- 本轮新增回归测试：
  - `AIReasoningEngineTest`：覆盖“你好”“你能做什么”命中 `GENERAL_CONVERSATION`。
  - `AILocalProcessorTest`：覆盖“你好”“你的底层是什么模型”本地路径保持人格化回复。
- 本轮验证：
  - `./gradlew testDebugUnitTest --tests com.example.aiaccounting.ai.AILocalProcessorTest --tests com.example.aiaccounting.ai.AIReasoningEngineTest` ✅
- 当前状态：**普通聊天、模型询问、能力询问** 的本地/推理层第一优先兜底已补齐；下一模块继续聚焦远程脏回复清洗收口。

- 本轮 review follow-up：
  - `AILocalProcessor` 的普通聊天本地兜底已开始按当前管家 ID 输出差异化语气，至少保证不同管家不会统一退回同一条泛化 AI 助手文案。
  - `AIAssistantRemoteResponseInterpreter` 已去掉对裸 `记录` 的记账 fallback 触发，避免“帮我记录一下会议要点”这类普通表达被误判为记账请求。
  - `AIAssistantViewModel` 在本地处理路径中已显式把当前管家 ID 透传给 `AILocalProcessor`。
- 补充回归测试：
  - `AILocalProcessorTest`：覆盖 `taotao` 普通问候返回差异化人格回复。
  - `AIAssistantRemoteResponseInterpreterTest`：覆盖普通“记录...”表达保持远程文本回复，不误走本地记账 fallback。
- 复验：
  - `./gradlew testDebugUnitTest --tests com.example.aiaccounting.ai.AILocalProcessorTest --tests com.example.aiaccounting.ai.AIReasoningEngineTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantRemoteResponseInterpreterTest` ✅
- 当前状态：模块 1 经过 review follow-up 后，已同时满足 **普通聊天兜底不僵硬** 与 **普通“记录...”表达不误记账** 两条边界。

#### 2026-03-25 外部灵感映射：EasyAccounts 对 AI 管家主线的可借鉴点
- 已对 `灵感文件/EasyAccounts-main` 做快速映射分析，结论是：**最值得借鉴的不是“它也有 AI”，而是它把 AI 当成工具化记账助手产品来设计。**
- 映射到当前项目后的结论如下：
  1. **工具化能力拆分（高价值）**
     - 当前项目已有明显基础：`Butler.kt` 的 system prompt 已约定 `query_accounts / query_categories / query_transactions / create_account / create_category / add_transaction` 等 typed action；`AIAssistantActionExecutor` 也已能将这些 action 归一化并执行。
     - 后续值得继续收口成更显式的 tool registry / tool semantics，而不是只停留在“解释 JSON 后 switch action”。
  2. **AI 操作留痕（高优先级借鉴）**
     - EasyAccounts 文档提到 AI 记账/更新自动打标签；当前项目尚未看到统一 `#AI记账`、`source=ai`、`createdByAI` 等留痕机制。
     - 这是最适合近期吸收的借鉴点：实现风险低，但对可追溯性、用户信任和后续问题回溯都很有价值。
  3. **人格配置产品化（中高价值）**
     - 当前项目人格能力分散在 `Butler.kt`、`IdentityConfirmationDetector`、`AIReasoningEngine`、`AILocalProcessor` 等处；虽然已经开始把本地聊天 fallback 按 butler id 输出不同语气，但还没有统一配置源。
     - EasyAccounts 的 role/name/character 思路提示，后续应把“聊天语气 / 澄清语气 / 成功失败语气”继续向单一人格配置层收口。
  4. **先查再执行的稳定策略（中高价值）**
     - 当前项目在 `AIAssistantActionExecutor.executeAddTransaction()` 与 `AIReasoningEngine.executeRecordTransaction()` 中已内嵌“账户/分类匹配 -> 缺失补建 -> 默认兜底”的逻辑。
     - 后续可继续产品化为显式的“查实体 -> 决定匹配/澄清/补建 -> 执行”编排层，以提升多笔动作、组合查询与失败解释的稳定性。
- 当前判断：
  - **值得优先借鉴**：工具化能力拆分、AI 留痕、人格配置收口、先查再执行。
  - **暂不优先借鉴**：MCP、外围设置展示、其 OCR 方案本身。
- 推荐吸收顺序：
  1. AI 留痕
  2. tool registry / typed action 语义显式化
  3. 人格配置统一收口
  4. 先查再执行编排层产品化

#### 模块 3：指令理解与执行链路重构（当前阶段）
- `AIAssistantViewModel` 已先抽出 `AIAssistantRemoteResponseInterpreter`
- 远程 AI 返回现在统一分三类处理：action JSON、本地记账 fallback、普通文本回复
- 已避免把“记住提醒”类普通对话误判成记账，并在第二段补回 `记下 / 记录 / 记录一笔` 等记账短语回退保护
- `processWithAIReasoning()` 已改为委派到 `AIAssistantMessageExecutionCoordinator`
- `processWithRemoteAI()` 已进一步瘦身，当前只保留上下文准备与 prompt 构建，再委派给 `AIAssistantRemoteExecutionHandler`
- `AIAssistantRemoteExecutionHandler` 中的 `stream 收集 / timeout / 失败 usage 记录` 已再下沉到新的 `AIAssistantRemoteStreamCollector`
- `AIAssistantRemoteResponseIntegrityChecker` 已从花括号/围栏启发式升级为结构化 JSON object 有效性校验，但仍只对“纯 JSON / 纯 fenced JSON”形态生效
- 模块 3C 第三段已新增 `AIAssistantRemoteExecutionResult`，把 timeout / transport failure / incomplete / action dispatch / local fallback / remote reply 统一为语义结果
- 当前 action 执行仅允许“纯 JSON / 纯 fenced JSON”响应命中，已避免 explanatory 文本中夹带 JSON 示例被误执行
- 模块 3D 第一段已新增 `AIAssistantMessageExecutionResult`，把主消息链路中的“最终回复”与“待确认”显式区分，避免在编排层过早把确认态抹平成普通字符串
- 模块 3D 第二段已将 `RequestClarification` 接入更通用的记账澄清闭环：缺金额时优先本地追问，补充金额后可沿最近澄清上下文继续走记账处理
- 模块 3D 第三段已新增 `PendingClarificationState` / `ClarificationTrigger` / `ClarificationFlowResult`，把 clarification 从轻量历史 continuation 升级为显式 pending clarification state
- 模块 3D 第四段已把 clarification 扩展到账户缺口：当用户明确记账、金额和交易类型已足够、但存在多个可用账户且消息未指明账户时，系统会先本地追问账户，而不是静默落默认账户
- 模块 3D 第五段已继续把 clarification 扩展到分类缺口与日期缺口：当用户已明确记账且金额/交易类型已足够时，若分类仍无法稳定推断，会先本地追问分类；若分类已足够但日期缺失，也会先本地追问日期，而不是直接落默认日期/模糊分类
- 模块 3D 第七段已继续把统一 pending interaction 下沉到 coordinator / route：`AIAssistantMessageExecutionCoordinator` 现在直接承接 pending clarification continuation，`AIAssistantMessageOrchestrator` 的路由入参也统一为 `PendingInteractionState`
- clarification continuation 续执行时，当前会先清本地 pending clarification；如果后续重新执行抛错，会 restore 原 pending clarification，避免用户补充后因为异常丢失上下文
- `ClarificationRequired` 现已显式携带 `originalMessage + question`，减少 `ViewModel` 对 continuation message 拼接与状态恢复细节的参与
- 已修复 resumed clarification message 可能被 `AIReasoningEngine` 按旧历史再次 prepend 原始请求、导致原始消息重复拼接的问题
- 模块 3D 第八段已把 clarification continuation 的“本地续执行 / 二次请求云端 AI”分叉提升为显式 continuation decision，避免补充信息后隐式重跑整条普通消息链
- 模块 3D 第九段已把 clarification continuation 从裸 message string 提升为结构化 payload / request model：`ClarificationContinuationRequest`、`AIAssistantContinuationPayload`、`RemoteExecutionRequest`、`ModificationExecutionRequest` 现在会显式携带原始消息、续执行消息、trigger、next step 与 route stage
- 模块 3D 第十段已把分析 → 澄清 → 确认 → 执行的阶段语义显式建模到主执行链：`AIAssistantInteractionStage` 与带 `stage` 的 `AIAssistantMessageExecutionResult` / route request 让 coordinator / orchestrator / ViewModel 在不重写 UI 的前提下保留用户可见分步语义
- 已补 chained clarification 回归：当用户补完一个缺口后又触发下一轮澄清时，新的 pending clarification 仍保留最初 `originalMessage`，避免后续回答只拼到中间续执行消息上而丢失原始记账意图

#### 本模块新增测试
- `AIAssistantRemoteResponseInterpreterTest`
  - 覆盖 action JSON 检测
  - 覆盖 typed action 检测
  - 覆盖普通文本回复 / 本地记账 fallback 分流
  - 覆盖 fallback 成功/失败时的回复合并
  - 覆盖“记住”类误判回归
  - 覆盖 `记下 / 记录一笔` 等记账短语回归
- `AIAssistantMessageOrchestratorTest`
  - 覆盖 pending modification 优先级
  - 覆盖 pending clarification 不会误走 modification flow
  - 覆盖远程可用/不可用时的路由差异
  - 覆盖修改交易意图进入 modification flow
  - 覆盖 structured remote / modification request payload 与 stage 映射
  - 覆盖 continuation decision 对本地续执行 / 二次上云 / modification continuation 的结构化分流
- `AIAssistantRemoteExecutionHandlerTest`
  - 覆盖 stream chunk 聚合后的正常返回
  - 覆盖 timeout 时返回 `Timeout` 语义结果并保留失败 usage 记录
  - 覆盖异常时返回 `TransportFailure` 语义结果
  - 覆盖不完整响应时返回 `IncompleteResponse` 并记录失败 usage
  - 覆盖远程文本回退为 `LocalFallbackRequested`
  - 覆盖 `ExecuteActions` 分支返回 `ActionExecutionRequested`
- `AIAssistantRemoteResponseIntegrityCheckerTest`
  - 覆盖空响应
  - 覆盖未闭合 fenced JSON
  - 覆盖花括号不平衡的 JSON 片段
  - 覆盖普通文本与完整 fenced JSON 的放行
  - 覆盖普通文本即使包含 `{预算}` 这类孤立花括号也不会误判不完整
  - 覆盖 bare JSON valid
  - 覆盖 bare / fenced JSON trailing comma invalid
  - 覆盖 non-json fenced text 仍按普通文本放行
- `AIAssistantRemoteStreamCollectorTest`
  - 覆盖 chunk 聚合成功
  - 覆盖 timeout 返回
  - 覆盖异常失败返回
- `AIAssistantMessageExecutionCoordinatorTest`
  - 覆盖 pending confirmation 继续执行时返回最终回复
  - 覆盖 modification flow 首次进入时返回 `ConfirmationRequired`
  - 覆盖 clarification 场景返回显式 `ClarificationRequired`
  - 覆盖 clarification continuation 成功续执行前先清 pending clarification
  - 覆盖 clarification continuation 续执行失败时 restore 原 pending clarification
  - 覆盖 confirmation / clarification 前不会提前触发远程执行
  - 覆盖 clarification continuation 的结构化 payload / stage 映射
  - 覆盖 chained clarification 仍保留最初 original message
- `AIAssistantPendingModificationLifecycleTest`
  - 覆盖 begin / continue 返回完整 `ModificationFlowResult`
  - 覆盖无 pending 时的 fallback finish
  - 覆盖失败确认保留状态
  - 覆盖需要再次确认时保留 pending 状态
- `AIAssistantPendingClarificationLifecycleTest`
  - 覆盖金额澄清 / 类型澄清 / 账户澄清 / 分类澄清 / 日期澄清 trigger 建立
  - 覆盖补充信息前重复追问、补齐后继续执行、取消后清理状态
  - 覆盖账户别名（如工行）回答可继续消费
  - 覆盖 lifecycle clear 后 pending clarification state 被清空
- `AIAssistantPendingInteractionLifecycleTest`
  - 覆盖统一 pending interaction facade 对 clarification / modification 的 currentState、begin、continue、clear 委派
  - 覆盖 clarification continuation 成功前保留 pending state、成功后再显式清理
- `AIAssistantPendingStateResetTest`
  - 覆盖 unified pending interaction clear 可同时清理 modification / clarification state
- `AIAssistantPendingClarificationLifecycleTest`
  - 覆盖缺金额记账请求先返回 `RequestClarification`
  - 覆盖金额补充消息在澄清上下文下继续产出 `RecordTransaction`
  - 覆盖“记一笔 + 已给金额但未指明类型”时先追问收入 / 支出 / 转账
  - 覆盖“记一笔 + 已给金额/类型/日期但多账户未指明账户”时先追问账户
  - 覆盖“记一笔 + 已给金额/类型/日期但分类缺失”时先追问分类
  - 覆盖“记一笔 + 已给金额/类型/分类但日期缺失”时先追问日期
  - 覆盖已显式给出账户或仅有单账户时不触发账户澄清

#### 当前实现边界
- `AIAssistantViewModel` 已不再直接持有原始 pending modification state，而是委派给 `AIAssistantPendingModificationLifecycle`
- `AIAssistantModificationCoordinator` 已为结束态补充 `shouldClearPending`，避免依赖用户输入文案猜测是否清理状态
- 失败确认场景现在会保留 pending state，用户仍可继续重试或取消
- `processWithRemoteAI()` 当前已不再直接处理 stream 调用、timeout 与 usage 记录，这些职责已分别抽到 `AIAssistantRemoteExecutionHandler` 与 `AIAssistantRemoteStreamCollector`
- 响应完整性检查已从 `ViewModel` 外移到 `AIAssistantRemoteResponseIntegrityChecker`
- 当前远程执行链已基本形成 `ViewModel -> Handler -> StreamCollector / IntegrityChecker / Interpreter` 的分层
- 当前 `processWithRemoteAI()` 已负责把 `AIAssistantRemoteExecutionResult` 统一映射为用户可见文案或副作用执行，handler 不再直接返回字符串分支
- 当前 `AIAssistantRemoteResponseIntegrityChecker` 已升级为结构化 JSON object 有效性校验，但校验范围仍故意与解释器保持一致，只覆盖“纯 JSON / 纯 fenced JSON”
- 当前交易修改确认流已不再只向上返回字符串，而是通过 `ModificationFlowResult` / `AIAssistantMessageExecutionResult` 保留“待确认”语义
- 当前通用 clarification 已不再只停留在“缺金额 + 最近历史拼接”的最小闭环，而是引入独立 `PendingClarificationState` / `ClarificationTrigger`，并由 `ClarificationRequired` 显式表示待澄清态
- 当前 clarification 已覆盖“缺金额”“金额已给但交易类型不明确”“多账户未指明账户”“分类缺失”“日期缺失”等本地可判定缺口
- 当前 `AIAssistantPendingInteractionLifecycle` 已把 clarification / confirmation 收口到统一高层入口，`ViewModel` 会通过统一 facade 管理 begin / continue / clear
- 当前统一 pending interaction 已进一步下沉到 `AIAssistantMessageExecutionCoordinator` / `AIAssistantMessageOrchestrator`：`ViewModel` 只负责把当前 pending interaction state 与 lifecycle 回调传入 coordinator，不再直接编排 clarification continuation 续执行细节
- 当前 clarification continuation 在 coordinator 中采用“续执行前先清、失败时 restore”语义，避免异常路径丢失待补充上下文
- 当前 `AIReasoningEngine` 已避免对已经包含原始请求的 clarification continuation message 再次拼接旧用户输入，减少 resumed message 重复污染后续解析
- 当前 clarification continuation 已不再只靠裸字符串续跑，而是通过结构化 request / continuation payload 显式携带 `originalMessage`、`resumedMessage`、`trigger` 与 `nextStep`
- 当前主消息执行结果与 route 已显式带 `AIAssistantInteractionStage`，让分析 / 澄清 / 确认 / 执行阶段语义可在链路中持续保留
- 当前 chained clarification 会沿用最初 original message 建立下一轮 pending clarification，避免多轮补充时丢失最初交易意图

#### 下个窗口默认接手点
- 模块 3D 第十段已收口完成
- 若继续问题3方向，优先进入 **问题2 / 模块2B：运行时模型性能监测** 与 **问题4 / 模块4A：网络测速模块** 的取舍评估，而不是回头重开 3D-9 / 3D-10
- 问题3 后续若再继续，应偏向更产品化的展示层落地（例如把 stage 语义进一步映射到消息 UI / session 元数据），而不是再回退成字符串 continuation
- 继续遵循当前工作流：测试先行 → 完成一个模块后更新 memory / 开发文档 / git commit / push


#### 已实现能力
- 模型选择界面已支持 **测试连接按钮**
- 邀请码绑定后支持 **Auto 自动优选模型**
- 邀请码网关默认基址为 `https://api.gdmon.dpdns.org`
- 历史 `workers.dev` 域名会在迁移逻辑中被识别并替换

#### 当前行为约定
- 邀请码绑定成功后，持久化的 `model` 值为空字符串，表示 **自动优选模型**
- 不再默认写死 `openai/gpt-oss-120b`
- AI 设置页存在两类测试结果：
  - 页面级连接测试结果 `TestResult`
  - 模型选择弹窗测试结果 `ModelTestResult`

### 2.3 AUTO 模型测试连接与回退最新状态

#### 模块 2：AUTO 模型超时与回退重构
- `testConnection` 现已支持 AUTO 模式下的有界回退
- 当 `model = ""` 时，测试连接会先用测试客户端获取模型列表，再依次尝试候选模型
- 当前实现为最多 2 次测试请求：首个候选失败后，仅再尝试 1 个备用模型，避免长时间无反馈
- FIXED 模式下不再偷偷切换模型；如果固定模型不可用，会直接返回明确引导信息
- AUTO 模式下如果优选模型超时或不可用，会返回更具体的提示，建议稍后重试或手动选模型

#### 本模块验证结果
- 扩展 `AIServiceModelFallbackTest`：覆盖聊天 fallback、AUTO 测试连接 fallback、FIXED 模型不可用提示
- 新增 `AISettingsConnectionTest`：覆盖设置页测试连接在 AUTO 失败时会正确落到错误态，不会一直停留在 testing
- 本地验证：
  - `./gradlew testDebugUnitTest --tests com.example.aiaccounting.data.service.AIServiceModelFallbackTest --tests com.example.aiaccounting.ui.viewmodel.AISettingsConnectionTest` ✅
  - `./gradlew testDebugUnitTest --continue` ✅

---

### 2.4 AI 助手图片识别模块最新状态

#### 模块 1：OCR 质量增强
- 模块1A / 1B / 1C / 1D 已全部完成：
  - 模块1A：本地图片 OCR 轻量预处理、账单信号提取与启发式置信度门控
  - 模块1B：补齐轻量多 profile 预处理增强（基础增强 / 细节增强 / 文档增强）
  - 模块1C：以多 profile / 多 pass OCR 集成方式实现多结果比对与最佳结果仲裁
  - 模块1D：结合图像质量指标、OCR 文本结构和跨 pass 一致性升级本地 confidence 模型
- 非原生多模态模型下，仍保持仅 **中高置信度** 图片结果会继续发送到云端 AI
- 低置信度图片仍会在本地直接提示用户重拍，避免把噪声 OCR 发送到云端导致误判

#### 当前实现边界
- 当前仍基于 ML Kit 中文 OCR + 图像标签能力
- 已明确不引入 OpenCV 或重型 OCR SDK，而是在现有链路上完成多 profile / 多 pass 增强
- 当前置信度已不再只看单轮启发式文本质量，而是综合图像质量、文本结构和跨 pass 一致性得到最终 confidence

#### 本模块验证结果
- 定向验证：
  - `./gradlew testDebugUnitTest --tests com.example.aiaccounting.data.service.image.ReceiptTextHeuristicsTest --tests com.example.aiaccounting.data.service.image.OcrResultSelectorTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantImageMessageHandlerTest` ✅
- 全量回归：
  - `./gradlew testDebugUnitTest --continue` ✅

### 2.5 四大问题总体完成度对照（按最初交付要求复盘）

> 说明：以下状态区分“当前模块化切片已完成”与“最初整套产品目标已全部完成”。截至 2026-03-23，前者已有明显进展，后者尚未全部收口。

#### 问题1：OCR识别精度问题
- **当前状态：已完成（模块1A / 1B / 1C / 1D 已全部收口）**
- **已完成**：
  - 本地 OCR 轻量预处理：缩放、灰度化、对比度增强
  - 多 profile 轻量预处理增强：基础增强 / 细节增强 / 文档增强
  - 多 pass OCR 结果比对与最佳结果仲裁
  - 账单关键信号提取：金额、时间、支付方式、商户
  - 综合图像质量、文本结构与跨 pass 一致性的增强版 confidence 模型
  - 非原生多模态模型下仅中高置信度结果送云端
  - 低置信度结果本地拦截并提示重拍
- **暂不采用**：
  - OpenCV 级重型图像处理
  - 新增重型 OCR 引擎依赖
- **验证**：
  - `ReceiptTextHeuristicsTest`
  - `OcrResultSelectorTest`
  - `AIAssistantImageMessageHandlerTest`
  - `./gradlew testDebugUnitTest --continue`

#### 问题2：自动优选模型导致超时
- **当前状态：已完成（连接测试链路 + 运行时性能调度体系已落地）**
- **已完成**：
  - AUTO 模式连接测试有界 fallback
  - 首候选失败时尝试 1 个备用模型
  - FIXED 模式保持不切模型语义
  - 404 仅在明确带 model 信号时才归为 model unavailable
  - reviewer follow-up 回归测试已补齐：AUTO timeout fallback / plain 404 classification
  - 新增运行时模型性能快照与推荐引擎，按 `provider + apiUrl` 隔离记录模型成功率、失败次数、timeout 次数与延迟聚合
  - 新增 `ModelExecutionPlanner`，将 AUTO/FIXED 语义从简单空模型判断提升为显式执行计划；AUTO 运行时聊天会优先使用近期表现更好的模型，FIXED 运行时聊天不再偷偷切换模型
  - 复用现有 `/models` ETag 缓存作为候选目录缓存，为模块2D提供轻量候选调度，不额外引入预请求预热
  - AI 设置页新增“模型推荐”摘要卡片，并与“智能路由建议”分离展示；provider / API URL 变化时会同步清理旧模型推荐状态
- **未完成/未落地为正式能力**：
  - 当前模块2B / 2C / 2D 已一并收口；若后续继续问题2，应转向更高阶的推荐解释性、跨 provider 策略或更细粒度的调度治理，而不是重复补当前规划内切片

#### 问题3：指令理解与执行流程优化
- **当前状态：部分完成（模块 3D 第十段完成，主链路结构化 continuation / stage model 已收口，剩更高层产品化展示）**
- **已完成**：
  - 远程响应解释、消息执行 coordinator、pending modification 生命周期收口
  - 远程 handler / stream collector / integrity checker 分层
  - 远程执行结果统一模型
  - 结构化 JSON 完整性校验
  - explanatory JSON 误执行风险修复
  - 通用 clarification 最小闭环：缺金额时先本地追问，补充金额后继续原请求
  - 显式 pending clarification state / trigger model
  - session 边界 pending state 清理
  - clarification continuation 完成后显式判断是本地续执行还是二次请求云端 AI
  - clarification continuation 的结构化 payload / request model
  - 分析 / 澄清 / 确认 / 执行阶段在 execution result / route 中的显式 stage 建模
- **未完成/未落地为正式能力**：
  - 把 stage 语义进一步映射到更丰富的消息 UI / session 元数据
  - 更高层、更完整的产品化思考展示与过程可视化
  - 问题2 / 问题4 仍未接入这套阶段语义的统一用户提示

#### 问题4：网络延迟与超时优化
- **当前状态：当前规划内模块已收口完成（已完成基础超时/连接稳定性修复，并完成模块4A 网络测速、模块4B 路由建议持久化、模块4C 请求级重试/退避首版、模块4D 压缩传输与增量更新首版、模块4E 网络状态监测与预警）**
- **已完成**：
  - 测试连接与邀请码网关超时参数上调
  - 连接池、pingInterval、IPv4-first DNS
  - 模块4A：网络测速模块（AI 设置页新增网络测速入口，统一测速 API 节点 / 邀请码网关，并返回结构化测速结果）
  - 模块4B：基于测速结果生成并持久化智能路由建议，在设置页展示推荐目标，并在 API URL / 网关地址 / provider 变化时自动清理旧建议
  - 模块4C：为连接测试与非流式请求引入统一重试执行器；对超时 / 连接失败 / 5xx 开启小步重试，并保持 AUTO 模式“同模型先重试、再切备用模型”的顺序，同时补齐 cancellation 透传与回归测试
  - 模块4D：为 OpenAI 兼容的非流式大 JSON 请求补上 gzip 传输；为 `/models` 获取引入基于 `ETag` / `If-None-Match` 的内存级条件刷新，在 304 时直接复用缓存模型列表；当前明确不改 stream chat 传输协议与 UI 层行为
  - 模块4E：在 AI 设置页新增统一网络预警卡片，基于离线状态、测速失败与高延迟结果生成提示；页面进入与恢复前台时自动刷新网络状态，并在 provider / API URL / 网关地址变化时同步清理旧测速结果与旧预警
- **未完成/未落地为正式能力**：
  - 当前规划内 4A~4E 已完成；若后续继续问题4，应作为新阶段重新定义目标

#### 当前建议优先级
1. **问题2** 当前规划内模块已完成；若后续继续，应按新阶段重新定义更高阶推荐/调度目标
2. **问题4** 当前规划内模块已完成；若后续继续，应按新阶段重新定义目标
3. **问题3** 仅在用户明确要求继续更高层产品化展示时再展开
4. **问题1** 当前不再是默认主线；若后续回头深化，则从 **模块1B**（OCR 深度预处理增强）开始

#### 重新编号后的剩余模块清单（供后续窗口直接接手）
- **问题1：OCR识别精度问题**
  - 已完成：模块1
  - 未完成：模块1B（深度预处理增强）、模块1C（多引擎 OCR 比对）、模块1D（原生置信度与模型优化）
- **问题2：自动优选模型导致超时**
  - 已完成：模块2、模块2B（运行时模型性能监测）、模块2C（动态切换策略）、模块2D（模型预加载与资源调度）
  - 未完成：当前规划内模块已收口；若后续继续，应作为新阶段重新定义目标
- **问题3：指令理解与执行流程优化**
  - 已完成：模块3A、模块3B、模块3C-1~4、模块3D-1~10
  - 未完成：更高层产品化展示 / session 元数据承接
- **问题4：网络延迟与超时优化**
  - 已完成：模块4A（网络测速）、模块4B（智能路由建议持久化与展示）、模块4C（连接测试 / 非流式请求重试退避首版）、模块4D（非流式 gzip 传输与 `/models` 条件刷新首版）、模块4E（网络状态监测与预警）+ 基础超时与连接稳定性修补
  - 未完成：当前规划内模块已收口；若后续继续，应按新阶段重新定义目标

#### 切换窗口时的默认接手点
- 问题2 的模块2B / 2C / 2D 已完成，不需要回退重做
- 问题3 的 3D-9 / 3D-10 已完成，不需要回退重做
- 问题4 当前规划内 4A~4E 已完成；若后续继续，应按新阶段重新定义目标而不是重复实现既有模块
- 默认不要回头重开问题1、问题2连接测试链路，或把问题4测速/重试/压缩/预警模块重复实现，除非用户明确改优先级

---

## 三、构建与发布流程

### 3.1 GitHub Actions 工作流

工作流文件：[`build.yml`](../.github/workflows/build.yml)

| 阶段 | Job | 命令 / 行为 |
|------|-----|-------------|
| 1 | Lint Check | `./gradlew lintDebug --continue` |
| 2 | Unit Tests | `./gradlew testDebugUnitTest --continue` |
| 3 | Build APK | `./gradlew assembleDebug` + `./gradlew assembleRelease` |
| 4 | release | tag 满足 `v*` 时自动创建 GitHub Release |

> 当前 release artifact 归档逻辑已修复：workflow 会优先探测 `app-release.apk`，并兼容 `app-release-unsigned.apk`，不再依赖单一固定文件名。

### 3.2 Release 环境变量

| 变量 | 作用 | 备注 |
|------|------|------|
| `KEYSTORE_PASSWORD` | keystore 密码 | 正式签名时必需 |
| `KEY_PASSWORD` | key alias 密码 | 正式签名时必需 |

### 3.3 发布前最低验证项

- `lintDebug` 通过
- `testDebugUnitTest` 通过
- `assembleRelease` 可构建
- GitHub Actions artifacts 中同时存在 debug / release APK
- AI 设置页：
  - 模型列表获取正常
  - 模型测试连接正常
  - 邀请码绑定后 Auto 自动优选生效

---

## 四、接口与配置方案

### 4.1 邀请码网关

默认网关基址：
- `https://api.gdmon.dpdns.org`

客户端能力：
- 通过 `/bootstrap` 获取邀请码绑定后的 token 与 API 地址
- 自动保存邀请码绑定结果
- 自动切换到 Auto 模型模式
- 自动迁移并替换废弃网关域名

### 4.2 AI 设置配置策略

| 场景 | 当前策略 |
|------|----------|
| 普通用户手动配置 | 可填写 API Key / API URL / 模型 |
| 邀请码绑定用户 | 自动下发 token 与 API 地址 |
| 绑定后模型策略 | `model = ""` 表示 Auto 自动优选 |
| 模型可用性验证 | 模型选择弹窗内支持单模型测试连接 |

---

## 五、已知问题与注意事项

### 5.1 已确认修复的问题
- 模型测试按钮拆分后造成的 `TestResult` 命名冲突已修复
- 旧测试仍断言 `openai/gpt-oss-120b` 为默认模型的问题已修正

### 5.2 当前注意事项
- 本地某些 bash 环境运行 Gradle 时可能打印 `uname: command not found`，但当前不影响本地 lint / unit test 结果
- `Build Release APK` 阶段仍可能出现 Apache POI 相关 R8 warning：`SVGUserAgent.getViewbox()` 在静态分析时被视为 unreachable；当前不影响 release 构建成功与 release artifact 上传
- 该 warning 已完成模块 1 归因：当前 app 仅使用 [ExcelExporter.kt](../app/src/main/java/com/example/aiaccounting/data/exporter/ExcelExporter.kt) 中的 `XSSFWorkbook` 基础导出能力，但 `org.apache.poi:poi-ooxml:5.2.5` jar 自带了 `org.apache.poi.xslf.draw.SVGUserAgent` 等 PPT/SVG 渲染类，R8 在 release 混淆时会扫描到该未使用路径并对 `getViewbox()` 给出 unreachable warning
- 模块 2 已将 POI 的 keep 范围从整个 `org.apache.poi.**` 收敛到当前 Excel 导出主路径所需的 `org.apache.poi.ss.**`、`org.apache.poi.xssf.**`、`org.apache.poi.openxml4j.**`，并补充 `-dontwarn org.openxmlformats.schemas.**` 以恢复 release 构建稳定性
- 模块 6A 已删除 direct dependency `org.apache.poi:poi:5.2.5`，仅保留 `org.apache.poi:poi-ooxml:5.2.5`；`dependencyInsight` 已确认 `poi` 仍由 `poi-ooxml` 传递引入，因此当前变更只是在依赖声明层面去冗余，不改变 release 结果
- 模块 6B 已新增 [ExcelExporterTest.kt](../app/src/test/java/com/example/aiaccounting/data/exporter/ExcelExporterTest.kt)，覆盖交易导出、空列表导出、月度汇总导出三类回归场景；当前已确认单测与 release 构建均通过，为后续进一步裁剪 POI 依赖提供安全网
- 模块 6D 已进一步核验本地 Gradle cache 中的 jar 组成：`poi-ooxml-5.2.5.jar` 内直接包含 `org.apache.poi.xslf.draw.SVGUserAgent` 等 xslf/SVG 渲染类，`poi-5.2.5.jar` 内直接包含 `org.apache.poi.sl.**` 演示文稿绘制链，而 `poi-ooxml-lite-5.2.5.jar` 不包含当前 warning 源类
- 这意味着剩余 `SVGUserAgent.getViewbox()` warning 的来源就是 `poi-ooxml` / `poi` 自带的未使用可选渲染路径，而不是当前业务代码额外触发的新依赖边
- 模块 6D 因此将该线索明确收口为结论：在 **不替换库、不重写导出链** 的前提下，当前 POI 依赖面已经没有明显低风险继续裁剪空间；若继续强裁，风险将高于收益
- 当前 release 构建仍依赖 `app/proguard-rules.pro` 中这组已归因的 POI 可选路径与 desktop API suppress 规则，将该 warning 维持为非阻塞项；后续若继续推进，应新开独立方案评估替代导出库、CSV 降级方案或导出模块解耦，而不是继续在现有 POI 依赖上做硬裁剪
- 旧文档中仍有部分历史路径、旧版本号、v1.0.0/v1.8.1 叙述，需要以后继续收敛

### 5.3 建议后续手工复核的文档
- `BUILD.md`：历史 Windows 路径和签名示例较旧
- `RELEASE_CHECKLIST.md`：已同步核心流程，但仍建议发布前按真实商店流程再次逐项核查

---

## 六、结论

截至 2026-03-22：
- v1.8.3 发布准备已完成
- AI 设置页模型测试能力已落地
- 邀请码绑定后的 Auto 自动优选行为已成为当前标准实现
- 最近一次 CI 阻塞已修复并完成本地验证
- 当前代码与核心开发文档已重新对齐
