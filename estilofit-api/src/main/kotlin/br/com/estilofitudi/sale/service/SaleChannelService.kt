package br.com.estilofitudi.sale.service

import br.com.estilofitudi.sale.domain.SaleChannel
import br.com.estilofitudi.sale.dto.CreateSaleChannelRequest
import br.com.estilofitudi.sale.dto.SaleChannelResponse
import br.com.estilofitudi.sale.dto.UpdateSaleChannelStatusRequest
import br.com.estilofitudi.sale.dto.toResponse
import br.com.estilofitudi.sale.repository.SaleChannelRepository
import br.com.estilofitudi.shared.exception.BusinessException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class SaleChannelService(
    private val channelRepository: SaleChannelRepository,
) {

    /** Lista canais. Por padrão só os ativos (para o formulário de venda); todos quando includeInactive=true. */
    fun findAll(includeInactive: Boolean): List<SaleChannelResponse> {
        val channels = if (includeInactive) {
            channelRepository.findAllByOrderByNameAsc()
        } else {
            channelRepository.findAllByActiveTrueOrderByNameAsc()
        }
        return channels.map { it.toResponse() }
    }

    @Transactional
    fun create(request: CreateSaleChannelRequest): SaleChannelResponse {
        val name = request.name.trim()
        if (channelRepository.existsByNameIgnoreCase(name)) {
            throw BusinessException("Já existe um canal de venda com o nome '$name'.")
        }
        val channel = SaleChannel(name = name)
        return channelRepository.save(channel).toResponse()
    }

    @Transactional
    fun updateStatus(id: UUID, request: UpdateSaleChannelStatusRequest): SaleChannelResponse {
        val channel = channelRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Canal de venda", id) }
        channel.active = request.active
        return channelRepository.save(channel).toResponse()
    }
}
