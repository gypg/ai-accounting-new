# 贡献指南

感谢你参与 AI记账 项目。

## 开发环境

### 前置要求
- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK 34
- 可用的 Gradle 环境

### 克隆与初始化
```bash
git clone <your-repo-url>
cd new-year-fresh
./gradlew tasks
```

## 开发工作流

1. 从 `main` 拉出工作分支
2. 完成开发后先本地执行 Lint 和单元测试
3. 确认关键功能手测通过后再提交 PR
4. 推送后观察 GitHub Actions 是否通过

## 常用命令

<!-- AUTO-GENERATED: GRADLE_COMMANDS_START -->
| Command | Description |
|---------|-------------|
| `./gradlew tasks` | 查看项目可用任务 |
| `./gradlew assembleDebug` | 构建 Debug APK |
| `./gradlew assembleRelease` | 构建 Release APK |
| `./gradlew lintDebug --continue` | 运行与 CI 一致的 Android Lint 检查 |
| `./gradlew testDebugUnitTest --continue` | 运行与 CI 一致的 Debug 单元测试 |
| `./gradlew clean` | 清理构建产物 |
<!-- AUTO-GENERATED: GRADLE_COMMANDS_END -->

## 测试要求

- 提交前至少运行：
  - `./gradlew lintDebug --continue`
  - `./gradlew testDebugUnitTest --continue`
- 涉及 AI 设置、邀请码绑定、模型切换时，需补充对应单元测试
- 涉及发布流程时，需额外验证 `assembleRelease`

## 代码与提交规范

### 代码要求
- 使用 Kotlin 编码规范
- 保持数据不可变更新风格
- 修改用户输入、配置、网络请求相关逻辑时，补齐错误处理
- 不要把密钥、密码或令牌写入源码

### 提交信息
使用 Conventional Commit 风格：

```text
<type>: <description>
```

常用类型：
- `feat`：新功能
- `fix`：缺陷修复
- `docs`：文档更新
- `refactor`：重构
- `test`：测试变更
- `chore`：构建或工具调整

## Pull Request 检查清单

提交 PR 前请确认：
- [ ] 已基于当前 `main` 合并或 rebase
- [ ] 本地 `lintDebug` 通过
- [ ] 本地 `testDebugUnitTest` 通过
- [ ] 文档已同步更新
- [ ] 未提交 keystore、密码、token 等敏感信息
- [ ] 变更说明清晰，便于回溯

## CI 说明

当前 GitHub Actions 主流程：
1. `Lint Check` → `./gradlew lintDebug --continue`
2. `Unit Tests` → `./gradlew testDebugUnitTest --continue`
3. `Build APK` → `./gradlew assembleDebug` 与 `./gradlew assembleRelease`
4. Tag 触发时自动创建 GitHub Release

## 安全与密钥

Release 签名依赖以下环境变量：

<!-- AUTO-GENERATED: ENV_START -->
| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `KEYSTORE_PASSWORD` | Release 构建时必需 | Release keystore 密码；为空时会回退到 debug 签名 | `your_keystore_password` |
| `KEY_PASSWORD` | Release 构建时必需 | `aiaccounting` key alias 的密码 | `your_key_password` |
<!-- AUTO-GENERATED: ENV_END -->

请始终通过环境变量注入，不要硬编码到 `app/build.gradle.kts` 或任何源码文件中。
