@echo off
setlocal enabledelayedexpansion

if "%~1"=="" (
    echo Uso: %~nx0 ^<protocolo^>
    exit /b 1
)

set PROTOCOL=%~1

:: Configuração do Java
if "%JAVA_HOME%"=="" (
    echo JAVA_HOME não está configurado. Por favor, configure-o ou adicione o caminho do Java ao PATH.
    exit /b 1
)
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

:: Configuração do projeto
set "PROJECT_ROOT=%~dp0"
set "CLASSES_DIR=%PROJECT_ROOT%target\classes"

:: Mudar local do JAR
set "POSTGRESQL_JAR=C:\Users\ianpo\.m2\repository\org\postgresql\postgresql\42.7.4\postgresql-42.7.4.jar"

:: Configuração do ClassPath
set "CP=%CLASSES_DIR%;%POSTGRESQL_JAR%"

:: Adicionar outras dependências, se necessário
if exist "%PROJECT_ROOT%target\dependency" (
    for %%i in ("%PROJECT_ROOT%target\dependency\*.jar") do (
        set "CP=!CP!;%%i"
    )
)

echo ClassPath: %CP%

:: Define os IDs de cada servidor e os IDs dos outros nós
set SERVER_1_ID=0
set SERVER_1_NODES=1 2

set SERVER_2_ID=1
set SERVER_2_NODES=0 2

set SERVER_3_ID=2
set SERVER_3_NODES=0 1

call mvn clean install

call docker-compose down
call docker-compose up -d

echo Iniciando servidores com protocolo %PROTOCOL%...

:: Inicia o primeiro servidor
start cmd /k ""%JAVA_EXE%" -Dfile.encoding=UTF-8 -classpath "%CP%" com.server.Server %PROTOCOL% %SERVER_1_ID% %SERVER_1_NODES%"
echo Servidor 1 iniciado com ID %SERVER_1_ID% e nós: %SERVER_1_NODES%

:: Inicia o segundo servidor
start cmd /k ""%JAVA_EXE%" -Dfile.encoding=UTF-8 -classpath "%CP%" com.server.Server %PROTOCOL% %SERVER_2_ID% %SERVER_2_NODES%"
echo Servidor 2 iniciado com ID %SERVER_2_ID% e nós: %SERVER_2_NODES%

:: Inicia o terceiro servidor
start cmd /k ""%JAVA_EXE%" -Dfile.encoding=UTF-8 -classpath "%CP%" com.server.Server %PROTOCOL% %SERVER_3_ID% %SERVER_3_NODES%"
echo Servidor 3 iniciado com ID %SERVER_3_ID% e nós: %SERVER_3_NODES%

:: Inicia o ApiGateway
start cmd /k ""%JAVA_EXE%" -Dfile.encoding=UTF-8 -classpath "%CP%" com.gateway.ApiGateway %PROTOCOL% %SERVER_1_ID% %SERVER_2_ID% %SERVER_3_ID%"
echo Gateway iniciado com protocolo %PROTOCOL%

endlocal