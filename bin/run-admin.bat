@echo off
echo.
echo [INFO] Run ruoyi-admin after stopping any previous local instance.
echo.

cd /d "%~dp0"

if "%SERVER_PORT%"=="" set SERVER_PORT=8081

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-ruoyi-admin.ps1" -Port %SERVER_PORT%
if errorlevel 2 (
    echo [ERROR] Port %SERVER_PORT% is occupied by another process.
    pause
    exit /b 2
)

cd /d "%~dp0..\ruoyi-admin\target"

set JAVA_OPTS=-Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m

java -Dfile.encoding=utf-8 %JAVA_OPTS% -jar ruoyi-admin.jar

pause
