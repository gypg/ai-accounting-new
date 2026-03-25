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
- 当前 release 构建仍依赖 `app/proguard-rules.pro` 中对 POI / AWT / XML schema 的 suppress 规则，将该 warning 维持为非阻塞项；下一步若继续推进，应优先从依赖层面缩减 POI，而不是继续扩大 suppress
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
