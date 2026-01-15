@echo off
setlocal

echo ========================================
echo Agent de Scan Local
echo ========================================
echo.

REM Vérifier Java 17
java -version 2>&1 | findstr /i "version" >nul
if %errorlevel% neq 0 (
    echo ERREUR: Java n'est pas installe ou non accessible dans le PATH
    echo Veuillez installer Java 17 ou plus recent
    pause
    exit /b 1
)

REM Vérifier version Java
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
    goto :check_version
)

:check_version
echo Version Java detectee: %JAVA_VERSION%
echo.

REM Chercher le JAR dans target/
set JAR_FILE=
if exist "target\scanner-cin-1.0.0.jar" (
    set JAR_FILE=target\scanner-cin-1.0.0.jar
) else if exist "scanner-cin-1.0.0.jar" (
    set JAR_FILE=scanner-cin-1.0.0.jar
) else (
    echo ERREUR: JAR introuvable!
    echo.
    echo Le fichier JAR n'a pas ete trouve. Veuillez compiler le projet avec:
    echo   mvn clean package
    echo.
    echo Ou placer le JAR dans le repertoire courant ou dans target/
    pause
    exit /b 1
)

echo JAR trouve: %JAR_FILE%
echo.

REM Configuration par défaut
set PORT=7070
set CONFIG_FILE=

REM Parser les arguments
:parse_args
if "%~1"=="" goto :start
if "%~1"=="--port" (
    set PORT=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--config" (
    set CONFIG_FILE=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--help" (
    echo Usage: start-windows.bat [options]
    echo.
    echo Options:
    echo   --port PORT        Port HTTP (defaut: 7070)
    echo   --config PATH      Chemin vers fichier de configuration
    echo   --help             Afficher cette aide
    echo.
    pause
    exit /b 0
)
shift
goto :parse_args

:start
echo Demarrage de l'agent de scan...
echo Port: %PORT%
if not "%CONFIG_FILE%"=="" (
    echo Fichier de configuration: %CONFIG_FILE%
    java -jar %JAR_FILE% --port=%PORT% --config=%CONFIG_FILE%
) else (
    java -jar %JAR_FILE% --port=%PORT%
)

if %errorlevel% neq 0 (
    echo.
    echo ERREUR: L'agent de scan s'est arrete avec une erreur
    pause
    exit /b %errorlevel%
)

pause