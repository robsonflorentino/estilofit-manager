# Feature 006 — Tela de Usuários (Frontend)

| Campo         | Valor                                  |
|---------------|----------------------------------------|
| Branch        | `feature/web-users`                    |
| Data          | 2026-09-01                             |
| Projeto       | `estilofit-web`                        |
| Status        | ✅ Concluída e verificada              |
| Depende de    | Feature 001 (API de usuários), Feature 004 (scaffold), Feature 005 (componentes base) |

---

## Objetivo

Trazer o módulo de Usuários (backend pronto) para o frontend, com listagem paginada, filtros e gestão completa. Exclusivo para o perfil Administrador.

---

## Escopo Entregue

### Componente novo
- `Pagination` — navegação de páginas reutilizável ("Mostrando X–Y de Z", anterior/próxima)

### Módulo de Usuários
- `types/user.ts` — `CreateUserRequest`, `UpdateUserRequest`, `UserFilters`
- `services/userService.ts` — list (paginado + filtros), create, update, updateStatus, resetPassword
- `pages/UsersPage.tsx` — tela completa

---

## Funcionalidades da Tela

| Recurso                    | Comportamento                                                    |
|----------------------------|------------------------------------------------------------------|
| Listagem paginada          | 10 por página, com o componente Pagination                       |
| Filtro por nome            | Busca parcial (reseta para a página 0)                           |
| Filtro por perfil          | Admin / Gestor / Vendedor / Todos                                |
| Filtro por status          | Ativos / Inativos / Todos                                        |
| Novo usuário               | Modal: nome, e-mail, senha, perfil                               |
| Editar usuário             | Modal: nome, e-mail, perfil (não altera senha)                   |
| Redefinir senha            | Modal dedicado com nova senha                                    |
| Ativar / Desativar         | Diálogo de confirmação (desativar impede login)                  |
| E-mail duplicado/em uso    | Erro exibido no campo do formulário (409)                        |
| Feedback                   | Toasts de sucesso/erro; validação com zod                        |

---

## Controle de Acesso

- Rota `/users` protegida por `RoleRoute roles={["ADMIN"]}`
- Item "Usuários" no menu visível apenas para Admin (grupo Administração)
- Gestor e Vendedor não veem o menu nem acessam a rota (caem em `/403`)

---

## Colunas da Tabela

| Coluna | Conteúdo                              |
|--------|---------------------------------------|
| Nome   | Nome do usuário                       |
| E-mail | E-mail (login)                        |
| Perfil | Badge roxo com o rótulo do perfil     |
| Status | Badge verde (Ativo) / vermelho (Inativo) |
| Ações  | Editar, Redefinir senha, Ativar/Desativar |

---

## Como Testar

Com backend + banco rodando e frontend em `npm run dev`, logado como admin:

1. Menu lateral → **Usuários**
2. Veja a lista paginada (começa com o admin)
3. Teste: criar usuário, filtrar por perfil/status/nome, editar, redefinir senha, desativar
4. Tente criar com e-mail já existente → erro no campo

---

## Verificação Realizada

| Cenário                                   | Status |
|-------------------------------------------|:------:|
| Typecheck TypeScript (0 erros)            | ✅     |
| API `/users` paginada responde (envelope) | ✅     |
| UsersPage transforma sem erro (Vite)      | ✅     |

> Verificação técnica automatizada. O teste visual interativo fica a cargo do usuário no navegador.

---

## Próximos Passos

Com Categorias e Usuários no frontend, o par de telas planejado está completo.
Próximo foco: **backend de Produtos + Variações** (branch `feature/api-products-variants` com tech design pendente das 5 decisões), e depois a tela de Produtos no frontend.
