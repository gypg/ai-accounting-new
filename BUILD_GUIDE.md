# AI记账 - 构建与发布指南

> 当前版本：`v1.8.3`（`versionCode 19`）

## 1. 当前构建事实

- `applicationId`: `com.moneytalk.ai`
- `namespace`: `com.example.aiaccounting`
- `minSdk`: 26
- `targetSdk`: 34
- `compileSdk`: 34
- `JDK`: 17
- Release 构建已启用：
  - `isMinifyEnabled = true`
  - `isShrinkResources = true`
- 当 `KEYSTORE_PASSWORD` 为空时，Release 会回退到 debug 签名，便于 CI 验证构建链路

## 2. 本地开发构建

### Debug 构建
```bash
./gradlew assembleDebug
```

输出位置：
- `app/build/outputs/apk/debug/app-debug.apk`

### 本地质量检查
```bash
./gradlew lintDebug --continue
./gradlew testDebugUnitTest --continue
```

### Release 构建
```bash
./gradlew assembleRelease
```

输出位置：
- `app/build/outputs/apk/release/app-release-unsigned.apk`

## 3. Release 签名配置

项目在 `app/build.gradle.kts` 中使用以下环境变量：

<!-- AUTO-GENERATED: ENV_START -->
| Variable | Required | Description |
|----------|----------|-------------|
| `KEYSTORE_PASSWORD` | 是（正式签名时） | keystore 密码 |
| `KEY_PASSWORD` | 是（正式签名时） | `aiaccounting` key alias 密码 |
<!-- AUTO-GENERATED: ENV_END -->

### Windows PowerShell
```powershell
$env:KEYSTORE_PASSWORD="your_keystore_password"
$env:KEY_PASSWORD="your_key_password"
./gradlew assembleRelease
```

### bash
```bash
export KEYSTORE_PASSWORD="your_keystore_password"
export KEY_PASSWORD="your_key_password"
./gradlew assembleRelease
```

## 4. GitHub Actions 流程

当前工作流文件：[`build.yml`](.github/workflows/build.yml)

<!-- AUTO-GENERATED: CI_PIPELINE_START -->
| Stage | Job name | Command / Behavior |
|-------|----------|--------------------|
| 1 | `Lint Check` | `./gradlew lintDebug --continue` |
| 2 | `Unit Tests` | `./gradlew testDebugUnitTest --continue` |
| 3 | `Build APK` | `./gradlew assembleDebug` + `./gradlew assembleRelease` |
| 4 | `release` | Tag 为 `v*` 时自动创建 GitHub Release |
<!-- AUTO-GENERATED: CI_PIPELINE_END -->

构建产物会被重命名并上传：
- `AI记账_v<version>_debug_<date>.apk`
- `AI记账_v<version>_release_<date>.apk`

## 5. 发布步骤

1. 本地执行质量检查
   ```bash
   ./gradlew lintDebug --continue
   ./gradlew testDebugUnitTest --continue
   ```
2. 本地构建 Release
   ```bash
   ./gradlew assembleRelease
   ```
3. 手动验证关键功能
   - AI 设置页模型选择
   - 模型测试连接按钮
   - 邀请码绑定与 Auto 自动优选模型
   - 主要记账与统计路径
4. 更新版本文档与发布清单
5. 创建并推送 tag，例如 `v1.8.3`
6. 观察 GitHub Actions 的 Lint / Unit Tests / Build APK / release 四段流程

## 6. 常见问题

### 6.1 CI 在 Lint / Unit Tests 前就失败
优先检查 Kotlin 编译错误；CI 的 Lint 和 Unit Tests 都会先触发 Kotlin 编译。

### 6.2 Release 未使用正式签名
检查是否已正确注入：
- `KEYSTORE_PASSWORD`
- `KEY_PASSWORD`

### 6.3 邀请码绑定后的模型值不是固定模型 ID
这是当前设计：邀请码绑定成功后保存空字符串，表示 **Auto 自动优选模型**，不是缺陷。

### 6.4 模型测试按钮相关改动导致 CI 挂掉
最近一次问题根因是 AI Settings 拆分后出现两个不同的 `TestResult` 类型。当前已通过重命名 `ModelTestResult` 并重新验证修复。
