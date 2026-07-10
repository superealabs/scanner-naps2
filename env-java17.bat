@echo off
REM ============================================================================
REM  env-java17.bat - Configure Java 17 pour la session cmd COURANTE uniquement.
REM
REM  Usage (depuis une fenetre cmd EXISTANTE) :
REM      env-java17.bat
REM
REM  NE PAS double-cliquer : la console jetable se ferme aussitot et la config
REM  est perdue. Il faut le lancer dans une fenetre cmd que vous gardez ouverte.
REM
REM  Resolution du JDK 17 (premier valide gagne) :
REM      1. variable d'environnement JDK17_HOME
REM      2. fichier jdk17.properties (cle JDK17_HOME=...)
REM      3. auto-detection (dossiers d'install connus, puis registre)
REM
REM  IMPORTANT : PAS de "setlocal" ici - les changements de JAVA_HOME / PATH
REM  doivent persister dans le terminal appelant.
REM ============================================================================

set "PROJECT_DIR=%~dp0"
set "CFG_FILE=%PROJECT_DIR%jdk17.properties"
set "JDK17_RESOLVED="
set "VALID="
set "CFG_HOME="

REM --- Avertissement double-clic (console jetable) ---------------------------
REM  Heuristique : un cmd lance par double-clic contient "/c" ET le nom du
REM  script dans %cmdcmdline%. Les deux conditions evitent les faux positifs
REM  (ex: appel via "call" depuis un autre .bat, ou depuis un autre outil).
set "_DCLICK="
echo %cmdcmdline% | find /i "/c" >nul && echo %cmdcmdline% | find /i "%~nx0" >nul && set "_DCLICK=1"
if defined _DCLICK (
    echo(
    echo [WARN] Ce script semble avoir ete double-clique.
    echo        Les changements ne persistent QUE s'il est lance depuis une
    echo        fenetre cmd DEJA ouverte :   env-java17.bat
    echo(
)
set "_DCLICK="

REM --- Source 1 : override via variable d'environnement ----------------------
if defined JDK17_HOME (
    call :validate17 "%JDK17_HOME%"
    if defined VALID (
        set "JDK17_RESOLVED=%JDK17_HOME%"
        goto :apply
    )
    echo [WARN] JDK17_HOME="%JDK17_HOME%" n'est pas un JDK 17 valide - ignore.
)

REM --- Source 2 : fichier de config jdk17.properties -------------------------
if exist "%CFG_FILE%" (
    for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%CFG_FILE%") do (
        if /i "%%a"=="JDK17_HOME" set "CFG_HOME=%%b"
    )
)
REM  Appel HORS bloc "(...)" : enchainer plusieurs call :sous-routine dans un
REM  meme bloc parenthese casse la resolution de label sous CMD.
if defined CFG_HOME call :try_config
if defined JDK17_RESOLVED goto :apply

REM --- Source 3 : auto-detection --------------------------------------------
call :autodetect
if defined JDK17_RESOLVED goto :apply

REM --- Rien trouve ----------------------------------------------------------
echo(
echo [ERROR] Aucun JDK 17 trouve.
echo   Solution : copiez jdk17.properties.example en jdk17.properties et renseignez :
echo        JDK17_HOME=C:\Chemin\Vers\jdk-17
echo   ou bien :  set JDK17_HOME=C:\Chemin\Vers\jdk-17   puis relancez env-java17.bat
echo(
call :cleanup_tmp
exit /b 1

REM ==========================================================================
:apply
if not defined _JDK17_ORIG_PATH set "_JDK17_ORIG_PATH=%PATH%"
set "JAVA_HOME=%JDK17_RESOLVED%"
set "PATH=%JAVA_HOME%\bin;%_JDK17_ORIG_PATH%"
echo(
echo [OK] JAVA_HOME=%JAVA_HOME%
"%JAVA_HOME%\bin\java.exe" -version
echo(
call :cleanup_tmp
goto :eof

REM ==========================================================================
REM  Sous-routines
REM ==========================================================================

:validate17
REM  %1 = home candidat. Definit VALID=1 si c'est un JDK 17, sinon vide VALID.
set "VALID="
set "V_HOME=%~1"
if "%V_HOME%"=="" goto :eof
if not exist "%V_HOME%\bin\java.exe"  goto :eof
if not exist "%V_HOME%\bin\javac.exe" goto :eof
set "V_TOK="
REM  On se place dans le dossier bin : appeler ".\java.exe" (sans chemin entre
REM  guillemets) evite le bug de for /f quand le chemin contient une espace.
pushd "%V_HOME%\bin" 2>nul
if errorlevel 1 goto :eof
for /f "tokens=3" %%v in ('.\java.exe -version 2^>^&1 ^| findstr /i "version"') do (
    if not defined V_TOK set "V_TOK=%%v"
)
popd
if not defined V_TOK goto :eof
set "V_TOK=%V_TOK:"=%"
for /f "delims=." %%m in ("%V_TOK%") do set "V_MAJ=%%m"
if "%V_MAJ%"=="17" set "VALID=1"
goto :eof

:autodetect
REM  Definit JDK17_RESOLVED si un JDK 17 est trouve (dossiers puis registre).
for %%R in (
    "%ProgramFiles%\Java\jdk-17*"
    "%ProgramFiles%\Java\jdk17*"
    "%ProgramFiles%\Eclipse Adoptium\jdk-17*"
    "%ProgramFiles%\Microsoft\jdk-17*"
    "%ProgramFiles%\Zulu\zulu-17*"
    "%ProgramFiles%\Amazon Corretto\jdk17*"
    "%ProgramFiles%\BellSoft\LibericaJDK-17*"
    "%ProgramFiles%\Semeru\jdk-17*"
    "%ProgramFiles(x86)%\Java\jdk-17*"
    "%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-17*"
    "%LOCALAPPDATA%\Programs\Java\jdk-17*"
) do (
    for /d %%d in (%%~R) do (
        call :validate17 "%%~d"
        if defined VALID (
            set "JDK17_RESOLVED=%%~d"
            goto :eof
        )
    )
)
call :reg_probe "HKLM\SOFTWARE\JavaSoft\JDK"
if defined JDK17_RESOLVED goto :eof
call :reg_probe "HKLM\SOFTWARE\Eclipse Adoptium\JDK"
if defined JDK17_RESOLVED goto :eof
call :reg_probe "HKLM\SOFTWARE\Microsoft\JDK"
goto :eof

:reg_probe
REM  %1 = cle registre racine. Definit JDK17_RESOLVED si JavaHome valide en 17.
set "R_KEY=%~1"
set "R_VER="
set "R_HOME="
for /f "tokens=2,*" %%a in ('reg query "%R_KEY%" /v CurrentVersion 2^>nul ^| findstr /i "CurrentVersion"') do set "R_VER=%%b"
if not defined R_VER goto :eof
echo %R_VER% | findstr /b /c:"17" >nul
if errorlevel 1 goto :eof
for /f "tokens=2,*" %%a in ('reg query "%R_KEY%\%R_VER%" /v JavaHome 2^>nul ^| findstr /i "JavaHome"') do set "R_HOME=%%b"
if not defined R_HOME goto :eof
call :validate17 "%R_HOME%"
if defined VALID set "JDK17_RESOLVED=%R_HOME%"
goto :eof

:try_config
REM  Nettoie la valeur lue dans jdk17.properties (retire d'eventuels
REM  guillemets), la valide, et definit JDK17_RESOLVED si c'est un JDK 17.
REM  Appelee HORS bloc "(...)" pour eviter le bug de resolution de label.
set "CFG_HOME=%CFG_HOME:"=%"
call :validate17 "%CFG_HOME%"
if defined VALID (
    set "JDK17_RESOLVED=%CFG_HOME%"
    goto :eof
)
echo [WARN] jdk17.properties : "%CFG_HOME%" n'est pas un JDK 17 valide - ignore.
goto :eof

:cleanup_tmp
REM  Efface les variables temporaires (garde JAVA_HOME, PATH, _JDK17_ORIG_PATH).
set "VALID="
set "V_HOME="
set "V_TOK="
set "V_MAJ="
set "CFG_HOME="
set "CFG_FILE="
set "PROJECT_DIR="
set "JDK17_RESOLVED="
set "R_KEY="
set "R_VER="
set "R_HOME="
goto :eof
