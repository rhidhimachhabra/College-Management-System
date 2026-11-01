
@echo off
title College Management System

cd /d "%~dp0"

if not exist out mkdir out

set "JAR=lib\mysql-connector-j-26.10.0.jar"

if not exist "%JAR%" (
    echo MySQL Connector JAR not found!
    pause
    exit /b 1
)

if not defined DB_USER (
    echo DB_USER environment variable is missing!
    pause
    exit /b 1
)

if not defined DB_PASSWORD (
    echo DB_PASSWORD environment variable is missing!
    pause
    exit /b 1
)

echo Compiling application...

javac -cp "%JAR%" -d out src\DBConnection.java

if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Starting College Management System...

java -cp "out;%JAR%" DBConnection

pause
