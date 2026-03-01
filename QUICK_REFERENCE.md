# AI记账应用 - 快速参考指南

## 🎯 项目核心目标

开发一款**纯本地、安全加密、AI智能**的安卓记账应用。

---

## 🔑 三大核心特性

### 1. 纯本地 + 安全加密
- ✅ 所有数据存储在本地
- ✅ SQLCipher数据库加密
- ✅ PIN码验证 + 生物识别
- ✅ API Key加密存储
- ✅ 代码混淆保护

### 2. AI智能助手
- ✅ 自然语言记账
- ✅ 智能查询分析
- ✅ 默认模型 + 自定义配置
- ✅ 对话历史管理

### 3. Excel导出
- ✅ 月度账单导出
- ✅ 自定义时间段导出
- ✅ .xlsx格式
- ✅ 可选密码保护

---

## 📋 开发流程（简化版）

```
第1步：安全系统
├── PIN验证系统
├── 数据库加密
└── 安全检查

第2步：记账功能
├── 账户管理
├── 分类管理
└── 交易记录

第3步：AI助手
├── AI服务集成
├── 自然语言处理
└── 对话界面

第4步：统计导出
├── 数据统计
├── 图表展示
└── Excel导出

第5步：优化完善
├── UI优化
├── 性能优化
└── 测试修复
```

---

## 🛠️ 技术栈（必选）

| 类别 | 技术 | 用途 |
|------|------|------|
| UI | Jetpack Compose | 现代UI框架 |
| 架构 | MVVM + Hilt | 架构模式 + 依赖注入 |
| 数据库 | Room + SQLCipher | 加密数据库 |
| 网络 | Retrofit | AI服务调用 |
| 安全 | Android Keystore | 密钥管理 |
| 导出 | Apache POI | Excel操作 |
| 生物识别 | Biometric | 指纹/面部识别 |
| 图表 | MPAndroidChart | 数据可视化 |

---

## 📁 关键文件位置

### 安全相关
```
security/
├── EncryptionManager.kt      # 加密管理
├── KeyManager.kt             # 密钥管理
├── BiometricHelper.kt        # 生物识别
└── PinValidator.kt           # PIN验证
```

### 数据相关
```
data/
├── local/
│   ├── database/            # 加密数据库
│   ├── dao/                 # 数据访问
│   └── preferences/         # 加密存储
├── model/                   # 数据模型
└── repository/              # 数据仓库
```

### AI相关
```
data/remote/
├── api/AIService.kt         # AI服务接口
└── dto/                     # AI数据传输

service/
└── AIService.kt             # AI服务实现
```

### 导出相关
```
data/exporter/
└── ExcelExporter.kt         # Excel导出器
```

---

## 🔐 安全实现要点

### 1. PIN码验证
```kotlin
// 存储PIN哈希
val pinHash = hashPin(userPin)  // 使用SHA-256
encryptedPrefs.edit().putString("pin_hash", pinHash).apply()

// 验证PIN
fun validatePin(inputPin: String): Boolean {
    val storedHash = encryptedPrefs.getString("pin_hash", null)
    return hashPin(inputPin) == storedHash
}
```

### 2. 数据库加密
```kotlin
// 从PIN派生数据库密钥
val dbKey = deriveKeyFromPin(userPin, salt)

// 创建加密数据库
val factory = SupportFactory(dbKey.toByteArray())
Room.databaseBuilder(...)
    .openHelperFactory(factory)
    .build()
```

### 3. API Key加密
```kotlin
// 使用Android Keystore加密存储
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context, "ai_settings", masterKey, ...
)
```

---

## 🤖 AI集成要点

### 1. 默认配置
```kotlin
object DefaultAIConfig {
    const val API_URL = "https://api.openai.com/v1/"
    const val MODEL = "gpt-3.5-turbo"
    const val TEMPERATURE = 0.7
}
```

### 2. 自然语言解析
```kotlin
// 示例：解析"今天午饭花了25元"
fun parseTransaction(text: String): ParsedTransaction {
    // 提取金额：25
    // 提取日期：今天
    // 匹配分类：餐饮
    // 识别类型：支出
}
```

