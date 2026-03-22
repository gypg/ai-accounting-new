# AI记账 — 产品开发文档

> 版本：v1.8.1 | 文档日期：2026-03-13 | 文档状态：内部评审稿

---

## 一、项目概述

### 1.1 产品定位

AI记账是一款面向中国大陆个人用户的智能记账 Android 应用。核心差异化在于：通过自然语言交互完成记账操作，降低传统手动录入的使用门槛。

### 1.2 目标用户

| 用户群体 | 核心需求 | 使用场景 |
|----------|----------|----------|
| 18-35岁年轻上班族 | 快速记账、月度统计 | 通勤、午休、睡前碎片时间 |
| 家庭财务管理者 | 多账户管理、预算控制 | 家庭收支规划、月末对账 |
| 自由职业者 | 多类型收入追踪、导出报表 | 项目结算、税务申报辅助 |

### 1.3 产品现状数据

| 指标 | 数值 |
|------|------|
| 版本号 | v1.8.1 (versionCode 18) |
| Kotlin 源文件数 | 146 |
| 代码行数（估算） | ~42,000 行 |
| 屏幕页面数 | 28 |
| 数据库表数 | 12 |
| Widget 组件数 | 5 种尺寸 |
| AI 管家角色数 | 5 |
| 支持的 AI 提供商 | 6（通义千问、DeepSeek、智谱、百度、OpenAI 兼容、自定义） |

---

## 二、功能模块清单

### 2.1 核心功能矩阵

```
┌─────────────────────────────────────────────────────┐
│                    AI记账 功能架构                      │
├──────────┬──────────┬──────────┬─────────────────────┤
│  记账模块  │  统计模块  │  AI模块   │     系统模块         │
├──────────┼──────────┼──────────┼─────────────────────┤
│ 手动记账   │ 月度统计   │ 自然语言   │ PIN/生物识别        │
│ 语音记账   │ 年度趋势   │ 语音交互   │ 数据加密(SQLCipher) │
│ 快速模板   │ 分类饼图   │ 图片识别   │ 数据导出(CSV/Excel) │
│ 悬浮窗记账 │ 预算追踪   │ 管家人格   │ 自动备份            │
│ Widget记账 │ 收支对比   │ 权限审计   │ 主题切换            │
│ 多账户管理 │ 趋势图表   │ 意图推理   │ Widget系统          │
│ 分类管理   │ 日/周/月   │ 上下文记忆 │ 悬浮窗服务          │
└──────────┴──────────┴──────────┴─────────────────────┘
```

### 2.2 功能完成度评估

| 模块 | 完成度 | 说明 |
|------|--------|------|
| 基础记账 | 95% | 手动/语音/模板/悬浮窗均已实现，标签系统待启用 |
| 统计分析 | 90% | 月度/年度/分类统计完整，自定义时间段分析待完善 |
| AI 交互 | 85% | 自然语言解析、意图推理、多提供商支持已完成，连接测试功能待实现 |
| 安全体系 | 90% | PIN + 生物识别 + 加密数据库 + 权限审计已完成 |
| 数据导出 | 85% | CSV/Excel 导出已实现，PDF 报表待开发 |
| Widget | 90% | 5 种尺寸 + 悬浮窗 + 图表已完成 |
| 主题系统 | 80% | Material 3 + 马年主题已完成，主题间过渡动画待优化 |

### 2.3 最近一轮工程收敛（2026-03-22）

**Session 29 P0 发布准备：**
- 创建完整优化流程规划文档 `OPTIMIZATION_ROADMAP.md`
- **P0 模块1完成**：确认 applicationId 已是正式包名 `com.moneytalk.ai`
  - namespace `com.example.aiaccounting` 与代码包名一致（决定 R 类包名）
  - applicationId `com.moneytalk.ai` 是应用唯一标识（已是正式包名）
  - 编译验证通过
- 准备进入 P0 模块2: ProGuard 验证

**Session 28 多模型协作与网关迁移审计：**
- 评价原 Zhipu 5 遗留的 IPv6 超时环境错误，修复重构网断联异常

