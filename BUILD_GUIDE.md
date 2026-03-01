# AI记账 - APK构建指南

## 方法一：使用Android Studio构建（推荐）

### 步骤1：安装Android Studio
1. 下载并安装 [Android Studio](https://developer.android.com/studio)
2. 安装时确保勾选：
   - Android SDK
   - Android SDK Platform
   - Android Virtual Device (可选)

### 步骤2：打开项目
1. 打开Android Studio
2. 选择 "Open an existing project"
3. 选择 `c:\Users\GYP\Documents\trae_projects\new-year` 文件夹

### 步骤3：生成签名密钥
1. 在Android Studio中，点击 **Build** > **Generate Signed Bundle / APK...**
2. 选择 **APK**，点击 **Next**
3. 点击 **Create new...** 创建新密钥
   - Key store path: 选择保存位置（如 `app/ai-accounting.keystore`）
   - Password: 设置密钥库密码
   - Key alias: `aiaccounting`
   - Key password: 设置密钥密码
   - Validity (years): 25
   - Certificate: 填写您的信息
4. 点击 **OK**

### 步骤4：构建APK
1. 选择刚创建的密钥
2. 选择 **release** 构建类型
3. 点击 **Finish**
4. 等待构建完成
5. APK文件位置：`app/release/app-release.apk`

---

## 方法二：使用命令行构建

### 前提条件
1. 安装 [JDK 11或更高版本](https://www.oracle.com/java/technologies/downloads/)
2. 安装 [Android SDK](https://developer.android.com/studio#command-tools)
3. 配置环境变量：
   ```bash
   # Windows
   set JAVA_HOME=C:\Program Files\Java\jdk-11
   set ANDROID_HOME=C:\Users\YourName\AppData\Local\Android\Sdk
   set PATH=%PATH%;%JAVA_HOME%\bin;%ANDROID_HOME%\tools;%ANDROID_HOME%\platform-tools
   ```

### 步骤1：生成签名密钥
```bash
cd c:\Users\GYP\Documents\trae_projects\new-year\app

keytool -genkey -v \
    -keystore ai-accounting.keystore \
    -alias aiaccounting \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass your_password \
    -keypass your_password
```

### 步骤2：配置签名信息
在 `app/build.gradle.kts` 中确认签名配置：
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("ai-accounting.keystore")
        storePassword = "your_password"
        keyAlias = "aiaccounting"
        keyPassword = "your_password"
    }
}
```

### 步骤3：构建发布APK
```bash
cd c:\Users\GYP\Documents\trae_projects\new-year

# 清理项目
./gradlew clean

# 运行测试
./gradlew test

# 构建发布版本
./gradlew assembleRelease
```

### 步骤4：找到APK文件
构建完成后，APK文件位于：
```
app/build/outputs/apk/release/app-release.apk
```

---

## 方法三：构建调试APK（无需签名）

如果只需要测试，可以构建调试版本：

```bash
cd c:\Users\GYP\Documents\trae_projects\new-year

# 构建调试版本
./gradlew assembleDebug
```

调试APK位置：
```
app/build/outputs/apk/debug/app-debug.apk
```

**注意**：调试APK可以直接安装，但不能上传到应用商店。

---

## 安装APK到手机

### 方法一：使用ADB
```bash
# 连接手机（确保开启USB调试）
adb devices

# 安装APK
adb install app-release.apk

# 覆盖安装
adb install -r app-release.apk
```

### 方法二：直接传输
1. 将APK文件复制到手机
2. 在手机上点击APK文件
3. 允许安装未知来源应用
4. 完成安装

---

## 验证APK

### 检查签名
```bash
# 查看APK签名信息
jarsigner -verify -verbose -certs app-release.apk

# 或使用apksigner
apksigner verify -v app-release.apk
```

### 查看APK信息
```bash
# 使用aapt查看APK信息
aapt dump badging app-release.apk

# 查看APK内容
unzip -l app-release.apk
```

---

## 常见问题

### 1. 构建失败：内存不足
在 `gradle.properties` 中添加：
```properties
org.gradle.jvmargs=-Xmx4096m
org.gradle.daemon=true
org.gradle.parallel=true
```

### 2. 签名错误
确保：
- 密钥库路径正确
- 密码正确
- 密钥别名正确

### 3. 安装失败：解析包错误
- 确保APK完整下载
- 检查APK是否签名
- 检查Android版本兼容性

### 4. Gradle同步失败
```bash
# 清理Gradle缓存
./gradlew cleanBuildCache

# 重新同步
./gradlew sync
```

---

## 发布到应用商店

### Google Play Store
1. 创建 [Google Play开发者账号](https://play.google.com/console) ($25一次性费用)
2. 创建新应用
3. 上传APK或AAB（推荐AAB）
4. 填写应用信息
5. 提交审核

### 国内应用商店
1. 华为应用市场
2. 小米应用商店
3. OPPO/vivo应用商店
4. 应用宝
5. 360手机助手

每个商店需要：
- 开发者账号
- 应用APK
- 应用截图
- 应用描述
- 隐私政策链接
- 软件著作权（部分需要）

---

## 文件说明

构建完成后会生成以下文件：

| 文件 | 说明 |
|------|------|
| `app-release.apk` | 签名后的发布版本 |
| `app-release-unsigned.apk` | 未签名的发布版本 |
| `app-debug.apk` | 调试版本 |
| `ai-accounting.keystore` | 签名密钥库（重要，请备份）|
| `output-metadata.json` | 构建元数据 |

---

## 重要提示

1. **备份密钥库**：`ai-accounting.keystore` 文件丢失后将无法更新应用
2. **保护密码**：不要将密钥密码提交到版本控制
3. **版本号**：每次发布更新版本号（versionCode和versionName）
4. **测试**：发布前务必在真机上测试

---

**需要帮助？** 查看 [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) 获取完整的发布检查清单。
