<div align="center">
  <h1>
    🤖 AI记账
  </h1>
  <p><strong>基于AI自然语言识别的本地记账应用 · 数据自主可控 · 隐私安全</strong></p>
  <p>
    <a href="#"><img src="https://img.shields.io/badge/Kotlin-1.9.20-blue?style=flat-square&logo=kotlin" alt="Kotlin"></a>
    <a href="#"><img src="https://img.shields.io/badge/Jetpack%20Compose-2023.10.01-green?style=flat-square&logo=android" alt="Compose"></a>
    <a href="#"><img src="https://img.shields.io/badge/MVVM-Architecture-orange?style=flat-square" alt="MVVM"></a>
    <a href="#"><img src="https://img.shields.io/badge/Room-SQLCipher-red?style=flat-square&logo=sqlite" alt="Database"></a>
    <a href="#"><img src="https://img.shields.io/badge/Hilt-DI-yellow?style=flat-square" alt="Hilt"></a>
  </p>
  <p>
    <a href="#-功能特性">功能特性</a> •
    <a href="#-系统架构">系统架构</a> •
    <a href="#-技术栈">技术栈</a> •
    <a href="#-数据库设计">数据库设计</a> •
    <a href="#-开发指南">开发指南</a>
  </p>
</div>

---

## 📋 目录

