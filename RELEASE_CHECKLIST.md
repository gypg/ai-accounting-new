# AI记账 - 发布检查清单

> 当前发布目标：`v1.8.3`

## 一、发布前检查

### 代码质量
- [x] `./gradlew lintDebug --continue` 通过
- [x] `./gradlew testDebugUnitTest --continue` 通过
- [x] 最近一次 CI 编译阻塞已修复
- [x] 文档与实现已同步

### 功能检查
- [x] AI 设置页可加载模型列表
- [x] 模型选择界面支持测试连接按钮
- [x] 邀请码绑定成功后可自动写入 token / API 地址
- [x] 邀请码绑定后模型模式为 Auto 自动优选
- [x] 普通 API 配置与保存流程可用

### 构建配置
- [x] `applicationId = com.moneytalk.ai`
- [x] `versionName = 1.8.3`
- [x] `versionCode = 19`
- [x] Release 已开启混淆与资源压缩
- [x] Release 签名走环境变量配置

### 发布资产
- [ ] 最终 APK 手工安装验证
- [ ] GitHub Actions artifacts 中同时存在 debug / release APK
- [ ] 商店素材最终复核
- [ ] 发布说明最终整理

## 二、执行步骤

### 1. 本地验证
```bash
./gradlew lintDebug --continue
./gradlew testDebugUnitTest --continue
./gradlew assembleRelease
```

### 2. 核心手测
- AI 设置页
- 模型测试连接
- 邀请码绑定流程
- 主要记账路径
- 启动、登录、统计页

### 3. GitHub Actions 核验
工作流顺序：
1. `Lint Check`
2. `Unit Tests`
3. `Build APK`
4. `release`（仅 tag `v*` 触发）

额外确认：
- `Upload Release APK` 不再出现 `No files were found with the provided path`
- artifacts 中可下载 release APK

### 4. Tag 与 Release
- 创建 tag：`v1.8.3`
- 推送 tag 到远程
- 确认 GitHub Release 自动生成并包含 APK 产物

## 三、已知问题

- 历史文档中存在旧版本号、旧路径、旧发布示例，现已开始收敛，但发布前仍建议人工复核所有面向外部的说明
- 本地 bash 环境可能出现 `uname: command not found` 日志，但当前不影响 lint / 单测通过
- `Build Release APK` 仍可能出现 Apache POI 相关的 R8 warning：`SVGUserAgent.getViewbox()` 在 Android / R8 静态分析下被视为 unreachable；当前不阻塞构建与 artifact 上传，如后续出现 Excel 导出异常再单独处理

## 四、回滚思路

若发布后发现高优先级问题：
1. 先停止继续推广当前版本
2. 基于 `main` 修复问题并重新执行 lint / unit test / release build
3. 重新打补丁版本 tag
4. 更新版本记录与发布说明
