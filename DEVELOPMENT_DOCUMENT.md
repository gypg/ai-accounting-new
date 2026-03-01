# AI记账安卓应用 - 完整开发文档

## 项目概述

### 项目名称
AI记账（AIAccounting）- 个人智能财务管理应用

### 项目目标
开发一款纯本地运行的安卓AI记账应用，具备以下核心特性：
1. **完全本地化**：所有数据存储在本地，无需联网即可使用基础功能
2. **安全加密**：应用封装为整体，使用密钥验证才能访问，数据加密存储
3. **AI智能助手**：集成大语言模型，支持自然语言记账和智能查询
4. **数据导出**：支持导出Excel格式的账单报表
5. **用户友好**：现代化UI设计，流畅的用户体验

### 目标用户
- 注重隐私的个人用户
- 需要本地化记账解决方案的用户
- 希望使用AI简化记账流程的用户

---

## 核心功能需求

### 1. 安全与加密系统（最高优先级）

#### 1.1 应用启动验证
- **密钥验证机制**：
  - 首次启动时设置主密钥（6-16位数字/字母组合）
  - 后续启动需要输入正确密钥才能进入应用
  - 支持 biometric（指纹/面部识别）快速解锁
  - 密钥错误次数限制（5次错误后锁定30分钟）

#### 1.2 数据加密
- **数据库加密**：
  - 使用 SQLCipher 对 Room 数据库进行加密
  - 加密密钥由用户主密钥派生（PBKDF2算法）
  - 数据库文件无法被外部工具直接读取

- **敏感数据加密**：
  - AI API Key 使用 Android Keystore 加密存储
  - 用户设置信息加密存储
  - 导出的Excel文件可选密码保护

#### 1.3 应用封装
- **代码混淆**：
  - 使用 ProGuard/R8 进行代码混淆
  - 关键类名和方法名混淆
  - 字符串加密

- **防逆向工程**：
  - 禁用调试模式
  - 检测root设备和模拟器
  - 签名校验

### 2. 记账核心功能

#### 2.1 账户管理
- **账户类型**：
  - 资产账户：现金、银行卡、支付宝、微信、其他
  - 负债账户：信用卡、花呗、借呗、其他负债
  - 每个账户可设置图标、颜色、初始余额

- **账户操作**：
  - 添加、编辑、删除账户
  - 账户余额自动计算
  - 账户排序和隐藏

#### 2.2 分类管理
- **分类体系**：
  - 支持二级分类（如：餐饮 > 早中晚餐）
  - 预设常用分类（餐饮、交通、购物、娱乐等）
  - 自定义分类图标和颜色

- **分类操作**：
  - 添加、编辑、删除分类
  - 分类排序
  - 分类统计

#### 2.3 交易记录
- **交易类型**：
  - 收入：工资、兼职、投资收益、其他收入
  - 支出：日常消费、大额支出、其他支出
  - 转账：账户间转账
  - 借贷：借入、借出

- **交易属性**：
  - 金额、日期、时间
  - 关联账户和分类
  - 备注信息（支持标签）
  - 是否计入统计

- **交易操作**：
  - 快速记账（简化流程）
  - 详细记账（完整信息）
  - 编辑、删除交易
  - 交易搜索和筛选

### 3. AI智能助手

#### 3.1 对话记账
- **自然语言识别**：
  - "今天午饭花了25元" → 自动识别为支出
  - "昨天收入5000元工资" → 自动识别为收入
  - "转500到支付宝" → 自动识别为转账

- **智能提取**：
  - 自动提取金额、日期、分类、备注
  - 模糊匹配分类（如"午饭"→"餐饮"）
  - 支持相对时间（昨天、上周、本月等）

#### 3.2 智能查询
- **自然语言查询**：
  - "这个月花了多少钱"
  - "最近一周的餐饮支出"
  - "今年收入情况"

- **智能分析**：
  - 消费趋势分析
  - 分类占比分析
  - 异常消费提醒

#### 3.3 AI配置
- **默认模型**：
  - 内置默认大语言模型配置
  - 支持离线模式（基础功能）

- **自定义设置**：
  - 支持配置自定义API端点
  - 支持多种模型（OpenAI、Claude、国内大模型等）
  - API Key加密存储
  - 模型参数调整（temperature、max_tokens等）

