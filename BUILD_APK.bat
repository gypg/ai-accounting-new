@echo off
chcp 65001
echo ==========================================
echo AI记账 - APK构建脚本
echo ==========================================
echo.

REM 检查Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到Java，请确保已安装JDK并配置JAVA_HOME
    echo 下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

echo [1/4] Java已安装
echo.

REM 检查Android SDK
if "%ANDROID_HOME%"=="" (
    echo [警告] 未设置ANDROID_HOME环境变量
    echo 请设置ANDROID_HOME指向您的Android SDK目录
echo 例如: set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
    echo.
)

REM 创建必要的目录结构
echo [2/4] 检查项目结构...
if not exist "gradle\wrapper" mkdir "gradle\wrapper"

REM 下载gradle-wrapper.jar（如果不存在）
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo 下载Gradle Wrapper...
    powershell -Command "Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle/wrapper/gradle-wrapper.jar'"
    if errorlevel 1 (
        echo [警告] 无法下载Gradle Wrapper，将使用本地Gradle
        goto USE_LOCAL_GRADLE
    )
)

echo [3/4] 开始构建APK...
echo.

REM 尝试使用Gradle Wrapper
if exist "gradlew.bat" (
    call gradlew.bat assembleDebug
    if errorlevel 1 goto USE_LOCAL_GRADLE
) else (
    goto USE_LOCAL_GRADLE
)

goto BUILD_SUCCESS

:USE_LOCAL_GRADLE
echo 尝试使用本地Gradle...
gradle --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Gradle，请安装Gradle或使用Android Studio构建
    echo 下载地址: https://gradle.org/install/
    pause
    exit /b 1
)

gradle assembleDebug
if errorlevel 1 (
    echo [错误] 构建失败
    pause
    exit /b 1
)

:BUILD_SUCCESS
echo.
echo ==========================================
echo 构建成功！
echo ==========================================
echo.

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo APK文件位置:
    echo   %CD%\app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo 文件大小:
    for %%I in ("app\build\outputs\apk\debug\app-debug.apk") do echo   %%~zI bytes
    echo.
    echo 您可以将此APK安装到Android设备
) else (
    echo [警告] 未找到生成的APK文件
)

echo.
pause
