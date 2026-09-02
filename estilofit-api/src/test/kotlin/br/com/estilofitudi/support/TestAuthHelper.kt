package br.com.estilofitudi.support

import br.com.estilofitudi.shared.security.JwtService
import br.com.estilofitudi.user.domain.Role
import br.com.estilofitudi.user.domain.User
import br.com.estilofitudi.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.*

/**
 * Utilitário para os testes de integração: cria usuários de cada perfil
 * e gera tokens JWT válidos para uso no header Authorization.
 */
@Component
class TestAuthHelper(
    @Autowired private val userRepository: UserRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val jwtService: JwtService,
) {

    fun createUser(
        role: Role,
        email: String = "${role.name.lowercase()}-${UUID.randomUUID()}@estilofit.com.br",
        active: Boolean = true,
    ): User {
        val user = User(
            name = "Teste ${role.name}",
            email = email,
            passwordHash = passwordEncoder.encode("senha12345"),
            role = role,
            active = active,
        )
        return userRepository.save(user)
    }

    fun tokenFor(user: User): String =
        jwtService.generateAccessToken(user.id, user.email, user.role.name)

    fun bearerFor(role: Role): String {
        val user = createUser(role)
        return "Bearer ${tokenFor(user)}"
    }
}