**Session 27 API代理网关重构与客户端适配：**
- 重新部署 Cloudflare Worker 与 KV 命名空间，实现更安全的 API 代理与邀请码分发机制。
- 客户端默认网关切换至自定义域名 `https://api.gdmon.dpdns.org`，绕过 `workers.dev` 的访问限制。
- 在 `AIConfigRepository` 与 `AISettingsViewModel` 中实现向后兼容的迁移逻辑，启动时自动拉黑废弃域名 `"workers.dev"`，同时确保自定义域名不被误删。
- 完成通过邀请码自动请求 `/bootstrap` 接口获取鉴权 Token 及下发 API 地址的产品化交互。

**Session 26 测试修复：**
- 修复 17 个 AISettings 相关测试的 `NoSuchElementException`：补齐 `getGatewayBaseUrl`、`getInviteApiBaseUrl`、`getInviteRpm`、`getInviteCodeMasked` 的 mock
- 修复 `TransactionRepositoryTest.getMonthRange` 的 `AssertionError`：修正 `getMonthRange` 实现，使 end 成为下月第一毫秒（exclusive upper bound）
- 单元测试全部通过（133 tests）

**Session 25 构建修复：**
- 修复 Session 24 国际化工作导致的构建失败，回退 20+ 文件
- 修复 `LoginScreen.kt` 引用错误（`currentPin` → `authSucceeded`）
- 删除冲突的 `AISettingsInviteSection.kt` 文件
- Debug APK 构建成功 (126MB)

**发布状态评估：**
- 功能层面：达到 Beta 发布水准
- 技术层面：编译通过、测试通过，可进入发布流程
- 当前任务：P0 发布准备（applicationId ✅ → ProGuard 验证 → 版本发布）

---

## 三、技术架构

### 3.1 整体架构

```
┌─────────────────────────────────────────────┐
│                 UI Layer                     │
│  Jetpack Compose + Material Design 3        │
│  28 Screens / 10 Components / 2 Themes      │
├─────────────────────────────────────────────┤
│              ViewModel Layer                 │
│  17 ViewModels + StateFlow + Hilt DI        │
├─────────────────────────────────────────────┤
│              Domain Layer                    │
│  AI Engine / Permission Manager / Security  │
├──────────┬──────────────────────────────────┤
│ Data     │  Repository (11) → DAO (9)       │
│ Layer    │  Room + SQLCipher (AES-256)       │
│          │  Entity (12 tables)              │
├──────────┼──────────────────────────────────┤
│ Service  │  AIService (6 providers)         │
│ Layer    │  OCR / Voice / Image Processing  │
│          │  Backup / Widget Update          │
└──────────┴──────────────────────────────────┘
```

### 3.2 技术栈

| 层级 | 技术选型 | 版本 |
|------|----------|------|
| 语言 | Kotlin | 1.9.20 |
| UI 框架 | Jetpack Compose | BOM 2024.02.00 |
| 设计系统 | Material Design 3 | Compose Material3 |
| 依赖注入 | Hilt (Dagger) | 2.48 |
| 数据库 | Room + SQLCipher | 2.6.1 / 4.5.4 |
| 网络 | Retrofit + OkHttp | 2.9.0 / 4.12.0 |
| 异步 | Kotlin Coroutines + Flow | 1.7.3 |
| 分页 | Paging3 | 3.2.1 |
| 后台任务 | WorkManager | 2.9.0 |
| 图表 | MPAndroidChart | 3.1.0 |
| OCR | ML Kit | 16.0.0 |
| 图片加载 | Coil | 2.5.0 |
| 构建工具 | Gradle (KTS) + KSP | 8.5 / 1.9.20-1.0.14 |
| 最低 SDK | Android 8.0 (API 26) | — |
| 目标 SDK | Android 14 (API 34) | — |
| JDK | Java 17 | 必须（Windows 建议使用独立 JDK17，不要用 Android Studio 自带 JBR21） |

### 3.3 数据流架构

```
用户输入 (文本/语音/图片)
    │
    ▼
NaturalLanguageParser ──→ 提取结构化数据 (金额/日期/分类/备注)
    │
    ▼
AIReasoningEngine ──→ 意图识别 + 置信度评分
    │                   ├─ 身份确认 (最高优先级)
    │                   ├─ 交易修改/删除
    │                   ├─ 记录交易
    │                   ├─ 查询信息
    │                   └─ 数据分析
    ▼
AIPermissionManager ──→ 风险评估
    │                    ├─ CRITICAL: 需人工确认 (删除账户/清空数据)
    │                    ├─ HIGH: 需确认 (修改/删除交易)
    │                    ├─ MEDIUM: 自动授权 (创建交易)
    │                    └─ LOW/MINIMAL: 直接执行 (查询/聊天)
    ▼
AIOperationExecutor ──→ 执行操作 → Database
    │
    ▼
AIPermissionLog ──→ 审计日志 (全量记录)
```

