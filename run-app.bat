
@echo off
setlocal
cd /d "%~dp0"

echo ================================================
echo Launching Smart Campus Lost and Found
echo ================================================

if not exist "bin" mkdir "bin"

javac -cp "lib\mysql-connector-j-8.4.0.jar;lib\flatlaf-3.5.4.jar" -d "bin" -sourcepath "src" src\com\campus\lostfound\Main.java

if errorlevel 1 (
    echo.
    echo Compilation failed. Please check the errors above.
    pause
    exit /b 1
)

java -cp "bin;lib\mysql-connector-j-8.4.0.jar;lib\flatlaf-3.5.4.jar" com.campus.lostfound.Main

if errorlevel 1 (
    echo.
    echo Application exited with an error.
    pause
)

endlocal
