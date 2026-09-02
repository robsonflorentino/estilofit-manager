package br.com.estilofitudi.shared.exception

/**
 * Exceção base para regras de negócio — mapeada para HTTP 422
 */
open class BusinessException(message: String) : RuntimeException(message)

/**
 * Recurso não encontrado — mapeado para HTTP 404
 */
class EntityNotFoundException(entity: String, id: Any)
    : RuntimeException("$entity com id '$id' não encontrado")

/**
 * Conflito de dados (duplicidade) — mapeado para HTTP 409
 */
class DataConflictException(message: String) : RuntimeException(message)

/**
 * Operação não permitida pelo estado atual do recurso — mapeado para HTTP 422
 */
class InvalidOperationException(message: String) : BusinessException(message)
