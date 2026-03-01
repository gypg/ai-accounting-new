# AI记账

一款基于AI自然语言识别的本地记账应用。

## 功能特点

- **AI智能记账**: 自然语言输入，自动识别金额、分类、时间
- **多账户管理**: 支持现金、银行卡、支付宝、微信等账户
- **详细统计**: 收支趋势图、分类饼图、月度对比
- **数据安全**: 本地加密存储，支持指纹/面部识别
- **数据备份**: 自动备份，支持Excel导出
- **深色模式**: 支持深色主题

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **架构**: MVVM + Hilt依赖注入
- **数据库**: Room + SQLCipher加密
- **图表**: MPAndroidChart

## 项目结构

```
app/src/main/java/com/example/aiaccounting/
├── ai/                          # AI自然语言解析
├── data/
│   ├── exporter/               # Excel导出
│   ├── local/                  # 本地数据库
│   │   ├── converter/          # 类型转换器
│   │   ├── dao/                # 数据库访问对象
│   │   ├── database/           # 数据库配置
│   │   └── entity/             # 数据实体
│   └── repository/             # 数据仓库
├── di/                          # 依赖注入模块
├── security/                    # 安全相关
├── service/                     # 后台服务
├── ui/
│   ├── animation/              # 动画
│   ├── navigation/             # 导航
│   ├── screens/                # 界面
│   ├── theme/                  # 主题
│   └── viewmodel/              # 视图模型
└── utils/                       # 工具类
```

## 构建APK

### 使用Android Studio
1. 打开项目
2. Build → Generate Signed Bundle / APK
3. 选择APK，创建签名密钥
4. 构建完成后在 `app/release/` 目录获取APK

### 使用命令行
```bash
./gradlew assembleRelease
```

## 使用说明

1. 首次使用设置PIN码
2. 在AI助手页面输入记账内容，如"午饭花了35元"
3. AI自动解析并确认记账

## 隐私说明

- 所有数据存储在本地，不上传服务器
- 数据库使用SQLCipher加密
- 支持生物识别保护

## 开源协议

MIT License