### 3.4 安全架构

| 安全层 | 实现方式 | 保护范围 |
|--------|----------|----------|
| 访问控制 | PIN (SHA-256 + Salt) + 生物识别 | 应用入口 |
| 数据加密 | SQLCipher (AES-256-GCM) | 全量数据库 |
| 密钥存储 | Android Keystore + PBKDF2 派生 | 加密密钥 |
| 配置加密 | EncryptedSharedPreferences | API Key 等敏感配置 |
| 操作审计 | AIPermissionLog (全量记录) | AI 操作行为 |
| 防暴力破解 | 5 次失败锁定 30 分钟 | 登录入口 |

### 3.5 数据库 Schema (v7)

| 表名 | 字段数 | 索引 | 外键 | 用途 |
|------|--------|------|------|------|
| accounts | 10 | — | — | 账户管理 |
| categories | 10 | — | — | 分类管理 (支持层级) |
| transactions | 13 | 4 | 2 | 交易记录 (核心表) |
| budgets | 9 | 1 | 1 | 预算管理 |
| transaction_templates | 8 | — | — | 快捷模板 |
| ai_conversations | 8 | — | — | AI 对话历史 |
| chat_sessions | 6 | — | — | 会话管理 |
| chat_messages | 6 | — | — | 消息存储 |
| chat_memories | 7 | — | — | 上下文记忆 |
| ai_permission_logs | 15 | — | — | 权限审计日志 |
| tags | 5 | — | — | 标签 (待启用) |
| transaction_tags | 2 | — | — | 交易-标签关联 (待启用) |
| custom_butlers | 18+ | — | — | 自定义管家市场 |

---

## 四、UI/UX 设计规范

### 4.1 设计系统现状

项目采用 Material Design 3 作为基础设计系统，附加马年 2026 节日主题。

**主题模式：**

| 模式 | 主色调 | 背景色 | 适用场景 |
|------|--------|--------|----------|
| Light | #2196F3 (蓝) | #FFFBFE (白) | 日间默认 |
| Dark | #2196F3 (蓝) | #1A1A2E (深灰) | 夜间护眼 |
| AMOLED | #2196F3 (蓝) | #000000 (纯黑) | OLED 省电 |
| Dynamic | 系统取色 | 系统取色 | Android 12+ |
| Horse 2026 | #E53935 (红) + #FFD700 (金) | 渐变红 | 马年节日 |

**功能色定义：**

| 语义 | 色值 | 用途 |
|------|------|------|
| 收入 | #4CAF50 (绿) | 收入金额、收入图表 |
| 支出 | #F44336 (红) | 支出金额、支出图表 |
| 转账 | #2196F3 (蓝) | 转账标识 |
| 警告 | #FF9800 (橙) | 预算超支提醒 |

**字体规范：**
- 遵循 Material 3 Typography 标准
- 13 级字体层级 (Display → Headline → Title → Body → Label)
- 字号范围：11sp ~ 57sp
- 字重：Normal (400) / Medium (500)

### 4.2 导航结构

```
应用启动
├── 首次使用 → SetupPin → InitialSetup → 主界面
└── 已设置 → Login (PIN/生物识别) → 主界面

主界面 (Bottom Navigation × 4)
├── 总览 (Overview / HorseOverview)
│   ├── 年度收支汇总
│   ├── 月度统计卡片
│   ├── 快捷操作入口
│   ├── 分类统计
│   └── 最近交易
├── 明细 (TransactionList / HorseTransaction)
│   ├── 按日期分组
│   ├── 搜索/筛选
│   └── 编辑/删除
├── 统计 (Statistics / HorseStatistics)
│   ├── 月度/年度切换
│   ├── 趋势折线图
│   ├── 分类饼图
│   └── 预算进度
└── 设置 (Settings / HorseSettings)
    ├── AI 助手配置
    ├── 账户管理
    ├── 分类管理
    ├── 预算管理
    ├── 数据导入/导出
    ├── 主题切换
    ├── AI管家（当前角色设置）
    ├── 管家市场（发现/创建/管理自定义管家）
    ├── PIN/生物识别
    ├── 个人中心
    └── 关于/公告

浮层页面
├── AddTransaction (记账)
├── AIAssistant (AI 对话)
├── EditTransaction (编辑)
└── WidgetQuickAdd (悬浮窗快捷记账)
```

