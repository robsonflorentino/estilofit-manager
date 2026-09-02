package br.com.estilofitudi.user.repository

import br.com.estilofitudi.user.domain.Role
import br.com.estilofitudi.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, UUID> {

    fun findByEmail(email: String): Optional<User>

    fun existsByEmail(email: String): Boolean

    fun existsByEmailAndIdNot(email: String, id: UUID): Boolean

    @Query("""
        SELECT u FROM User u
        WHERE (:name = '' OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:role IS NULL OR u.role = :role)
          AND (:active IS NULL OR u.active = :active)
    """)
    fun findAllWithFilters(
        @Param("name") name: String,
        @Param("role") role: Role?,
        @Param("active") active: Boolean?,
        pageable: Pageable,
    ): Page<User>
}
