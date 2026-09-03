@echo off
REM ============================================================================
REM  Backup do banco do EstiloFit Manager (Windows)
REM  Gera um arquivo backup-AAAA-MM-DD_HH-MM.sql na pasta "backups".
REM  Uso manual: clique duas vezes neste arquivo.
REM  Uso agendado: aponte o Agendador de Tarefas do Windows para ele.
REM ============================================================================

REM Vai para a pasta do projeto (a pasta-pai deste script), qualquer que seja o diretorio atual.
cd /d "%~dp0.."

REM Nome do container do Postgres (definido no docker-compose.prod.yml).
set "CONTAINER=estilofit_db"

REM Usuario e banco. Ajuste se voce trocou no .env.prod.
set "DB_USER=estilofit"
set "DB_NAME=estilofit_manager"

REM Monta um carimbo de data/hora seguro para nome de arquivo (AAAA-MM-DD_HH-MM).
for /f "tokens=1-3 delims=/-. " %%a in ("%date%") do set "D=%%c-%%b-%%a"
REM Alguns sistemas trazem a data como AAAA-MM-DD; normaliza pegando via PowerShell (robusto).
for /f %%t in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HH-mm"') do set "STAMP=%%t"

if not exist "backups" mkdir "backups"
set "OUTFILE=backups\backup-%STAMP%.sql"

echo Gerando backup em "%OUTFILE%" ...
docker exec %CONTAINER% pg_dump -U %DB_USER% %DB_NAME% > "%OUTFILE%"

if %ERRORLEVEL% NEQ 0 (
  echo.
  echo [ERRO] Falha ao gerar o backup. O container "%CONTAINER%" esta rodando?
  echo Verifique com: docker ps
  REM Remove arquivo vazio criado pelo redirecionamento em caso de erro.
  if exist "%OUTFILE%" del "%OUTFILE%"
  exit /b 1
)

echo Backup concluido: "%OUTFILE%"

REM ---------------------------------------------------------------------------
REM  Retencao: mantem apenas os 30 backups mais recentes (apaga os mais antigos).
REM ---------------------------------------------------------------------------
powershell -NoProfile -Command "Get-ChildItem 'backups\backup-*.sql' | Sort-Object LastWriteTime -Descending | Select-Object -Skip 30 | Remove-Item -Force"

exit /b 0
