package br.com.estilofitudi.shared.exception

import java.time.LocalDateTime

data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val fieldErrors: List<FieldErrorDetail>? = null,
)

data class FieldErrorDetail(
    val field: String,
    val message: String,
)
