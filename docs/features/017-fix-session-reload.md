# Fix — Sessão perdida ao recarregar a página

| Campo         | Valor                                       |
|---------------|---------------------------------------------|
| Branch        | `feature/api-seller-ranking` (commit dedicado) |
| Data          | 2026-09-02                                  |
| Tipo          | Correção (auth)                             |
| Status        | ✅ Corrigido e verificado                   |

---

## Sintoma
Ao recarregar (F5) qualquer página autenticada, o usuário caía para a tela de login.

## Causa raiz
1. O access token vive **em memória** (por segurança) e é perdido no reload; **nada restaurava a sessão** no boot do app — o guard de rota via `isAuthenticated = false` e redirecionava para `/login`.
2. Agravante em dev: o cookie do refresh token era emitido com `Secure` (apenas HTTPS). Em `http://localhost`, o navegador não armazenava/enviava o cookie, então nem o refresh manual funcionaria.

## Correção

### Backend
- `AppProperties.cookie.secure` — atributo `Secure` do cookie agora é configurável (`COOKIE_SECURE`, default `true`). Produção mantém `true` (HTTPS); dev sobe com `COOKIE_SECURE=false`.
- `AuthController` — cookies de refresh (set/clear) usam a flag; `Secure` só é adicionado quando habilitado.
- `RefreshResponse` + `AuthService.refresh` — passaram a incluir o `user`, permitindo reidratar a sessão completa em uma única chamada.

### Frontend
- `authService.refresh()` — chama `POST /auth/refresh`.
- `authStore` — novo estado `isInitializing` e ação `restoreSession()` (tenta refresh no boot; sucesso restaura user + token, falha segue como não autenticado).
- `App.tsx` — dispara `restoreSession()` no mount e exibe um loader enquanto `isInitializing`, evitando o flash para `/login`.

## Verificação
- Suíte de testes: 140, 0 falhas (mudança de auth não quebrou nada; `AuthServiceTest` de refresh segue verde).
- Manual (API, dev com `COOKIE_SECURE=false`): login grava o cookie `refreshToken` **sem** `Secure`; `POST /auth/refresh` usando o cookie retorna 200 com novo `accessToken` e o `user`.

## Nota de operação
- **Dev**: subir a API com `COOKIE_SECURE=false` (HTTP). 
- **Produção**: manter `COOKIE_SECURE=true` (padrão) sob HTTPS.
