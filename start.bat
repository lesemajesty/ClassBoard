@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion
title 智慧电子班牌 - 一键启动
cd /d "%~dp0"

rem ============================================================
rem  数据库账号：也可以用环境变量 MYSQL_USER / MYSQL_PASSWORD 覆盖
rem ============================================================
if not defined MYSQL_USER set "MYSQL_USER=root"
if not defined MYSQL_PASSWORD set "MYSQL_PASSWORD=1234"

set "APP_PORT=8080"
set "APP_URL=http://localhost:%APP_PORT%/"

rem 传 --no-browser 可跳过自动打开浏览器（服务器/调试时用）
set "OPEN_BROWSER=1"
if /i "%~1"=="--no-browser" set "OPEN_BROWSER=0"

echo ============================================================
echo   智慧电子班牌 · 一键启动
echo ============================================================
echo   数据库：%MYSQL_USER%@localhost:3306/classboard
echo   访问地址：%APP_URL%
echo ============================================================
echo.

rem ---------- 1. 检查 Java ----------
where java >nul 2>nul
if errorlevel 1 (
    echo [错误] 未找到 Java。请安装 JDK 17 并把 JAVA_HOME 指向安装目录。
    goto :fail
)
if not defined JAVA_HOME (
    echo [错误] 未设置 JAVA_HOME。请把它指向 JDK 17 安装目录后重新运行。
    goto :fail
)
echo [检查] JAVA_HOME=%JAVA_HOME%

rem ---------- 2. 检查 Maven ----------
where mvn >nul 2>nul
if errorlevel 1 (
    echo [错误] 未找到 Maven。请安装 Maven 3.9+ 并加入 PATH。
    goto :fail
)
echo [检查] Maven 已就绪

rem ---------- 3. 应用是否已经在运行 ----------
curl.exe -s -f -o NUL --max-time 2 "%APP_URL%login" >nul 2>nul
if not errorlevel 1 (
    echo [提示] 应用已经在运行：%APP_URL%
    if "%OPEN_BROWSER%"=="1" (
        start "" "%APP_URL%"
        echo [提示] 已在浏览器中打开。
    )
    goto :done
)

echo.
echo [启动] 正在拉起应用，首次启动约需 15 秒，请稍候...
echo [提示] 启动完成后自动打开浏览器；按 Ctrl+C 停止应用。
echo.

if "%OPEN_BROWSER%"=="0" goto :skip_browser
start "" /b cmd /c call "%~dp0scripts\open-when-ready.bat" "%APP_URL%"
:skip_browser

call mvn spring-boot:run
if errorlevel 1 (
    echo.
    echo [错误] 应用启动失败，请查看上方 Maven 日志。常见原因：
    echo        1. MySQL 服务没有启动：net start MySQL8.0
    echo        2. 数据库口令不正确，可先设置环境变量再重试：
    echo           set MYSQL_PASSWORD=你的密码
    goto :fail
)
goto :end

:done
echo.
pause
exit /b 0

:fail
echo.
pause
exit /b 1

:end
echo.
echo 应用已停止。
pause
exit /b 0
