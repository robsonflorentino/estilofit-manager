package br.com.estilofitudi.user.service

import br.com.estilofitudi.shared.exception.BusinessException
import br.com.estilofitudi.shared.exception.DataConflictException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import br.com.estilofitudi.user.domain.Role
import br.com.estilofitudi.user.domain.User
import br.com.estilofitudi.user.dto.CreateUserRequest
import br.com.estilofitudi.user.dto.UpdateUserRequest
import br.com.estilofitudi.user.repository.UserRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@ExtendWith(MockKExtension::class)
class UserServiceTest {

    @MockK
    lateinit var userRepository: UserRepository

    @MockK
    lateinit var passwordEncoder: PasswordEncoder

    @InjectMockKs
    lateinit var userService: UserService

    private fun sampleUser(
        email: String = "user@estilofit.com.br",
        role: Role = Role.SELLER,
        hash: String = "hashed",
    ) = User(name = "Fulano", email = email, passwordHash = hash, role = role)

    @Test
    fun `create codifica a senha e salva`() {
        val request = CreateUserRequest("Carlos", "carlos@estilofit.com.br", "senha12345", Role.SELLER)
        every { userRepository.existsByEmail(request.email) } returns false
        every { passwordEncoder.encode("senha12345") } returns "hash-bcrypt"
        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.create(request)

        assertEquals("carlos@estilofit.com.br", result.email)
        assertEquals(Role.SELLER, result.role)
        verify { passwordEncoder.encode("senha12345") }
        verify { userRepository.save(any()) }
    }

    @Test
    fun `create lanca conflito quando email ja existe`() {
        val request = CreateUserRequest("Carlos", "existe@estilofit.com.br", "senha12345", Role.SELLER)
        every { userRepository.existsByEmail(request.email) } returns true

        assertThrows<DataConflictException> { userService.create(request) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `findById lanca not found quando nao existe`() {
        val id = UUID.randomUUID()
        every { userRepository.findById(id) } returns Optional.empty()

        assertThrows<EntityNotFoundException> { userService.findById(id) }
    }

    @Test
    fun `update lanca conflito quando email pertence a outro usuario`() {
        val id = UUID.randomUUID()
        every { userRepository.findById(id) } returns Optional.of(sampleUser())
        every { userRepository.existsByEmailAndIdNot("novo@estilofit.com.br", id) } returns true

        val request = UpdateUserRequest("Novo Nome", "novo@estilofit.com.br", Role.MANAGER)
        assertThrows<DataConflictException> { userService.update(id, request) }
    }

    @Test
    fun `changeMyPassword lanca erro quando senha atual esta incorreta`() {
        val user = sampleUser(hash = "hash-atual")
        every { userRepository.findByEmail(user.email) } returns Optional.of(user)
        every { passwordEncoder.matches("errada", "hash-atual") } returns false

        val ex = assertThrows<BusinessException> {
            userService.changeMyPassword(user.email, "errada", "novaSenha123")
        }
        assertTrue(ex.message!!.contains("incorreta"))
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `changeMyPassword atualiza o hash quando senha atual esta correta`() {
        val user = sampleUser(hash = "hash-atual")
        every { userRepository.findByEmail(user.email) } returns Optional.of(user)
        every { passwordEncoder.matches("atual123", "hash-atual") } returns true
        every { passwordEncoder.encode("novaSenha123") } returns "novo-hash"
        every { userRepository.save(any()) } answers { firstArg() }

        userService.changeMyPassword(user.email, "atual123", "novaSenha123")

        assertEquals("novo-hash", user.passwordHash)
        verify { userRepository.save(user) }
    }

    @Test
    fun `updateStatus altera active do usuario`() {
        val id = UUID.randomUUID()
        val user = sampleUser().apply { active = true }
        every { userRepository.findById(id) } returns Optional.of(user)
        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.updateStatus(id, active = false)

        assertFalse(result.active)
    }
}
