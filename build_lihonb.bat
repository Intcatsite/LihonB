@echo off
chcp 65001 >nul
title Сборка LihonB Beta
cd /d "%~dp0"
python scripts\build_lihonb.py
if errorlevel 1 pause
