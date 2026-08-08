@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo Building FastAIVectorDB module...
call mvn -q install -DskipTests
if errorlevel 1 (
    echo Failed to build FastAIVectorDB module.
    pause
    exit /b 1
)

echo Running FastAIVectorDB Demo...
cd examples\Demo
call mvn -q clean compile
if errorlevel 1 (
    echo Demo compilation failed.
    pause
    exit /b 1
)

call mvn -q exec:java "-Dexec.mainClass=demo.Demo" %*
cd ..\..
pause
