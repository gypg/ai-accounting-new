# AI记账 - 快速开始指南

## 🎯 目标：获取APK文件

您的GitHub用户名：`gypg`

---

## 方案一：使用GitHub Actions自动构建（推荐）

### 步骤1：在GitHub上创建仓库

1. 打开浏览器，访问：https://github.com/new
2. 填写信息：
   - **Repository name**: `ai-accounting`
   - **Description**: `AI智能记账Android应用`
   - 选择 **Public**（公开）
   - **不要勾选** "Initialize this repository with a README"
3. 点击 **Create repository**

### 步骤2：推送代码到GitHub

**方法A：使用批处理脚本（最简单）**

1. 打开文件资源管理器
2. 进入文件夹：`C:\Users\GYP\Documents\trae_projects\new-year`
3. 双击运行 `push-to-github.bat`
4. 按提示完成推送

**方法B：使用命令行**

打开CMD或PowerShell，执行：

```bash
cd C:\Users\GYP\Documents\trae_projects\new-year

# 配置Git（只需一次）
git config user.email "你的邮箱@example.com"
git config user.name "gypg"

# 提交代码
git add .
git commit -m "Initial commit"

# 添加远程仓库
git remote add origin https://github.com/gypg/ai-accounting.git

# 推送代码
git push -u origin main
```

如果提示输入用户名密码，输入您的GitHub用户名和个人访问令牌。

### 步骤3：获取APK文件

1. 访问：https://github.com/gypg/ai-accounting/actions
2. 等待构建完成（约5-10分钟）
3. 点击最新的工作流运行
4. 在 **Artifacts** 部分下载 `debug-apk`
5. 解压下载的文件，得到 `app-debug.apk`

### 步骤4：安装到手机

1. 将APK传输到手机（微信、QQ、数据线）
2. 在手机上点击APK文件
3. 允许"安装未知来源应用"
4. 完成安装

---

## 方案二：使用Android Studio构建

如果您想自己构建或修改代码：

1. **下载安装Android Studio**
   - 官网：https://developer.android.com/studio

2. **打开项目**
   - 打开Android Studio
   - File → Open
   - 选择 `C:\Users\GYP\Documents\trae_projects\new-year`

3. **等待同步完成**
   - 首次打开会自动下载依赖
   - 可能需要10-20分钟

4. **构建APK**
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - 构建完成后，点击右下角提示查看APK位置

5. **找到APK**
   - 位置：`app\build\outputs\apk\debug\app-debug.apk`

---

## 方案三：找朋友帮忙

如果您觉得以上步骤太复杂：

1. 将整个 `new-year` 文件夹复制给朋友
2. 让朋友按照"方案二"操作
3. 朋友构建好APK后传给您

---

## 📱 安装后的首次使用

1. **打开应用**
   - 找到"AI记账"图标，点击打开

2. **设置PIN码**
   - 首次使用需要设置4位PIN码
   - 用于保护您的记账数据

3. **开始使用**
   - 点击底部"AI助手"
   - 输入记账内容，如："午饭花了35元"
   - AI自动识别并记账

---

## 🔧 常见问题

### Q1: 推送时提示权限错误？
**解决**：使用个人访问令牌代替密码
1. 访问 https://github.com/settings/tokens
2. 点击 Generate new token
3. 勾选 repo 权限
4. 生成后复制令牌，用作密码

### Q2: GitHub Actions构建失败？
**解决**：
1. 检查代码是否完整推送
2. 查看Actions日志获取详细错误
3. 确保 `.github/workflows/build.yml` 文件存在

### Q3: 安装APK时提示"解析包错误"？
**解决**：
- 确保APK完整下载
- 检查手机Android版本（需要8.0以上）
- 尝试重新下载APK

### Q4: 应用闪退？
**解决**：
- 检查手机存储空间
- 清除应用数据后重试
- 查看是否有报错信息

---

## 📂 项目文件说明

| 文件/文件夹 | 说明 |
|------------|------|
| `app/src/main/` | 所有源代码 |
| `push-to-github.bat` | 一键推送脚本 |
| `BUILD_GUIDE.md` | 详细构建指南 |
| `GITHUB_SETUP.md` | GitHub设置指南 |
| `README.md` | 项目说明文档 |

---

## 🆘 需要帮助？

如果遇到问题：

1. 查看详细文档：`BUILD_GUIDE.md`
2. 检查GitHub Actions日志
3. 在GitHub仓库创建Issue

---

**🎉 完成以上步骤后，您就可以在手机上使用AI记账应用了！**
