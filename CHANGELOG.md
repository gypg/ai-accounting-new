# 更新日志

本文档记录 AI 记账项目的所有重要变更。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [Unreleased]

### 文档改进
- 📚 重构文档结构，按类别组织到 `docs/` 目录
  - `docs/architecture/` - 架构设计文档
  - `docs/development/` - 开发指南
  - `docs/audit/` - 审计报告
  - `docs/archive/` - 临时文档归档
- 新增 `docs/PROJECT_STATUS.md` - 项目状态总览
- 新增 `docs/README.md` - 文档中心导航

---

## [1.8.1] - 2026-09-13

### 🔒 安全增强

#### 网络安全
- 新增 `network_security_config.xml` 强制 HTTPS
- 禁用明文流量传输
- OkHttp 日志拦截器过滤 API 密钥和 Bearer Token

#### 数据库安全
- 修复密钥派生方案，使用 PBKDF2-SHA256（100,000 次迭代）
- 调用已有的 `SecurityManager.deriveDatabaseKey()` 而非弱密钥生成

#### 构建安全
- 修复 release buildType 签名配置（使用 release 签名而非 debug）
- 环境变量控制：CI 使用 release 签名，本地开发回退到 debug 签名

### 🐛 Bug 修复

#### 日期处理
- 修复时区问题：迁移 `Calendar` 到 `java.time` API
- 修复跨年边界判断（超过 180 天视为去年）
- 修复 2 月 30 日等无效日期的静默调整
- 添加 `@Synchronized` 防止多线程竞态

#### 资源管理
- 修复 `MediaRecorder` 资源泄漏
- 添加 `finally` 块确保 `release()` 执行
- 添加重入保护门闩（`isRecording` 标志）
- 确保临时文件删除

#### UI 性能
- 为所有 `LazyColumn`/`LazyRow` 添加 `key` 参数（11 个文件）
- 修复列表重组时状态丢失和动画异常

### ⚡ 性能优化

#### ViewModel 优化
- `StatisticsViewModel`: O(3n) → O(n) 单次遍历
  - 预计算时间筛选条件
  - 预建 Category 索引
  - 单次遍历计算所有统计
- `OverviewViewModel`: 减少 85% 重组
  - 7 个派生 StateFlow → 单一 `combine`
  - 时间边界预计算
  - 单次遍历更新所有统计

#### ProGuard 规则收紧
- 精确保留序列化字段（`@SerializedName`）
- 针对性保留公共 API
- 移除整包保留规则
- 预计 APK 体积减少 20-30%

### 🔧 工具链升级

#### 构建工具
- AGP: 8.2.0 → **8.7.3**
- Gradle: 8.5 → **8.9**
- Kotlin: 1.9.20 → **2.0.21** (K2 编译器)
- KSP: 1.9.20-1.0.14 → **2.0.21-1.0.28**

#### 构建配置
- 添加 `android.overridePathCheck=true` 支持中文路径
- JVM 内存: -Xmx4096m
- Gradle 网络超时: 10 秒

### 📝 文档
- 新增 `BUILD_VERIFICATION_REPORT.md` - 构建验证报告
- 更新 `BUILD_GUIDE.md` - 增加故障排查章节
- 更新 `proguard-rules.pro` - 添加详细注释

### 🔄 代码质量
- 解决 Git 合并冲突（主分支 vs 远程分支）
- 清理冲突标记
- 统一代码库状态

---

## [1.8.0] - 2024-04 (GitHub 历史版本)

### 功能特性
- ✨ AI 智能记账（自然语言解析）
- 📷 OCR 票据识别
- 🎤 语音记账
- 📊 统计分析与可视化
- 🔐 SQLCipher 加密存储
- 👆 生物识别解锁
- 🌙 深色模式
- 📦 Excel 导入导出

### 技术栈
- Kotlin + Jetpack Compose
- MVVM 架构
- Room + Hilt
- Coroutines + Flow

---

## [1.7.x] - 2024-03

### 早期版本
- 核心记账功能
- 基础 UI 框架
- 数据库设计

---

## 版本说明

### 版本号规范
格式: `MAJOR.MINOR.PATCH`

- **MAJOR**: 重大架构变更或不兼容更新
- **MINOR**: 新增功能，向后兼容
- **PATCH**: Bug 修复和性能优化

### 发布类型
- **🔒 安全增强**: Security
- **✨ 新功能**: Added
- **🐛 Bug 修复**: Fixed
- **⚡ 性能优化**: Performance
- **🔧 工具链升级**: Build
- **📝 文档**: Documentation
- **🔄 代码质量**: Refactor
- **❌ 移除**: Removed
- **⚠️ 弃用**: Deprecated

---

## 链接

- [GitHub Releases](https://github.com/gypg/ai-accounting-new/releases)
- [GitHub Issues](https://github.com/gypg/ai-accounting-new/issues)
- [项目文档](docs/README.md)

---

**维护者**: [@gypg](https://github.com/gypg)  
**最后更新**: 2026-09-14
