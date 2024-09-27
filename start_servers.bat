@echo off
if "%~1"=="" (
    echo Uso: %~nx0 ^<protocolo^>
    exit /b 1
)

set PROTOCOL=%~1

:: Define os IDs de cada servidor e os IDs dos outros nós
set SERVER_1_ID=0
set SERVER_1_NODES=1 2

set SERVER_2_ID=1
set SERVER_2_NODES=0 2

set SERVER_3_ID=2
set SERVER_3_NODES=0 1

echo Compilando classes Java...

:: Compila as classes Java usando um loop for
for %%f in (src/com/server/*.java) do (
    javac -d target/classes %%f
    if errorlevel 1 (
        echo Erro ao compilar %%f. Verifique o código-fonte e tente novamente.
        exit /b 1
    )
)

echo Iniciando servidores com protocolo %PROTOCOL%...

:: Inicia o primeiro servidor
start cmd /k "java -cp target/classes com.server.Server %PROTOCOL% %SERVER_1_ID% %SERVER_1_NODES%"
echo Servidor 1 iniciado com ID %SERVER_1_ID% e nós: %SERVER_1_NODES%

:: Inicia o segundo servidor
start cmd /k "java -cp target/classes com.server.Server %PROTOCOL% %SERVER_2_ID% %SERVER_2_NODES%"
echo Servidor 2 iniciado com ID %SERVER_2_ID% e nós: %SERVER_2_NODES%

:: Inicia o terceiro servidor
start cmd /k "java -cp target/classes com.server.Server %PROTOCOL% %SERVER_3_ID% %SERVER_3_NODES%"
echo Servidor 3 iniciado com ID %SERVER_3_ID% e nós: %SERVER_3_NODES%
