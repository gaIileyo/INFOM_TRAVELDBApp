@echo off
REM Compile and run HomestayBooking application
cd /d "%~dp0"

echo Compiling HomestayBooking...
javac -cp lib/mysql-connector-j-9.5.0.jar HomestayBooking.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Running HomestayBooking...
java -cp .;lib/mysql-connector-j-9.5.0.jar HomestayBooking

pause