### 4.3 Widget 设计规范

| 尺寸 | 信息密度 | 交互 |
|------|----------|------|
| 1×1 | 仅余额 | 点击进入应用 |
| 2×1 | 时间 + 余额 | 点击进入应用 |
| 3×1 | 时间 + 收入/支出 | 点击进入应用 |
| 3×2 | 收入/支出/余额 + 预算进度 | 快捷记账按钮 |
| 4×3 | 完整仪表盘 + 趋势图 + 饼图 | 快捷记账按钮 |

### 4.4 AI 管家角色设计

| 角色 | 人格定位 | 语言风格 |
|------|----------|----------|
| 小财娘 | 温柔贴心的财务助手 | 亲切、鼓励型 |
| 桃桃 | 活泼开朗的记账伙伴 | 轻松、幽默型 |
| 顾沉 | 沉稳专业的理财顾问 | 严谨、分析型 |
| 苏浅 | 知性优雅的财务管家 | 温和、建议型 |
| 易水寒 | 冷静理性的数据分析师 | 直接、数据型 |

---

## 五、开发规范

### 5.1 代码组织结构

```
app/src/main/java/com/example/aiaccounting/
├── ai/                          # AI 引擎层 (12 文件)
│   ├── AIOperationExecutor      # 操作执行器（真实写库）
│   ├── AIReasoningEngine        # 意图推理引擎（编排动作/上下文连续）
│   ├── AIMessageParser          # 消息解析（金额/日期/备注/查询类型等）
│   ├── AICategoryInferrer       # 分类推断（含子分类语义）
│   ├── AIKeywordMatcher         # 意图检测辅助（关键词匹配）
│   ├── AILocalProcessor         # 本地离线处理器（无网络/远程仅文本时兜底执行）
│   ├── AIInformationSystem      # 信息查询系统
│   ├── NaturalLanguageParser    # 自然语言解析（历史遗留，逐步收敛到 AIMessageParser）
│   ├── QueryIntentParser        # 查询意图解析（历史遗留，逐步收敛到 AIInformationSystem）
│   ├── IdentityConfirmationDetector  # 身份确认检测
│   ├── TransactionModificationHandler # 交易修改处理
│   └── AIPermissionExecutor     # 权限执行器
├── data/
│   ├── local/
│   │   ├── dao/                 # 数据访问对象 (9 文件)
│   │   ├── database/            # Room 数据库定义
│   │   ├── entity/              # 数据实体 (12 表)
│   │   └── converter/           # 类型转换器
│   ├── model/                   # 数据模型
│   ├── repository/              # 仓库层 (11 文件)
│   ├── service/                 # 服务层 (AI/OCR/语音)
│   ├── exporter/                # 导出 (CSV/Excel)
│   ├── importer/                # 导入 (账单)
│   └── storage/                 # 存储管理
├── di/                          # Hilt 依赖注入模块 (5 文件)
├── security/                    # 安全层 (PIN/加密/权限)
├── service/                     # Android 服务 (备份)
├── ui/
│   ├── components/              # 可复用组件 (10 文件)
│   │   └── charts/              # 图表组件
│   ├── navigation/              # 导航定义
│   ├── screens/                 # 页面 (28 文件)
│   ├── theme/                   # 主题系统
│   └── viewmodel/               # ViewModel (17 文件)
├── utils/                       # 工具类
└── widget/                      # 桌面小组件系统 (11 文件)
```

### 5.2 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| Screen | `功能名Screen` | `OverviewScreen`, `AISettingsScreen` |
| ViewModel | `功能名ViewModel` | `OverviewViewModel`, `ExportViewModel` |
| Repository | `实体名Repository` | `TransactionRepository`, `AccountRepository` |
| DAO | `实体名Dao` | `TransactionDao`, `AccountDao` |
| Entity | 实体名 (单数) | `Transaction`, `Account`, `Category` |
| Composable | 帕斯卡命名 | `YearlySummaryCard`, `QuickActionCards` |
| 主题变量 | `Theme名Colors.属性` | `HorseTheme2026Colors.Gold` |

