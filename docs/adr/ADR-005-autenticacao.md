# ADR-005 — Autenticação e Autorização: JWT + RBAC

## Status
Aceito

## Data
2026-09-01

## Contexto
O sistema possui três perfis de acesso com permissões distintas (Administrador, Gestor, Vendedor). É necessário um mecanismo de autenticação seguro e stateless que funcione bem com a API REST, garantindo proteção tanto no backend quanto no frontend.

## Decisão
Utilizar **JWT (JSON Web Token)** para autenticação stateless, com **RBAC (Role-Based Access Control)** implementado em duas camadas independentes:

1. **Backend** — Spring Security como autoridade final e inegociável
2. **Frontend** — React Router Guards como controle de navegação e UX

---

## Perfis e Responsabilidades

### ADMIN (Administrador do Sistema)
Nível mais alto da hierarquia. Responsável pela operação e configuração técnica da plataforma. **Não é o dono da loja** — é quem mantém o sistema funcionando. Tem acesso a tudo para fins de suporte e administração.

- Gerencia usuários (criar, editar, ativar/desativar)
- Acessa configurações técnicas do sistema (margens padrão, thresholds, canais de venda, tamanhos)
- Acessa todos os módulos e relatórios de negócio (para suporte e auditoria)
- Pode operar qualquer funcionalidade em caso de necessidade

### MANAGER (Gestor — Proprietário da Loja)
É o dono/responsável pelo negócio. Tem acesso completo a tudo que envolve a operação comercial da loja. **Não gerencia usuários nem configurações técnicas do sistema** — isso é responsabilidade do Admin.

- Gerencia produtos, variações, fornecedores e estoque
- Registra entradas de mercadoria e lotes
- Registra e cancela vendas
- Gerencia contas a receber e dá baixa em parcelas
- Acessa **todos** os relatórios de negócio, incluindo pró-labore estimado
- Visualiza todos os alertas de promoção

### SELLER (Vendedor)
Operador de vendas do dia a dia. Acesso restrito ao essencial para registrar vendas e consultar o que precisa para operar.

- Registra vendas
- Consulta estoque disponível das variações
- Visualiza relatório das **suas próprias vendas** (filtrado pelo usuário logado)
- Não acessa: fornecedores, produtos, estoque gerencial, relatórios de negócio, pró-labore, contas a receber, usuários ou configurações

---

## Tabela de Permissões

| Funcionalidade                        | Admin | Gestor | Vendedor |
|---------------------------------------|:-----:|:------:|:--------:|
| **SISTEMA**                           |       |        |          |
| Gerenciar usuários                    |  ✅   |   ❌   |    ❌    |
| Configurações do sistema              |  ✅   |   ❌   |    ❌    |
| **NEGÓCIO — CADASTROS**               |       |        |          |
| Cadastrar produtos/variações          |  ✅   |   ✅   |    ❌    |
| Gerenciar fornecedores                |  ✅   |   ✅   |    ❌    |
| Configurar canais de venda            |  ✅   |   ✅   |    ❌    |
| Configurar margem de lucro padrão     |  ✅   |   ✅   |    ❌    |
| **NEGÓCIO — ESTOQUE**                 |       |        |          |
| Entrada de mercadoria (lotes)         |  ✅   |   ✅   |    ❌    |
| Ajuste manual de estoque              |  ✅   |   ✅   |    ❌    |
| Consultar estoque                     |  ✅   |   ✅   |    ✅    |
| **NEGÓCIO — VENDAS**                  |       |        |          |
| Registrar venda                       |  ✅   |   ✅   |    ✅    |
| Cancelar venda                        |  ✅   |   ✅   |    ❌    |
| **NEGÓCIO — FINANCEIRO**              |       |        |          |
| Contas a receber / dar baixa          |  ✅   |   ✅   |    ❌    |
| **NEGÓCIO — RELATÓRIOS**              |       |        |          |
| Relatório de estoque                  |  ✅   |   ✅   |    ❌    |
| Relatório de vendas (loja toda)       |  ✅   |   ✅   |    ❌    |
| Relatório de vendas (próprias)        |  ✅   |   ✅   |    ✅    |
| Relatório de produtos mais vendidos   |  ✅   |   ✅   |    ❌    |
| Relatório de margem de lucro          |  ✅   |   ✅   |    ❌    |
| Relatório de pró-labore estimado      |  ✅   |   ✅   |    ❌    |
| Fluxo de caixa projetado              |  ✅   |   ✅   |    ❌    |
| Alertas de promoção                   |  ✅   |   ✅   |    ❌    |

---

## Camada 1 — Backend: Spring Security (autoridade final)

### Princípio
O backend **nunca confia** no frontend. Todo endpoint é protegido independentemente do que o frontend exibe ou esconde. Um request sem token válido ou sem a role adequada recebe `401 Unauthorized` ou `403 Forbidden` — sem exceção.

### Fluxo de Autenticação
1. Usuário faz `POST /auth/login` com email e senha
2. API valida credenciais e retorna:
   - `accessToken` — JWT com validade de **8 horas**, contém `userId`, `email` e `role`
   - `refreshToken` — JWT com validade de **7 dias**, usado para renovar o access token
