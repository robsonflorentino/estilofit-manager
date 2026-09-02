# ADR-009 — Estratégia de Tratamento de Erros

## Status
Aceito

## Data
2026-09-01

## Contexto
O sistema precisa de uma estratégia consistente para tratar e comunicar erros tanto no backend (API REST) quanto no frontend (React), garantindo que:
- Erros sejam centralizados e não espalhados pelo código
- O cliente sempre receba uma resposta com formato previsível
- Mensagens de erro sejam úteis para o usuário final, sem expor detalhes internos do sistema
- O frontend consiga tratar automaticamente os casos mais comuns (expiração de token, acesso negado, erro de servidor)

## Decisão
Adotar tratamento de erros centralizado em ambas as camadas, com formato de resposta padronizado.

---

## Camada 1 — Backend: GlobalExceptionHandler

### Mecanismo
Utilizar `@ControllerAdvice` com `@ExceptionHandler` para interceptar todas as exceções antes de chegarem ao cliente. Um único handler centraliza o mapeamento de exceção → resposta HTTP.

### Categorias de Erro e HTTP Status

| Categoria            | Exceção                          | HTTP Status | Quando ocorre                                  |
|----------------------|----------------------------------|:-----------:|------------------------------------------------|
| Validação de entrada | `MethodArgumentNotValidException`| `400`       | Campos obrigatórios ausentes, formato inválido |
| Recurso não encontrado| `EntityNotFoundException`        | `404`       | ID não existe no banco                         |
| Conflito de dados    | `DataConflictException`          | `409`       | Email duplicado, SKU duplicado, CNPJ já existe |
| Regra de negócio     | `BusinessException`              | `422`       | Estoque insuficiente, venda já cancelada, etc. |
| Não autenticado      | `AuthenticationException`        | `401`       | Token ausente, expirado ou inválido            |
| Sem permissão        | `AccessDeniedException`          | `403`       | Role insuficiente para a ação                  |
| Erro inesperado      | `Exception` (catch-all)          | `500`       | Qualquer exceção não mapeada                   |

### Exceções de Domínio (customizadas)
```kotlin
// Exceção base de negócio — sempre HTTP 422
open class BusinessException(message: String) : RuntimeException(message)

// Exceção para recursos não encontrados — sempre HTTP 404
class EntityNotFoundException(entity: String, id: Any)
    : RuntimeException("$entity com id '$id' não encontrado")

// Exceção para conflitos de dados — sempre HTTP 409
class DataConflictException(message: String) : RuntimeException(message)
```

### Formato Padrão de Resposta de Erro
Todo erro retorna o mesmo envelope JSON:

```json
{
  "timestamp": "2026-09-01T10:00:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Estoque insuficiente para BLS-001-M-AZL. Disponível: 2, solicitado: 5.",
  "path": "/api/v1/sales"
}
```

Erros de validação incluem adicionalmente o campo `fieldErrors`:
```json
{
  "timestamp": "2026-09-01T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/products",
  "fieldErrors": [
    { "field": "name",       "message": "Nome é obrigatório" },
    { "field": "categoryId", "message": "Categoria é obrigatória" }
  ]
}
```

Erros internos (`500`) **nunca** expõem stack trace ou detalhes técnicos:
```json
{
  "timestamp": "2026-09-01T10:00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Ocorreu um erro inesperado. Tente novamente ou contate o suporte.",
  "path": "/api/v1/sales"
}
```

### Implementação (GlobalExceptionHandler)
```kotlin
@ControllerAdvice
class GlobalExceptionHandler(private val request: HttpServletRequest) {

    // 400 — Validação de campos
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.map {
            FieldError(field = it.field, message = it.defaultMessage ?: "Inválido")
        }
        return ResponseEntity.badRequest().body(
            ErrorResponse(
                status = 400,
                error = "Bad Request",
                message = "Validation failed",
                path = request.requestURI,
                fieldErrors = fieldErrors
            )
        )
    }

    // 404 — Recurso não encontrado
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleNotFound(ex: EntityNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(404).body(ErrorResponse(404, "Not Found", ex.message!!, request.requestURI))

    // 409 — Conflito de dados
    @ExceptionHandler(DataConflictException::class)
    fun handleConflict(ex: DataConflictException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(409).body(ErrorResponse(409, "Conflict", ex.message!!, request.requestURI))

    // 422 — Regra de negócio violada
    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(422).body(ErrorResponse(422, "Unprocessable Entity", ex.message!!, request.requestURI))

    // 500 — Erro inesperado (catch-all)
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> {
        // Log completo internamente, mas resposta genérica para o cliente
        log.error("Erro inesperado em ${request.requestURI}", ex)
        return ResponseEntity.status(500).body(
            ErrorResponse(500, "Internal Server Error",
                "Ocorreu um erro inesperado. Tente novamente ou contate o suporte.",
                request.requestURI)
        )
    }
}
```

