# Feature 004 — Scaffold do Frontend + Login

| Campo         | Valor                                  |
|---------------|----------------------------------------|
| Branch        | `feature/web-scaffold-login`           |
| Data          | 2026-09-01                             |
| Projeto       | `estilofit-web`                        |
| Status        | ✅ Concluída e verificada end-to-end   |
| Depende de    | Feature 001 (auth do backend)          |
| ADRs          | ADR-005 (auth), ADR-006 (frontend), ADR-009 (erros), ADR-010 (paginação) |

---

## Objetivo

Criar a base do frontend web (`estilofit-web`) com autenticação funcional conectada à API real, para acompanhar visualmente cada feature de negócio daqui pra frente.

---

## Escopo Entregue

### Infraestrutura
- Projeto **Vite + React 19 + TypeScript**
- **Tailwind CSS 3** configurado com o tema completo do design system (cores da marca, dark mode, fontes, radius, shadows)
- Classes utilitárias base (`.btn-primary`, `.btn-secondary`, `.input-base`, `.card`)
- `.env` / `.env.example` com `VITE_API_BASE_URL`

### Camada de API (`lib/api.ts`)
- Cliente Axios com `withCredentials` (para o httpOnly cookie do refresh token)
- **Access token em memória** (nunca em localStorage) — mitiga XSS
- Request interceptor injeta `Authorization: Bearer`
- Response interceptor (ADR-009):
  - `401` → tenta refresh silencioso 1x; se falhar, limpa sessão e vai pra `/login`
  - `403` → toast "sem permissão"
  - `500+` → toast de erro de servidor
- Helper `getApiErrorMessage`

### Autenticação
- `authService` (login, logout)
- `authStore` (Zustand): estado do usuário, `login`, `logout`, `clear`, `hasRole`

### Guards e Rotas (ADR-005)
- `PrivateRoute` — exige autenticação, redireciona pra `/login`
- `RoleRoute` — exige perfil, redireciona pra `/403`
- `RoleGuard` — oculta elementos de UI por perfil (UX, não segurança)

### Telas
- **LoginPage** — identidade visual EstiloFit (marca ESTILO+FIT, dark mode), validação com react-hook-form + zod, estados de loading, toasts
- **DashboardPage** — placeholder com KPI cards (serão ligados à API nas próximas features)
- **ForbiddenPage** (403)
- **AppLayout** — sidebar colapsável (menu filtrado por perfil, grupos Principal/Administração) + topbar (usuário, logout)

---

## Estrutura de Pastas

```
estilofit-web/src/
├── components/RoleGuard.tsx
├── layouts/AppLayout.tsx
├── lib/api.ts
├── pages/
│   ├── LoginPage.tsx
│   ├── DashboardPage.tsx
│   └── ForbiddenPage.tsx
├── routes/
│   ├── PrivateRoute.tsx
│   └── RoleRoute.tsx
├── services/authService.ts
├── store/authStore.ts
├── types/api.ts
├── App.tsx        ← router
├── main.tsx       ← Toaster + handler de sessão
└── index.css      ← Tailwind + tema
```

---

## Menu por Perfil (implementado no AppLayout)

| Item              | ADMIN | MANAGER | SELLER |
|-------------------|:-----:|:-------:|:------:|
| Dashboard         |  ✅   |   ✅    |   ✅   |
| Produtos          |  ✅   |   ✅    |   ❌   |
| Estoque           |  ✅   |   ✅    |   ✅   |
| Fornecedores      |  ✅   |   ✅    |   ❌   |
| Vendas            |  ✅   |   ✅    |   ✅   |
| Contas a Receber  |  ✅   |   ✅    |   ❌   |
| Relatórios        |  ✅   |   ✅    |   ❌   |
| Alertas           |  ✅   |   ✅    |   ❌   |
| Usuários          |  ✅   |   ❌    |   ❌   |
| Configurações     |  ✅   |   ❌    |   ❌   |

> As rotas dessas telas ainda serão criadas conforme cada módulo de negócio for implementado. Nesta feature, apenas o Dashboard tem página real; os demais itens do menu apontam para rotas que serão adicionadas.

---

## Requisito de Ambiente

O frontend exige **Node.js ≥ 20.19** (o Vite 8 depende disso). No ambiente de desenvolvimento foi instalado o **Node 22.12.0 LTS** de forma isolada em `~/.local/node`, sem afetar o Homebrew ou outros pacotes do sistema.

---

## Como Rodar

```bash
# 1. Backend + banco (ver Feature 001)
cd estilofit-manager && docker compose up -d
cd estilofit-api && ./gradlew bootRun

# 2. Frontend
cd estilofit-web
npm install
npm run dev
```

Acesse `http://localhost:5173` e faça login com:
```
admin@estilofit.com.br / admin@123
```

---

## Verificação Realizada

| Cenário                                        | Esperado                       | Status |
|------------------------------------------------|--------------------------------|:------:|
| Typecheck TypeScript (`tsc`)                   | 0 erros                        | ✅     |
| Dev server Vite sobe                           | :5173 HTTP 200                 | ✅     |
| Login via API com Origin do frontend           | 200 + token                    | ✅     |
| CORS libera a origem do frontend               | Allow-Origin + Allow-Credentials | ✅   |
| Refresh token entregue como httpOnly cookie    | Secure + HttpOnly + SameSite   | ✅     |

---

## Próximos Passos
- As telas de negócio (produtos, estoque, vendas...) serão criadas junto com cada feature de backend correspondente
- Retomar a feature de **Produtos + Variações** (backend), começando pelas decisões pendentes do tech design (003)
