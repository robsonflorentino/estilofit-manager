package br.com.estilofitudi.settings.dto

import br.com.estilofitudi.settings.domain.SettingType
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.LocalDateTime

data class SettingResponse(
    val key: String,
    val label: String,
    val value: String,
    val type: SettingType,
    val min: BigDecimal,
    val max: BigDecimal?,
    val description: String?,
    val updatedAt: LocalDateTime?,
    val updatedByName: String?,
)

data class UpdateSettingRequest(
    @field:NotBlank(message = "Valor é obrigatório")
    val value: String,
)
