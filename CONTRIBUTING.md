# 贡献指南

感谢您对本项目的关注！我们欢迎各种形式的贡献。

## 如何贡献

### 报告问题

如果您发现了bug或有功能建议，请通过 [GitHub Issues](https://github.com/yourusername/ai-accounting/issues) 提交。

提交问题时，请包含以下信息：
- 问题描述
- 复现步骤
- 期望行为
- 实际行为
- 设备信息（Android版本、设备型号等）
- 截图（如有）

### 提交代码

1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个 Pull Request

### 代码规范

- 使用 Kotlin 编码规范
- 遵循现有的代码风格
- 添加适当的注释
- 确保代码通过 lint 检查
- 编写单元测试（如适用）

### 提交信息规范

提交信息应该清晰描述更改内容：

```
类型: 简短描述

详细描述（可选）
```

类型包括：
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

## 开发环境设置

### 要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34

### 构建

```bash
./gradlew assembleDebug
```

### 运行测试

```bash
./gradlew test
```

### 运行Lint检查

```bash
./gradlew lint
```

## 行为准则

- 尊重所有参与者
- 接受建设性批评
- 关注对社区最有利的事情
- 展现同理心

## 许可证

通过贡献您的代码，您同意将其授权为 MIT 许可证。