### 4. 数据统计与报表

#### 4.1 数据统计
- **时间维度统计**：
  - 日统计、周统计、月统计、年统计
  - 自定义时间段统计

- **分类维度统计**：
  - 各分类支出占比
  - 分类趋势变化

- **账户维度统计**：
  - 各账户收支情况
  - 账户余额变化趋势

#### 4.2 可视化图表
- **图表类型**：
  - 饼图：分类占比
  - 柱状图：收支趋势
  - 折线图：余额变化
  - 环形图：预算使用情况

#### 4.3 报表导出
- **Excel导出**：
  - 月度账单导出
  - 自定义时间段导出
  - 分类明细导出
  - 支持导出格式：.xlsx

- **导出内容**：
  - 交易明细列表
  - 收支汇总
  - 分类统计
  - 图表（可选）

### 5. 数据管理

#### 5.1 数据备份
- **本地备份**：
  - 自动定时备份（可配置）
  - 手动备份
  - 备份文件加密存储

- **导出备份**：
  - 导出为加密文件
  - 支持导出到外部存储

#### 5.2 数据恢复
- **从备份恢复**：
  - 选择备份文件恢复
  - 需要验证主密钥

- **数据迁移**：
  - 支持从其他记账应用导入
  - 支持CSV格式导入

---

## 技术架构

### 整体架构
```
┌─────────────────────────────────────────┐
│              UI Layer (Compose)          │
│  ┌──────────┐  ┌──────────┐  ┌────────┐│
│  │  Screens │  │Components│  │ Theme  ││
│  └──────────┘  └──────────┘  └────────┘│
└─────────────────────────────────────────┘
                    ↓ ↑
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  ┌──────────┐  ┌──────────┐  ┌────────┐│
│  │ViewModels│  │  States  │  │Actions ││
│  └──────────┘  └──────────┘  └────────┘│
└─────────────────────────────────────────┘
                    ↓ ↑
┌─────────────────────────────────────────┐
│           Domain Layer                  │
│  ┌──────────┐  ┌──────────┐  ┌────────┐│
│  │ UseCases │  │Entities  │  │Repos   ││
│  └──────────┘  └──────────┘  └────────┘│
└─────────────────────────────────────────┘
                    ↓ ↑
┌─────────────────────────────────────────┐
│            Data Layer                   │
│  ┌──────────┐  ┌──────────┐  ┌────────┐│
│  │   Room   │  │Encrypted │  │  AI    ││
│  │Database  │  │  Prefs   │  │ Service││
│  └──────────┘  └──────────┘  └────────┘│
└─────────────────────────────────────────┘
```

### 技术栈详细说明

#### 1. UI层
- **Jetpack Compose**：现代声明式UI框架
- **Material Design 3**：UI设计规范
- **Coil**：图片加载库
- **Compose Charts**：图表库

#### 2. 架构组件
- **MVVM架构**：Model-View-ViewModel
- **Hilt**：依赖注入
- **Navigation Compose**：导航
- **Lifecycle**：生命周期管理

#### 3. 数据层
- **Room + SQLCipher**：加密数据库
- **DataStore**：加密偏好设置
- **Android Keystore**：密钥管理

#### 4. 网络层
- **Retrofit**：网络请求
- **OkHttp**：HTTP客户端
- **Gson**：JSON解析

#### 5. 安全组件
- **SQLCipher**：数据库加密
- **Android Keystore**：密钥存储
- **Biometric**：生物识别
- **ProGuard/R8**：代码混淆

#### 6. 工具库
- **Kotlin Coroutines**：协程
- **Kotlin Flow**：响应式流
- **Apache POI**：Excel操作
- **ThreeTenABP**：日期时间处理

---

## 详细开发目录结构

