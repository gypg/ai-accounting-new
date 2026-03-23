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
- 已避免把“记住提醒”类普通对话误判成记账，但仍保留 `记账 / 记个账 / 记一笔` 等常见记账短语识别
- `processWithAIReasoning()` 已改为委派到 `AIAssistantMessageExecutionCoordinator`
- 当前 `ViewModel` 继续保留 UI state、仓库存储、session 入口，消息理解与执行编排开始独立化

#### 本模块新增测试
- `AIAssistantRemoteResponseInterpreterTest`
  - 覆盖 action JSON 检测
  - 覆盖 typed action 检测
  - 覆盖普通文本回复 / 本地记账 fallback 分流
  - 覆盖 fallback 成功/失败时的回复合并
  - 覆盖“记住”类误判回归
- `AIAssistantMessageOrchestratorTest`
  - 覆盖 pending modification 优先级
  - 覆盖远程可用/不可用时的路由差异
  - 覆盖修改交易意图进入 modification flow

#### 当前实现边界
- 本阶段尚未把 pending modification 生命周期完全抽离出 `AIAssistantViewModel`
- `processWithRemoteAI()` 仍保留 prompt 构建、stream 调用、usage 记录与超时控制
- 下一步优先继续给 `AIAssistantMessageExecutionCoordinator` 补更细粒度测试，再考虑进一步收口修改确认状态


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
