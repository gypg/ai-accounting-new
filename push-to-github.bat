@echo off
chcp 65001
cls

echo ==========================================
echo AI记账 - GitHub推送工具
echo ==========================================
echo.

REM 检查Git是否安装
git --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到Git，请先安装Git
    pause
    exit /b 1
)

echo [1/4] 正在配置Git用户信息...
git config user.email "your-email@example.com"
git config user.name "gypg"

echo.
echo [2/4] 正在提交代码...
git add .
git commit -m "Initial commit: AI记账完整项目"

echo.
echo [3/4] 正在添加远程仓库...
git remote add origin https://github.com/gypg/ai-accounting.git 2>nul

echo.
echo [4/4] 正在推送到GitHub...
git push -u origin main

if errorlevel 1 (
    echo.
    echo 尝试推送到master分支...
    git push -u origin master
)

echo.
echo ==========================================
echo 推送完成！
echo ==========================================
echo.
echo 请访问以下链接查看：
echo   https://github.com/gypg/ai-accounting
echo.
echo 查看Actions构建状态：
echo   https://github.com/gypg/ai-accounting/actions
echo.

pause