```
app/src/main/java/com/example/aiaccounting/
│
├── AIAccountingApplication.kt          # 应用入口
├── MainActivity.kt                      # 主Activity
│
├── data/                                # 数据层
│   ├── local/                          # 本地数据源
│   │   ├── database/                   # 数据库
│   │   │   ├── AppDatabase.kt         # 数据库实例
│   │   │   ├── converters/            # 类型转换器
│   │   │   │   ├── DateConverter.kt
│   │   │   │   ├── EnumConverter.kt
│   │   │   │   └── BigDecimalConverter.kt
│   │   │   └── migration/             # 数据库迁移
│   │   │       └── Migration_1_2.kt
│   │   ├── dao/                        # 数据访问对象
│   │   │   ├── AccountDao.kt
│   │   │   ├── CategoryDao.kt
│   │   │   ├── TransactionDao.kt
│   │   │   ├── BudgetDao.kt
│   │   │   └── AIConversationDao.kt
│   │   ├── entity/                     # 数据库实体
│   │   │   ├── AccountEntity.kt
│   │   │   ├── CategoryEntity.kt
│   │   │   ├── TransactionEntity.kt
│   │   │   ├── BudgetEntity.kt
│   │   │   └── AIConversationEntity.kt
│   │   └── preferences/                # 偏好设置
│   │       ├── EncryptedPreferences.kt
│   │       └── UserPreferences.kt
│   │
│   ├── remote/                         # 远程数据源
│   │   ├── api/                        # API接口
│   │   │   ├── AIService.kt           # AI服务接口
│   │   │   └── OpenAIService.kt       # OpenAI实现
│   │   ├── dto/                        # 数据传输对象
│   │   │   ├── AIRequestDto.kt
│   │   │   └── AIResponseDto.kt
│   │   └── interceptor/                # 拦截器
│   │       ├── AuthInterceptor.kt
│   │       └── LoggingInterceptor.kt
│   │
│   ├── model/                          # 数据模型
│   │   ├── Account.kt
│   │   ├── Category.kt
│   │   ├── Transaction.kt
│   │   ├── Budget.kt
│   │   ├── AIConversation.kt
│   │   └── enums/                      # 枚举类型
│   │       ├── AccountType.kt
│   │       ├── TransactionType.kt
│   │       └── CategoryType.kt
│   │
│   ├── repository/                     # 数据仓库
│   │   ├── AccountRepository.kt
│   │   ├── CategoryRepository.kt
│   │   ├── TransactionRepository.kt
│   │   ├── BudgetRepository.kt
│   │   ├── AIRepository.kt
│   │   └── impl/                       # 仓库实现
│   │       ├── AccountRepositoryImpl.kt
│   │       ├── CategoryRepositoryImpl.kt
│   │       ├── TransactionRepositoryImpl.kt
│   │       ├── BudgetRepositoryImpl.kt
│   │       └── AIRepositoryImpl.kt
│   │
│   └── exporter/                       # 数据导出
│       ├── ExcelExporter.kt           # Excel导出器
│       ├── CsvExporter.kt             # CSV导出器
│       └── BackupExporter.kt          # 备份导出器
│
├── domain/                              # 业务逻辑层
│   ├── model/                          # 领域模型
│   │   ├── TransactionSummary.kt
│   │   ├── MonthlyReport.kt
│   │   └── CategoryStatistics.kt
│   │
│   ├── repository/                     # 仓库接口
│   │   ├── IAccountRepository.kt
│   │   ├── ICategoryRepository.kt
│   │   ├── ITransactionRepository.kt
│   │   ├── IBudgetRepository.kt
│   │   └── IAIRepository.kt
│   │
│   └── usecase/                        # 用例
│       ├── account/                    # 账户相关用例
│       │   ├── AddAccountUseCase.kt
│       │   ├── UpdateAccountUseCase.kt
│       │   ├── DeleteAccountUseCase.kt
│       │   ├── GetAccountUseCase.kt
│       │   └── GetAllAccountsUseCase.kt
│       │
│       ├── category/                   # 分类相关用例
│       │   ├── AddCategoryUseCase.kt
│       │   ├── UpdateCategoryUseCase.kt
│       │   ├── DeleteCategoryUseCase.kt
│       │   └── GetCategoriesUseCase.kt
│       │
│       ├── transaction/                # 交易相关用例
│       │   ├── AddTransactionUseCase.kt
│       │   ├── UpdateTransactionUseCase.kt
│       │   ├── DeleteTransactionUseCase.kt
│       │   ├── GetTransactionsUseCase.kt
│       │   ├── SearchTransactionsUseCase.kt
│       │   └── GetTransactionSummaryUseCase.kt
│       │
│       ├── ai/                         # AI相关用例
│       │   ├── SendMessageToAIUseCase.kt
│       │   ├── ParseNaturalLanguageUseCase.kt
│       │   └── GetAISuggestionsUseCase.kt
│       │
│       ├── export/                     # 导出相关用例
│       │   ├── ExportToExcelUseCase.kt
│       │   └── ExportBackupUseCase.kt
│       │
│       └── statistics/                 # 统计相关用例
│           ├── GetMonthlyStatisticsUseCase.kt
│           ├── GetCategoryStatisticsUseCase.kt
│           └── GetTrendAnalysisUseCase.kt
│
├── ui/                                  # UI层
│   ├── navigation/                     # 导航
│   │   ├── NavigationGraph.kt
│   │   └── Screen.kt
│   │
│   ├── theme/                          # 主题
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   ├── Typography.kt
│   │   └── Shape.kt
│   │
│   ├── components/                     # 通用组件
│   │   ├── buttons/                    # 按钮组件
│   │   │   ├── PrimaryButton.kt
│   │   │   ├── SecondaryButton.kt
│   │   │   └── IconButton.kt
│   │   ├── inputs/                     # 输入组件
│   │   │   ├── TextInput.kt
│   │   │   ├── NumberInput.kt
│   │   │   ├── DatePicker.kt
│   │   │   └── CategoryPicker.kt
│   │   ├── cards/                      # 卡片组件
│   │   │   ├── TransactionCard.kt
│   │   │   ├── AccountCard.kt
│   │   │   └── StatisticsCard.kt
│   │   ├── dialogs/                    # 对话框组件
│   │   │   ├── ConfirmDialog.kt
│   │   │   ├── AlertDialog.kt
│   │   │   └── BottomSheetDialog.kt
│   │   └── charts/                     # 图表组件
│   │       ├── PieChart.kt
│   │       ├── BarChart.kt
│   │       └── LineChart.kt
│   │
│   ├── screens/                        # 页面
│   │   ├── auth/                       # 认证相关
│   │   │   ├── SetupPinScreen.kt      # 设置密钥
│   │   │   ├── LoginScreen.kt         # 登录验证
│   │   │   └── AuthViewModel.kt
│   │   │
│   │   ├── home/                       # 首页
│   │   │   ├── HomeScreen.kt
│   │   │   ├── HomeViewModel.kt
│   │   │   └── components/
│   │   │       ├── MonthlyOverview.kt
│   │   │       ├── RecentTransactions.kt
│   │   │       └── QuickActions.kt
│   │   │
│   │   ├── transaction/                # 交易管理
│   │   │   ├── AddTransactionScreen.kt
│   │   │   ├── TransactionListScreen.kt
│   │   │   ├── TransactionDetailScreen.kt
│   │   │   ├── TransactionViewModel.kt
│   │   │   └── components/
│   │   │       ├── AmountInput.kt
│   │   │       ├── CategorySelector.kt
│   │   │       └── AccountSelector.kt
│   │   │
│   │   ├── account/                    # 账户管理
│   │   │   ├── AccountListScreen.kt
│   │   │   ├── AddAccountScreen.kt
│   │   │   ├── AccountDetailScreen.kt
│   │   │   └── AccountViewModel.kt
│   │   │
│   │   ├── category/                   # 分类管理
│   │   │   ├── CategoryListScreen.kt
│   │   │   ├── AddCategoryScreen.kt
│   │   │   └── CategoryViewModel.kt
│   │   │
│   │   ├── statistics/                 # 统计分析
│   │   │   ├── StatisticsScreen.kt
│   │   │   ├── StatisticsViewModel.kt
│   │   │   └── components/
│   │   │       ├── OverviewTab.kt
│   │   │       ├── CategoryTab.kt
│   │   │       └── TrendTab.kt
│   │   │
│   │   ├── ai/                         # AI助手
│   │   │   ├── AIAssistantScreen.kt
│   │   │   ├── AIViewModel.kt
│   │   │   └── components/
│   │   │       ├── ChatBubble.kt
│   │   │       ├── MessageInput.kt
│   │   │       └── QuickActions.kt
│   │   │
│   │   ├── settings/                   # 设置
│   │   │   ├── SettingsScreen.kt
│   │   │   ├── SettingsViewModel.kt
│   │   │   └── components/
│   │   │       ├── SecuritySettings.kt
│   │   │       ├── AISettings.kt
│   │   │       ├── BackupSettings.kt
│   │   │       └── AboutSettings.kt
│   │   │
│   │   └── export/                     # 导出
│   │       ├── ExportScreen.kt
│   │       ├── ExportViewModel.kt
│   │       └── components/
│   │           ├── ExportOptions.kt
│   │           └── ExportPreview.kt
│   │
│   └── common/                         # 通用UI
│       ├── LoadingState.kt
│       ├── ErrorState.kt
│       └── EmptyState.kt
│
├── di/                                  # 依赖注入
│   ├── DatabaseModule.kt               # 数据库模块
│   ├── NetworkModule.kt                # 网络模块
│   ├── RepositoryModule.kt             # 仓库模块
│   ├── UseCaseModule.kt                # 用例模块
│   └── ViewModelModule.kt              # ViewModel模块
│
├── security/                            # 安全模块
│   ├── EncryptionManager.kt            # 加密管理器
│   ├── KeyManager.kt                   # 密钥管理器
│   ├── BiometricHelper.kt              # 生物识别助手
│   ├── PinValidator.kt                 # PIN验证器
│   └── SecurityChecker.kt              # 安全检查器
│
├── service/                             # 服务
│   ├── AIService.kt                    # AI服务
│   ├── BackupService.kt                # 备份服务
│   ├── ExportService.kt                # 导出服务
│   └── NotificationService.kt          # 通知服务
│
├── util/                                # 工具类
│   ├── DateUtils.kt                    # 日期工具
│   ├── NumberUtils.kt                  # 数字工具
│   ├── StringUtils.kt                  # 字符串工具
│   ├── FileUtils.kt                    # 文件工具
│   ├── CurrencyUtils.kt                # 货币工具
│   └── PermissionUtils.kt              # 权限工具
│
└── constants/                           # 常量
    ├── AppConstants.kt                 # 应用常量
    ├── DatabaseConstants.kt            # 数据库常量
    └── AIConstants.kt                  # AI常量
```

