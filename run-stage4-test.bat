@echo off
echo ==================================================
echo   Compiling Smart Campus Stage 4...
echo ==================================================
"%USERPROFILE%\.jdks\ms-21.0.12.1\bin\javac.exe" -cp "lib\mysql-connector-j-8.4.0.jar;lib\flatlaf-3.5.4.jar;src" -d bin src\com\campus\lostfound\config\*.java src\com\campus\lostfound\models\*.java src\com\campus\lostfound\utils\*.java src\com\campus\lostfound\dao\*.java src\com\campus\lostfound\ui\common\*.java src\com\campus\lostfound\ui\auth\*.java src\com\campus\lostfound\ui\student\*.java src\com\campus\lostfound\Main.java src\com\campus\lostfound\Stage4Test.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ==================================================
echo   Running Stage 4 Automated Test...
echo ==================================================
"%USERPROFILE%\.jdks\ms-21.0.12.1\bin\java.exe" -cp "bin;lib\mysql-connector-j-8.4.0.jar;lib\flatlaf-3.5.4.jar" com.campus.lostfound.Stage4Test

echo.
pause
