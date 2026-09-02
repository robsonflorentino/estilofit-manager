package br.com.estilofitudi.auth.service

import br.com.estilofitudi.auth.dto.LoginRequest
import br.com.estilofitudi.auth.dto.LoginResponse
import br.com.estilofitudi.auth.dto.RefreshResponse
import br.com.estilofitudi.shared.config.AppProperties
import br.com.estilofitudi.shared.exception.BusinessException
import br.com.estilofitudi.shared.security.JwtService
import br.com.estilofitudi.user.dto.toResponse
import br.com.estilofitudi.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val appProperties: AppProperties,
) {

    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { BusinessException("E-mail ou senha inválidos") }

        if (!user.active) {
            throw BusinessException("Usuário desativado. Entre em contato com o administrador.")
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw BusinessException("E-mail ou senha inválidos")
        }

        val accessToken = jwtService.generateAccessToken(
            userId = user.id,
            email  = user.email,
            role   = user.role.name,
        )

        return LoginResponse(
            accessToken = accessToken,
            expiresIn   = appProperties.jwt.accessTokenExpiration,
            user        = user.toResponse(),
        )
    }

    fun refresh(refreshToken: String): RefreshResponse {
        if (!jwtService.validateToken(refreshToken)) {
            throw BusinessException("Refresh token inválido ou expirado. Faça login novamente.")
        }

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw BusinessException("Token inválido")
        }

        val userId = jwtService.extractUserId(refreshToken)

        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException("Usuário não encontrado") }

        if (!user.active) {
            throw BusinessException("Usuário desativado.")
        }

        val newAccessToken = jwtService.generateAccessToken(
            userId = user.id,
            email  = user.email,
            role   = user.role.name,
        )

        return RefreshResponse(
            accessToken = newAccessToken,
            expiresIn   = appProperties.jwt.accessTokenExpiration,
        )
    }
}
