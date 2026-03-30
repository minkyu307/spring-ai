@echo off
setlocal

:: 스크립트 실행 위치와 무관하게 프로젝트 루트에서 동작하도록 고정
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul

if not exist "webapp\package.json" goto :fail_webapp_missing

:: 태그: YYMMDDHHmm 자동 생성 (PowerShell로 날짜 포맷, 로케일 무관)
for /f %%T in ('powershell -NoProfile -Command "Get-Date -Format yyMMddHHmm"') do set TAG=%%T

echo.
echo [Webapp] Build React assets
echo -----------------------------------------------
pushd "webapp" >nul
call npm run build
if errorlevel 1 goto :fail_webapp_build
popd >nul

echo.
echo [Jib] Build ^& Push  ^(tag: %TAG%^)
echo -----------------------------------------------
call mvnw.cmd jib:build -Djib.to.tags=%TAG%,latest -DsendCredentialsOverHttp=true
if errorlevel 1 goto :fail_jib

echo.
echo [DONE] React build + 10.253.12.87:12000/spring-ai:%TAG% pushed successfully.
popd >nul
endlocal
exit /b 0

:fail_webapp_missing
echo [FAIL] webapp\package.json not found.
popd >nul
endlocal
exit /b 1

:fail_webapp_build
echo [FAIL] npm run build failed.
popd >nul
popd >nul
endlocal
exit /b 1

:fail_jib
echo [FAIL] Jib build/push failed.
popd >nul
endlocal
exit /b 1
