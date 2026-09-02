package br.com.estilofitudi.shared.config

import br.com.estilofitudi.user.domain.Role
import br.com.estilofitudi.user.domain.User
import br.com.estilofitudi.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * Garante que existe um usuário administrador padrão na primeira inicialização.
 * A senha é codificada com o PasswordEncoder da aplicação (BCrypt strength 12),
 * evitando hash hardcoded nas migrations.
 */
@Component
class DataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(DataInitializer::class.java)

    companion object {
        const val DEFAULT_ADMIN_EMAIL = "admin@estilofit.com.br"
        const val DEFAULT_ADMIN_PASSWORD = "admin@123"
    }

    override fun run(vararg args: String?) {
        if (userRepository.existsByEmail(DEFAULT_ADMIN_EMAIL)) {
            return
        }

        val admin = User(
            name = "Administrador",
            email = DEFAULT_ADMIN_EMAIL,
            passwordHash = passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD),
            role = Role.ADMIN,
            active = true,
        )
        userRepository.save(admin)

        log.warn(
            "Usuário administrador padrão criado: {} / {} — TROQUE A SENHA NO PRIMEIRO ACESSO!",
            DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD,
        )
    }
}