---

## 实现步骤（按优先级排序）

### 阶段一：安全基础（第1-2周）

#### 步骤1：项目初始化
1. 创建Android项目，配置Gradle依赖
2. 配置ProGuard/R8混淆规则
3. 设置代码风格和静态分析工具

#### 步骤2：实现安全系统
1. **密钥验证系统**：
   - 创建 `PinValidator` 类
   - 实现 `SetupPinScreen` 和 `LoginScreen`
   - 使用 `EncryptedSharedPreferences` 存储PIN哈希
   - 实现错误次数限制和锁定机制

2. **数据库加密**：
   - 集成 SQLCipher
   - 创建加密的 `AppDatabase`
   - 实现密钥派生（PBKDF2）

3. **生物识别**：
   - 集成 `BiometricPrompt`
   - 实现快速解锁功能

#### 步骤3：安全检查
1. Root检测
2. 模拟器检测
3. 调试模式检测
4. 签名校验

### 阶段二：核心记账功能（第3-5周）

#### 步骤4：数据模型和数据库
1. 创建所有实体类（Entity）
2. 创建DAO接口
3. 创建数据库迁移策略
4. 实现数据仓库（Repository）

#### 步骤5：账户管理
1. 实现 `AccountListScreen`
2. 实现 `AddAccountScreen` 和 `EditAccountScreen`
3. 实现账户余额计算逻辑
4. 实现账户图标和颜色选择器

