# AI记账 - 自动化构建与部署脚本
# 功能：构建APK -> 运行测试 -> 重命名 -> 移动到测试版文件夹 -> 推送到MuMu模拟器
# 作者：AI Assistant
# 版本：1.0.0

param(
    [string]$BuildType = "debug",           # 构建类型：debug 或 release
    [string]$VersionName = "",              # 版本名称（可选，默认从build.gradle读取）
    [string]$TestFolder = "test_builds",    # 测试版文件夹名称
    [switch]$SkipTests = $false,            # 是否跳过测试
    [switch]$SkipBuild = $false,            # 是否跳过构建（使用现有APK）
    [string]$MuMuDevice = "127.0.0.1:16384" # MuMu模拟器地址
)

# 设置错误处理
$ErrorActionPreference = "Stop"

# 颜色定义
$Colors = @{
    Success = "Green"
    Error = "Red"
    Warning = "Yellow"
    Info = "Cyan"
    Normal = "White"
}

# 日志函数
function Write-Log {
    param(
        [string]$Message,
        [string]$Level = "Info"
    )
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $color = $Colors[$Level]
    Write-Host "[$timestamp] [$Level] $Message" -ForegroundColor $color
    
    # 同时写入日志文件
    $logFile = "$PSScriptRoot\auto_build.log"
    "[$timestamp] [$Level] $Message" | Out-File -FilePath $logFile -Append -Encoding UTF8
}

# 检查命令是否存在
function Test-Command {
    param([string]$Command)
    $null = Get-Command $Command -ErrorAction SilentlyContinue
    return $?
}

# 获取版本号
function Get-VersionInfo {
    Write-Log "正在读取版本信息..." "Info"
    
    $buildGradlePath = "$PSScriptRoot\..\app\build.gradle.kts"
    
    if (-not (Test-Path $buildGradlePath)) {
        # 尝试其他路径
        $buildGradlePath = "$PSScriptRoot\..\app\build.gradle"
    }
    
    if (Test-Path $buildGradlePath) {
        $content = Get-Content $buildGradlePath -Raw
        
        # 尝试提取版本号
        $versionCodeMatch = [regex]::Match($content, 'versionCode\s*=\s*(\d+)')
        $versionNameMatch = [regex]::Match($content, "versionName\s*=\s*[""'](.+?)[""']")
        
        $script:VersionCode = if ($versionCodeMatch.Success) { $versionCodeMatch.Groups[1].Value } else { "1" }
        $script:VersionName = if ($versionNameMatch.Success) { $versionNameMatch.Groups[1].Value } else { "1.0.0" }
        
        Write-Log "版本号: $script:VersionName (Build $script:VersionCode)" "Success"
    } else {
        Write-Log "未找到build.gradle文件，使用默认版本号" "Warning"
        $script:VersionCode = "1"
        $script:VersionName = "1.0.0"
    }
}

# 运行测试
function Run-Tests {
    if ($SkipTests) {
        Write-Log "跳过测试阶段" "Warning"
        return $true
    }
    
    Write-Log "开始运行单元测试..." "Info"
    
    try {
        Set-Location "$PSScriptRoot\.."
        
        # 运行单元测试
        $testResult = & .\gradlew.bat test --continue 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Log "✓ 所有测试通过" "Success"
            return $true
        } else {
            Write-Log "✗ 测试失败" "Error"
            Write-Log $testResult "Error"
            return $false
        }
    }
    catch {
        Write-Log "测试执行出错: $_" "Error"
        return $false
    }
}

# 构建APK
function Build-APK {
    if ($SkipBuild) {
        Write-Log "跳过构建阶段，使用现有APK" "Warning"
        return $true
    }
    
    Write-Log "开始构建APK ($BuildType)..." "Info"
    
    try {
        Set-Location "$PSScriptRoot\.."
        
        # 清理旧构建
        Write-Log "清理旧构建文件..." "Info"
        & .\gradlew.bat clean 2>&1 | Out-Null
        
        # 构建APK
        $buildCommand = if ($BuildType -eq "release") { 
            ".\gradlew.bat assembleRelease" 
        } else { 
            ".\gradlew.bat assembleDebug" 
        }
        
        Write-Log "执行: $buildCommand" "Info"
        $buildResult = & $buildCommand 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Log "✓ APK构建成功" "Success"
            return $true
        } else {
            Write-Log "✗ APK构建失败" "Error"
            Write-Log $buildResult "Error"
            return $false
        }
    }
    catch {
        Write-Log "构建过程出错: $_" "Error"
        return $false
    }
}

