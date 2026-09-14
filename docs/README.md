# 项目文档中心

本目录包含 AI 记账项目的完整技术文档。

---

## 📚 文档目录

### 📖 核心文档（根目录）

#### 项目管理
- **[PROJECT_STATUS.md](PROJECT_STATUS.md)** - 项目当前状态总览
  - 当前版本、健康度评分
  - 已完成工作和进行中任务
  - 优先级任务清单
  - 近期计划

#### 开发指南
- **[DEVELOPMENT_DOCUMENT.md](DEVELOPMENT_DOCUMENT.md)** - 完整开发文档（118KB）
  - 详细的技术实现说明
  - 模块设计与接口定义
  
- **[USER_GUIDE.md](USER_GUIDE.md)** - 用户使用指南
  - 功能说明与使用教程
  - 常见问题解答

#### 项目信息
- **[ACKNOWLEDGMENTS.md](ACKNOWLEDGMENTS.md)** - 致谢与开源许可
- **[PRIVACY_POLICY.md](PRIVACY_POLICY.md)** - 隐私政策
- **[OPTIMIZATION_ROADMAP.md](OPTIMIZATION_ROADMAP.md)** - 优化路线图

#### 历史记录
- **[DELIVERY_SUMMARY_2026_03_25.md](DELIVERY_SUMMARY_2026_03_25.md)** - 交付总结（2026-03-25）
- **[SESSION_20_HANDOFF.md](SESSION_20_HANDOFF.md)** - 会话交接文档
- **[ACCEPTANCE_CHECKLIST_P0.md](ACCEPTANCE_CHECKLIST_P0.md)** - P0 验收清单

---

### 🏗️ architecture/ - 架构设计

- **[DESIGN_DOCUMENT.md](architecture/DESIGN_DOCUMENT.md)** (39KB) - 系统架构设计文档
  - MVVM 架构详解
  - 模块划分与依赖关系
  - 数据流设计
  - 技术选型说明

**适用对象**: 架构师、技术负责人、高级开发者

---

### 🔍 audit/ - 审计报告

- **[BUILD_VERIFICATION_REPORT.md](audit/BUILD_VERIFICATION_REPORT.md)** (6.2KB) - 构建验证报告
  - 构建配置验证结果
  - 发现的问题与修复

**适用对象**: QA 工程师、技术审计人员、项目经理

**说明**: 详细的代码审计报告（包含 CRITICAL_FIXES_COMPLETED.md、PROJECT_HEALTH_REPORT.md、ARCHITECTURE_REFACTOR_PLAN.md 等）已保存在项目根目录的 `备份文件/` 文件夹中，因文件较大（165KB+）未复制到此处。

---

### 💻 development/ - 开发指南

#### 构建与发布
- **[BUILD_GUIDE.md](development/BUILD_GUIDE.md)** (7.2KB) - 构建指南
  - 环境配置
  - 构建命令
  - 签名配置
  - 故障排查

- **[VERSION.md](development/VERSION.md)** (3.9KB) - 版本管理
  - 版本号规范
  - 版本历史

- **[RELEASE_CHECKLIST.md](development/RELEASE_CHECKLIST.md)** (3.9KB) - 发布检查清单
  - 发布前验证项
  - 发布流程

#### 贡献指南
- **[CONTRIBUTING.md](development/CONTRIBUTING.md)** (2.9KB) - 贡献指南
  - 代码规范
  - 提交流程
  - PR 模板

**适用对象**: 所有开发者、DevOps 工程师

---

### 📦 archive/ - 临时文档归档

- **[BUILD.md](archive/BUILD.md)** (2.2KB) - 构建进度记录（已归档）
- **[PROGRESS.md](archive/PROGRESS.md)** (22KB) - 任务进度跟踪（已归档）
- **[task_plan.md](archive/task_plan.md)** (6.2KB) - 任务计划（已归档）

**说明**: 这些是工作过程中的临时文档，保留作为历史参考。

---

## 🚀 快速导航

### 我想...

#### 了解项目当前状态
→ [PROJECT_STATUS.md](PROJECT_STATUS.md)

#### 搭建开发环境
→ [BUILD_GUIDE.md](development/BUILD_GUIDE.md)

#### 理解系统架构
→ [DESIGN_DOCUMENT.md](architecture/DESIGN_DOCUMENT.md)

#### 贡献代码
→ [CONTRIBUTING.md](development/CONTRIBUTING.md)

#### 查看代码审计结果
→ [BUILD_VERIFICATION_REPORT.md](audit/BUILD_VERIFICATION_REPORT.md)

#### 准备发布新版本
→ [RELEASE_CHECKLIST.md](development/RELEASE_CHECKLIST.md)

#### 查看完整开发文档
→ [DEVELOPMENT_DOCUMENT.md](DEVELOPMENT_DOCUMENT.md)

---

## 📊 文档统计

| 分类 | 文件数 | 总大小 |
|------|--------|--------|
| 根目录 | 9 个 | ~160 KB |
| architecture/ | 1 个 | 39 KB |
| audit/ | 1 个 | 6.2 KB |
| development/ | 4 个 | ~18 KB |
| archive/ | 3 个 | ~31 KB |
| **docs/ 小计** | **18 个** | **~254 KB** |
| **项目根目录** | **8 个大型文档** | **~521 KB** |
| **文档总计** | **26 个** | **~775 KB** |

---

## 📝 文档维护

### 更新频率
- **PROJECT_STATUS.md**: 每次重要里程碑后更新
- **BUILD_GUIDE.md**: 构建配置变更时更新
- **DESIGN_DOCUMENT.md**: 架构调整时更新
- **CONTRIBUTING.md**: 开发流程变更时更新

### 文档规范
- 使用 Markdown 格式
- 包含目录（超过 200 行）
- 代码示例使用语法高亮
- 链接使用相对路径

### 项目根目录的重要文档

以下大型技术文档保存在项目根目录 `备份文件/`：
- **CRITICAL_FIXES_COMPLETED.md** (165KB) - 关键修复详细报告
- **ARCHITECTURE_REFACTOR_PLAN.md** (85KB) - 架构重构计划
- **PROJECT_HEALTH_REPORT.md** (120KB) - 7 维度健康评估
- **COMPLETE_FIX_CHECKLIST.md** (45KB) - 完整问题修复清单
- **QUICK_START.md** (12KB) - 快速开始指南
- **IMPROVEMENT_RECOMMENDATIONS.md** (68KB) - 改进建议
- **EXECUTIVE_SUMMARY.md** (18KB) - 执行总结
- **BUILD_STATUS.md** (8KB) - 构建状态

**说明**: 这些文档因文件较大且为代码审计工作产物，保存在项目根目录便于查阅。

---

## 🔗 外部链接

- **GitHub 仓库**: https://github.com/gypg/ai-accounting-new
- **问题跟踪**: https://github.com/gypg/ai-accounting-new/issues
- **项目主页**: 查看根目录 [README.md](../README.md)

---

**最后更新**: 2026-09-14  
**文档版本**: v1.0  
**维护者**: [@gypg](https://github.com/gypg)