### 5.3 架构规范

| 规则 | 说明 |
|------|------|
| 单向数据流 | UI → ViewModel (事件) → Repository → DAO → DB |
| 状态管理 | StateFlow + collectAsState，禁止 LiveData |
| 协程作用域 | ViewModel 中使用 viewModelScope，禁止 GlobalScope |
| 数据库访问 | 必须通过 Repository，禁止 DAO 直接暴露给 UI |
| 依赖注入 | 全部通过 Hilt，禁止手动实例化 Repository/Service |
| 异步操作 | suspend 函数 + Flow，禁止 runBlocking |
| 数据库迁移 | 必须编写 Migration，禁止 fallbackToDestructiveMigration |

### 5.4 Git 工作流

| 分支 | 用途 | 命名规范 |
|------|------|----------|
| main | 稳定发布版本 | — |
| develop | 开发集成分支 | — |
| feature/* | 功能开发 | `feature/ai-voice-input` |
| fix/* | 缺陷修复 | `fix/widget-crash` |
| release/* | 发布准备 | `release/v1.9.0` |

**提交信息格式：**
```
<type>: <description>

type: feat / fix / refactor / perf / docs / test / chore
```

### 5.5 构建与发布

| 环境 | 签名 | 混淆 | 包名后缀 |
|------|------|------|----------|
| Debug | 自动生成 debug 签名 | 关闭 | `.debug` |
| Release (无密钥) | 回退 debug 签名 | 开启 (R8) | — |
| Release (有密钥) | release keystore | 开启 (R8) | — |

**构建命令：**
```bash
# Windows（Git Bash）建议先指定 JDK 17（示例：Microsoft OpenJDK 17）
export JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.18.8-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"

# Debug
./gradlew assembleDebug

# Release (需设置环境变量)
KEYSTORE_PASSWORD=xxx KEY_PASSWORD=xxx ./gradlew assembleRelease
```

**APK 命名规范：**
```
AI记账_v{版本号}_debug_{日期}_{时间}.apk
示例：AI记账_v1.8.1_debug_20260312_163000.apk
```

---

## 六、团队协作流程

### 6.1 角色职责

| 角色 | 职责范围 | 交付物 |
|------|----------|--------|
| 产品经理 | 需求定义、优先级排序、验收标准 | PRD、用户故事、验收用例 |
| UI/UX 设计 | 视觉设计、交互设计、设计规范 | Figma 设计稿、设计 Token、动效规范 |
| Android 开发 | 功能实现、性能优化、代码审查 | 可编译代码、单元测试 |
| AI 工程师 | NLP 模型调优、意图识别优化 | 解析准确率报告、模型配置 |
| QA 测试 | 功能测试、兼容性测试、性能测试 | 测试报告、缺陷清单 |

### 6.2 迭代流程

```
需求评审 (Day 1)
    │
    ▼
设计评审 (Day 2-3)
    │  产品经理 + 设计师 + 开发 共同参与
    ▼
技术方案评审 (Day 3-4)
    │  开发 + AI 工程师 确认实现方案
    ▼
开发实现 (Day 4-12)
    │  每日站会同步进度
    ▼
代码审查 (持续)
    │  至少 1 人 Review 后合并
    ▼
QA 测试 (Day 12-14)
    │  功能 + 兼容性 + 性能
    ▼
发布 (Day 14)
    │  编译 → 签名 → 分发
    ▼
复盘 (Day 15)
```

### 6.3 质量门禁

| 阶段 | 检查项 | 通过标准 |
|------|--------|----------|
| 代码提交 | 编译通过 | 0 error |
| 代码审查 | 架构规范、命名规范、安全检查 | 至少 1 人 Approve |
| 集成测试 | 核心流程回归 | 记账/统计/导出 全部通过 |
| 发布前 | APK 体积、启动时间、内存占用 | 体积 < 150MB（Debug），Release < 50MB；冷启动 < 3s |

---

### 6.4 Windows 构建常见问题

#### AAPT2 daemon 启动失败
在部分 Windows 环境下，可能出现 `AAPT2 daemon startup failed`（常见原因是系统缺少 VC++ 运行库 / 进程创建受限）。

推荐解决步骤：
1. 先停止 Gradle daemon：
   - `./gradlew --stop`
2. 使用 `--no-daemon` 重新构建：
   - `./gradlew :app:clean :app:assembleDebug --no-daemon`
3. 如果仍失败，可临时在 PowerShell 会话里清空 AAPT2 binary 环境变量（仅用于排查）：
   - `$env:ANDROID_AAPT2_BINARY=""; ./gradlew :app:clean :app:assembleDebug --no-daemon`

> 注：长期方案建议安装/修复 Microsoft Visual C++ Redistributable（通用运行库）。


## 七、现状评估与改进方案

### 7.1 标准化程度评估

| 维度 | 当前状态 | 评分 (1-5) | 说明 |
|------|----------|------------|------|
| 架构分层 | MVVM + Repository + Hilt DI | 4 | 分层清晰，职责明确，少数文件存在跨层调用 |
| 代码规范 | 命名统一，注释中文为主 | 3.5 | 命名规范一致，但部分文件过大（AISettingsScreen 1740 行） |
| 数据库管理 | Room + SQLCipher + Migration 模板 | 4 | 加密完整，已建立 Migration 机制，exportSchema 已开启（v7） |
| 依赖注入 | Hilt 全覆盖 | 4.5 | 所有 Repository/Service/ViewModel 均通过 Hilt 注入 |
| 异步处理 | Coroutines + Flow | 4 | 已消除 GlobalScope 和 runBlocking，Flow 合并已优化 |
| 安全体系 | PIN + 生物识别 + 加密 + 审计 | 4.5 | 多层安全防护，权限审计完整 |
| 测试覆盖 | JUnit + MockK + Espresso | 2 | 测试框架已引入，但实际测试用例覆盖不足 |
| CI/CD | GitHub Actions (build.yml) | 2.5 | 有基础构建流程，缺少自动化测试和发布流程 |
| 文档 | 代码注释为主 | 2 | 缺少 API 文档、架构决策记录 |

**综合标准化评分：3.4 / 5**

### 7.2 视觉设计评估

| 维度 | 当前状态 | 评分 (1-5) | 说明 |
|------|----------|------------|------|
| 设计系统 | Material 3 + 自定义马年主题 | 3.5 | 有完整色彩/字体定义，缺少间距/圆角/阴影统一 Token |
| 色彩一致性 | 功能色定义明确 | 4 | 收入/支出/转账色彩语义清晰 |
| 组件复用 | 10 个共享组件 | 3 | 基础组件已抽取，但 Screen 内仍有大量 private Composable 未复用 |
| 主题切换 | 5 种主题模式 | 4 | 支持亮/暗/AMOLED/动态/马年，切换流畅 |
| 图表可视化 | MPAndroidChart + 自绘 | 3.5 | 趋势图/饼图已实现，样式与 Material 3 融合度一般 |
| 动效 | 基础页面转场 | 2.5 | 缺少微交互动效（按钮反馈、列表动画、数字变化） |
| 适配性 | 单一屏幕尺寸 | 2.5 | 未做平板/折叠屏适配，横屏支持不足 |
| 无障碍 | contentDescription 部分覆盖 | 2.5 | 部分组件有描述，未做系统性无障碍审查 |

**综合视觉评分：3.2 / 5**

### 7.3 改进方案与优先级

#### P0 — 发布前必须完成

| 编号 | 改进项 | 负责角色 | 预估工时 | 影响 |
|------|--------|----------|----------|------|
| P0-1 | 开启 Room exportSchema，建立 Schema 版本基线 | 开发 | 2h | 数据安全 |
| P0-2 | applicationId 从 `com.example` 改为正式包名 | 开发 | 1h | 应用商店发布 |
| P0-3 | 补充 ProGuard 规则验证（release 包功能回归） | QA | 4h | 发布质量 |
| P0-4 | 启用标签系统（数据库已支持，代码被 TODO 注释） | 开发 | 8h | 功能完整性 |

#### P1 — 标准化提升

| 编号 | 改进项 | 负责角色 | 预估工时 | 影响 |
|------|--------|----------|----------|------|
| P1-1 | 建立 Design Token 文件（间距/圆角/阴影/动效时长） | 设计 + 开发 | 16h | 视觉一致性 |
| P1-2 | 拆分大文件：AISettingsScreen → 子组件文件 | 开发 | 8h | 可维护性 |
| P1-3 | 核心流程单元测试（记账/统计/导出） | 开发 | 24h | 代码质量 |
| P1-4 | CI 流程补全（自动测试 + Lint 检查 + APK 产物归档） | 开发 | 16h | 工程效率 |
| P1-5 | 图表组件统一为 Compose Canvas 自绘，替代 MPAndroidChart | 开发 | 24h | 包体积 / 一致性 |

#### P2 — 体验优化

| 编号 | 改进项 | 负责角色 | 预估工时 | 影响 |
|------|--------|----------|----------|------|
| P2-1 | 添加微交互动效（按钮涟漪、列表进入、数字滚动） | 设计 + 开发 | 16h | 用户体验 |
| P2-2 | 平板/折叠屏适配（自适应布局） | 设计 + 开发 | 32h | 用户覆盖 |
| P2-3 | 无障碍审查与修复 | QA + 开发 | 16h | 合规性 |
| P2-4 | SharedPreferences 迁移至 Jetpack DataStore | 开发 | 16h | 技术债务 |
| P2-5 | AI 响应缓存机制 | AI 工程师 | 8h | 性能/成本 |

#### P3 — 长期规划

| 编号 | 改进项 | 负责角色 | 预估工时 | 影响 |
|------|--------|----------|----------|------|
| P3-1 | PDF 财务报表导出 | 开发 | 24h | 功能扩展 |
| P3-2 | 云端同步（可选） | 后端 + 开发 | 80h+ | 多设备支持 |
| P3-3 | iOS 版本（Compose Multiplatform） | 开发 | 200h+ | 平台覆盖 |
| P3-4 | PC 桌面端 | 开发 | 160h+ | 平台覆盖 |

### 7.4 可行性结论

**标准化可行性：高**

项目架构基础扎实（MVVM + Hilt + Room + Compose），已具备标准化的骨架。主要差距在测试覆盖和 CI/CD 流程，这些可以在不改动现有架构的前提下逐步补全。预计 P0 + P1 改进项在 2 个迭代周期（4 周）内可完成。

**美观度提升可行性：中高**

Material 3 设计系统已正确接入，色彩体系和字体层级完整。主要差距在：缺少统一的 Design Token、微交互动效不足、图表与主题融合度一般。这些改进不涉及架构变更，属于 UI 层的渐进式优化。建议优先建立 Design Token，后续改进可逐步推进。

**商业化发布可行性：中**

核心功能完整度高（85%+），安全体系完善。阻塞项为：applicationId 仍为 `com.example`、缺少隐私政策页面、缺少应用商店所需的截图和描述文案。这些是发布前的必要准备工作，技术难度低但需要产品和设计配合。

---

## 八、附录

### 8.1 依赖清单

| 依赖 | 版本 | 用途 | 许可证 |
|------|------|------|--------|
| Jetpack Compose BOM | 2024.02.00 | UI 框架 | Apache 2.0 |
| Hilt | 2.48 | 依赖注入 | Apache 2.0 |
| Room | 2.6.1 | 数据库 ORM | Apache 2.0 |
| SQLCipher | 4.5.4 | 数据库加密 | BSD |
| Retrofit | 2.9.0 | HTTP 客户端 | Apache 2.0 |
| OkHttp | 4.12.0 | 网络层 | Apache 2.0 |
| Coil | 2.5.0 | 图片加载 | Apache 2.0 |
| MPAndroidChart | 3.1.0 | 图表 | Apache 2.0 |
| Apache POI | 5.2.5 | Excel 导出 | Apache 2.0 |
| ML Kit | 16.0.0 | OCR 识别 | Google ToS |
| Paging3 | 3.2.1 | 分页加载 | Apache 2.0 |
| WorkManager | 2.9.0 | 后台任务 | Apache 2.0 |
| Gson | 2.10.1 | JSON 解析 | Apache 2.0 |

### 8.2 文件统计

| 目录 | 文件数 | 说明 |
|------|--------|------|
| ai/ | 12 | AI 引擎 |
| data/ | 45+ | 数据层（DAO/Entity/Repository/Service） |
| di/ | 5 | 依赖注入模块 |
| security/ | 4 | 安全层 |
| ui/screens/ | 28 | 页面 |
| ui/viewmodel/ | 17 | 状态管理 |
| ui/components/ | 10 | 共享组件 |
| ui/theme/ | 4 | 主题系统 |
| widget/ | 11 | 桌面小组件 |
| res/drawable/ | 30+ | 图片/图标资源 |
| res/layout/ | 5+ | Widget XML 布局 |

### 8.3 已完成的优化记录 (v1.8.1)

| 优化项 | 改动 | 效果 |
|--------|------|------|
| Widget 协程安全 | 移除 GlobalScope，改为同步读取 | 消除内存泄漏风险 |
| CSV 导出异步化 | runBlocking → suspend | 消除 ANR 风险 |
| Release 签名 | 根据环境变量自动切换 | 支持 CI/CD 发布 |
| 图片资源压缩 | PNG → WebP | 体积减少 97%（3.18MB → 0.07MB） |
| 数据库迁移保护 | fallbackToDestructiveMigrationFrom(1,2,3) | 保护 v4+ 用户数据 |
| ViewModel Flow 合并 | 4 个 stateIn → 1 个 allStats | 减少内存开销 |
| 本地离线处理器抽离 | AIAssistantViewModel 本地规则迁移至 AILocalProcessor + Hilt 注入 | 降低 god class 复杂度，离线/远程纯文本时可稳定执行写库 |
| 远程 JSON 兼容性增强 | actions 数组支持 type=query；create_account 支持 accountType 字段 | 避免查询动作丢失、避免账户类型误解析 |
| AI 查询性能优化 | 交易列表/最近交易改为 SQL LIMIT；分析类查询避免分类 N+1 | 降低全表读取与多次 DB 查询开销 |
| 安全：组件暴露收敛 | WidgetClickReceiver 改为 exported=false；WidgetQuickAddActivity 保持 exported=false | 降低外部 App 触发敏感行为的风险 |
| 安全：敏感 key 加密存储 | custom model API key 迁移至 SecurityManager 加密区，移除明文回退 | 避免明文落盘导致泄露 |
| 数据库加密修复 | SQLCipher SupportFactory 直接使用 PBKDF2 派生字节，避免 UTF-8 String 往返损坏密钥 | 避免降低密钥熵/错误密钥导致无法解密 |
---

> 文档维护：每次版本发布后更新功能完成度和优化记录。
> 下次评审节点：v1.9.0 发布前。



## Phase 4: 自定义 AI 管家市场系统 (v1.8.1+)

### 1. 核心需求与设计
- **目标**：赋予用户完全自定义 AI 管家设定的能力（如名字、称号、互动口癖及偏好），以替代系统固化的 5 个内置角色。
- **支持导入导出**：方便在不同设备备份或社区玩家互相分享有趣的 AI 设定（JSON 格式）。

### 2. Room 持久化支持
- `AppDatabase` 升级至 **v7** (`MIGRATION_6_7`)。
- 新增表 **`custom_butlers`** 及其对应的实体 `CustomButlerEntity`。支持软删除（Soft Delete）、头像类型（RESOURCE vs LOCAL_PATH）以及 5 项行为控制滑杆信息：
  - `communicationStyle` (简短/啰嗦)
  - `emotionIntensity` (冷淡/热情)
  - `professionalism` (幽默/专业)
  - `humor` (严肃/逗趣)
  - `proactivity` (被动/主动)

### 3. Prompt 生成引擎架构
- `ButlerPromptEngine`：此纯函数的引擎替代了以前写死的巨大 Markdown 文本。
- 它负责读取自定义 Entity 上的各个性格值，自动将其映射至自然语义描述。最终将文本严格控制在 Token 限制（最多不超 800 字）下输出。从而以**无侵入**的方式，喂给下层的通义/Kimi模型去执行标准的 JSON 返回规则。

### 4. 头像（相册）保存与交互设计
- 管家市场同时兼容列表和单项下拉操作。
- 采用 Jetpack Compose 中的 `PhotoPicker` 获取外部图库图片 URI，然后依靠 Stream 文件读写，以 UUID 唯一标识命名，将头像永久性复制到 App 的内部隐私目录 `Context.filesDir/butler_avatars` 中，彻底解决权限过期引发的崩溃。
- **跨设备分享（内嵌封包）：** 在通过 `exportToJson` 导出为文件时，图片会被编码成 Base64 内嵌进单层 Json 封包中。导入端解码即可直接恢复，不会因目录不对而丢失头像。

