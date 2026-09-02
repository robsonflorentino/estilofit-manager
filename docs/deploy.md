# Deploy — EstiloFit Manager (VPS / Docker)

Guia para publicar o sistema num VPS Linux (ex.: Hostinger) usando Docker.
A stack sobe com um comando: Postgres + API (Spring Boot) + Frontend (nginx com proxy da API).

---

## Arquitetura da stack

```
Internet ──▶ [ nginx :80 (frontend) ]
                 │  /            → estáticos do React (SPA)
                 │  /api/…       → proxy para a API
                 ▼
             [ api :8080 (Spring Boot, profile prod) ]
                 ▼
             [ postgres :5432 (rede interna, com volume) ]
```
Frontend e API ficam na **mesma origem** (o nginx faz proxy de `/api`), então não há
problema de CORS e o cookie de sessão funciona naturalmente.

---

## Pré-requisitos no VPS

1. Um VPS Linux (Ubuntu recomendado). Para esta stack, **2 GB de RAM** já é confortável.
2. Docker e Docker Compose:
   ```bash
   curl -fsSL https://get.docker.com | sh
   sudo usermod -aG docker $USER   # relogue após este comando
   ```
3. Git (`sudo apt install -y git`).

---

## Passo a passo

### 1. Clonar o projeto
```bash
git clone <URL_DO_REPO> estilofit-manager
cd estilofit-manager
```

### 2. Criar o arquivo de ambiente de produção
```bash
cp .env.prod.example .env.prod
```
Edite `.env.prod` e preencha com valores reais. **Gere segredos fortes:**
```bash
# senha do banco
openssl rand -base64 24
# segredo do JWT
openssl rand -base64 48
```
Preencha `DB_PASSWORD`, `JWT_SECRET`, e ajuste `CORS_ALLOWED_ORIGINS` para o seu domínio.
Mantenha `COOKIE_SECURE=true` (pressupõe HTTPS na frente — ver seção HTTPS).

> O `.env.prod` **não** é versionado (está no `.gitignore`). Nunca o commite.

### 3. Subir a stack
```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```
O primeiro build leva alguns minutos (compila a API e o frontend). Nas próximas vezes é rápido.

> **Alternativa (recomendada): usar as imagens já publicadas no GHCR.**
> Em vez de buildar no VPS, o workflow de Release publica as imagens a cada tag `v*`
> (ver `docs/ci-cd.md`). Assim você pode usar um compose que só faz `pull` das imagens
> `ghcr.io/robsonflorentino/estilofit-manager-api:<versão>` e `-web:<versão>`, sem compilar
> no servidor — mais rápido e leve. Basta trocar o bloco `build:` por `image:` no compose.

O Flyway aplica as migrations automaticamente na primeira subida. O usuário administrador
padrão é criado na inicialização (ver credenciais internas do projeto).

### 4. Verificar
```bash
docker compose -f docker-compose.prod.yml ps         # todos "Up"; o db deve estar "healthy"
docker compose -f docker-compose.prod.yml logs -f api # acompanhar a API
curl -i http://localhost/                             # frontend responde 200
```
Acesse `http://SEU_IP/` no navegador.

---

## HTTPS e domínio (recomendado para produção)

Com `COOKIE_SECURE=true`, a aplicação exige HTTPS para manter a sessão. Opções:

- **Proxy reverso com Caddy** (mais simples — HTTPS automático via Let's Encrypt):
  aponte o domínio para o IP do VPS e coloque um Caddy na frente encaminhando para o `web:80`.
- **Nginx + Certbot** no host, ou o proxy/manager de SSL do próprio provedor.

Enquanto não houver HTTPS (ex.: testando por IP), rode com `COOKIE_SECURE=false` — mas isso
**não é recomendado** para produção, pois o refresh token trafega sem a proteção do Secure.

Ajuste `CORS_ALLOWED_ORIGINS` e (se usar proxy externo) `WEB_PORT` conforme necessário.

---

## Operação do dia a dia

```bash
# atualizar após um novo commit
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

# parar / iniciar
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# logs
docker compose -f docker-compose.prod.yml logs -f api
```

### Backup do banco
```bash
docker exec estilofit_db pg_dump -U estilofit estilofit_manager > backup_$(date +%F).sql
```
Automatize com um cron diário e guarde os backups fora do VPS.

---

## Checklist de segurança antes de ir ao ar

- [ ] `JWT_SECRET` forte e único (não o valor de exemplo)
- [ ] `DB_PASSWORD` forte
- [ ] `COOKIE_SECURE=true` e HTTPS ativo na frente
- [ ] `SWAGGER_ENABLED=false` (padrão)
- [ ] `CORS_ALLOWED_ORIGINS` restrito ao seu domínio
- [ ] Trocar a senha do administrador padrão após o primeiro login
- [ ] Firewall do VPS: expor apenas 80/443 (e 22 para SSH); o Postgres **não** deve ficar público
- [ ] Rotina de backup do banco configurada
- [ ] `.env.prod` fora do controle de versão (confirmado no `.gitignore`)

---

## Notas
- A porta do Postgres **não é exposta** para fora no compose de produção (fica só na rede interna) — mais seguro.
- O build da API roda `bootJar -x test` (os testes usam Testcontainers e exigem Docker; rode-os no ambiente de desenvolvimento/CI, não no build da imagem).
- Perfil ativo da API em produção: `prod` (`application-prod.yml`).
