package br.com.estilofitudi.shared.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = extractToken(request)

        if (token != null && jwtService.validateToken(token)) {
            try {
                if (jwtService.isAccessToken(token)) {
                    val userId = jwtService.extractUserId(token)
                    val role   = jwtService.extractRole(token)
                    val email  = jwtService.extractEmail(token)

                    val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))
                    val authentication = UsernamePasswordAuthenticationToken(
                        /* principal   */ email,
                        /* credentials */ null,
                        /* authorities */ authorities,
                    ).also {
                        it.details = WebAuthenticationDetailsSource().buildDetails(request)
                        // Armazena o userId como detalhe para uso nos controllers
                        request.setAttribute("userId", userId)
                    }

                    SecurityContextHolder.getContext().authentication = authentication
                }
            } catch (ex: Exception) {
                log.warn("Erro ao processar token JWT: ${ex.message}")
                SecurityContextHolder.clearContext()
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        return header.removePrefix("Bearer ").trim()
    }
}
