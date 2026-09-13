# AI记账 - 自动化构建与部署脚本

## 📋 功能概述

本脚本实现完整的APK自动化构建与部署流程：

```
运行测试 → 构建APK → 重命名 → 移动到测试文件夹 → 推送到MuMu模拟器
```

## 🚀 快速开始

### 1. 前置要求

- Windows 操作系统
- PowerShell 5.0 或更高版本
- Android SDK（包含ADB工具）
- Gradle 构建环境
- MuMu模拟器已安装并运行

### 2. 目录结构

```
new-year-fresh/
├── app/
│   └── build/outputs/apk/     # APK生成目录
├── test_builds/               # 测试版APK存放目录（自动创建）
├── scripts/
│   ├── auto_build_and_deploy.ps1  # 主脚本
│   ├── auto_build.log             # 日志文件
│   └── README.md                  # 本说明文档
└── ...
```

### 3. 基本使用

#### 方式一：完整流程（推荐）

```powershell
# 打开PowerShell，切换到scripts目录
cd C:\Users\GYP\Documents\trae_projects\new-year-fresh\scripts

# 执行完整流程（测试+构建+部署）
.\auto_build_and_deploy.ps1
```

#### 方式二：仅构建并部署（跳过测试）

```powershell
.\auto_build_and_deploy.ps1 -SkipTests
```

#### 方式三：使用现有APK部署（跳过构建）

```powershell
.\auto_build_and_deploy.ps1 -SkipBuild
```

#### 方式四：Release版本构建

```powershell
.\auto_build_and_deploy.ps1 -BuildType release
```

## ⚙️ 参数说明

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `BuildType` | string | "debug" | 构建类型：debug 或 release |
| `TestFolder` | string | "test_builds" | 测试版文件夹名称 |
| `SkipTests` | switch | false | 是否跳过测试阶段 |
| `SkipBuild` | switch | false | 是否跳过构建阶段 |
| `MuMuDevice` | string | "127.0.0.1:16384" | MuMu模拟器地址 |

## 📁 文件命名规则

生成的APK文件将按照以下格式自动重命名：

```
AI记账_v{版本号}_{构建类型}_{时间戳}.apk
```

**示例：**
- `AI记账_v1.8.1_debug_20250115_143022.apk`
- `AI记账_v1.8.1_release_20250115_143022.apk`

## 🔧 配置说明

### 1. MuMu模拟器地址

默认使用 `127.0.0.1:16384`，如果您的MuMu模拟器使用不同端口，请修改：

```powershell
.\auto_build_and_deploy.ps1 -MuMuDevice "127.0.0.1:7555"
```

### 2. 测试文件夹位置

默认在项目根目录创建 `test_builds` 文件夹，可通过参数修改：

```powershell
.\auto_build_and_deploy.ps1 -TestFolder "my_builds"
```

## 📝 日志记录

脚本会自动记录所有操作日志到 `scripts/auto_build.log` 文件：

```
[2025-01-15 14:30:22] [Info] 开始构建APK (debug)...
[2025-01-15 14:32:15] [Success] ✓ APK构建成功
[2025-01-15 14:32:16] [Info] 新文件名: AI记账_v1.8.1_debug_20250115_143022.apk
...
```

## ✅ 验证方法

### 1. 检查APK文件

```powershell
# 查看测试文件夹中的APK
Get-ChildItem ..\test_builds\*.apk | Sort-Object LastWriteTime -Descending
```

### 2. 检查MuMu模拟器

```powershell
# 连接MuMu模拟器
adb connect 127.0.0.1:16384

# 查看已安装的应用
adb shell pm list packages | findstr aiaccounting
```

### 3. 查看日志

```powershell
# 查看最后20行日志
Get-Content .\auto_build.log -Tail 20
```

## 🐛 故障排除

### 问题1：ADB命令未找到

**错误信息：**
```
未找到ADB工具
```

**解决方案：**
1. 确认Android SDK已安装
2. 检查环境变量：`$env:LOCALAPPDATA\Android\Sdk\platform-tools`
3. 或将adb添加到系统PATH

### 问题2：MuMu模拟器连接失败

**错误信息：**
```
✗ MuMu模拟器未连接
```

**解决方案：**
1. 确保MuMu模拟器正在运行
2. 检查MuMu的ADB调试端口（默认16384）
3. 在MuMu设置中开启ADB调试

### 问题3：测试失败

**错误信息：**
```
✗ 测试失败
```

**解决方案：**
1. 查看详细错误日志
2. 单独运行测试：`..\gradlew.bat test`
3. 使用 `-SkipTests` 参数跳过测试（仅用于调试）

### 问题4：构建失败

**错误信息：**
```
✗ APK构建失败
```

**解决方案：**
1. 检查Gradle配置：`..\gradlew.bat --version`
2. 清理并重新构建：`..\gradlew.bat clean`
3. 检查网络连接（下载依赖）

## 🔒 安全说明

1. **自动清理**：脚本会自动保留最近10个APK文件，旧的会自动删除
2. **错误处理**：任何步骤失败都会终止流程并记录错误
3. **日志保密**：日志文件包含构建信息，请勿上传到公共仓库

## 📊 流程图

```
┌─────────────────┐
│   开始执行脚本   │
└────────┬────────┘
         ▼
┌─────────────────┐
│   读取版本信息   │
└────────┬────────┘
         ▼
┌─────────────────┐     ┌──────────┐
│   运行单元测试   │────▶│ 测试失败？ │──▶ 终止流程
└────────┬────────┘     └──────────┘
         ▼
┌─────────────────┐     ┌──────────┐
│   构建APK文件    │────▶│ 构建失败？ │──▶ 终止流程
└────────┬────────┘     └──────────┘
         ▼
┌─────────────────┐
│  生成新文件名    │
└────────┬────────┘
         ▼
┌─────────────────┐
│ 移动到测试文件夹 │
└────────┬────────┘
         ▼
┌─────────────────┐     ┌──────────┐
│ 推送到MuMu模拟器 │────▶│ 推送失败？ │──▶ 记录警告
└────────┬────────┘     └──────────┘
         ▼
┌─────────────────┐
│   流程完成 ✓    │
└─────────────────┘
```

## 📝 更新日志

### v1.0.0 (2025-01-15)
- ✨ 初始版本发布
- ✨ 支持完整的自动化构建流程
- ✨ 支持APK自动重命名和移动
- ✨ 支持MuMu模拟器自动推送
- ✨ 添加详细的日志记录
- ✨ 添加错误处理机制

## 📞 技术支持

如有问题，请查看日志文件 `auto_build.log` 获取详细信息。

---

**作者：** AI Assistant  
**版本：** 1.0.0  
**最后更新：** 2025-01-15
