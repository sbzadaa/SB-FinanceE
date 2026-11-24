@echo off
setlocal
cd /d "%~dp0"
if exist "gradle\wrapper\gradle-wrapper.jar" goto CHECK
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $url='https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar'; $target='gradle/wrapper/gradle-wrapper.jar'; Invoke-WebRequest -Uri $url -OutFile $target -UseBasicParsing"
if errorlevel 1 (echo ERRO: falha ao baixar o Gradle Wrapper oficial. & exit /b 1)
:CHECK
powershell -NoProfile -ExecutionPolicy Bypass -Command "$expected='81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f'; $actual=(Get-FileHash 'gradle/wrapper/gradle-wrapper.jar' -Algorithm SHA256).Hash.ToLower(); if($actual -ne $expected){Write-Host 'FALHA: hash diferente do divulgado pelo Gradle.'; exit 1}; Write-Host 'Gradle Wrapper verificado com SHA-256.'"
if errorlevel 1 (del /q "gradle\wrapper\gradle-wrapper.jar" 2>nul & exit /b 1)
exit /b 0
