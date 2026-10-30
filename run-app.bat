
@echo off
setlocal
cd /d "%~dp0"

echo ================================================
echo Launching Smart Campus Lost and Found
echo ================================================

if exist "%USERPROFILE%\.jdks\ms-21.0.12.1\bin\javac.exe" (
    set "PATH=%USERPROFILE%\.jdks\ms-21.0.12.1\bin;%PATH%"
)

if not exist "bin" mkdir "bin"

javac --release 21 -cp "lib\mysql-connector-j-8.4.0.jar;lib\flatlaf-3.5.4.jar" -d "bin" -sourcepath "src" src\com\campus\lostfound\Main.java

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
