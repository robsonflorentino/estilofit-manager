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

O Docker Desktop também pode ligar sozinho ao iniciar o Windows (Settings → General →
"Start Docker Desktop when you sign in"), deixando a aplicação disponível automaticamente.

---

## Backup dos dados (recomendado)

Os dados ficam no notebook. Faça cópias de vez em quando:
```powershell
docker exec estilofit_db pg_dump -U estilofit estilofit_manager > backup.sql
```
Guarde o `backup.sql` num pen drive ou na nuvem. Para restaurar num futuro (ex.: ao migrar
para um servidor), usa-se esse arquivo.

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