# 查找生成的APK
function Find-GeneratedAPK {
    Write-Log "查找生成的APK文件..." "Info"
    
    $apkPath = if ($BuildType -eq "release") {
        "$PSScriptRoot\..\app\build\outputs\apk\release\app-release.apk"
    } else {
        "$PSScriptRoot\..\app\build\outputs\apk\debug\app-debug.apk"
    }
    
    if (Test-Path $apkPath) {
        Write-Log "✓ 找到APK: $apkPath" "Success"
        return $apkPath
    } else {
        # 尝试查找任何APK文件
        $apkFiles = Get-ChildItem "$PSScriptRoot\..\app\build\outputs\apk" -Recurse -Filter "*.apk" | Select-Object -First 1
        
        if ($apkFiles) {
            Write-Log "✓ 找到APK: $($apkFiles.FullName)" "Success"
            return $apkFiles.FullName
        } else {
            Write-Log "✗ 未找到APK文件" "Error"
            return $null
        }
    }
}

# 生成新文件名
function Get-NewFileName {
    param([string]$OriginalPath)
    
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $buildTypeStr = if ($BuildType -eq "release") { "release" } else { "debug" }
    
    # 重命名规则: AI记账_v{版本号}_{构建类型}_{时间戳}.apk
    $newFileName = "AI记账_v${script:VersionName}_${buildTypeStr}_${timestamp}.apk"
    
    Write-Log "新文件名: $newFileName" "Info"
    return $newFileName
}

# 确保测试文件夹存在
function Ensure-TestFolder {
    $testFolderPath = "$PSScriptRoot\..\$TestFolder"
    
    if (-not (Test-Path $testFolderPath)) {
        Write-Log "创建测试文件夹: $testFolderPath" "Info"
        New-Item -ItemType Directory -Path $testFolderPath -Force | Out-Null
    }
    
    return $testFolderPath
}

# 移动并重命名APK
function Move-APKToTestFolder {
    param(
        [string]$SourcePath,
        [string]$DestFolder,
        [string]$NewFileName
    )
    
    Write-Log "移动APK到测试文件夹..." "Info"
    
    try {
        $destPath = Join-Path $DestFolder $NewFileName
        
        # 如果文件已存在，先删除
        if (Test-Path $destPath) {
            Remove-Item $destPath -Force
        }
        
        Copy-Item $SourcePath $destPath -Force
        
        Write-Log "✓ APK已复制到: $destPath" "Success"
        return $destPath
    }
    catch {
        Write-Log "✗ 移动APK失败: $_" "Error"
        return $null
    }
}

# 检查MuMu模拟器连接
function Test-MuMuConnection {
    Write-Log "检查MuMu模拟器连接..." "Info"
    
    try {
        $adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
        
        if (-not (Test-Path $adbPath)) {
            Write-Log "未找到ADB工具，尝试使用系统PATH中的adb" "Warning"
            $adbPath = "adb"
        }
        
        # 连接MuMu模拟器
        & $adbPath connect $MuMuDevice 2>&1 | Out-Null
        
        # 检查设备连接
        $devices = & $adbPath devices 2>&1
        
        if ($devices -match $MuMuDevice) {
            Write-Log "✓ MuMu模拟器已连接" "Success"
            return $true
        } else {
            Write-Log "✗ MuMu模拟器未连接" "Error"
            return $false
        }
    }
    catch {
        Write-Log "检查连接时出错: $_" "Error"
        return $false
    }
}

