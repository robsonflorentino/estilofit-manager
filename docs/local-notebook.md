# Rodar localmente no notebook (Windows)

Guia para usar o EstiloFit Manager no notebook, sem servidor nem internet.
A aplicação roda em containers Docker e é acessada pelo navegador em `http://localhost`.

> Uso local/interno. A base começa **limpa**: apenas o usuário administrador,
> os canais de venda (Instagram, WhatsApp, Presencial) e as configurações do sistema.
> Categorias, produtos, fornecedores, vendedores e vendas são cadastrados do zero.

---

## 1. Instalar o Docker Desktop

1. Baixe o **Docker Desktop para Windows**: https://www.docker.com/products/docker-desktop/
2. Instale e reinicie o notebook se for pedido.
3. Abra o Docker Desktop e aguarde ficar "Running" (ícone da baleia estável).

> Requer Windows 10/11. O instalador ativa o WSL2 automaticamente se necessário.

---

## 2. Obter o projeto

Opção A — com Git instalado (PowerShell):
```powershell
git clone https://github.com/robsonflorentino/estilofit-manager.git
cd estilofit-manager
```
Opção B — sem Git: baixe o projeto como .zip pelo GitHub, extraia, e abra o PowerShell
dentro da pasta extraída.

---

## 3. Criar o arquivo de configuração local

Na pasta do projeto, crie um arquivo chamado **`.env.prod`** com este conteúdo
(pode copiar e colar; troque as senhas se quiser):

```
DB_NAME=estilofit_manager
DB_USERNAME=estilofit
DB_PASSWORD=umaSenhaLocalQualquer
JWT_SECRET=troque_por_um_texto_longo_e_aleatorio_para_a_maquina_local_1234567890
JWT_ACCESS_TOKEN_EXPIRATION=28800
JWT_REFRESH_TOKEN_EXPIRATION=604800
COOKIE_SECURE=false
CORS_ALLOWED_ORIGINS=http://localhost
SWAGGER_ENABLED=false
WEB_PORT=80
```

> **Importante:** `COOKIE_SECURE=false` é obrigatório no uso local (sem HTTPS).
> Se ficar `true`, o login "cai" ao recarregar a página.

Se a porta 80 estiver ocupada no notebook, troque `WEB_PORT=80` por `WEB_PORT=8080`
(e acesse `http://localhost:8080`).

---

## 4. Subir a aplicação

No PowerShell, dentro da pasta do projeto:
```powershell
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```
A primeira vez leva alguns minutos (compila tudo). Nas próximas é rápido.

---

## 5. Acessar

Abra o navegador em **http://localhost** (ou `http://localhost:8080` se trocou a porta).

Login inicial:
- **E-mail:** `admin@estilofit.com.br`
- **Senha:** `admin@123`

Troque a senha do admin no primeiro acesso e comece a cadastrar categorias, produtos,
fornecedores e a equipe.

---

## Operação do dia a dia

```powershell
# parar (os dados ficam salvos)
docker compose -f docker-compose.prod.yml down

# iniciar de novo
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

---

## Subir a aplicação automaticamente no boot do Windows

A stack já está configurada com `restart: unless-stopped` nos três containers
(postgres, api, web). Ou seja: **assim que o Docker Desktop liga, os containers voltam
sozinhos** — desde que da última vez você tenha usado `up -d` (e não `down`/`stop`).

Falta só garantir que o **Docker Desktop** inicie junto com o Windows.

### 1. Fazer o Docker Desktop iniciar ao ligar o notebook
1. Abra o **Docker Desktop**.
2. Vá em **Settings** (engrenagem) → **General**.
3. Marque **"Start Docker Desktop when you sign in"**.
4. (Recomendado) Marque também **"Open Docker Dashboard at startup"** desmarcado,
   para não abrir a janela do Docker toda vez.
5. Clique em **Apply & restart**.

Pronto. Toda vez que ela ligar o notebook e fizer login no Windows, o Docker sobe e,
logo em seguida, a aplicação sobe sozinha. Basta abrir `http://localhost` no navegador.

> Observação: o Docker Desktop só inicia **após o login** no Windows. Se o notebook
> ligar e parar na tela de login, a aplicação só sobe quando ela entrar na conta.
> Para o uso no dia a dia da loja isso costuma ser suficiente.

### 2. (Opcional) Abrir a loja no navegador automaticamente
Para o navegador já abrir a tela de login ao ligar:
1. Pressione **Win + R**, digite `shell:startup` e Enter (abre a pasta de Inicialização).
2. Crie um atalho nessa pasta apontando para:
   ```
   http://localhost
   ```
   (Botão direito → Novo → Atalho → cole `http://localhost` → Avançar → Concluir.)

Assim, ao ligar e logar: Docker sobe → containers sobem → navegador abre a loja.

> Dica: dê ~1 minuto após o login antes de esperar a tela carregar na primeira vez do dia,
> pois a API leva alguns segundos para ficar pronta.

---

## Backup dos dados (recomendado)

Os dados ficam no notebook. Vale copiar o banco com frequência.

**Backup na hora:** clique duas vezes em `scripts\backup.bat`. Ele gera um arquivo
`backups\backup-AAAA-MM-DD_HH-MM.sql` e mantém os 30 backups mais recentes.

**Backup automático (agendado):** dá para o Windows rodar o backup sozinho todo dia
pelo Agendador de Tarefas. O passo a passo completo está em `scripts\README.md`
(resumo: Agendador de Tarefas → Criar Tarefa → disparador diário → ação apontando
para o `scripts\backup.bat`).

> Guarde os arquivos da pasta `backups\` num pen drive ou na nuvem de vez em quando —
> são eles que salvam os dados se o notebook falhar ou ao migrar para um servidor no futuro.

**Comando manual equivalente** (se preferir digitar):
```powershell
docker exec estilofit_db pg_dump -U estilofit estilofit_manager > backup.sql
```

**Restaurar um backup:**
```powershell
docker exec -i estilofit_db psql -U estilofit -d estilofit_manager < backups\backup-AAAA-MM-DD_HH-MM.sql
```

---

## Acessar de outro aparelho na mesma rede (opcional)

Para abrir no celular/tablet da loja apontando para o notebook:
1. Descubra o IP do notebook: `ipconfig` (procure "Endereço IPv4", ex.: `192.168.0.15`).
2. No `.env.prod`, ajuste `CORS_ALLOWED_ORIGINS=http://192.168.0.15` e recrie a stack.
3. Acesse `http://192.168.0.15` no outro aparelho (mesma rede Wi-Fi).

> Continua sem HTTPS (rede local), então `COOKIE_SECURE=false`. Não exponha isso à internet.

---

## Se algo der errado
- Docker Desktop precisa estar "Running" antes de subir a stack.
- Ver logs: `docker compose -f docker-compose.prod.yml logs -f`
- Porta 80 ocupada: troque `WEB_PORT` (passo 3) e recrie com `up -d`.
