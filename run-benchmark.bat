@echo off
chcp 65001 >nul
cls

echo ⚡ Building Main Project & Native DLL...
call compile.bat >nul 2>&1
call mvn clean install -DskipTests -q
if %ERRORLEVEL% NEQ 0 ( echo ❌ Build failed. & pause & exit /b %ERRORLEVEL% )

echo 🛠  Compiling Benchmark...
cd examples\Benchmark
call mvn clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 ( echo ❌ Benchmark build failed. & pause & exit /b %ERRORLEVEL% )

echo 🚀 Running Benchmark...
java -jar target\benchmarks.jar -v EXTRA 2>nul

cd ..\..
pause
