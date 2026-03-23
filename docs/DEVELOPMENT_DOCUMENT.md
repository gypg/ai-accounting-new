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
- 当前 `ViewModel` 继续保留 UI state、仓库存储、session 入口，消息理解与执行编排开始独立化

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
  - 覆盖远程可用/不可用时的路由差异
  - 覆盖修改交易意图进入 modification flow
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
  - 覆盖确认前不会提前触发远程执行
- `AIAssistantPendingModificationLifecycleTest`
  - 覆盖 begin / continue 返回完整 `ModificationFlowResult`
  - 覆盖无 pending 时的 fallback finish
  - 覆盖失败确认保留状态
  - 覆盖需要再次确认时保留 pending 状态

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
- 当前显式确认态仍主要服务于 modification flow，尚未扩展成更通用的 clarification / confirmation UI 协议
- 下一步若继续模块 3，应优先把显式确认态从修改交易流推广到更通用的需求确认闭环，而不是先回头做 mapper 下沉

#### 下个窗口默认接手点
- 模块 3D 第一段已收口完成
- 若继续本方向，直接进入 **模块 3D 第二段：更通用的 confirmation / clarification 闭环**
- 优先建立“需要确认”的最小触发规则，并把确认后二次进入远程执行链的路径补齐
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
- 本地图片 OCR 已增加轻量预处理：缩放、灰度化、对比度增强
- OCR 结果已增加启发式质量评分，按 `HIGH / MEDIUM / LOW / NONE` 分级
- 本地会提取账单关键信号：金额、时间、支付方式、商户
- 非原生多模态模型下，仅 **中高置信度** 图片结果会继续发送到云端 AI
- 低置信度图片会在本地直接提示用户重拍，避免把噪声 OCR 发送到云端导致误判

#### 当前实现边界
- 当前仍基于 ML Kit 中文 OCR + 图像标签能力
- 暂未引入重型 OCR 引擎替换，也未引入 OpenCV
- 当前置信度为本地启发式质量分，不是 OCR 引擎原生置信度

#### 本模块验证结果
- 新增 `ReceiptTextHeuristicsTest`，覆盖金额/日期/支付方式/商户提取、质量评分、关键行筛选
- 新增 `AIAssistantImageMessageHandlerTest`，覆盖低置信度拦截、仅高置信度结果进入提示词、配置错误短路
- 本地验证：
  - `./gradlew testDebugUnitTest --tests com.example.aiaccounting.data.service.image.ReceiptTextHeuristicsTest --tests com.example.aiaccounting.ui.viewmodel.AIAssistantImageMessageHandlerTest` ✅
  - `./gradlew testDebugUnitTest --continue` ✅

### 2.5 四大问题总体完成度对照（按最初交付要求复盘）

> 说明：以下状态区分“当前模块化切片已完成”与“最初整套产品目标已全部完成”。截至 2026-03-23，前者已有明显进展，后者尚未全部收口。

#### 问题1：OCR识别精度问题
- **当前状态：部分完成（当前模块目标已完成）**
- **已完成**：
  - 本地 OCR 轻量预处理：缩放、灰度化、对比度增强
  - 账单关键信号提取：金额、时间、支付方式、商户
  - 启发式质量评分与 `HIGH / MEDIUM / LOW / NONE` 分级
  - 非原生多模态模型下仅中高置信度结果送云端
  - 低置信度结果本地拦截并提示重拍
- **未完成/未落地为正式能力**：
  - 多 OCR 引擎比对机制
  - 更重的图像预处理链路（如倾斜校正、OpenCV 级增强）
  - OCR 引擎原生置信度体系与模型级优化

#### 问题2：自动优选模型导致超时
- **当前状态：部分完成（连接测试链路已完成，运行时性能调度体系未完成）**
- **已完成**：
  - AUTO 模式连接测试有界 fallback
  - 首候选失败时尝试 1 个备用模型
  - FIXED 模式保持不切模型语义
  - 404 仅在明确带 model 信号时才归为 model unavailable
  - reviewer follow-up 回归测试已补齐：AUTO timeout fallback / plain 404 classification
- **未完成/未落地为正式能力**：
  - 运行时模型性能监测机制
  - 响应时间阈值驱动的动态切换
  - 模型预加载与资源调度管理

#### 问题3：指令理解与执行流程优化
- **当前状态：部分完成（模块 3C 第四段完成，整套交互确认机制未完全产品化）**
- **已完成**：
  - 远程响应解释、消息执行 coordinator、pending modification 生命周期收口
  - 远程 handler / stream collector / integrity checker 分层
  - 远程执行结果统一模型
  - 结构化 JSON 完整性校验
  - explanatory JSON 误执行风险修复
- **未完成/未落地为正式能力**：
  - 面向用户的分步意图澄清 UI/交互机制
  - 明确的“需求确认后再二次请求云端 AI”的完整闭环
  - 更彻底的结构化分步上送协议与可视化思考/确认流程

#### 问题4：网络延迟与超时优化
- **当前状态：部分完成（仅完成基础超时与连接稳定性修复）**
- **已完成**：
  - 测试连接与邀请码网关超时参数上调
  - 连接池、pingInterval、IPv4-first DNS
- **未完成/未落地为正式能力**：
  - 网络连接测速模块
  - 智能路由与最低延迟节点选择
  - 明确的自动重试 + 退避算法体系
  - 压缩传输 / 增量更新
  - 网络状态监测与预警提示

#### 当前建议优先级
1. 先把 **问题3 的确认态显式结果模型切片** 提交到 git，确保代码 / memory / 文档 / git 状态一致
2. 然后继续 **问题3**，把显式 confirmation 结果从 modification flow 推广到更通用的需求确认闭环
3. **问题4** 当前仍停留在早期稳定性修复阶段，后续若恢复推进，应单独拆成新的网络能力模块群，不建议混入模块3连续重构中

#### 已对齐的下一轮计划（2026-03-23）
- 本轮已完成：
  - 问题2 reviewer follow-up 已补码、补测、补文档，并已提交 git
  - 问题3 已新增“确认态显式结果模型”最小切片，为后续通用 confirmation / clarification 闭环铺路
  - 四大问题的“已完成 / 未完成”边界已重新梳理清楚
- 下一轮对话默认目标：**继续问题3，而不是回头扩问题1/2/4**
- 下一轮优先做的最小切片：**更通用的需求确认闭环**
  - 对存在歧义或信息不完整的用户输入，不直接进入执行
  - 先进入 clarification / confirmation 状态
  - 由系统生成结构化确认问题
  - 用户确认后，再决定是否二次请求云端 AI 或进入执行链路
- 下一轮建议顺序：
  1. 为“需要确认”的触发条件建立最小判定规则
  2. 把确认态从 modification flow 扩展到更通用主消息链路
  3. 为确认前不执行、确认后二次进入执行链路补回归测试
  4. 该最小闭环稳定后，再评估是否继续下沉 `AIAssistantRemoteExecutionResult` 到 UI 文案 / 副作用 mapper
- 明确暂不在下一轮处理：
  - 问题1 的多 OCR 引擎 / OpenCV 深化方案
  - 问题2 的运行时模型性能监测 / 预加载 / 调度
  - 问题4 的测速、智能路由、退避、压缩与网络预警

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
