#!/usr/bin/env powershell
# Compile and run HomestayBooking application

Set-Location $PSScriptRoot

Write-Host "Compiling HomestayBooking..." -ForegroundColor Yellow
& "C:\Program Files\Microsoft\jdk-21.0.8.9-hotspot\bin\javac.exe" -cp "lib/mysql-connector-j-9.5.0.jar" HomestayBooking.java

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Running HomestayBooking..." -ForegroundColor Yellow
& "C:\Program Files\Microsoft\jdk-21.0.8.9-hotspot\bin\java.exe" -cp ".;lib/mysql-connector-j-9.5.0.jar" HomestayBooking

Read-Host "Press Enter to exit"
