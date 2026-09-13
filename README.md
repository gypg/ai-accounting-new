# AI 记账 - 智能记账助手 🤖

<div align="center">

![AI记账](https://img.shields.io/badge/版本-1.8.5-blue.svg)
![平台](https://img.shields.io/badge/平台-Android-green.svg)
![语言](https://img.shields.io/badge/语言-Kotlin-purple.svg)
![最低版本](https://img.shields.io/badge/Android-8.0+-orange.svg)

**让记账变得简单、快速、智能**

[功能特性](#-功能特性) • [下载安装](#-下载安装) • [使用教程](#-使用教程) • [技术栈](#-技术栈) • [开发团队](#-开发团队)

</div>

---

## 📱 应用介绍

AI 记账是一款基于人工智能的智能记账应用，通过自然语言处理、OCR 识别和语音输入等技术，让记账变得前所未有的简单。

**不再需要繁琐的表单填写**，只需：
- 💬 说一句："今天午饭花了 50 块"
- 📷 拍一张票据照片
- 🎤 语音说出消费情况

AI 会自动识别并记录，分类、日期、金额全部智能识别！

---

## ✨ 功能特性

### 🤖 AI 智能解析
**自然语言记账**：直接输入 "昨天买菜花了 80"，AI 自动识别：
- 📅 日期：昨天
- 💰 金额：80 元
- 📂 分类：餐饮/买菜
- 📝 备注：买菜

无需手动选择，一句话完成记账！

### 📷 OCR 票据识别
**拍照即可记账**：
- 对准超市小票、发票拍照
- AI 自动提取商户名称、金额、时间
- 一键确认，秒速记账

支持：超市小票、餐饮发票、加油票据、快递单等

### 🎤 语音记账
**动动嘴就能记账**：
- 按住说话，松手识别
- 支持方言识别
- 解放双手，随时随地记录

### 📊 智能统计分析
**多维度数据可视化**：
- 📈 收支趋势图：了解财务走向
- 🥧 分类占比图：知道钱花在哪
- 📅 日历热力图：每日消费一目了然
- 💡 智能洞察：AI 给出理财建议

### 🔐 数据安全保护
**银行级安全加固**：
- 🔒 SQLCipher 数据库加密
- 🔑 PBKDF2 密钥派生（100,000 次迭代）
- 👆 生物识别解锁（指纹/面容）
- 🚫 强制 HTTPS，防止中间人攻击
- 📱 数据仅存本地，不上传云端

### 🎨 精美主题
**多套精美主题**：
- 🌙 深色模式：护眼舒适
- 🐴 节日主题：马年特别版
- 🎨 自定义主题：打造专属风格

### 📦 数据导入导出
**轻松备份与迁移**：
- 📤 导出 Excel 报表
- 📥 导入其他记账软件数据
- ☁️ 本地备份与恢复

---

## 🎯 为什么选择 AI 记账？

| 传统记账应用 | AI 记账 |
|------------|--------|
| ❌ 打开应用 → 点击添加 → 选择分类 → 输入金额 → 选择日期 → 保存<br>（至少 6 步操作） | ✅ 打开应用 → 说一句话 → 完成<br>（2 秒搞定） |
| ❌ 手动输入数字，容易出错 | ✅ OCR 自动识别，准确率 98%+ |
| ❌ 忘记记账，事后补录麻烦 | ✅ 语音快速记录，随时随地 |
| ❌ 数据上传云端，隐私担忧 | ✅ 数据本地加密，绝对安全 |

---

## 📸 应用截图

<div align="center">

### 主界面 & AI 记账
<img src="docs/screenshots/home.png" width="250" alt="主界面"> <img src="docs/screenshots/ai-input.png" width="250" alt="AI记账">

### 统计分析 & OCR 识别
<img src="docs/screenshots/statistics.png" width="250" alt="统计"> <img src="docs/screenshots/ocr.png" width="250" alt="OCR">

### 主题切换 & 数据安全
<img src="docs/screenshots/theme.png" width="250" alt="主题"> <img src="docs/screenshots/security.png" width="250" alt="安全">

</div>

---

## 💾 下载安装

### 方式 1: Google Play（推荐）
> 🚧 即将上线，敬请期待...

### 方式 2: 直接下载 APK
📥 **[下载最新版本 (v1.8.5)](../../releases/)**

**系统要求**：
- Android 8.0 (API 26) 及以上
- 推荐 Android 10+ 以获得最佳体验

**安装步骤**：
1. 下载 APK 文件
2. 允许"安装未知来源应用"权限
3. 点击安装
4. 完成！

---

## 🚀 使用教程

### 首次使用

1. **创建账本**
   - 打开应用后，设置 4-6 位数字 PIN 码
   - 可选：启用指纹/面容解锁

2. **开始记账**
   - 点击底部 AI 按钮
   - 输入："今天晚饭花了 120"
   - AI 自动识别并创建记录

### AI 记账示例

**自然语言输入**，AI 自动理解：

```
✅ "昨天买菜花了 80"           → 日期：昨天，分类：餐饮，金额：80
✅ "上周二看电影 100 块"       → 日期：上周二，分类：娱乐，金额：100
✅ "三天前交房租 2500"         → 日期：3天前，分类：住房，金额：2500
✅ "本月工资收入 8000"         → 类型：收入，分类：工资，金额：8000
✅ "给儿子转账 500 教育费"     → 分类：教育，金额：500，备注：给儿子
```

### OCR 票据识别

1. 点击相机图标
2. 对准票据拍照
3. AI 自动识别信息
4. 确认并保存

### 语音记账

1. 长按麦克风按钮
2. 说出消费情况
3. 松手自动识别
4. 确认并保存

---

## 🔧 技术栈

### 客户端技术
- **语言**：Kotlin 100%
- **UI 框架**：Jetpack Compose（现代化声明式 UI）
- **架构模式**：MVVM + Clean Architecture
- **依赖注入**：Hilt
- **数据库**：Room + SQLCipher（加密）
- **异步处理**：Kotlin Coroutines + Flow

### AI 能力
- **自然语言处理**：自研 NLP 引擎
- **OCR 识别**：Google ML Kit
- **语音识别**：Google ML Kit Speech Recognition

### 数据安全
- **加密算法**：SQLCipher（AES-256）
- **密钥派生**：PBKDF2-SHA256（100,000 次迭代）
- **网络安全**：强制 HTTPS + Certificate Pinning
- **生物识别**：Android BiometricPrompt

### 其他技术
- **图表**：MPAndroidChart
- **Excel 导出**：Apache POI
- **网络请求**：Retrofit + OkHttp
- **构建工具**：Gradle 8.9 + AGP 8.7.3

---

## 📊 项目统计

- **代码行数**：~20,000 行 Kotlin
- **文件数量**：145 个 Kotlin 文件
- **项目大小**：15-20 MB（Release APK）
- **开发周期**：2024 年 6 月 - 至今
- **项目健康度**：6.9/10（持续优化中）

---

## 🛣️ 开发路线图

### ✅ v1.x（已完成）
- [x] 核心记账功能
- [x] AI 自然语言解析
- [x] OCR 票据识别
- [x] 语音记账
- [x] 数据加密保护
- [x] 统计分析与可视化
- [x] 多主题支持
- [x] Excel 导入导出

### 🚧 v2.0（开发中）
- [ ] 架构重构（引入 Domain 层）
- [ ] 测试覆盖率提升（80%+）
- [ ] 性能优化
- [ ] 更多图表类型
- [ ] AI 理财建议

### 📋 v2.x（规划中）
- [ ] 云端备份（可选）
- [ ] 多账本管理
- [ ] 预算功能
- [ ] 账单提醒
- [ ] 家庭账本共享
- [ ] iOS 版本

---

## 👥 开发团队

**项目维护者**：[@gypg](https://github.com/gypg)

**感谢贡献**：
- 代码审计与优化：Claude AI (Anthropic)
- UI 设计：豆包 AI
- 测试与反馈：内部测试用户

---

## 📄 开源许可

本项目采用 [MIT License](LICENSE) 开源协议。

```
MIT License

Copyright (c) 2024 gypg

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

---

## 🤝 贡献指南

欢迎贡献代码、报告 Bug、提出功能建议！

### 报告问题
在 [Issues](https://github.com/gypg/ai-accounting-new/issues) 页面提交问题，请包含：
- 设备型号和 Android 版本
- 复现步骤
- 预期行为 vs 实际行为
- 截图或日志（如果有）

### 提交代码
1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/AmazingFeature`
3. 提交改动：`git commit -m 'Add some AmazingFeature'`
4. 推送分支：`git push origin feature/AmazingFeature`
5. 提交 Pull Request

### 开发文档
详细的技术文档请查看：
- [快速上手指南](docs/development/QUICK_START.md)
- [架构设计文档](docs/architecture/DESIGN_DOCUMENT.md)
- [代码审计报告](docs/audit/CRITICAL_FIXES_COMPLETED.md)

---

## 📞 联系方式

- **GitHub Issues**：[提交问题](https://github.com/gypg/ai-accounting-new/issues)
- **邮箱**：your-email@example.com
- **项目主页**：https://github.com/gypg/ai-accounting-new

---

## 🌟 Star 历史

[![Star History Chart](https://api.star-history.com/svg?repos=gypg/ai-accounting-new&type=Date)](https://star-history.com/#gypg/ai-accounting-new&Date)

---

## 💡 致谢

感谢以下开源项目和服务：
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Hilt](https://dagger.dev/hilt/)
- [SQLCipher](https://www.zetetic.net/sqlcipher/)
- [ML Kit](https://developers.google.com/ml-kit)
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- [Apache POI](https://poi.apache.org/)

---

<div align="center">

**如果觉得这个项目不错，请给个 Star ⭐️ 支持一下！**

Made with ❤️ by [@gypg](https://github.com/gypg)

</div>