3. Chamadas subsequentes enviam o access token no header:
   ```
   Authorization: Bearer <accessToken>
   ```
4. Spring Security intercepta cada request, valida a assinatura do JWT e extrai a role
5. Anotações `@PreAuthorize` protegem cada endpoint com a role mínima requerida

### Proteção de Endpoints (exemplos)
```kotlin
@PreAuthorize("hasRole('ADMIN')")
fun getProLaboreReport(): ResponseEntity<ProLaboreReportDTO>

@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
fun createSupplyLot(): ResponseEntity<SupplyLotDTO>

@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SELLER')")
fun createSale(): ResponseEntity<SaleDTO>
```

### Respostas de Erro de Autenticação
| Situação                        | HTTP Status | Mensagem                          |
|---------------------------------|-------------|-----------------------------------|
| Token ausente                   | `401`       | `Authentication required`         |
| Token expirado                  | `401`       | `Token expired`                   |
| Token inválido/malformado       | `401`       | `Invalid token`                   |
| Role insuficiente               | `403`       | `Access denied`                   |
| Usuário desativado              | `401`       | `User account is disabled`        |

### Segurança do Token
- Assinado com **HS256** usando uma chave secreta configurada via variável de ambiente (`JWT_SECRET`)
- Senhas armazenadas com hash **BCrypt** (strength 12)
- Refresh token armazenado como **httpOnly cookie** no browser (não acessível por JavaScript)
- Access token armazenado em **memória** no frontend (não em localStorage)

---

## Camada 2 — Frontend: React Router Guards (controle de navegação)

### Princípio
O frontend protege rotas e esconde elementos de UI para evitar que o usuário tente acessar funcionalidades para as quais não tem permissão — melhorando a UX, não a segurança (que é garantida pelo backend).

### Fluxo de Acesso no Frontend
1. Toda rota da aplicação (exceto `/login`) exige usuário autenticado
2. Usuário não autenticado é redirecionado para `/login` automaticamente
3. Usuário autenticado sem a role necessária para uma rota vê uma página `403 - Acesso Negado`
4. Elementos de UI (botões, menus, abas) são ocultados se o usuário não tem a role necessária

### Componentes de Proteção
```tsx
// Rota que exige autenticação (qualquer role)
<PrivateRoute>
  <DashboardPage />
</PrivateRoute>

// Rota que exige role específica
<RoleRoute roles={['ADMIN']}>
  <ProLaboreReportPage />
</RoleRoute>

// Elemento de UI condicional por role
<RoleGuard roles={['ADMIN', 'MANAGER']}>
  <Button>Cancelar Venda</Button>
</RoleGuard>
```

### Regras de Redirecionamento
| Situação                              | Comportamento                        |
|---------------------------------------|--------------------------------------|
| Acesso a qualquer rota sem login      | Redireciona para `/login`            |
| Login bem-sucedido                    | Redireciona para `/dashboard`        |
| Acesso a rota sem role suficiente     | Exibe página `/403`                  |
| Token expirado durante uso            | Tenta renovar via refresh token; se falhar, redireciona para `/login` |
| Usuário desativado                    | Redireciona para `/login` com mensagem |

### Itens de Menu por Role
O menu lateral exibe apenas os módulos acessíveis ao perfil logado:
- **SELLER:** Dashboard, Vendas (apenas registrar), Estoque (somente consulta)
- **MANAGER:** todos exceto Usuários, Configurações e Pró-labore
- **ADMIN:** acesso total

---

## Camada 3 — Documentação da API: Springdoc OpenAPI (Swagger)

### Decisão
Utilizar **Springdoc OpenAPI 2.x** para gerar automaticamente a documentação interativa da API.

### Acesso
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

### Autenticação no Swagger
O Swagger UI é configurado para suportar autenticação via Bearer Token:
1. Clicar em "Authorize" no Swagger UI
2. Inserir o token obtido no endpoint `POST /auth/login`
3. Todos os requests subsequentes no Swagger enviarão o header `Authorization: Bearer <token>`

### Configuração de Segurança no Swagger
```kotlin
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
```

### Ambientes
| Ambiente      | Swagger UI disponível | Observação                              |
|---------------|----------------------|-----------------------------------------|
| Desenvolvimento | ✅ Sim             | Acesso livre para facilitar o desenvolvimento |
| Produção      | ❌ Desabilitado      | Swagger desativado em produção por segurança |

---

## Justificativa
- Duas camadas de controle: backend garante segurança real, frontend garante boa UX
- JWT stateless elimina necessidade de session storage no servidor
- Spring Security + `@PreAuthorize` é a abordagem mais robusta e testável no ecossistema Spring
- Springdoc OpenAPI integra nativamente com Spring Boot 3 e gera documentação sempre atualizada
- Swagger desabilitado em produção evita exposição desnecessária da superfície de ataque

## Consequências
- Access token em memória (não em localStorage) mitiga ataques XSS
- Refresh token em httpOnly cookie mitiga acesso por scripts maliciosos
- Renovação automática do token é transparente para o usuário
- Qualquer mudança de permissão só tem efeito após o token atual expirar (comportamento conhecido do JWT)
- Swagger UI disponível apenas em desenvolvimento — time de backend usa para testar endpoints durante o desenvolvimento
