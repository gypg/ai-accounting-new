@echo off
chcp 65001
cls

echo ==========================================
echo AI记账 - 签名密钥生成工具
echo ==========================================
echo.

REM 检查Java是否安装
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到Java环境，请先安装JDK
    pause
    exit /b 1
)

echo [1/5] 正在生成签名密钥...

REM 生成密钥库
keytool -genkey -v \
    -keystore app\ai-accounting.keystore \
    -alias aiaccounting \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=AI Accounting, OU=Development, O=AI Accounting Team, L=Beijing, ST=Beijing, C=CN" \
    -storepass aiaccounting123 \
    -keypass aiaccounting123

if errorlevel 1 (
    echo [错误] 密钥生成失败
    pause
    exit /b 1
)

echo.
echo [2/5] 正在验证密钥...

keytool -list -v \
    -keystore app\ai-accounting.keystore \
    -alias aiaccounting \
    -storepass aiaccounting123

if errorlevel 1 (
    echo [错误] 密钥验证失败
    pause
    exit /b 1
)

echo.
echo [3/5] 正在导出证书...

keytool -export -rfc \
    -keystore app\ai-accounting.keystore \
    -alias aiaccounting \
    -file app\ai-accounting.cer \
    -storepass aiaccounting123

if errorlevel 1 (
    echo [错误] 证书导出失败
    pause
    exit /b 1
)

echo.
echo [4/5] 正在生成密钥指纹...

keytool -list -v \
    -keystore app\ai-accounting.keystore \
    -alias aiaccounting \
    -storepass aiaccounting123 | findstr "SHA256:" > app\fingerprint.txt

echo.
echo ==========================================
echo [5/5] 密钥生成完成！
echo ==========================================
echo.
echo 生成的文件：
echo   - app\ai-accounting.keystore (密钥库)
echo   - app\ai-accounting.cer (证书)
echo   - app\fingerprint.txt (密钥指纹)
echo.
echo 密钥信息：
echo   - 别名：aiaccounting
echo   - 密码：aiaccounting123
echo   - 有效期：10000天（约27年）
echo.
echo ⚠️ 重要提示：
echo   1. 请妥善保管密钥库文件和密码
echo   2. 密钥库文件用于应用签名，丢失将无法更新应用
echo   3. 建议将密钥库备份到安全位置
echo   4. 不要将此脚本中的密码用于生产环境
echo.

pause