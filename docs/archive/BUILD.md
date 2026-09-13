# 构建指南

## 环境要求

- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK 34
- Gradle Wrapper（使用仓库自带 `./gradlew`）

## 当前构建事实

- `applicationId`: `com.moneytalk.ai`
- `namespace`: `com.example.aiaccounting`
- `versionName`: `1.8.4`
- `versionCode`: `20`
- `org.gradle.jvmargs`: `-Xmx4096m -Dfile.encoding=UTF-8`

## 构建步骤

### 1. 使用 Android Studio（推荐）

1. 打开项目根目录
2. 等待 Gradle 同步完成
3. 使用以下菜单执行构建：
   - Debug APK：`Build -> Build Bundle(s) / APK(s) -> Build APK(s)`
   - Release APK：`Build -> Generate Signed Bundle / APK...`

常见输出位置：
- Debug：`app/build/outputs/apk/debug/app-debug.apk`
- Release：`app/build/outputs/apk/release/app-release.apk`（若签名或环境不同，也可能出现 `app-release-unsigned.apk`，CI 已兼容两种产物）

### 2. 使用命令行

```bash
# 清理
./gradlew clean

# 质量检查
./gradlew lintDebug --continue
./gradlew testDebugUnitTest --continue

# 构建 Debug
./gradlew assembleDebug

# 构建 Release
./gradlew assembleRelease
```

## GitHub Actions 自动构建

项目已配置 GitHub Actions：

1. `Lint Check`
2. `Unit Tests`
3. `Build APK`
4. `release`（tag 为 `v*` 时触发）

工作流对应命令：
- `./gradlew lintDebug --continue`
- `./gradlew testDebugUnitTest --continue`
- `./gradlew assembleDebug`
- `./gradlew assembleRelease`

构建产物可在 Actions 页面 artifacts 中下载。

当前 workflow 会优先归档 `app-release.apk`，并兼容 `app-release-unsigned.apk`，因此不再依赖单一固定的 release 文件名。

## Release 签名

正式签名依赖环境变量：
- `KEYSTORE_PASSWORD`
- `KEY_PASSWORD`

若未提供 `KEYSTORE_PASSWORD`，Release 构建会回退到 debug 签名，仅用于验证构建链路，不应用于正式发布。

## 已知注意事项

- Lint 与 Unit Tests 在执行前都会先进行 Kotlin 编译，因此类型错误会同时阻塞两个 CI job
- 本地部分 bash 环境可能打印 `uname: command not found`，但当前不影响 lint 与 unit test 执行结果
- 邀请码绑定成功后，模型字段保存为空字符串，表示 Auto 自动优选模型，这是当前设计行为