### Erros 401 e 403
Tratados diretamente pelo Spring Security via `AuthenticationEntryPoint` e `AccessDeniedHandler`, sem passar pelo `GlobalExceptionHandler`. Retornam o mesmo formato `ErrorResponse`.

### Logging
| Categoria   | Nível de Log | Detalhes logados                         |
|-------------|:------------:|------------------------------------------|
| `400`       | `WARN`       | Campos inválidos e valores recebidos     |
| `404`       | `INFO`       | Entidade e ID buscado                    |
| `409`       | `WARN`       | Dado conflitante                         |
| `422`       | `WARN`       | Mensagem da regra violada                |
| `401`/`403` | `WARN`       | Usuário, role e endpoint tentado         |
| `500`       | `ERROR`      | Stack trace completo para investigação   |

---

## Camada 2 — Frontend: Axios Interceptor Global

### Mecanismo
Configurar um interceptor de resposta no Axios que intercepta **todos** os erros antes de chegarem aos componentes React.

```typescript
// src/lib/api.ts
axios.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ErrorResponse>) => {
    const status = error.response?.status

    switch (status) {
      case 401:
        // Tenta renovar o token silenciosamente
        const renewed = await tryRefreshToken()
        if (renewed) {
          // Reexecuta a requisição original com o novo token
          return axios.request(error.config!)
        }
        // Refresh falhou — redireciona para login
        authStore.clear()
        router.navigate('/login', { state: { reason: 'session_expired' } })
        break

      case 403:
        // Acesso negado — exibe mensagem (não redireciona)
        toast.error('Você não tem permissão para realizar esta ação.')
        break

      case 500:
        // Erro de servidor — toast genérico
        toast.error('Erro interno do servidor. Tente novamente em instantes.')
        break
    }

    return Promise.reject(error)
  }
)
```

### Tratamento nos Componentes

Erros `400` e `422` **não** são tratados globalmente — cada formulário/componente é responsável por exibir os erros contextuais:

```typescript
// Uso em formulário
const onSubmit = async (data: SaleFormData) => {
  try {
    await createSale(data)
    toast.success('Venda registrada com sucesso!')
    navigate('/sales')
  } catch (error) {
    if (isAxiosError(error)) {
      if (error.response?.status === 400) {
        // Mapeia fieldErrors para os campos do React Hook Form
        error.response.data.fieldErrors?.forEach(({ field, message }) => {
          form.setError(field as keyof SaleFormData, { message })
        })
      } else if (error.response?.status === 422) {
        // Exibe a mensagem de negócio como toast de erro
        toast.error(error.response.data.message)
      }
    }
  }
}
```

### Resumo de Responsabilidades por Status

| Status | Tratado por         | Comportamento no Frontend                                    |
|--------|---------------------|--------------------------------------------------------------|
| `400`  | Componente          | Erros mapeados nos campos do formulário (React Hook Form)    |
| `401`  | Interceptor global  | Tenta refresh; se falhar, redireciona para `/login`          |
| `403`  | Interceptor global  | Toast "sem permissão" — sem redirecionamento                 |
| `404`  | Componente          | Exibe mensagem contextual ou página 404                      |
| `409`  | Componente          | Toast ou mensagem inline (ex: "email já cadastrado")         |
| `422`  | Componente          | Toast com a mensagem da regra de negócio violada             |
| `500`  | Interceptor global  | Toast genérico de erro de servidor                           |

### Componente de Toast
Utilizar **react-hot-toast** — leve, sem dependências, compatível com Tailwind.

```typescript
// Configuração global (main.tsx)
<Toaster
  position="top-right"
  toastOptions={{
    duration: 4000,
    style: {
      background: '#242424',
      color: '#E8E8E8',
      border: '1px solid #333333',
    },
    success: { iconTheme: { primary: '#22C55E', secondary: '#242424' } },
    error:   { iconTheme: { primary: '#EF4444', secondary: '#242424' } },
  }}
/>
```

---

## Justificativa
- `@ControllerAdvice` centraliza todo o tratamento no backend — sem try/catch nos controllers
- Exceções de domínio customizadas deixam o código de serviço expressivo: `throw BusinessException("Estoque insuficiente")`
- Interceptor Axios global evita repetição de tratamento de 401/403/500 em cada chamada
- Separação clara: erros globais (401, 403, 500) no interceptor; erros contextuais (400, 409, 422) nos componentes
- Erros 500 nunca expõem detalhes técnicos ao cliente, mas são logados completamente no servidor

## Consequências
- Todo erro de negócio deve lançar `BusinessException` — nunca retornar código HTTP manualmente nos services
- Mensagens de erro devem ser escritas em português, claras para o usuário final
- Stack traces de erros 500 ficam apenas nos logs do servidor — nunca na resposta da API
- O frontend depende do formato `ErrorResponse` definido no contrato da API — qualquer mudança deve ser coordenada
