# ADR-012 — Estratégia de Deploy

## Status
Aceito

## Data
2026-09-01

## Contexto
O sistema precisa de uma estratégia de deploy que seja:
- **Acessível financeiramente** — o orçamento é limitado
- **Simples de operar** — sem necessidade de infraestrutura complexa para manter
- **Confiável** — disponibilidade adequada para uma loja em operação
- Com possibilidade de **migrar para AWS** no futuro quando o negócio crescer

## Decisão
Utilizar **Railway** como plataforma de deploy inicial, com plano de migração documentado para AWS quando o volume e o orçamento justificarem.

---

## Por que não AWS agora

A AWS é excelente, mas tem um custo de entrada e complexidade operacional que não se justificam para uma loja pequena no início:

| Aspecto              | AWS (ECS + RDS + ALB)  | Railway               |
|----------------------|------------------------|-----------------------|
| Custo mensal estimado| ~R$ 300–600/mês        | ~R$ 25–80/mês         |
| Configuração inicial | Alta (VPC, IAM, etc.)  | Baixa (GUI simples)   |
| Manutenção           | Alta                   | Mínima                |
| Escalabilidade       | Ilimitada              | Suficiente para início|
| Migração futura      | Fácil (containers)     | —                     |

---

## Railway — Plataforma Inicial

### O que é
Railway é uma plataforma PaaS (Platform as a Service) que gerencia infraestrutura automaticamente. Deploy via Git push, banco PostgreSQL gerenciado incluso, HTTPS automático e variáveis de ambiente pelo painel.

### Custo estimado
- **Plano Hobby:** ~US$ 5/mês (suficiente para início)
- **Plano Pro:** ~US$ 20/mês (quando precisar de mais recursos)
- Banco PostgreSQL: incluso no plano, sem custo adicional inicial

### Arquitetura no Railway

```
┌─────────────────────────────────────────────┐
│                  Railway                     │
│                                             │
│  ┌─────────────────┐   ┌─────────────────┐  │
│  │  estilofit-api  │   │  estilofit-web  │  │
│  │  (Spring Boot)  │   │  (React/Nginx)  │  │
│  │  Container      │   │  Container      │  │
│  └────────┬────────┘   └────────┬────────┘  │
│           │                     │           │
│  ┌────────▼────────┐            │           │
│  │   PostgreSQL    │            │           │
│  │   (gerenciado)  │            │           │
│  └─────────────────┘            │           │
└─────────────────────────────────┼───────────┘
                                  │
                          ┌───────▼───────┐
                          │   Usuário     │
                          │   (browser)   │
                          └───────────────┘
```

### Serviços no Railway
| Serviço          | Tipo          | Observação                                      |
|------------------|---------------|-------------------------------------------------|
| `estilofit-api`  | Web Service   | Deploy do Dockerfile do backend                 |
| `estilofit-web`  | Static Site   | Build do React servido via Nginx                |
| `postgres`       | Database      | PostgreSQL gerenciado pelo Railway              |

### Fluxo de Deploy
```
git push origin main
       │
       ▼
Railway detecta push
       │
       ▼
Build automático (Dockerfile)
       │
       ▼
Flyway executa migrations pendentes
       │
       ▼
Nova versão em produção (zero-downtime)
```

### Domínio
- Railway fornece domínios gratuitos (`*.up.railway.app`)
- Domínio personalizado pode ser configurado apontando DNS — recomendado usar `api.estilofit.com.br` e `app.estilofit.com.br`

---

## Dockerfiles

### Backend (`estilofit-api/Dockerfile`)
```dockerfile
# Stage 1 — Build
FROM gradle:8-jdk21-alpine AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# Stage 2 — Runtime (imagem menor)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend (`estilofit-web/Dockerfile`)
```dockerfile
# Stage 1 — Build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2 — Serve com Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

### Nginx config (`estilofit-web/nginx.conf`)
```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    # Necessário para React Router (SPA)
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache para assets estáticos
    location ~* \.(js|css|png|jpg|svg|ico|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## Plano de Migração para AWS (futuro)

Quando o negócio crescer e justificar o investimento, a migração é natural porque já usamos containers:

### Fase 1 — Migração do Banco (RDS)
- Criar instância RDS PostgreSQL na AWS
- Fazer dump do Railway e restore no RDS
- Atualizar variável `DB_HOST` no backend

### Fase 2 — Migração da API (ECS Fargate)
- Fazer push da imagem Docker para ECR (Elastic Container Registry)
- Criar task definition no ECS Fargate
- Configurar Application Load Balancer

### Fase 3 — Migração do Frontend (S3 + CloudFront)
- Build do React e upload para bucket S3
- Configurar CloudFront como CDN
- Certificado SSL via ACM (gratuito)

### Custo AWS quando migrar
| Serviço           | Tipo                   | Custo estimado/mês |
|-------------------|------------------------|--------------------|
| RDS PostgreSQL    | db.t3.micro (free tier)| ~R$ 0–80           |
| ECS Fargate       | 0.25 vCPU / 0.5GB      | ~R$ 50–100         |
| S3 + CloudFront   | Site estático          | ~R$ 5–15           |
| ALB               | Load Balancer          | ~R$ 80             |
| **Total**         |                        | **~R$ 135–275/mês**|

---

## Ambientes

| Ambiente      | Branch   | URL                                  | Banco               |
|---------------|----------|--------------------------------------|---------------------|
| Desenvolvimento | local  | `http://localhost:8080`              | Docker local        |
| Produção      | `main`   | `https://api.estilofit.up.railway.app` | Railway PostgreSQL |

> Ambiente de homologação pode ser adicionado no Railway sem custo extra quando necessário.

---

## Justificativa
- Railway oferece 90% dos benefícios da AWS a 10% do custo para o porte atual
- A arquitetura em containers garante portabilidade — migrar para AWS no futuro é questão de reconfigurar destino, não reescrever código
- Deploy via git push reduz fricção operacional para uma pessoa gerenciar
- PostgreSQL gerenciado no Railway elimina a necessidade de DBA para manutenção do banco

## Consequências
- Deploy em produção é disparado automaticamente pelo push na branch `main` — cuidado com merges diretos
- Recomendado criar branch `develop` para desenvolvimento e só mergear na `main` quando estiver pronto para produção
- Variáveis de ambiente configuradas no painel do Railway — nunca em arquivos commitados
- Backups do banco são responsabilidade manual (ver ADR-013)
