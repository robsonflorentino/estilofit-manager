package br.com.estilofitudi.user.service

import br.com.estilofitudi.shared.dto.PageResponse
import br.com.estilofitudi.shared.exception.DataConflictException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import br.com.estilofitudi.shared.exception.BusinessException
import br.com.estilofitudi.user.domain.Role
import br.com.estilofitudi.user.domain.User
import br.com.estilofitudi.user.dto.*
import br.com.estilofitudi.user.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    fun findAll(
        name: String,
        role: Role?,
        active: Boolean?,
        pageable: Pageable,
    ): PageResponse<UserResponse> {
        val page = userRepository.findAllWithFilters(name, role, active, pageable)
        return PageResponse.from(page.map { it.toResponse() })
    }

    fun findById(id: UUID): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Usuário", id) }
        return user.toResponse()
    }

    fun findByEmail(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("Usuário", email) }
    }

    fun getMe(email: String): UserResponse = findByEmail(email).toResponse()

    @Transactional
    fun create(request: CreateUserRequest): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw DataConflictException("E-mail '${request.email}' já está cadastrado")
        }
        val user = User(
            name = request.name,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            role = request.role,
        )
        return userRepository.save(user).toResponse()
    }

    @Transactional
    fun update(id: UUID, request: UpdateUserRequest): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Usuário", id) }

        if (userRepository.existsByEmailAndIdNot(request.email, id)) {
            throw DataConflictException("E-mail '${request.email}' já está em uso por outro usuário")
        }

        user.name  = request.name
        user.email = request.email
        user.role  = request.role

        return userRepository.save(user).toResponse()
    }

    @Transactional
    fun updateStatus(id: UUID, active: Boolean): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Usuário", id) }
        user.active = active
        return userRepository.save(user).toResponse()
    }

    @Transactional
    fun resetPassword(id: UUID, newPassword: String) {
        val user = userRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Usuário", id) }
        user.passwordHash = passwordEncoder.encode(newPassword)
        userRepository.save(user)
    }

    @Transactional
    fun changeMyPassword(email: String, currentPassword: String, newPassword: String) {
        val user = findByEmail(email)
        if (!passwordEncoder.matches(currentPassword, user.passwordHash)) {
            throw BusinessException("Senha atual incorreta")
        }
        user.passwordHash = passwordEncoder.encode(newPassword)
        userRepository.save(user)
    }
}
