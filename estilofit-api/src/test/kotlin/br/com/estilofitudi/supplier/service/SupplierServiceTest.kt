package br.com.estilofitudi.supplier.service

import br.com.estilofitudi.shared.exception.DataConflictException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import br.com.estilofitudi.supplier.domain.Supplier
import br.com.estilofitudi.supplier.dto.SupplierRequest
import br.com.estilofitudi.supplier.repository.SupplierRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class SupplierServiceTest {

    @MockK
    lateinit var supplierRepository: SupplierRepository

    @InjectMockKs
    lateinit var service: SupplierService

    @Test
    fun `create salva fornecedor sem CNPJ`() {
        val slot = slot<Supplier>()
        every { supplierRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.create(SupplierRequest(name = "Moda Brasil"))

        assertEquals("Moda Brasil", result.name)
        assertTrue(result.active)
        assertNull(result.cnpj)
    }

    @Test
    fun `create valida CNPJ duplicado`() {
        every { supplierRepository.existsByCnpj("12.345.678/0001-99") } returns true

        assertThrows<DataConflictException> {
            service.create(SupplierRequest(name = "X", cnpj = "12.345.678/0001-99"))
        }
        verify(exactly = 0) { supplierRepository.save(any()) }
    }

    @Test
    fun `create normaliza campos vazios para null`() {
        val slot = slot<Supplier>()
        every { supplierRepository.existsByCnpj(any()) } returns false
        every { supplierRepository.save(capture(slot)) } answers { slot.captured }

        service.create(SupplierRequest(name = "  Fornecedor  ", contactPhone = "  ", cnpj = ""))

        assertEquals("Fornecedor", slot.captured.name)
        assertNull(slot.captured.contactPhone)
        assertNull(slot.captured.cnpj)
    }

    @Test
    fun `update lanca not found quando nao existe`() {
        val id = UUID.randomUUID()
        every { supplierRepository.findById(id) } returns Optional.empty()

        assertThrows<EntityNotFoundException> {
            service.update(id, SupplierRequest(name = "X"))
        }
    }

    @Test
    fun `update valida CNPJ duplicado de outro fornecedor`() {
        val id = UUID.randomUUID()
        every { supplierRepository.findById(id) } returns Optional.of(Supplier(name = "Atual"))
        every { supplierRepository.existsByCnpjAndIdNot("11.111.111/0001-11", id) } returns true

        assertThrows<DataConflictException> {
            service.update(id, SupplierRequest(name = "Atual", cnpj = "11.111.111/0001-11"))
        }
    }

    @Test
    fun `updateStatus altera active`() {
        val id = UUID.randomUUID()
        val supplier = Supplier(name = "Fornecedor", active = true)
        every { supplierRepository.findById(id) } returns Optional.of(supplier)
        every { supplierRepository.save(any()) } answers { firstArg() }

        val result = service.updateStatus(id, active = false)

        assertFalse(result.active)
    }
}
