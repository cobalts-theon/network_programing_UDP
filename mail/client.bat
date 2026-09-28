@echo off
setlocal
cd /d "%~dp0"
title Mail UDP - Client
if not exist build mkdir build
javac -encoding UTF-8 -d build MailUI.java
if errorlevel 1 goto compile_error
java -cp build MailUI
if errorlevel 1 pause
exit /b

:compile_error
echo Khong bien dich duoc. Kiem tra da cai JDK va them javac vao PATH.
pause
exit /b 1
