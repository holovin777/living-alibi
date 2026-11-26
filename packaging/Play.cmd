@echo off
setlocal
"%~dp0runtime\bin\java.exe" -jar "%~dp0launcher.jar" %*
if errorlevel 1 pause
