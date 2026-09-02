package br.com.estilofitudi.auth.service

import br.com.estilofitudi.auth.dto.LoginRequest
import br.com.estilofitudi.shared.config.AppProperties
import br.com.estilofitudi.shared.exception.BusinessException
import br.com.estilofitudi.shared.security.JwtService
import br.com.estilofitudi.user.domain.Role
import br.com.estilofitudi.user.domain.User
import br.com.estilofitudi.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<org.springframework.security.crypto.password.PasswordEncoder>()
    private val jwtService = mockk<JwtService>()
    private val appProperties = AppProperties().apply {
        jwt.secret = "test-secret"
        jwt.accessTokenExpiration = 28800
    }

    private val authService = AuthService(userRepository, passwordEncoder, jwtService, appProperties)

    private fun sampleUser(active: Boolean = true) = User(
        name = "Ana",
        email = "ana@estilofit.com.br",
        passwordHash = "hash",
        role = Role.MANAGER,
        active = active,
    )

    @Test
    fun `login retorna token quando credenciais sao validas`() {
        val user = sampleUser()
        every { userRepository.findByEmail(user.email) } returns Optional.of(user)
        every { passwordEncoder.matches("senha12345", "hash") } returns true
        every { jwtService.generateAccessToken(user.id, user.email, "MANAGER") } returns "token-jwt"

        val response = authService.login(LoginRequest(user.email, "senha12345"))

        assertEquals("token-jwt", response.accessToken)
        assertEquals(28800, response.expiresIn)
        assertEquals(user.email, response.user.email)
    }

    @Test
    fun `login falha quando email nao existe`() {
        every { userRepository.findByEmail("naoexiste@estilofit.com.br") } returns Optional.empty()

        assertThrows<BusinessException> {
            authService.login(LoginRequest("naoexiste@estilofit.com.br", "qualquer"))
        }
    }

    @Test
    fun `login falha quando usuario esta desativado`() {
        val user = sampleUser(active = false)
        every { userRepository.findByEmail(user.email) } returns Optional.of(user)

        val ex = assertThrows<BusinessException> {
            authService.login(LoginRequest(user.email, "senha12345"))
        }
        assertTrue(ex.message!!.contains("desativado"))
    }

    @Test
    fun `login falha quando senha esta incorreta`() {
        val user = sampleUser()
        every { userRepository.findByEmail(user.email) } returns Optional.of(user)
        every { passwordEncoder.matches("errada", "hash") } returns false

        assertThrows<BusinessException> {
            authService.login(LoginRequest(user.email, "errada"))
        }
    }

    @Test
    fun `refresh falha quando token e invalido`() {
        every { jwtService.validateToken("token-ruim") } returns false

        assertThrows<BusinessException> { authService.refresh("token-ruim") }
    }

    @Test
    fun `refresh falha quando token nao e do tipo refresh`() {
        every { jwtService.validateToken("access-token") } returns true
        every { jwtService.isRefreshToken("access-token") } returns false

        assertThrows<BusinessException> { authService.refresh("access-token") }
    }

    @Test
    fun `refresh gera novo access token quando refresh e valido`() {
        val user = sampleUser()
        every { jwtService.validateToken("refresh-ok") } returns true
        every { jwtService.isRefreshToken("refresh-ok") } returns true
        every { jwtService.extractUserId("refresh-ok") } returns user.id
        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { jwtService.generateAccessToken(user.id, user.email, "MANAGER") } returns "novo-access"

        val response = authService.refresh("refresh-ok")

        assertEquals("novo-access", response.accessToken)
    }
}
