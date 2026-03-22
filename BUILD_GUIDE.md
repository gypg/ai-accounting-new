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
- `app/build/outputs/apk/release/app-release.apk`
- 若签名环境不同，也可能出现 `app/build/outputs/apk/release/app-release-unsigned.apk`

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

当前 workflow 会优先探测 `app-release.apk`，并兼容 `app-release-unsigned.apk`，避免因为 release 输出文件名差异而导致 artifact 上传 warning。

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

### 6.5 Release 构建中的 Apache POI / R8 warning
当前已确认 warning 的来源不是业务侧 Excel 导出逻辑本身，而是 `org.apache.poi:poi-ooxml:5.2.5` jar 内自带的 `org.apache.poi.xslf.draw.SVGUserAgent` 等 PPT/SVG 渲染类。

已完成的归因结论：
- App 当前仅在 [ExcelExporter.kt](app/src/main/java/com/example/aiaccounting/data/exporter/ExcelExporter.kt) 中使用 `XSSFWorkbook`、样式、合并单元格等基础 `.xlsx` 写入能力
- Release 依赖树显示当前 POI 相关主依赖为：
  - `org.apache.poi:poi:5.2.5`
  - `org.apache.poi:poi-ooxml:5.2.5`
  - `org.apache.poi:poi-ooxml-lite:5.2.5`
  - `org.apache.xmlbeans:xmlbeans:5.2.0`
- `poi-ooxml-5.2.5.jar` 内实际包含 `org/apache/poi/xslf/draw/SVGUserAgent.class`
- `assembleRelease` 时 R8 会对该类中的 `getViewbox()` 做类型检查，并输出：
  - `The method java.awt.geom.Rectangle2D org.apache.poi.xslf.draw.SVGUserAgent.getViewbox() does not type check and will be assumed to be unreachable.`

这说明当前 warning 更接近 **POI 附带的未使用 PPT/SVG 渲染路径被 R8 静态分析到**，而不是当前 Excel 导出功能直接调用了有问题的 API。

模块 2 当前已完成的收敛：
- 将 `-keep class org.apache.poi.** { *; }` 缩小为仅保留当前 Excel 导出主路径需要的 `org.apache.poi.ss.**`、`org.apache.poi.xssf.**`、`org.apache.poi.openxml4j.**`
- 新增 `-dontwarn org.openxmlformats.schemas.**`，避免 R8 因 XML schema 生成类缺失而中断 release 构建
- 重新执行 `assembleRelease` 后，release 构建保持成功，且当前 `ExcelExporter` 使用范围下未出现新增构建阻塞

当前状态说明：
- warning **尚未被彻底消除**
- 当前 release 构建仍依赖 `app/proguard-rules.pro` 中对 POI / AWT / XML schema 的 suppress 规则将其维持为非阻塞项
- 在当前实现范围内这是可接受状态；后续若继续推进，下一步应优先尝试真正缩减 POI 依赖，而不是继续扩大 suppress 范围
