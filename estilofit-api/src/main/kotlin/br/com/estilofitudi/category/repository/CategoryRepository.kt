package br.com.estilofitudi.category.repository

import br.com.estilofitudi.category.domain.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CategoryRepository : JpaRepository<Category, UUID> {

    fun findAllByActiveTrueOrderByNameAsc(): List<Category>

    fun findAllByOrderByNameAsc(): List<Category>

    fun existsByNameIgnoreCase(name: String): Boolean

    fun existsByNameIgnoreCaseAndIdNot(name: String, id: UUID): Boolean
}