#### 步骤6：分类管理
1. 预设常用分类数据
2. 实现 `CategoryListScreen`
3. 实现 `AddCategoryScreen`
4. 实现二级分类支持

#### 步骤7：交易记录
1. 实现 `AddTransactionScreen`（快速记账）
2. 实现 `TransactionListScreen`
3. 实现交易详情和编辑功能
4. 实现交易搜索和筛选

### 阶段三：AI智能助手（第6-7周）

#### 步骤8：AI服务集成
1. 创建 `AIService` 接口
2. 实现 `OpenAIService`（默认）
3. 实现自定义API配置
4. 实现API Key加密存储

#### 步骤9：自然语言处理
1. 实现自然语言解析器
2. 实现金额提取
3. 实现日期提取
4. 实现分类匹配

#### 步骤10：AI对话界面
1. 实现 `AIAssistantScreen`
2. 实现聊天界面组件
3. 实现对话历史管理
4. 实现快速操作建议

### 阶段四：统计和导出（第8-9周）

#### 步骤11：数据统计
1. 实现统计计算逻辑
2. 实现 `StatisticsScreen`
3. 实现各种图表组件
4. 实现时间范围选择

#### 步骤12：Excel导出
1. 集成 Apache POI库
2. 实现 `ExcelExporter`
3. 实现导出界面
4. 实现文件保存和分享