# 推送APK到MuMu模拟器
function Push-APKToMuMu {
    param([string]$ApkPath)
    
    Write-Log "推送APK到MuMu模拟器..." "Info"
    
    try {
        $adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
        if (-not (Test-Path $adbPath)) {
            $adbPath = "adb"
        }
        
        # 安装APK
        Write-Log "正在安装APK..." "Info"
        $installResult = & $adbPath -s $MuMuDevice install -r "$ApkPath" 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Log "✓ APK安装成功" "Success"
            
            # 启动应用
            Write-Log "启动应用..." "Info"
            $packageName = if ($BuildType -eq "debug") { 
                "com.example.aiaccounting.debug" 
            } else { 
                "com.example.aiaccounting" 
            }
            
            & $adbPath -s $MuMuDevice shell am start -n "$packageName/com.example.aiaccounting.MainActivity" 2>&1 | Out-Null
            
            Write-Log "✓ 应用已启动" "Success"
            return $true
        } else {
            Write-Log "✗ APK安装失败" "Error"
            Write-Log $installResult "Error"
            return $false
        }
    }
    catch {
        Write-Log "推送过程出错: $_" "Error"
        return $false
    }
}

# 清理旧APK文件（保留最近10个）
function Clean-OldAPKs {
    param([string]$TestFolderPath)
    
    Write-Log "清理旧APK文件..." "Info"
    
    try {
        $apkFiles = Get-ChildItem $TestFolderPath -Filter "*.apk" | Sort-Object LastWriteTime -Descending
        
        if ($apkFiles.Count -gt 10) {
            $filesToDelete = $apkFiles | Select-Object -Skip 10
            foreach ($file in $filesToDelete) {
                Remove-Item $file.FullName -Force
                Write-Log "删除旧文件: $($file.Name)" "Info"
            }
        }
        
        Write-Log "✓ 清理完成" "Success"
    }
    catch {
        Write-Log "清理过程出错: $_" "Warning"
    }
}

# 主函数
function Main {
    Write-Log "========================================" "Info"
    Write-Log "AI记账 - 自动化构建与部署" "Info"
    Write-Log "========================================" "Info"
    
    $startTime = Get-Date
    
    try {
        # 1. 获取版本信息
        Get-VersionInfo
        
        # 2. 运行测试
        if (-not (Run-Tests)) {
            Write-Log "测试未通过，终止流程" "Error"
            exit 1
        }
        
        # 3. 构建APK
        if (-not (Build-APK)) {
            Write-Log "构建失败，终止流程" "Error"
            exit 1
        }
        
        # 4. 查找APK
        $apkPath = Find-GeneratedAPK
        if (-not $apkPath) {
            Write-Log "未找到APK文件，终止流程" "Error"
            exit 1
        }
        
        # 5. 生成新文件名
        $newFileName = Get-NewFileName -OriginalPath $apkPath
        
        # 6. 确保测试文件夹存在
        $testFolderPath = Ensure-TestFolder
        
        # 7. 移动并重命名APK
        $newApkPath = Move-APKToTestFolder -SourcePath $apkPath -DestFolder $testFolderPath -NewFileName $newFileName
        if (-not $newApkPath) {
            Write-Log "移动APK失败，终止流程" "Error"
            exit 1
        }
        
        # 8. 清理旧APK
        Clean-OldAPKs -TestFolderPath $testFolderPath
        
        # 9. 检查MuMu连接
        if (-not (Test-MuMuConnection)) {
            Write-Log "MuMu模拟器未连接，跳过推送步骤" "Warning"
            Write-Log "APK已保存到: $newApkPath" "Success"
        } else {
            # 10. 推送到MuMu
            if (Push-APKToMuMu -ApkPath $newApkPath) {
                Write-Log "✓ 完整流程执行成功！" "Success"
            } else {
                Write-Log "推送失败，但APK已保存到: $newApkPath" "Warning"
            }
        }
        
        $endTime = Get-Date
        $duration = $endTime - $startTime
        Write-Log "总耗时: $($duration.ToString('mm\:ss'))" "Info"
        Write-Log "========================================" "Info"
        
        exit 0
    }
    catch {
        Write-Log "流程执行出错: $_" "Error"
        Write-Log "异常详情: $($_.Exception.ToString())" "Error"
        Write-Log $_.ScriptStackTrace "Error"
        exit 1
    }
}

# 执行主函数
Main
