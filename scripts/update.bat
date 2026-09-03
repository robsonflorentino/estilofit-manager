@echo off
REM ============================================================================
REM  Atualiza o EstiloFit Manager para a versao mais recente (Windows)
REM  PRESERVA os dados: faz backup, baixa a nova versao e reconstroi os
REM  containers. NUNCA apaga o volume do banco.
REM  Uso: clique duas vezes neste arquivo (ou rode no PowerShell).
REM ============================================================================

REM Vai para a pasta do projeto (a pasta-pai deste script).
cd /d "%~dp0.."

echo ============================================================
echo  Atualizacao do EstiloFit Manager
echo ============================================================
echo.

REM --- 1. Verifica se o Docker esta rodando ---------------------------------
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
  echo [ERRO] O Docker Desktop nao esta rodando. Abra o Docker Desktop,
  echo        aguarde ficar "Running" e rode este arquivo de novo.
  echo.
  pause
  exit /b 1
)

REM --- 2. Backup do banco ANTES de atualizar (seguranca) --------------------
echo [1/4] Fazendo backup do banco antes de atualizar...
call "%~dp0backup.bat"
if %ERRORLEVEL% NEQ 0 (
  echo.
  echo [ATENCAO] O backup falhou. A atualizacao foi CANCELADA por seguranca.
  echo Verifique se a aplicacao esta rodando (docker ps) e tente de novo.
  echo.
  pause
  exit /b 1
)
echo.

REM --- 3. Baixa a nova versao do codigo (branch main) -----------------------
echo [2/4] Baixando a versao mais recente...
git checkout main
git pull
if %ERRORLEVEL% NEQ 0 (
  echo.
  echo [ERRO] Nao foi possivel baixar a nova versao (git pull).
  echo Verifique a conexao com a internet.
  echo.
  pause
  exit /b 1
)
echo.

REM --- 4. Reconstroi e sobe os containers (PRESERVANDO o banco) -------------
echo [3/4] Reconstruindo a aplicacao (isso pode levar alguns minutos)...
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
if %ERRORLEVEL% NEQ 0 (
  echo.
  echo [ERRO] Falha ao subir a aplicacao. Os dados continuam salvos.
  echo Rode "docker compose -f docker-compose.prod.yml logs" para ver o motivo.
  echo.
  pause
  exit /b 1
)
echo.

echo [4/4] Pronto! A aplicacao foi atualizada.
echo Aguarde cerca de 1 minuto e acesse http://localhost
echo (a primeira resposta pode demorar enquanto a API termina de subir).
echo.
echo Os dados foram preservados e ha um backup em: backups\
echo.
pause
exit /b 0
