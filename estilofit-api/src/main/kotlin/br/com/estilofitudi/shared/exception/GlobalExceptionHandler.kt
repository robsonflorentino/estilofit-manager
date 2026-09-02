package br.com.estilofitudi.shared.exception

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // 400 — Validação de campos
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.map {
            FieldErrorDetail(field = it.field, message = it.defaultMessage ?: "Valor inválido")
        }
        log.warn("Validação falhou em ${request.requestURI}: $fieldErrors")
        return ResponseEntity.badRequest().body(
            ErrorResponse(
                status = 400,
                error = "Bad Request",
                message = "Validação dos dados falhou",
                path = request.requestURI,
                fieldErrors = fieldErrors,
            )
        )
    }

    // 404 — Recurso não encontrado
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleNotFound(
        ex: EntityNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.info("Recurso não encontrado em ${request.requestURI}: ${ex.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                status = 404,
                error = "Not Found",
                message = ex.message ?: "Recurso não encontrado",
                path = request.requestURI,
            )
        )
    }

    // 409 — Conflito de dados
    @ExceptionHandler(DataConflictException::class)
    fun handleConflict(
        ex: DataConflictException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.warn("Conflito de dados em ${request.requestURI}: ${ex.message}")
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(
                status = 409,
                error = "Conflict",
                message = ex.message ?: "Conflito de dados",
                path = request.requestURI,
            )
        )
    }

    // 422 — Regra de negócio violada
    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(
        ex: BusinessException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.warn("Regra de negócio violada em ${request.requestURI}: ${ex.message}")
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
            ErrorResponse(
                status = 422,
                error = "Unprocessable Entity",
                message = ex.message ?: "Operação não permitida",
                path = request.requestURI,
            )
        )
    }

    // 401 — Não autenticado (Spring Security)
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(
        ex: AuthenticationException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.warn("Falha de autenticação em ${request.requestURI}: ${ex.message}")
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse(
                status = 401,
                error = "Unauthorized",
                message = "Autenticação necessária",
                path = request.requestURI,
            )
        )
    }

    // 403 — Sem permissão (Spring Security)
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.warn("Acesso negado em ${request.requestURI}")
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse(
                status = 403,
                error = "Forbidden",
                message = "Você não tem permissão para realizar esta ação",
                path = request.requestURI,
            )
        )
    }

    // 500 — Erro inesperado (catch-all)
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.error("Erro inesperado em ${request.requestURI}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                status = 500,
                error = "Internal Server Error",
                message = "Ocorreu um erro inesperado. Tente novamente ou contate o suporte.",
                path = request.requestURI,
            )
        )
    }
}
