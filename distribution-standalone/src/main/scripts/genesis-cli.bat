@rem .
@rem Copyright (c) 2010-2013 Grid Dynamics Consulting Services, Inc, All Rights Reserved
@rem .

@echo off

if "%OS%"=="Windows_NT" goto nt
echo Unsupported Windows version: %OS%
pause
goto :eof

:nt
set GENESIS_HOME=%~dp0..

:execute
%JAVA_HOME%\bin\java -cp %GENESIS_HOME%/lib/*;* -Dgenesis_home=%GENESIS_HOME% -Dbackend.properties=file:%GENESIS_HOME%/conf/genesis.properties -Dlogback.configurationFile=%GENESIS_HOME%/conf/logback-cli.xml -Dakka.loglevel="ERROR" -XX:MaxPermSize=400M com.griddynamics.genesis.cli.GenesisShell %1 %2 %3 %4 %5 %6 %7 %8 %9


:end
endlocal

:finish
cmd /C exit /B %ERRORLEVEL%
