@echo off
setlocal

:: 태그: YYMMDDHHmm 자동 생성 (PowerShell로 날짜 포맷, 로케일 무관)
for /f %%T in ('powershell -NoProfile -Command "Get-Date -Format yyMMddHHmm"') do set TAG=%%T

echo.
echo [Jib] Build ^& Push  ^(tag: %TAG%^)
echo -----------------------------------------------
call mvnw.cmd jib:build -Djib.to.tags=%TAG%,latest -DsendCredentialsOverHttp=true
if %ERRORLEVEL% neq 0 ( echo [FAIL] Jib build/push failed & exit /b 1 )

echo.
echo [DONE] 10.253.12.87:12000/spring-ai:%TAG% pushed successfully.
endlocal