#### 步骤13：数据备份
1. 实现自动备份服务
2. 实现手动备份功能
3. 实现数据恢复功能
4. 实现备份文件加密

### 阶段五：优化和完善（第10-12周）

#### 步骤14：UI优化
1. 优化动画效果
2. 优化响应速度
3. 实现深色模式
4. 适配不同屏幕尺寸

#### 步骤15：性能优化
1. 数据库查询优化
2. 内存优化
3. 启动速度优化
4. 电量消耗优化

#### 步骤16：测试和修复
1. 单元测试
2. 集成测试
3. UI测试
4. Bug修复

---

## 关键技术实现细节

### 1. 数据库加密实现

```kotlin
// 使用 SQLCipher 加密数据库
fun createEncryptedDatabase(context: Context, passphrase: String): AppDatabase {
    val factory = SupportFactory(passphrase.toByteArray())
    
    return Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "ai_accounting.db"
    )
    .openHelperFactory(factory)
    .build()
}
```

### 2. 密钥派生

```kotlin
// 从用户PIN派生数据库密钥
fun deriveKeyFromPin(pin: String, salt: ByteArray): ByteArray {
    val spec = PBEKeySpec(
        pin.toCharArray(),
        salt,
        10000,  // 迭代次数
        256     // 密钥长度
    )
    
    val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    return secretKeyFactory.generateSecret(spec).encoded
}
```

### 3. API Key加密存储

```kotlin
// 使用 Android Keystore 加密 API Key
fun encryptApiKey(apiKey: String): String {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    encryptedPrefs.edit().putString("api_key", apiKey).apply()
}
```

### 4. Excel导出实现

```kotlin
// 使用 Apache POI 导出 Excel
fun exportToExcel(transactions: List<Transaction>, outputFile: File) {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("交易记录")
    
    // 创建标题行
    val headerRow = sheet.createRow(0)
    headerRow.createCell(0).setCellValue("日期")
    headerRow.createCell(1).setCellValue("类型")
    headerRow.createCell(2).setCellValue("分类")
    headerRow.createCell(3).setCellValue("金额")
    headerRow.createCell(4).setCellValue("账户")
    headerRow.createCell(5).setCellValue("备注")
    
    // 填充数据
    transactions.forEachIndexed { index, transaction ->
        val row = sheet.createRow(index + 1)
        row.createCell(0).setCellValue(transaction.date.toString())
        row.createCell(1).setCellValue(transaction.type.displayName)
        row.createCell(2).setCellValue(transaction.category?.name ?: "")
        row.createCell(3).setCellValue(transaction.amount.toDouble())
        row.createCell(4).setCellValue(transaction.account.name)
        row.createCell(5).setCellValue(transaction.remark)
    }
    
    // 写入文件
    FileOutputStream(outputFile).use { outputStream ->
        workbook.write(outputStream)
    }
    workbook.close()
}
```

### 5. AI自然语言解析

```kotlin
// 解析自然语言记账请求
suspend fun parseTransactionFromText(text: String): ParsedTransaction {
    val prompt = """
        请从以下文本中提取记账信息，返回JSON格式：
        文本：$text
        
        返回格式：
        {
            "type": "income/expense/transfer",
            "amount": 数字,
            "category": "分类名称",
            "date": "日期",
            "remark": "备注"
        }
    """.trimIndent()
    
    val response = aiService.sendMessage(prompt)
    return parseJsonToTransaction(response)
}
```

