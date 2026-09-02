# Feature 005 — Tela de Categorias (Frontend)

| Campo         | Valor                                  |
|---------------|----------------------------------------|
| Branch        | `feature/web-categories`               |
| Data          | 2026-09-01                             |
| Projeto       | `estilofit-web`                        |
| Status        | ✅ Concluída e verificada              |
| Depende de    | Feature 002 (API de categorias), Feature 004 (scaffold web) |

---

## Objetivo

Trazer o módulo de Categorias (já pronto no backend) para o frontend, com uma tela de CRUD completa. Também estabelece a base de data fetching (React Query) e os componentes reutilizáveis que as próximas telas vão usar.

---

## Escopo Entregue

### Data fetching
- **TanStack Query (React Query)** adicionado e configurado no `main.tsx` (`QueryClientProvider`, defaults: retry 1, sem refetch no foco, staleTime 30s)

### Componentes base reutilizáveis (`src/components/`)
- `PageHeader` — cabeçalho de página (ícone, título, descrição, ação)
- `Badge` — selo de status (success, danger, warning, info, purple)
- `Modal` — modal genérico (overlay, header com fechar, body, footer)
- `ConfirmDialog` — diálogo de confirmação (com variante destrutiva)
- `DataTable<T>` — tabela genérica por colunas, com estados de loading e vazio

> Esses componentes serão reaproveitados por Usuários, Produtos, Fornecedores e demais telas.

### Módulo de Categorias
- `types/category.ts` — `Category`, `CategoryRequest`
- `services/categoryService.ts` — list, create, rename, updateStatus
- `pages/CategoriesPage.tsx` — tela completa

---

## Funcionalidades da Tela

| Recurso                       | Comportamento                                               |
|-------------------------------|-------------------------------------------------------------|
| Listagem                      | Tabela com nome + status, ordenada pela API                 |
| Filtro "Mostrar inativas"     | Alterna entre `onlyActive=true/false`                       |
| Nova categoria                | Modal com validação (nome 2–100 caracteres)                 |
| Renomear                      | Mesmo modal, pré-preenchido                                 |
| Ativar / Desativar            | Diálogo de confirmação (desativar é ação destrutiva)        |
| Nome duplicado (409)          | Erro exibido no campo do formulário                         |
| Feedback                      | Toasts de sucesso/erro                                      |

---

## Controle de Acesso

- Rota `/categories` protegida por `RoleRoute roles={["ADMIN", "MANAGER"]}`
- Item "Categorias" no menu (ícone `FolderTree`) visível apenas para Admin e Gestor
- Vendedor não vê o menu nem acessa a rota (cai em `/403`)

---

## Como Testar

Com backend + banco rodando (ver Feature 001) e frontend em `npm run dev`:

1. Acesse `http://localhost:5173` e faça login como admin
2. Clique em **Categorias** no menu lateral
3. Veja as 8 categorias seed listadas
4. Teste: criar nova, renomear, desativar, marcar "mostrar inativas", tentar nome duplicado

---

## Verificação Realizada

| Cenário                                   | Status |
|-------------------------------------------|:------:|
| Typecheck TypeScript (0 erros)            | ✅     |
| Frontend serve (HTTP 200)                 | ✅     |
| CategoriesPage transforma sem erro (Vite) | ✅     |
| Backend responde categorias (8 seed)      | ✅     |

> Verificação técnica automatizada. O teste visual interativo (clicar, criar, editar) fica a cargo do usuário no navegador.

---

## Notas Técnicas

- **Vite/IPv6:** o dev server escuta em `[::1]` (IPv6). No navegador use `localhost` (resolve certo). `curl 127.0.0.1` falha — use `curl "http://[::1]:PORT"` ou `localhost`.

---

## Próxima Feature
`feature/web-users` — Tela de Usuários (Admin only), reaproveitando os componentes base desta feature.