- [项目简介](#-项目简介)
- [功能特性](#-功能特性)
- [系统架构](#-系统架构)
- [技术栈](#-技术栈)
- [数据库设计](#-数据库设计)
- [UI设计规范](#-ui设计规范)
- [AI模块设计](#-ai模块设计)
- [安全设计](#-安全设计)
- [开发指南](#-开发指南)
- [测试规范](#-测试规范)
- [发布规范](#-发布规范)

---

## 🎯 项目简介

AI记账是一款**开源的Android本地记账应用**，采用先进的AI自然语言识别技术，让用户通过简单的文字描述即可完成记账操作。所有数据**本地存储、加密保护**，无需联网，确保用户隐私绝对安全。

### 核心理念

| 理念 | 说明 |
|------|------|
| 🔒 **隐私优先** | 纯本地存储，数据不上传任何服务器 |
| 🤖 **AI智能** | 自然语言识别，3秒完成记账 |
| 🎨 **现代UI** | Material Design 3 + Jetpack Compose |
| 🔧 **开源免费** | MIT协议，完全开源，永久免费 |

---

## ✨ 功能特性

### 核心功能

| 功能模块 | 功能描述 | 状态 |
|----------|----------|------|
| 🤖 **AI智能记账** | 自然语言输入，自动识别金额、分类、时间 | ✅ |
| 💰 **多账户管理** | 支持现金、银行卡、支付宝、微信等账户 | ✅ |
| 📊 **详细统计** | 收支趋势图、分类饼图、月度对比 | ✅ |
| 🔍 **智能搜索** | 按时间、账户、分类、备注筛选 | ✅ |
| 📤 **数据导出** | Excel报表导出 | ✅ |
| 🎨 **深色模式** | 支持深色/浅色主题切换 | ✅ |

### 安全功能

| 功能 | 说明 |
|------|------|
| 🔐 **数据库加密** | SQLCipher AES-256加密 |
| 🔢 **PIN码保护** | 6位数字PIN码 |
| 👆 **生物识别** | 指纹/面部识别解锁 |
| 🛡️ **应用锁定** | 后台自动锁定 |

### AI功能详情

```
用户输入: "昨天午饭花了35元"
         ↓
AI解析:  {
           "type": "EXPENSE",
           "amount": 35.0,
           "category": "餐饮",
           "date": "2024-01-14",
           "confidence": 0.95
         }
         ↓
确认记账 → 完成
```

---

## 🏗️ 系统架构

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        Presentation Layer                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Screens   │  │  ViewModels │  │   UI Components     │  │
│  │  (Compose)  │  │   (State)   │  │  (Material Design)  │  │
│  └──────┬──────┘  └──────┬──────┘  └─────────────────────┘  │
└─────────┼────────────────┼──────────────────────────────────┘
          │                │
          ▼                ▼
┌─────────────────────────────────────────────────────────────┐
│                        Domain Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Repository │  │   UseCases  │  │      Models         │  │
│  │  (Pattern)  │  │  (Business) │  │   (Entities)        │  │
│  └──────┬──────┘  └─────────────┘  └─────────────────────┘  │
└─────────┼────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                        Data Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │    Room     │  │  DataStore  │  │   SQLCipher         │  │
│  │  (Local DB) │  │  (Prefs)    │  │  (Encryption)       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 模块依赖关系

```
app
├── ai/                    # AI自然语言解析模块
├── data/
│   ├── local/            # 本地数据层
│   │   ├── database/     # Room数据库
│   │   ├── dao/          # 数据访问对象
│   │   ├── entity/       # 数据实体
│   │   └── prefs/        # 偏好设置
│   ├── repository/       # 数据仓库
│   ├── exporter/         # 数据导出
│   └── importer/         # 数据导入
├── di/                    # 依赖注入模块 (Hilt)
├── security/              # 安全模块
├── service/               # 后台服务
├── ui/
│   ├── screens/          # 界面页面
│   ├── viewmodel/        # 视图模型
│   ├── components/       # 可复用组件
│   ├── navigation/       # 导航配置
│   ├── theme/            # 主题配置
│   └── animation/        # 动画库
└── utils/                 # 工具类
```

---

## 🛠️ 技术栈

### 编程语言与平台

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 1.9.20 | 主要开发语言 |
| Java | 17 | 兼容支持 |
| Android SDK | 34 (API 34) | 目标平台 |
| Min SDK | 26 (API 26) | 最低支持 |

### 核心框架

| 框架 | 版本 | 用途 |
|------|------|------|
| Jetpack Compose | BOM 2023.10.01 | UI框架 |
| Material Design 3 | 1.2.0 | 设计系统 |
| Hilt | 2.48 | 依赖注入 |
| Room | 2.6.1 | 本地数据库 |
| Navigation Compose | 2.7.5 | 页面导航 |
| ViewModel | 2.6.2 | 状态管理 |
| DataStore | 1.0.0 | 偏好存储 |

### 安全与加密

| 库 | 版本 | 用途 |
|----|------|------|
| SQLCipher | 4.5.4 | 数据库加密 |
| Security Crypto | 1.1.0-alpha06 | 加密SharedPreferences |
| Biometric | 1.1.0 | 生物识别认证 |

### 数据处理

| 库 | 版本 | 用途 |
|----|------|------|
| Apache POI | 5.2.5 | Excel导出 |
| Gson | 2.10.1 | JSON序列化 |
| MPAndroidChart | v3.1.0 | 图表展示 |

### 网络与图片

| 库 | 版本 | 用途 |
|----|------|------|
| Retrofit | 2.9.0 | REST API (预留) |
| OkHttp | 4.12.0 | HTTP客户端 |
| Coil | 2.5.0 | 图片加载 |

### 测试框架

| 框架 | 版本 | 用途 |
|------|------|------|
| JUnit 4 | 4.13.2 | 单元测试 |
| Espresso | 3.5.1 | UI测试 |
| Compose Test | BOM 2023.10.01 | Compose测试 |

---

## 🗄️ 数据库设计

### ER图

```
┌─────────────┐       ┌─────────────────┐       ┌─────────────┐
│   Account   │       │   Transaction   │       │   Category  │
├─────────────┤       ├─────────────────┤       ├─────────────┤
│ PK id       │◄──────┤ PK id           │──────►│ PK id       │
│    name     │  1:N  │ FK accountId    │  N:1  │    name     │
│    type     │       │ FK categoryId   │       │    type     │
│    balance  │       │    type         │       │    icon     │
│    icon     │       │    amount       │       │    color    │
│    color    │       │    date         │       │    parentId │
│    isDefault│       │    note         │       └─────────────┘
└─────────────┘       │    tags         │              │
                      │    transferAccId│              │
                      │    isRecurring  │              │
                      └─────────────────┘              │
                                                       │
                      ┌─────────────────┐              │
                      │   Transaction   │              │
                      │    Template     │◄─────────────┘
                      ├─────────────────┤         1:N
                      │ PK id           │
                      │    name         │
                      │    accountId    │
                      │    categoryId   │
                      │    type         │
                      │    amount       │
                      │    note         │
                      └─────────────────┘

┌─────────────┐       ┌─────────────────┐
│   Budget    │       │  AIConversation │
├─────────────┤       ├─────────────────┤
│ PK id       │       │ PK id           │
│    categoryId│      │    message      │
│    amount   │       │    response     │
│    period   │       │    timestamp    │
│    startDate│       │    type         │
│    endDate  │       └─────────────────┘
└─────────────┘
```

### 实体详情

#### 1. Transaction (交易记录)

```kotlin
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["date"]),
        Index(value = ["type"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,                    // 账户ID
    val categoryId: Long,                   // 分类ID
    val type: TransactionType,              // 类型: INCOME/EXPENSE/TRANSFER
    val amount: Double,                     // 金额
    val date: Long,                         // 日期 (Unix timestamp)
    val note: String = "",                  // 备注
    val tags: String = "",                  // 标签 (逗号分隔)
    val transferAccountId: Long? = null,    // 转账目标账户
    val attachmentPath: String? = null,     // 附件路径
    val isRecurring: Boolean = false,       // 是否周期性
    val recurringInterval: String? = null,  // 周期: daily/weekly/monthly/yearly
    val recurringEndDate: Long? = null,     // 周期结束日期
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

#### 2. Account (账户)

```kotlin
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                       // 账户名称
    val type: AccountType,                  // 账户类型
    val balance: Double = 0.0,              // 余额
    val icon: String = "💳",                // 图标 (Emoji)
    val color: String = "#2196F3",          // 颜色 (Hex)
    val isDefault: Boolean = false,         // 是否默认账户
    val isArchived: Boolean = false,        // 是否归档
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class AccountType {
    CASH,           // 现金
    BANK,           // 银行账户
    CREDIT_CARD,    // 信用卡
    DEBIT_CARD,     // 借记卡
    ALIPAY,         // 支付宝
    WECHAT,         // 微信
    OTHER           // 其他
}
```

#### 3. Category (分类)

```kotlin
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                       // 分类名称
    val type: TransactionType,              // 类型: INCOME/EXPENSE
    val icon: String = "📁",                // 图标
    val color: String = "#2196F3",          // 颜色
    val isDefault: Boolean = false,         // 是否默认分类
    val parentId: Long? = null,             // 父分类ID (支持二级分类)
    val order: Int = 0,                     // 排序
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class TransactionType {
    INCOME,     // 收入
    EXPENSE,    // 支出
    TRANSFER    // 转账
}
```

#### 4. TransactionTemplate (记账模板)

```kotlin
@Entity(tableName = "transaction_templates")
data class TransactionTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                       // 模板名称
    val accountId: Long,                    // 账户ID
    val categoryId: Long,                   // 分类ID
    val type: TransactionType,              // 交易类型
    val amount: Double? = null,             // 金额 (可选)
    val note: String = "",                  // 备注
    val icon: String = "📝",                // 图标
    val order: Int = 0,                     // 排序
    val createdAt: Long = System.currentTimeMillis()
)
```

#### 5. Budget (预算)

```kotlin
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long?,                  // 分类ID (null表示总预算)
    val amount: Double,                     // 预算金额
    val period: BudgetPeriod,               // 周期类型
    val startDate: Long,                    // 开始日期
    val endDate: Long? = null,              // 结束日期
    val alertThreshold: Double = 0.8,       // 提醒阈值 (80%)
    val isActive: Boolean = true,           // 是否激活
    val createdAt: Long = System.currentTimeMillis()
)

enum class BudgetPeriod {
    DAILY,      // 日预算
    WEEKLY,     // 周预算
    MONTHLY,    // 月预算
    YEARLY,     // 年预算
    CUSTOM      // 自定义
}
```

#### 6. AIConversation (AI对话记录)

```kotlin
@Entity(tableName = "ai_conversations")
data class AIConversation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val message: String,                    // 用户消息
    val response: String,                   // AI回复
    val timestamp: Long = System.currentTimeMillis(),
    val type: ConversationType,             // 对话类型
    val parsedResult: String? = null,       // 解析结果 (JSON)
    val isSuccessful: Boolean = true        // 是否成功
)

enum class ConversationType {
    QUERY,      // 查询
    ADD,        // 添加
    UPDATE,     // 更新
    DELETE,     // 删除
    CHAT        // 闲聊
}
```

### 数据库索引策略

| 表名 | 索引字段 | 用途 |
|------|----------|------|
| transactions | accountId | 按账户查询 |
| transactions | categoryId | 按分类查询 |
| transactions | date | 按日期查询 |
| transactions | type | 按类型查询 |
| transactions | (date, type) | 统计查询优化 |

### 数据库加密

```kotlin
// SQLCipher 加密配置
val factory = SupportFactory(sqlitePassword.toByteArray())

val db = Room.databaseBuilder(
    context,
    AppDatabase::class.java,
    "ai_accounting.db"
)
.openHelperFactory(factory)  // 启用加密
.build()
```

---

## 🎨 UI设计规范

### 设计原则

1. **Material Design 3**: 遵循Google最新设计规范
2. **响应式布局**: 适配不同屏幕尺寸
3. **无障碍支持**: 支持屏幕阅读器和字体缩放
4. **流畅动画**: 60fps动画体验

### 颜色系统

```kotlin
// 主色调
val PrimaryLight = Color(0xFF6B9FFF)      // 浅蓝
val PrimaryDark = Color(0xFF4A7FD9)       // 深蓝

// 功能色
val IncomeColor = Color(0xFF4CAF50)       // 收入 - 绿色
val ExpenseColor = Color(0xFFE53935)      // 支出 - 红色
val TransferColor = Color(0xFF2196F3)     // 转账 - 蓝色

// 暗色主题
val DarkBackground = Color(0xFF1C1B1F)
val DarkSurface = Color(0xFF1C1B1F)
val DarkOnBackground = Color(0xFFE6E1E5)

// 亮色主题
val LightBackground = Color(0xFFFFFBFE)
val LightSurface = Color(0xFFFFFBFE)
val LightOnBackground = Color(0xFF1C1B1F)
```

### 字体规范

| 用途 | 字号 | 字重 |
|------|------|------|
| 标题 | 24sp | Bold |
| 副标题 | 18sp | Medium |
| 正文 | 16sp | Regular |
| 辅助文字 | 14sp | Regular |
| 标签 | 12sp | Medium |

### 间距系统

```kotlin
// 基础间距 (8dp为基准)
val SpaceXS = 4.dp
val SpaceS = 8.dp
val SpaceM = 16.dp
val SpaceL = 24.dp
val SpaceXL = 32.dp
val SpaceXXL = 48.dp

// 圆角
val RadiusS = 4.dp
val RadiusM = 8.dp
val RadiusL = 16.dp
val RadiusXL = 24.dp
```

### 页面结构

```
┌─────────────────────────────┐
│        Status Bar           │
├─────────────────────────────┤
│                             │
│        Content Area         │
│                             │
│                             │
│                             │
│                             │
│                             │
│                             │
├─────────────────────────────┤
│        Bottom Nav           │ (主页面显示)
│      (NavigationBar)        │
└─────────────────────────────┘
```

### 导航结构

```
AppNavigation
├── SetupPin (首次启动)
├── Login (验证PIN)
├── InitialSetup (初始设置)
└── Main (主界面)
    ├── Overview (总览) ← 默认
    ├── Transactions (明细)
    ├── Statistics (统计)
    └── Settings (设置)
        ├── Profile (个人资料)
        ├── Accounts (账户管理)
        ├── Categories (分类管理)
        ├── Templates (模板管理)
        ├── AI Settings (AI设置)
        ├── Export (数据导出)
        └── Import (数据导入)
```

---

## 🤖 AI模块设计

### 架构设计

```
┌─────────────────────────────────────────┐
│           NaturalLanguageParser          │
│  ┌─────────┐ ┌─────────┐ ┌───────────┐  │
│  │ Amount  │ │  Date   │ │ Category  │  │
│  │Extractor│ │Extractor│ │  Matcher  │  │
│  └────┬────┘ └────┬────┘ └─────┬─────┘  │
│       └────────────┴────────────┘        │
│                   │                      │
│                   ▼                      │
│           ┌─────────────┐                │
│           │ Confidence  │                │
│           │ Calculator  │                │
│           └─────────────┘                │
└─────────────────────────────────────────┘
```

### 解析流程

```kotlin
// 1. 输入标准化
fun normalizeInput(input: String): String {
    return input
        .replace("，", ",")
        .replace("。", ".")
        .replace("块", "元")
        .replace("块钱", "元")
}

// 2. 金额提取
fun extractAmount(input: String): Double? {
    val patterns = listOf(
        "(\\d+\\.?\\d*)\\s*[元块]",     // 25元、25块
        "[¥￥]\\s*(\\d+\\.?\\d*)",     // ¥25
        "花了\\s*(\\d+\\.?\\d*)",      // 花了25
    )
    // ... 匹配逻辑
}

// 3. 日期提取
fun extractDate(input: String): Date? {
    return when {
        input.contains("今天") -> Calendar.getInstance().time
        input.contains("昨天") -> getYesterday()
        input.contains("前天") -> getDayBeforeYesterday()
        // ... 其他日期
    }
}

// 4. 分类匹配
fun matchCategory(input: String, categories: List<Category>): String? {
    // 精确匹配
    // 模糊匹配 (关键词)
    // 语义匹配
}

// 5. 置信度计算
fun calculateConfidence(amount: Double?, type: TransactionType?, category: String?): Float {
    var confidence = 0f
    if (amount != null) confidence += 0.4f
    if (type != null) confidence += 0.3f
    if (category != null) confidence += 0.3f
    return confidence
}
```

### 关键词映射

```kotlin
val categoryKeywords = mapOf(
    "餐饮" to listOf("吃", "饭", "餐", "美食", "餐厅", "外卖", "食堂", 
                     "早餐", "午餐", "晚餐", "宵夜", "火锅", "烧烤", "奶茶", "咖啡"),
    "交通" to listOf("车", "地铁", "公交", "打车", "出租", "滴滴", 
                     "高铁", "火车", "飞机", "油费", "停车", "过路费"),
    "购物" to listOf("买", "购", "淘宝", "京东", "拼多多", "天猫", 
                     "超市", "商场", "衣服", "鞋子", "包包", "化妆品"),
    "娱乐" to listOf("玩", "电影", "游戏", "KTV", "唱歌", "旅游", 
                     "旅行", "门票", "会员", "充值", "视频", "音乐"),
    "居住" to listOf("房", "租", "水电", "物业", "煤气", "宽带", 
                     "维修", "家具", "家电", "装修"),
    "医疗" to listOf("药", "医院", "看病", "体检", "医保", "诊所", "牙医", "眼镜"),
    "教育" to listOf("书", "学费", "培训", "课程", "学习", "考试", "资料", "文具"),
    "通讯" to listOf("话费", "流量", "宽带", "手机", "电话"),
    "工资" to listOf("工资", "薪水", "薪酬", "月薪", "年薪", "收入", "发工资"),
    "投资" to listOf("股票", "基金", "理财", "投资", "收益", "利息", "分红")
)
```

### 使用示例

```kotlin
// 简单记账
val result = parser.parse("午饭花了35元")
// Result: ParsedResult(
//   type = EXPENSE,
//   amount = 35.0,
//   category = "餐饮",
//   date = today,
//   confidence = 0.95
// )

// 复杂记账
val result = parser.parse("昨天在超市买水果花了58块5")
// Result: ParsedResult(
//   type = EXPENSE,
//   amount = 58.5,
//   category = "购物",
//   date = yesterday,
//   remark = "水果",
//   confidence = 0.92
// )
```

---

## 🔒 安全设计

### 安全架构

```
┌─────────────────────────────────────────┐
│           Application Layer              │
│  ┌─────────┐ ┌─────────┐ ┌───────────┐  │
│  │  PIN    │ │Biometric│ │ App Lock  │  │
│  │  Auth   │ │  Auth   │ │  Timer    │  │
│  └────┬────┘ └────┬────┘ └─────┬─────┘  │
└───────┼───────────┼────────────┼────────┘
        │           │            │
        ▼           ▼            ▼
┌─────────────────────────────────────────┐
│           Security Manager               │
│         (SecurityManager.kt)             │
└─────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────┐
│           Encryption Layer               │
│  ┌─────────────┐    ┌────────────────┐  │
│  │  SQLCipher  │    │ EncryptedPrefs │  │
│  │ (AES-256)   │    │   (AES-256)    │  │
│  └─────────────┘    └────────────────┘  │
└─────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────┐
│           Storage Layer                  │
│  ┌─────────────┐    ┌────────────────┐  │
│  │  Database   │    │  SharedPrefs   │  │
│  │   Files     │    │     Files      │  │
│  └─────────────┘    └────────────────┘  │
└─────────────────────────────────────────┘
```

### 认证流程

```
首次启动                    日常使用
   │                          │
   ▼                          ▼
┌─────────┐              ┌─────────┐
│ Setup   │              │  App    │
│  PIN    │              │ Launch  │
└────┬────┘              └────┬────┘
     │                        │
     ▼                        ▼
┌─────────┐              ┌─────────┐
│Encrypt  │              │ Check   │
│Database │              │ Timeout │
└────┬────┘              └────┬────┘
     │                        │
     ▼                        ▼
┌─────────┐              ┌─────────┐
│Initial  │         ┌────┤  PIN    │
│ Setup   │         │Yes │  Input  │
└────┬────┘         │    └────┬────┘
     │              │         │
     ▼              │         ▼
┌─────────┐         │    ┌─────────┐
│  Main   │◄────────┘    │Biometric│◄── 可选
│  App    │              │  Auth   │
└─────────┘              └────┬────┘
                              │
                              ▼
                         ┌─────────┐
                         │  Main   │
                         │  App    │
                         └─────────┘
```

### 加密实现

```kotlin
// 数据库加密
class DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        securityManager: SecurityManager
    ): AppDatabase {
        val password = securityManager.getDatabasePassword()
        val factory = SupportFactory(password.toByteArray())
        
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ai_accounting.db"
        )
        .openHelperFactory(factory)
        .build()
    }
}

// SharedPreferences加密
fun provideEncryptedSharedPreferences(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    return EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

// PIN码哈希存储
fun hashPin(pin: String): String {
    val salt = generateSecureRandomSalt()
    return BCrypt.hashpw(pin, salt)
}

fun verifyPin(pin: String, hashedPin: String): Boolean {
    return BCrypt.checkpw(pin, hashedPin)
}
```

### 安全策略

| 策略 | 实现 | 说明 |
|------|------|------|
| 数据库加密 | SQLCipher AES-256 | 全盘加密 |
| 密钥存储 | Android Keystore | 硬件级安全 |
| PIN保护 | BCrypt哈希 | 防暴力破解 |
| 生物识别 | BiometricPrompt | 指纹/面部 |
| 自动锁定 | 5分钟无操作 | 后台保护 |
| 截屏保护 | FLAG_SECURE | 防敏感信息泄露 |

---

## 📱 开发指南

### 环境要求

| 工具 | 版本 | 说明 |
|------|------|------|
| Android Studio | Hedgehog (2023.1.1) | 推荐版本 |
| JDK | 17 | Java开发工具包 |
| Kotlin | 1.9.20 | 编程语言 |
| Gradle | 8.2 | 构建工具 |
| Android SDK | 34 | 目标SDK |

### 项目设置

```bash
# 1. 克隆项目
git clone https://github.com/yourusername/ai-accounting.git
cd ai-accounting

# 2. 使用Android Studio打开
# File → Open → 选择项目文件夹

# 3. 同步Gradle
# 点击 "Sync Now" 或运行
./gradlew sync

# 4. 构建项目
./gradlew build

# 5. 运行测试
./gradlew test
```

### 代码规范

#### Kotlin编码规范

```kotlin
// ✅ 正确: 使用val，函数式风格
fun calculateTotal(transactions: List<Transaction>): Double {
    return transactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }
}

// ❌ 错误: 使用var，命令式风格
fun calculateTotal(transactions: List<Transaction>): Double {
    var total = 0.0
    for (t in transactions) {
        if (t.type == TransactionType.EXPENSE) {
            total += t.amount
        }
    }
    return total
}
```

#### Compose编码规范

```kotlin
// ✅ 正确: 使用remember和derivedStateOf
@Composable
fun TransactionList(transactions: List<Transaction>) {
    val sortedTransactions by remember(transactions) {
        derivedStateOf { transactions.sortedByDescending { it.date } }
    }
    
    LazyColumn {
        items(sortedTransactions, key = { it.id }) { transaction ->
            TransactionItem(transaction)
        }
    }
}

// ✅ 正确: 状态提升
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    AddTransactionContent(
        amount = uiState.amount,
        onAmountChange = viewModel::onAmountChange,
        onSave = viewModel::saveTransaction
    )
}
```

#### 命名规范

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 类名 | PascalCase | `TransactionRepository` |
| 函数名 | camelCase | `getTransactionsByDate()` |
| 变量名 | camelCase | `transactionList` |
| 常量名 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Compose函数 | PascalCase | `TransactionCard()` |
| 资源文件 | snake_case | `ic_add_transaction.xml` |

### Git工作流

```bash
# 1. 创建功能分支
git checkout -b feature/ai-parser-improvement

# 2. 提交代码
git add .
git commit -m "feat: 优化AI解析器金额提取算法

- 支持更多金额格式
- 提高小数识别准确率
- 添加单元测试"

# 3. 推送分支
git push origin feature/ai-parser-improvement

# 4. 创建Pull Request
# 在GitHub上创建PR，等待Code Review

# 5. 合并到主分支
# 通过Review后合并
```

### 提交信息规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type:**
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具

**示例:**
```
feat(ai): 添加图片识别记账功能

- 集成ML Kit文本识别
- 支持小票自动解析
- 添加置信度评估

Closes #123
```

---

## 🧪 测试规范

### 测试金字塔

```
         /\
        /  \
       / E2E\      UI测试 (15%)
      /______\
     /        \
    / Integration\  集成测试 (25%)
   /______________\
  /                \
 /    Unit Tests    \ 单元测试 (60%)
/____________________\
```

### 单元测试

```kotlin
@Test
fun `extractAmount should return correct value for simple input`() {
    // Given
    val input = "花了35元"
    
    // When
    val result = parser.extractAmount(input)
    
    // Then
    assertEquals(35.0, result)
}

@Test
fun `extractAmount should handle decimal values`() {
    // Given
    val input = "超市购物58.5元"
    
    // When
    val result = parser.extractAmount(input)
    
    // Then
    assertEquals(58.5, result)
}
```

### UI测试

```kotlin
@Test
fun addTransactionFlow() {
    // 启动应用
    composeTestRule.setContent {
        AIAccountingTheme {
            MainApp()
        }
    }
    
    // 点击添加按钮
    composeTestRule.onNodeWithContentDescription("添加")
        .performClick()
    
    // 输入金额
    composeTestRule.onNodeWithTag("amount_input")
        .performTextInput("100")
    
    // 选择分类
    composeTestRule.onNodeWithText("餐饮")
        .performClick()
    
    // 保存
    composeTestRule.onNodeWithText("保存")
        .performClick()
    
    // 验证
    composeTestRule.onNodeWithText("记账成功")
        .assertIsDisplayed()
}
```

### 测试覆盖率要求

| 模块 | 覆盖率要求 |
|------|------------|
| Utils | >90% |
| ViewModel | >80% |
| Repository | >70% |
| UI | 关键流程覆盖 |

---

## 🚀 发布规范

### 版本号规范

采用语义化版本控制 (Semantic Versioning):

```
MAJOR.MINOR.PATCH

MAJOR: 不兼容的API修改
MINOR: 向下兼容的功能新增
PATCH: 向下兼容的问题修复

示例: 1.2.3
```

### 发布流程

```
1. 更新版本号
   └── build.gradle.kts
       └── versionCode = 2
       └── versionName = "1.1.0"

2. 更新CHANGELOG.md

3. 创建发布分支
   └── git checkout -b release/1.1.0

4. 最终测试
   └── ./gradlew test
   └── ./gradlew connectedAndroidTest

5. 构建发布版本
   └── ./gradlew assembleRelease

6. 创建Git标签
   └── git tag -a v1.1.0 -m "Release version 1.1.0"
   └── git push origin v1.1.0

7. 创建GitHub Release
   └── 上传APK
   └── 填写Release Notes

8. 合并到主分支
   └── git checkout main
   └── git merge release/1.1.0
```

### 发布检查清单

- [ ] 版本号已更新
- [ ] CHANGELOG已更新
- [ ] 所有测试通过
- [ ] ProGuard规则已验证
- [ ] 签名密钥已配置
- [ ] 应用图标正确
- [ ] 隐私政策已更新
- [ ] 截图已准备
- [ ] Release Notes已编写

---

## 📊 性能指标

### 启动性能

| 指标 | 目标 | 测试方法 |
|------|------|----------|
| 冷启动时间 | < 1.5s | Android Profiler |
| 热启动时间 | < 0.5s | Android Profiler |
| 首帧渲染 | < 16ms | Systrace |

### 运行时性能

| 指标 | 目标 | 说明 |
|------|------|------|
| 内存占用 | < 150MB | 正常使用 |
| 数据库查询 | < 100ms | 1000条记录 |
| 动画帧率 | 60fps | 页面切换 |
| APK大小 | < 50MB | 发布版本 |

### 优化策略

```kotlin
// 1. 数据库查询优化
@Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
suspend fun getTransactionsByDateRange(start: Long, end: Long): List<Transaction>

// 2. 图片加载优化
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(imageUrl)
        .crossfade(true)
        .size(200, 200)
        .build(),
    contentDescription = null
)

// 3. 列表优化
LazyColumn {
    items(
        items = transactions,
        key = { it.id }  // 使用key优化重组
    ) { transaction ->
        TransactionItem(transaction)
    }
}
```

---

## 📚 文档清单

| 文档 | 路径 | 说明 |
|------|------|------|
| 设计文档 | `DESIGN_DOCUMENT.md` | 本文件，完整设计规范 |
| 构建指南 | `BUILD_GUIDE.md` | APK构建详细步骤 |
| 发布清单 | `RELEASE_CHECKLIST.md` | 发布前检查项 |
| 隐私政策 | `PRIVACY_POLICY.md` | 用户隐私说明 |
| 开发进度 | `PROGRESS.md` | 项目进度跟踪 |
| 快速参考 | `QUICK_REFERENCE.md` | 常用代码片段 |

---

## 🤝 贡献指南

### 如何贡献

1. **Fork** 项目
2. **创建分支** (`git checkout -b feature/AmazingFeature`)
3. **提交更改** (`git commit -m 'Add some AmazingFeature'`)
4. **推送分支** (`git push origin feature/AmazingFeature`)
5. **创建Pull Request**

### 代码审查标准

- 代码符合Kotlin规范
- 通过所有测试
- 新增功能包含测试
- 文档已更新
- 无安全漏洞

---

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源协议。

```
MIT License

Copyright (c) 2024 AI记账

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">

**[⬆ 回到顶部](#-目录)**

Made with ❤️ by AI记账团队

</div>