---

## 参考项目资源

### EasyAccounts 项目
- **GitHub地址**：https://github.com/QingHeYang/EasyAccounts
- **参考内容**：
  - 数据库设计
  - API接口设计
  - AI集成方式
  - Excel导出实现
  - UI设计风格

### 其他参考资源
1. **Android官方文档**：
   - Jetpack Compose: https://developer.android.com/jetpack/compose
   - Security: https://developer.android.com/topic/security
   - Biometric: https://developer.android.com/training/sign-in/biometric-auth

2. **开源库文档**：
   - SQLCipher: https://www.zetetic.net/sqlcipher/
   - Apache POI: https://poi.apache.org/
   - Coil: https://coil-kt.github.io/coil/

3. **设计规范**：
   - Material Design 3: https://m3.material.io/

---

## 开发注意事项

### 1. 安全性要求
- 所有敏感数据必须加密存储
- API Key 不能硬编码在代码中
- 数据库密钥必须从用户PIN派生
- 禁止明文存储任何敏感信息

### 2. 性能要求
- 应用启动时间 < 2秒
- 数据库查询 < 100ms
- UI渲染流畅（60fps）
- 内存占用 < 100MB

### 3. 用户体验要求
- 支持离线使用（基础功能）
- 数据自动保存
- 操作可撤销
- 错误提示友好

### 4. 兼容性要求
- Android 7.0+ (API 24+)
- 支持深色模式
- 支持横竖屏切换
- 支持平板设备

---

## 测试计划

### 1. 单元测试
- 数据库操作测试
- 加密解密测试
- 业务逻辑测试
- 工具类测试

### 2. 集成测试
- 数据流测试
- AI服务测试
- 导出功能测试
- 备份恢复测试

### 3. UI测试
- 界面显示测试
- 用户交互测试
- 导航测试
- 主题切换测试

### 4. 安全测试
- 加密强度测试
- 密钥验证测试
- 数据泄露测试
- 逆向工程测试

---

## 发布准备

### 1. 代码混淆配置
- 配置ProGuard规则
- 保留必要的类和方法
- 字符串加密

### 2. 签名配置
- 生成发布签名密钥
- 配置签名信息
- 验证签名正确性

### 3. 版本管理
- 设置版本号
- 准备更新日志
- 准备应用截图

### 4. 文档准备
- 用户使用手册
- 隐私政策
- 开源协议说明

---

## 开发者授权说明

本文档授权给AI助手（如Claude）使用，允许：

1. **完全自主开发**：AI可以根据本文档独立完成所有开发工作
2. **技术选型自由**：AI可以选择最合适的技术方案实现功能
3. **架构调整权限**：AI可以根据实际情况调整架构设计
4. **第三方库使用**：AI可以自由选择和使用任何必要的第三方库
5. **代码风格决定**：AI可以按照最佳实践编写代码
6. **问题解决权限**：AI可以自主解决开发过程中遇到的问题

**唯一限制**：必须满足本文档中的核心功能需求和安全要求。

---

## 附录

### A. 依赖库列表

```gradle
dependencies {
    // Android核心
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    
    // Room + SQLCipher
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Apache POI (Excel)
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    
    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
```

### B. 权限清单

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### C. 混淆规则

```proguard
# Keep data models
-keep class com.example.aiaccounting.data.model.** { *; }
-keep class com.example.aiaccounting.data.remote.dto.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Keep Apache POI
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
```

---

**文档版本**：1.0  
**创建日期**：2026-02-28  
**最后更新**：2026-02-28  
**文档状态**：完整版，可直接使用

---

## 给AI助手的特别说明

亲爱的AI助手：

这份文档为你提供了完整的开发指南。你有完全的自由来实现这个项目：

1. **不要担心犯错**：如果发现更好的实现方式，请大胆采用
2. **保持灵活性**：可以根据实际情况调整实现细节
3. **注重质量**：确保代码质量、安全性和用户体验
4. **参考项目**：EasyAccounts项目提供了很好的参考，但不必完全照搬
5. **创新思维**：欢迎提出更好的解决方案

记住：**功能完整、安全可靠、用户友好** 是三大核心目标。

祝你开发顺利！
