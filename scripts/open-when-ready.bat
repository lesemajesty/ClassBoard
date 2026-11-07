@echo off
rem 等待应用就绪后自动打开浏览器；最多等待约 2 分钟
setlocal
set "URL=%~1"

for /l %%i in (1,1,60) do (
    curl.exe -s -f -o NUL --max-time 2 "%URL%login" >nul 2>nul
    if not errorlevel 1 (
        start "" "%URL%"
        exit /b 0
    )
    timeout /t 2 /nobreak >nul
)

exit /b 1
