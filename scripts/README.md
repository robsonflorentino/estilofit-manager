# Scripts (Windows)

## update.bat — atualizar para a versão mais recente
Atualiza a aplicação **preservando os dados** já cadastrados. Ele:
1. Faz um **backup** do banco antes de qualquer coisa (se o backup falhar, cancela).
2. Baixa a versão mais recente (`git checkout main` + `git pull`).
3. Reconstrói e sobe os containers com `up -d --build` — **sem apagar o banco**.

- **Rodar:** clique duas vezes em `scripts\update.bat`.
- **Requisito:** Docker Desktop rodando e conexão com a internet.
- Após atualizar, aguarde ~1 minuto e acesse `http://localhost` (a API demora alguns
  segundos para subir; um 502 momentâneo é normal nesse intervalo).

> ⚠️ **NUNCA** rode `docker compose ... down -v` para atualizar. O `-v` apaga o volume do
> banco e você perde todos os dados. A atualização certa (via `update.bat` ou os comandos
> abaixo) mantém o volume e apenas aplica as novas migrations no banco existente.

### Atualização manual (equivalente, no PowerShell)
```powershell
# 1) backup por seguranca
.\scripts\backup.bat
# 2) baixar a nova versao
git checkout main
git pull
# 3) reconstruir preservando os dados (SEM -v)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

### Se a API não subir após atualizar (502 que não passa)
Veja o motivo no log da API:
```powershell
docker compose -f docker-compose.prod.yml logs api
```
- `password authentication failed`: o `.env.prod` foi alterado (senha do banco) depois que
  o volume já existia. **Não apague o volume** se há dados reais — restaure o `.env.prod`
  para a senha original com que o banco foi criado.
- Erro de migration (`Migration ... failed`): não apague nada; guarde o log e peça suporte.

---

## backup.bat
Gera um dump do banco em `backups\backup-AAAA-MM-DD_HH-MM.sql` e mantém os 30 backups
mais recentes (apaga os mais antigos automaticamente).

- **Rodar na hora:** clique duas vezes em `scripts\backup.bat`.
- **Requisito:** o Docker Desktop e o container `estilofit_db` precisam estar rodando.

### Agendar backup automático (Agendador de Tarefas do Windows)
1. Abra o menu Iniciar, digite **Agendador de Tarefas** e abra.
2. À direita, clique em **Criar Tarefa** (não "Tarefa Básica", para termos mais opções).
3. **Aba Geral:**
   - Nome: `Backup EstiloFit`
   - Marque **Executar estando o usuário conectado ou não**.
   - Marque **Executar com privilégios mais altos**.
4. **Aba Disparadores** → **Novo**:
   - Iniciar a tarefa: **Em um agendamento** → **Diariamente**.
   - Escolha o horário (ex.: 22:00, com a loja já fechada).
   - (Opcional) Marque **Repetir a cada** 4 horas, se quiser mais de um backup por dia.
5. **Aba Ações** → **Novo**:
   - Ação: **Iniciar um programa**.
   - Programa/script: navegue até o `backup.bat`, por exemplo
     `C:\Users\<usuario>\estilofit-manager\scripts\backup.bat`
   - Deixe "Adicionar argumentos" e "Iniciar em" em branco (o script já entra na pasta certa).
6. **Aba Condições:** desmarque "Iniciar a tarefa somente se o computador estiver
   em fonte de alimentação" se ela usa o notebook na bateria.
7. **OK.** Vai pedir a senha do Windows (necessária para rodar mesmo sem login).

### Testar o agendamento
No Agendador, selecione a tarefa `Backup EstiloFit` e clique em **Executar** (menu direito).
Confira se apareceu um arquivo novo em `backups\`.

> Se o notebook estiver desligado no horário agendado, o Windows roda a tarefa no próximo
> boot se você marcar, na aba **Configurações**, "Executar a tarefa assim que possível
> após uma inicialização agendada perdida".

### Restaurar um backup (quando precisar)
```powershell
docker exec -i estilofit_db psql -U estilofit -d estilofit_manager < backups\backup-AAAA-MM-DD_HH-MM.sql
```
> Restaurar sobrescreve/insere dados no banco atual. Faça num banco vazio ou tenha certeza
> do que está fazendo. Para uma restauração limpa, o ideal é recriar a stack com volume novo antes.
