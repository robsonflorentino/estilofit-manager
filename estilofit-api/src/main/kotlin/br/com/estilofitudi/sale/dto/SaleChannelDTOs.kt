package br.com.estilofitudi.sale.dto

import br.com.estilofitudi.sale.domain.SaleChannel
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.*

data class CreateSaleChannelRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    val name: String,
)

data class UpdateSaleChannelStatusRequest(
    val active: Boolean,
)

data class SaleChannelResponse(
    val id: UUID,
    val name: String,
    val active: Boolean,
)

fun SaleChannel.toResponse() = SaleChannelResponse(id = id, name = name, active = active)
