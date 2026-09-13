# 完整优化流程规划

> 创建日期: 2026-03-22 | 目标: 系统化完成所有待优化项

---

## 一、P0 - 发布前必做 (v1.8.3)

### 模块1: applicationId 修改 (1h)
- [ ] 修改 `build.gradle.kts` 中的 applicationId
- [ ] 更新相关配置引用
- [ ] 验证 Debug 编译通过
- [ ] 更新开发文档
- [ ] Git 提交: `refactor: 修改 applicationId 为正式包名`

### 模块2: ProGuard 验证 (2h)
- [ ] 检查 `proguard-rules.pro` 完整性
- [ ] 执行 Release 构建
- [ ] 功能回归测试
- [ ] 修复混淆导致的问题（如有）
- [ ] 更新文档记录
- [ ] Git 提交: `build: 验证 ProGuard 规则并修复`

### 模块3: 版本发布准备 (1h)
- [ ] 更新 `VERSION.md` 至 v1.8.3
- [ ] 更新 `build.gradle.kts` versionCode/Name
- [ ] 生成 Release APK
- [ ] 创建发布说明
- [ ] Git 提交: `release: v1.8.3 发布准备`

---

## 二、P1 - 标准化提升 (v1.9.0)

### 模块4: Design Token 系统 (16h)
- [ ] 创建 `ui/theme/DesignTokens.kt`
- [ ] 定义间距系统 (4/8/12/16/24/32dp)
- [ ] 定义圆角系统 (4/8/12/16dp)
- [ ] 定义阴影层级 (1-5)
- [ ] 定义动效时长 (100/200/300/500ms)
- [ ] 迁移现有硬编码值
- [ ] 更新文档
- [ ] Git 提交: `feat: 建立统一 Design Token 系统`

### 模块5: 测试覆盖提升 - 工具类 (8h)
- [ ] 补充 `DateUtils` 测试
- [ ] 补充 `MoneyUtils` 测试
- [ ] 补充 `SecurityUtils` 测试
- [ ] 验证覆盖率 > 80%
- [ ] Git 提交: `test: 补充工具类测试覆盖`

### 模块6: 测试覆盖提升 - Repository (12h)
- [ ] 补充 `TransactionRepository` 复杂查询测试
- [ ] 补充 `AccountRepository` 边界测试
- [ ] 补充 `CategoryRepository` 层级测试
- [ ] 验证覆盖率 > 80%
- [ ] Git 提交: `test: 补充 Repository 测试覆盖`

### 模块7: 测试覆盖提升 - ViewModel (16h)
- [ ] 补充 `OverviewViewModel` 测试
- [ ] 补充 `TransactionListViewModel` 测试
- [ ] 补充 `StatisticsViewModel` 测试
- [ ] 验证覆盖率 > 80%
- [ ] Git 提交: `test: 补充 ViewModel 测试覆盖`

### 模块8: CI/CD 流程完善 (16h)
- [ ] 配置 GitHub Actions 自动测试
- [ ] 添加 Lint 检查到 CI
- [ ] 配置 APK 产物归档
- [ ] 添加覆盖率报告
- [ ] Git 提交: `ci: 完善自动化流程`

### 模块9: 图表组件优化 (24h)
- [ ] 创建 `ComposeCanvasChart` 基础组件
- [ ] 迁移趋势折线图
- [ ] 迁移分类饼图
- [ ] 移除 MPAndroidChart 依赖
- [ ] 验证视觉效果一致性
- [ ] Git 提交: `refactor: 图表组件迁移至 Compose Canvas`

---

## 三、P2 - 体验优化 (v1.10.0)

### 模块10: 微交互动效 (16h)
- [ ] 添加按钮涟漪效果优化
- [ ] 添加列表进入动画
- [ ] 添加数字滚动动画
- [ ] 添加页面转场优化
- [ ] Git 提交: `feat: 添加微交互动效`

### 模块11: 标签系统启用 (8h)
- [ ] 启用 `tags` 和 `transaction_tags` 表
- [ ] 实现标签选择 UI
- [ ] 实现标签筛选功能
- [ ] 更新文档
- [ ] Git 提交: `feat: 启用标签系统`

### 模块12: 平板适配 (32h)
- [ ] 分析现有布局问题
- [ ] 设计自适应布局方案
- [ ] 实现主界面适配
- [ ] 实现统计页面适配
- [ ] 实现设置页面适配
- [ ] Git 提交: `feat: 平板/折叠屏适配`

### 模块13: 无障碍优化 (16h)
- [ ] 补充 contentDescription
- [ ] 优化触摸目标尺寸
- [ ] 添加屏幕阅读器支持
- [ ] 无障碍测试验证
- [ ] Git 提交: `feat: 无障碍功能优化`

### 模块14: SharedPreferences 迁移 (16h)
- [ ] 识别所有 SharedPreferences 使用
- [ ] 迁移至 DataStore
- [ ] 验证数据迁移正确性
- [ ] Git 提交: `refactor: SharedPreferences 迁移至 DataStore`

---

## 四、P3 - 长期规划 (v2.0.0+)

### 模块15: PDF 报表导出 (24h)
- [ ] 调研 PDF 生成库
- [ ] 设计报表模板
- [ ] 实现月度报表导出
- [ ] 实现年度报表导出
- [ ] Git 提交: `feat: PDF 财务报表导出`

### 模块16: 云端同步 (80h+)
- [ ] 设计同步架构
- [ ] 实现后端 API
- [ ] 实现客户端同步逻辑
- [ ] 冲突解决机制
- [ ] Git 提交: `feat: 云端同步功能`

### 模块17: 国际化支持 (40h)
- [ ] 提取所有字符串资源
- [ ] 翻译为英文
- [ ] 实现语言切换
- [ ] 测试多语言显示
- [ ] Git 提交: `feat: 国际化支持`

---

## 五、执行策略

### 每个模块的标准流程：

1. **开始前**
   - 更新记忆文件，标记当前任务
   - 阅读相关代码和文档

2. **执行中**
   - 编写代码/测试
   - 实时验证功能
   - 记录关键决策

3. **完成后**
   - 更新开发文档
   - 更新记忆文件
   - Git 提交并推送
   - 标记 Todo 完成

### 版本发布节奏：

- **v1.8.3**: P0 完成 (预计 1 周)
- **v1.9.0**: P1 完成 (预计 4-6 周)
- **v1.10.0**: P2 完成 (预计 8-12 周)
- **v2.0.0**: P3 部分功能 (预计 6+ 月)

---

## 六、当前任务

**立即执行**: P0 模块1 - applicationId 修改
