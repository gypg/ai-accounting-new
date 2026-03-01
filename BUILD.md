# 构建指南

## 环境要求

- Android Studio Arctic Fox 或更高版本
- JDK 11 或更高版本
- Android SDK API 26+

## 构建步骤

### 1. 使用 Android Studio（推荐）

```bash
# 打开项目
File -> Open -> 选择项目文件夹

# 等待 Gradle 同步完成

# 构建 APK
Build -> Build Bundle(s) / APK(s) -> Build APK(s)
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

### 2. 使用命令行

```bash
# 清理并构建
./gradlew clean assembleDebug

# 或构建发布版本
./gradlew assembleRelease
```

## GitHub Actions 自动构建

项目已配置 GitHub Actions，推送代码后自动构建 APK。

构建的 APK 可在 Actions 页面的 Artifacts 中下载。

## 项目统计

- **源代码**: ~8,500 行 Kotlin
- **测试代码**: ~2,000 行
- **UI 界面**: 8 个主要页面
- **数据库表**: 5 个实体
