# AI记账

一款基于AI自然语言识别的本地记账Android应用。

## 项目背景

这个项目源于一个温馨的小故事：

> 我在家里部署了 [EasyAccounts](https://github.com/QingHeYang/EasyAccounts) 这个开源记账软件（一个优秀的Docker部署Web版记账工具），父亲看到后很好奇。当我告诉他这是部署在网页上的记账软件时，他说："给我用都不用，太麻烦了，手机上的才方便。"

父亲的话让我意识到，对于长辈来说，手机App的便捷性远胜于Web应用。于是决定开发一款专为手机设计的AI记账App，让像父亲这样的用户能够随时随地轻松记账。

**特别感谢**: 本项目在功能设计和AI记账理念上借鉴了 [EasyAccounts](https://github.com/QingHeYang/EasyAccounts) 项目，感谢原作者的开源贡献！

## 功能特点

- **AI智能记账**: 自然语言输入，自动识别金额、分类、时间
- **AI对话助手**: 支持连续对话，可查询账目、获取财务建议
- **图片识别记账**: 拍照识别收据、账单自动记账
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
- **AI服务**: 支持OpenAI、Claude等主流大模型API

## 项目结构

```
app/src/main/java/com/example/aiaccounting/
├── ai/                          # AI自然语言解析与操作执行
├── data/
│   ├── exporter/               # Excel导出
│   ├── importer/               # 数据导入
│   ├── local/                  # 本地数据库
│   │   ├── converter/          # 类型转换器
│   │   ├── dao/                # 数据库访问对象
│   │   ├── database/           # 数据库配置
│   │   └── entity/             # 数据实体
│   ├── model/                  # 数据模型
│   ├── repository/             # 数据仓库
│   └── service/                # AI服务
├── di/                          # 依赖注入模块
├── security/                    # 安全相关
├── service/                     # 后台服务
├── ui/
│   ├── animation/              # 动画
│   ├── components/             # 可复用组件
│   ├── navigation/             # 导航
│   ├── screens/                # 界面
│   ├── theme/                  # 主题
│   └── viewmodel/              # 视图模型
└── utils/                       # 工具类
```

## 交付与验收文档

- [本阶段交付总结](docs/DELIVERY_SUMMARY_2026_03_25.md)
- [阶段性交付验收清单（P0）](docs/ACCEPTANCE_CHECKLIST_P0.md)
- [构建与发布指南](BUILD_GUIDE.md)
- [发布检查清单](RELEASE_CHECKLIST.md)

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
4. 支持图片识别：点击输入框的图片按钮，选择收据照片

## 隐私说明

- 所有数据存储在本地，不上传服务器
- 数据库使用SQLCipher加密
- 支持生物识别保护
- AI功能需要配置自己的API Key，数据直接发送给AI服务商

## 相关项目与致谢

- [EasyAccounts](https://github.com/QingHeYang/EasyAccounts) - 优秀的中文家庭记账软件，支持Docker部署
- [Payment Webfont](https://github.com/orlandotm/payment-webfont) - 支付系统图标库，本项目银行卡图标参考自此项目

## 开源协议

MIT License

MIT License
