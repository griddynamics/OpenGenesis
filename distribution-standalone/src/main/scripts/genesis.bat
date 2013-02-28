@rem .
@rem Copyright (c) 2010-2013 Grid Dynamics Consulting Services, Inc, All Rights Reserved
@rem .

@echo off
setlocal enableextensions

if "%OS%"=="Windows_NT" goto nt
echo Unsupported Windows version: %OS%
pause
goto :eof

:nt
set REALPATH=%~dp0

set DIST_BITS=32
if "%PROCESSOR_ARCHITECTURE%" == "AMD64" goto amd64
if not "%ProgramW6432%" == "" goto amd64
goto pickwrapper

:amd64
set DIST_BITS=64

:pickwrapper
set WRAPPER_EXE=%REALPATH%jsw\windows-x86-%DIST_BITS%\wrapper.exe
if exist "%WRAPPER_EXE%" goto pickconfig
echo Missing wrapper executable: %WRAPPER_EXE%
pause
goto end

:pickconfig
set WRAPPER_CONF=%REALPATH%\jsw\conf\wrapper.conf
if exist "%WRAPPER_CONF%" goto execute
echo Missing wrapper config: %WRAPPER_CONF%
pause
goto end

:execute
for /F %%v in ('echo %1^|findstr "^console$ ^start$ ^stop$ ^restart$ ^install$ ^uninstall"') do call :exec set COMMAND=%%v

if "%COMMAND%" == "" (
    echo Usage: %0 { console : start : stop : restart : install : uninstall }
    pause
    goto end
) else (
    shift
)

call :%COMMAND%
if errorlevel 1 pause
goto end

:console
"%WRAPPER_EXE%" -c "%WRAPPER_CONF%"
goto :eof

:start
"%WRAPPER_EXE%" -t "%WRAPPER_CONF%"
goto :eof

:stop
"%WRAPPER_EXE%" -p "%WRAPPER_CONF%"
goto :eof

:install
"%WRAPPER_EXE%" -i "%WRAPPER_CONF%"
goto :eof

:uninstall
"%WRAPPER_EXE%" -r "%WRAPPER_CONF%"
goto :eof

:restart
call :stop
call :start
goto :eof

:exec
%*
goto :eof

:end
endlocal

:finish
cmd /C exit /B %ERRORLEVEL%
