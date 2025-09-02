@echo off
setlocal enabledelayedexpansion

REM Simple runner for the renovated v2 CLI (Windows).
REM Usage: run-v2.bat --cases=10 --seed=42
set ARGS=%*
if "%ARGS%"=="" set ARGS=--cases=10 --seed=42

call gradlew.bat :cli:installDist
call cli\build\install\cli\bin\cli.bat %ARGS%

