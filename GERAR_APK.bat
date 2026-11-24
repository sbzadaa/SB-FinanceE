@echo off
setlocal
cd /d "%~dp0"
echo ================================================
echo   SB FINANCE 3.0 - GERAR APK DE TESTE
 echo ================================================
call "PREPARAR_GRADLE.bat"
if errorlevel 1 (echo Falha na preparacao do Gradle. & pause & exit /b 1)
call "gradlew.bat" :app:assembleDebug --stacktrace
if errorlevel 1 (echo ERRO NO BUILD. Veja o texto acima. & pause & exit /b 1)
echo.
echo APK gerado em:
echo %~dp0app\build\outputs\apk\debug\app-debug.apk
explorer "%~dp0app\build\outputs\apk\debug"
pause
