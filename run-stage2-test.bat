@echo off
echo ==================================================
echo   Compiling Smart Campus Stage 2...
echo ==================================================
"%USERPROFILE%\.jdks\ms-21.0.12.1\bin\javac.exe" -cp "lib\mysql-connector-j-8.4.0.jar;src" -d bin src\com\campus\lostfound\config\*.java src\com\campus\lostfound\models\*.java src\com\campus\lostfound\utils\*.java src\com\campus\lostfound\dao\*.java src\com\campus\lostfound\Stage2Test.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ==================================================
echo   Running Stage 2 Test...
echo ==================================================
"%USERPROFILE%\.jdks\ms-21.0.12.1\bin\java.exe" -cp "bin;lib\mysql-connector-j-8.4.0.jar" com.campus.lostfound.Stage2Test

echo.
pause
