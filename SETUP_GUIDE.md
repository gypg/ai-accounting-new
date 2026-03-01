# AI记账安卓应用 - 开发环境配置指南

## 一、开发环境准备

### 1. 安装 Android Studio
1. 下载 Android Studio: https://developer.android.com/studio
2. 安装 Android Studio（建议使用最新版本）
3. 安装 Android SDK（API 34 或更高版本）
4. 配置 Android 虚拟设备（AVD）或连接真机

### 2. 安装 JDK
1. 下载 JDK 17: https://adoptium.net/
2. 安装 JDK 17
3. 配置环境变量 JAVA_HOME

### 3. 配置 Gradle
项目已配置 Gradle Wrapper，首次构建时会自动下载 Gradle 8.4

## 二、项目导入和构建

### 1. 导入项目
1. 打开 Android Studio
2. 选择 "Open an Existing Project"
3. 选择项目根目录（new-year 文件夹）
4. 等待 Gradle 同步完成

### 2. 构建项目
1. 点击 Build -> Make Project (Ctrl+F9)
2. 等待构建完成
3. 解决可能的构建错误

### 3. 运行项目
1. 连接安卓设备或启动模拟器
2. 点击 Run -> Run 'app' (Shift+F10)
3. 选择目标设备
4. 等待应用安装和启动

## 三、后端服务部署

### 1. 部署 EasyAccounts 后端
```bash
# 克隆项目
git clone https://github.com/QingHeYang/EasyAccounts.git
cd EasyAccounts

# 启动服务
docker compose up -d

# 访问服务
# http://localhost:10669
```

### 2. 配置后端地址
在应用中配置后端服务器地址：
- 如果使用本地模拟器: `http://10.0.2.2:10669/`
- 如果使用真机: `http://[你的电脑IP]:10669/`

## 四、AI 功能配置

### 1. 获取 API Key
选择一个 AI 服务提供商：
- OpenAI: https://platform.openai.com/
- Claude: https://console.anthropic.com/
- 国内大模型: 如通义千问、文心一言等

### 2. 配置 API Key
在应用的设置页面配置：
- API Key
- 模型名称
- API 基础 URL（如果使用第三方服务）

## 五、项目结构说明

```
app/src/main/java/com/example/aiaccounting/
├── data/                    # 数据层
│   ├── local/              # 本地数据（Room数据库）
│   ├── remote/             # 远程数据（网络API）
│   ├── model/              # 数据模型
│   └── repository/         # 数据仓库
├── domain/                  # 业务逻辑层
│   ├── usecase/            # 用例
│   └── repository/         # 仓库接口
├── ui/                      # UI层
│   ├── screens/            # 屏幕/页面
│   ├── components/         # 可复用组件
│   ├── theme/              # 主题配置
│   └── navigation/         # 导航
├── di/                      # 依赖注入
├── utils/                   # 工具类
├── AIAccountingApplication.kt  # 应用类
└── MainActivity.kt          # 主Activity
```

## 六、开发建议

### 1. 学习资源
- Kotlin 官方文档: https://kotlinlang.org/docs/
- Jetpack Compose: https://developer.android.com/jetpack/compose
- Android 开发指南: https://developer.android.com/guide

### 2. 开发顺序
1. 熟悉项目结构和代码
2. 实现基础记账功能（添加、编辑、删除交易）
3. 实现数据展示和统计
4. 集成 AI 功能
5. 优化 UI 和用户体验
6. 测试和调试

### 3. 调试技巧
- 使用 Android Studio 的 Logcat 查看日志
- 使用断点调试
- 使用 Layout Inspector 检查 UI
- 使用 Network Inspector 检查网络请求

## 七、常见问题

### 1. Gradle 同步失败
- 检查网络连接
- 清理项目: Build -> Clean Project
- 使缓存失效: File -> Invalidate Caches

### 2. 无法连接后端服务
- 检查后端服务是否正常运行
- 检查网络地址配置
- 检查防火墙设置

### 3. AI 功能无响应
- 检查 API Key 是否正确
- 检查网络连接
- 检查 API 配额是否用完

## 八、下一步

1. 完善各个页面的具体功能实现
2. 实现数据同步功能
3. 添加数据备份和恢复功能
4. 优化 UI 设计
5. 添加更多 AI 功能

祝你开发顺利！
