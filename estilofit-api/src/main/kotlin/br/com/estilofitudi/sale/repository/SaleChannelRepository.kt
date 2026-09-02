package br.com.estilofitudi.sale.repository

import br.com.estilofitudi.sale.domain.SaleChannel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SaleChannelRepository : JpaRepository<SaleChannel, UUID> {
    fun findAllByActiveTrueOrderByNameAsc(): List<SaleChannel>
    fun findAllByOrderByNameAsc(): List<SaleChannel>
    fun existsByNameIgnoreCase(name: String): Boolean
}