### 3. 对话记账
```kotlin
// AI提示词模板
val prompt = """
你是记账助手。从用户输入中提取：
- 类型（收入/支出/转账）
- 金额
- 分类
- 日期
- 备注

用户输入：$userInput
返回JSON格式。
"""
```

---

## 📊 Excel导出要点

### 1. Apache POI使用
```kotlin
fun exportToExcel(transactions: List<Transaction>): File {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("交易记录")
    
    // 创建标题行
    // 填充数据
    // 写入文件
    
    return outputFile
}
```

### 2. 导出内容
- 交易明细（日期、类型、分类、金额、账户、备注）
- 收支汇总
- 分类统计

---

## 🎨 UI设计要点

### 1. Material Design 3
- 使用Material 3组件
- 支持动态颜色
- 支持深色模式

### 2. 关键页面
- **首页**：月度概览 + 最近交易 + 快速操作
- **记账**：简化流程 + 快速输入
- **AI助手**：聊天界面 + 快捷操作
- **统计**：图表展示 + 数据分析
- **设置**：安全设置 + AI配置 + 备份

### 3. 导航结构
```
底部导航：
├── 首页
├── 统计
├── 记账（中间大按钮）
├── 账户
└── 设置

浮动按钮：AI助手
```

---

## 📱 用户流程

### 首次使用
```
启动应用
  ↓
设置PIN码
  ↓
（可选）配置生物识别
  ↓
（可选）配置AI服务
  ↓
开始使用
```

### 日常使用
```
打开应用
  ↓
PIN验证/生物识别
  ↓
查看首页概览
  ↓
快速记账 / AI记账 / 查看统计
  ↓
导出报表（可选）
```

---

## ⚠️ 重要注意事项

### 安全方面
- ❌ 不要硬编码API Key
- ❌ 不要明文存储敏感数据
- ❌ 不要跳过PIN验证
- ✅ 必须加密所有敏感数据
- ✅ 必须验证用户身份

### 功能方面
- ❌ 不要依赖网络（基础功能）
- ❌ 不要上传用户数据
- ✅ 必须支持离线使用
- ✅ 必须提供数据导出

### 性能方面
- ❌ 不要阻塞主线程
- ❌ 不要过度消耗内存
- ✅ 启动时间 < 2秒
- ✅ 内存占用 < 100MB

---

## 🚀 快速开始

### 第一步：环境准备
1. 安装Android Studio
2. 安装JDK 17
3. 配置Android SDK

### 第二步：项目导入
1. 打开Android Studio
2. 导入项目文件夹
3. 等待Gradle同步

### 第三步：开始开发
1. 阅读 `DEVELOPMENT_DOCUMENT.md`（完整文档）
2. 参考 `TASK_CHECKLIST.md`（任务清单）
3. 按阶段实施开发

---

## 📚 参考资源

### 必看文档
1. `DEVELOPMENT_DOCUMENT.md` - 完整开发文档
2. `TASK_CHECKLIST.md` - 详细任务清单
3. EasyAccounts项目 - 参考实现

### 在线资源
- Android官方文档
- Jetpack Compose指南
- Material Design 3规范

---

## ✅ 成功标准

### 功能完整
- ✅ 安全验证系统
- ✅ 完整记账功能
- ✅ AI智能助手
- ✅ 数据统计
- ✅ Excel导出
- ✅ 数据备份

### 安全可靠
- ✅ 数据加密存储
- ✅ PIN码验证
- ✅ 防逆向工程
- ✅ 代码混淆

### 用户友好
- ✅ 流畅体验
- ✅ 离线可用
- ✅ 界面美观
- ✅ 操作简单

---

## 💡 给AI助手的话

亲爱的AI助手：

这是一个完整的、可落地的项目。你有完全的自由来实现它：

1. **技术自由**：选择最合适的技术方案
2. **架构自由**：设计最优的架构
3. **实现自由**：用你认为最好的方式实现
4. **创新自由**：提出更好的解决方案

**唯一要求**：满足核心功能需求和安全要求。

现在，开始你的创造之旅吧！🚀

---

**快速参考版本**：1.0  
**创建日期**：2026-02-28  
**用途**：AI助手快速上手指南
