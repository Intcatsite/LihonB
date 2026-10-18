@echo off
chcp 65001 >nul
cd /d "%~dp0"
set "SOURCE=OriginalAPKtiktokcom.zhiliaoapp.musically_47.1.3-2024701030_minAPI23(arm64-v8a)(nodpi)_apkmirror.com.apk"
set "OUTPUT=ComplectAPKlihont-2.0-nothing-settings.apk"
if not exist "%SOURCE%" (
  echo Clean TikTok 47.1.3 base is missing:
  echo   %SOURCE%
  exit /b 2
)
py -m lihont.upgrade "%SOURCE%" -o "%OUTPUT%" --work "work/build-nothing-settings"
if errorlevel 1 exit /b %errorlevel%
echo.
echo Built: %OUTPUT%
