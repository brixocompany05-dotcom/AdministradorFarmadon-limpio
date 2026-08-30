@echo off
setlocal

echo Cancelando compilaciones Gradle previas...

wmic process where "name='java.exe' and commandline like '%%GradleWrapperMain%%'" get ProcessId /format:csv 2>nul | findstr /R /C:"^[0-9]" > gradle_pids.txt
for /f "skip=1 tokens=2 delims=," %%p in (gradle_pids.txt) do (
    echo  - matando PID %%p
    taskkill /F /PID %%p >nul 2>&1
)
del /f gradle_pids.txt >nul 2>&1

echo Ejecutando compilacion...
call gradlew.bat :app:compileDebugKotlin

endlocal
