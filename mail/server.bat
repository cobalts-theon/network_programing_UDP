@echo off
setlocal
cd /d "%~dp0"
title Mail UDP - Server
if not exist build mkdir build
javac -encoding UTF-8 -d build Server.java
if errorlevel 1 goto compile_error
java -cp build Server
pause
exit /b

:compile_error
echo Khong bien dich duoc. Kiem tra da cai JDK va them javac vao PATH.
pause
exit /b 1
