package br.com.estilofitudi.supplier.service

import br.com.estilofitudi.shared.dto.PageResponse
import br.com.estilofitudi.shared.exception.DataConflictException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import br.com.estilofitudi.supplier.domain.Supplier
import br.com.estilofitudi.supplier.dto.SupplierRequest
import br.com.estilofitudi.supplier.dto.SupplierResponse
import br.com.estilofitudi.supplier.dto.toResponse
import br.com.estilofitudi.supplier.repository.SupplierRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class SupplierService(
    private val supplierRepository: SupplierRepository,
) {

    fun findAll(name: String, active: Boolean?, pageable: Pageable): PageResponse<SupplierResponse> {
        val page = supplierRepository.findAllWithFilters(name, active, pageable)
        return PageResponse.from(page.map { it.toResponse() })
    }

    fun findById(id: UUID): SupplierResponse {
        val supplier = supplierRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Fornecedor", id) }
        return supplier.toResponse()
    }

    @Transactional
    fun create(request: SupplierRequest): SupplierResponse {
        val cnpj = request.cnpj?.trim()?.ifBlank { null }
        if (cnpj != null && supplierRepository.existsByCnpj(cnpj)) {
            throw DataConflictException("CNPJ '$cnpj' já está cadastrado")
        }
        val supplier = Supplier(
            name = request.name.trim(),
            contactPhone = request.contactPhone?.trim()?.ifBlank { null },
            contactEmail = request.contactEmail?.trim()?.ifBlank { null },
            whatsapp = request.whatsapp?.trim()?.ifBlank { null },
            cnpj = cnpj,
            address = request.address?.trim()?.ifBlank { null },
            notes = request.notes?.trim()?.ifBlank { null },
        )
        return supplierRepository.save(supplier).toResponse()
    }

    @Transactional
    fun update(id: UUID, request: SupplierRequest): SupplierResponse {
        val supplier = supplierRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Fornecedor", id) }

        val cnpj = request.cnpj?.trim()?.ifBlank { null }
        if (cnpj != null && supplierRepository.existsByCnpjAndIdNot(cnpj, id)) {
            throw DataConflictException("CNPJ '$cnpj' já está cadastrado por outro fornecedor")
        }

        supplier.name = request.name.trim()
        supplier.contactPhone = request.contactPhone?.trim()?.ifBlank { null }
        supplier.contactEmail = request.contactEmail?.trim()?.ifBlank { null }
        supplier.whatsapp = request.whatsapp?.trim()?.ifBlank { null }
        supplier.cnpj = cnpj
        supplier.address = request.address?.trim()?.ifBlank { null }
        supplier.notes = request.notes?.trim()?.ifBlank { null }

        return supplierRepository.save(supplier).toResponse()
    }

    @Transactional
    fun updateStatus(id: UUID, active: Boolean): SupplierResponse {
        val supplier = supplierRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Fornecedor", id) }
        supplier.active = active
        return supplierRepository.save(supplier).toResponse()
    }
}
