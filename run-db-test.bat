@echo off
echo ==================================================
echo   Compiling Smart Campus Database Test...
echo ==================================================
"C:\Program Files\Java\jdk-26.0.1\bin\javac.exe" -cp "lib\mysql-connector-j-8.4.0.jar;src" -d bin src\com\campus\lostfound\config\DBConnection.java src\com\campus\lostfound\DatabaseTest.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ==================================================
echo   Running Database Test...
echo ==================================================
"C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "bin;lib\mysql-connector-j-8.4.0.jar" com.campus.lostfound.DatabaseTest

echo.
pause
