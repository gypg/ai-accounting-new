# GitHub 仓库设置指南

## 您的GitHub信息
- **用户名**: gypg
- **仓库名**: ai-accounting (建议)

---

## 步骤1: 在GitHub上创建仓库

1. 访问 https://github.com/new
2. 填写信息：
   - **Repository name**: `ai-accounting`
   - **Description**: `AI智能记账Android应用 - 基于自然语言识别的本地记账软件`
   - **Visibility**: 选择 Public (公开) 或 Private (私有)
   - **Initialize this repository with**: 不要勾选任何选项
3. 点击 **Create repository**

---

## 步骤2: 初始化本地Git仓库

打开终端，执行以下命令：

```bash
cd c:\Users\GYP\Documents\trae_projects\new-year

# 初始化Git仓库
git init

# 添加所有文件
git add .

# 提交
git commit -m "🎉 Initial commit: AI记账完整项目

- AI自然语言记账功能
- 多账户管理
- 分类管理
- 收支统计图表
- 数据备份与恢复
- 深色模式支持
- 生物识别安全"
```

---

## 步骤3: 推送到GitHub

```bash
# 添加远程仓库
git remote add origin https://github.com/gypg/ai-accounting.git

# 推送代码
git push -u origin main
```

如果默认分支是master：
```bash
git push -u origin master
```

---

## 步骤4: 验证GitHub Actions

推送完成后：

1. 访问 `https://github.com/gypg/ai-accounting/actions`
2. 应该能看到 **Build APK** 工作流正在运行
3. 等待约5-10分钟构建完成
4. 点击最新的工作流运行记录
5. 在 **Artifacts** 部分下载APK文件

---

## 获取APK文件

构建完成后，您可以下载：

1. **debug-apk**: 调试版本（可直接安装测试）
2. **release-apk**: 发布版本（未签名，需要签名后才能安装）

---

## 可选: 创建Release版本

1. 在GitHub仓库页面点击 **Releases**
2. 点击 **Create a new release**
3. 填写版本信息：
   - **Tag version**: `v1.0.0`
   - **Release title**: `AI记账 v1.0.0 首次发布`
   - **Description**: 复制下面的内容

```markdown
## 🎉 AI记账 v1.0.0 首次发布

### ✨ 主要功能
- **AI智能记账**: 自然语言输入，自动识别金额、分类、时间
- **多账户管理**: 现金、银行卡、支付宝、微信等账户管理
- **详细统计**: 收支趋势图、分类饼图、月度对比
- **数据安全**: 本地加密存储，支持指纹/面部识别
- **数据备份**: 自动备份，支持Excel导出
- **深色模式**: 支持深色主题

### 📱 安装要求
- Android 8.0 (API 26) 或更高版本
- 无需网络权限，纯本地运行

### 🔒 隐私说明
- 所有数据存储在本地
- 不上传任何数据到服务器
- 支持应用锁定保护

### 📥 下载
- [下载 APK](https://github.com/gypg/ai-accounting/releases/download/v1.0.0/app-release.apk)

### 📝 使用说明
1. 下载并安装APK
2. 首次使用设置PIN码
3. 在AI助手页面输入记账内容，如"午饭花了35元"
4. AI自动解析并确认记账

---
**完整源码**: https://github.com/gypg/ai-accounting
```

4. 上传构建好的APK文件
5. 点击 **Publish release**

---

## 常见问题

### 1. 推送被拒绝
```bash
# 如果提示权限错误，使用SSH方式
git remote remove origin
git remote add origin git@github.com:gypg/ai-accounting.git
git push -u origin main
```

### 2. 分支名称不同
```bash
# 如果GitHub使用main，本地使用master
git branch -M main
git push -u origin main
```

### 3. GitHub Actions构建失败
1. 检查 `gradle.properties` 是否有执行权限
2. 查看Actions日志获取详细错误信息
3. 确保所有文件已正确提交

---

## 后续更新代码

修改代码后，推送到GitHub：

```bash
git add .
git commit -m "更新说明"
git push
```

GitHub Actions会自动重新构建APK。

---

## 项目链接

- **仓库地址**: https://github.com/gypg/ai-accounting
- **Actions页面**: https://github.com/gypg/ai-accounting/actions
- **Releases页面**: https://github.com/gypg/ai-accounting/releases

---

**完成以上步骤后，您就可以在GitHub Actions中自动构建APK了！**
