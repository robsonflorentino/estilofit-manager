package br.com.estilofitudi.auth.controller

import br.com.estilofitudi.auth.dto.LoginRequest
import br.com.estilofitudi.auth.dto.LoginResponse
import br.com.estilofitudi.auth.dto.RefreshResponse
import br.com.estilofitudi.auth.service.AuthService
import br.com.estilofitudi.shared.config.AppProperties
import br.com.estilofitudi.shared.exception.BusinessException
import br.com.estilofitudi.shared.security.JwtService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Login, logout e renovação de token")
class AuthController(
    private val authService: AuthService,
    private val jwtService: JwtService,
    private val appProperties: AppProperties,
) {

    companion object {
        const val REFRESH_TOKEN_COOKIE = "refreshToken"
    }

    @PostMapping("/login")
    @Operation(
        summary = "Login",
        description = "Autentica o usuário. Retorna accessToken no body e refreshToken como httpOnly cookie.",
    )
    fun login(
        @Valid @RequestBody request: LoginRequest,
        response: HttpServletResponse,
    ): ResponseEntity<LoginResponse> {
        val loginResponse = authService.login(request)

        // Gera o refresh token e o entrega como httpOnly cookie
        val user = loginResponse.user
        val refreshToken = jwtService.generateRefreshToken(user.id)
        setRefreshTokenCookie(response, refreshToken)

        return ResponseEntity.ok(loginResponse)
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Renovar token",
        description = "Renova o accessToken usando o refreshToken do httpOnly cookie.",
    )
    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<RefreshResponse> {
        val refreshToken = extractRefreshTokenFromCookie(request)
            ?: throw BusinessException("Refresh token não encontrado. Faça login novamente.")

        val refreshResponse = authService.refresh(refreshToken)

        // Rotaciona o refresh token — emite um novo cookie
        val userId = jwtService.extractUserId(refreshToken)
        val newRefreshToken = jwtService.generateRefreshToken(userId)
        setRefreshTokenCookie(response, newRefreshToken)

        return ResponseEntity.ok(refreshResponse)
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout",
        description = "Invalida o refreshToken apagando o cookie. O accessToken expira naturalmente.",
    )
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {
        clearRefreshTokenCookie(response)
        return ResponseEntity.noContent().build()
    }

    // ── Helpers de cookie ────────────────────────────────────────────────────

    private fun setRefreshTokenCookie(response: HttpServletResponse, token: String) {
        val cookie = Cookie(REFRESH_TOKEN_COOKIE, token).apply {
            isHttpOnly  = true
            secure      = true          // apenas HTTPS — em dev pode desabilitar se necessário
            path        = "/api/v1/auth"
            maxAge      = appProperties.jwt.refreshTokenExpiration.toInt()
            // SameSite via header pois a API Cookie do Servlet não suporta diretamente
        }
        response.addCookie(cookie)
        // Garante SameSite=Strict para proteção CSRF
        response.addHeader(
            "Set-Cookie",
            "${REFRESH_TOKEN_COOKIE}=$token; Path=/api/v1/auth; HttpOnly; Secure; SameSite=Strict; Max-Age=${appProperties.jwt.refreshTokenExpiration}",
        )
    }

    private fun clearRefreshTokenCookie(response: HttpServletResponse) {
        response.addHeader(
            "Set-Cookie",
            "$REFRESH_TOKEN_COOKIE=; Path=/api/v1/auth; HttpOnly; Secure; SameSite=Strict; Max-Age=0",
        )
    }

    private fun extractRefreshTokenFromCookie(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == REFRESH_TOKEN_COOKIE }
            ?.value
}
