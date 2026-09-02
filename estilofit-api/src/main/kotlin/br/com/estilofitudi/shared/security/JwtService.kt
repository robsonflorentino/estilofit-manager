package br.com.estilofitudi.shared.security

import br.com.estilofitudi.shared.config.AppProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class JwtService(private val appProperties: AppProperties) {

    private val log = LoggerFactory.getLogger(JwtService::class.java)

    private val signingKey by lazy {
        Keys.hmacShaKeyFor(appProperties.jwt.secret.toByteArray())
    }

    fun generateAccessToken(userId: UUID, email: String, role: String): String =
        buildToken(
            subject = userId.toString(),
            claims = mapOf("email" to email, "role" to role, "type" to "ACCESS"),
            expirationSeconds = appProperties.jwt.accessTokenExpiration,
        )

    fun generateRefreshToken(userId: UUID): String =
        buildToken(
            subject = userId.toString(),
            claims = mapOf("type" to "REFRESH"),
            expirationSeconds = appProperties.jwt.refreshTokenExpiration,
        )

    fun validateToken(token: String): Boolean {
        return try {
            extractAllClaims(token)
            true
        } catch (ex: ExpiredJwtException) {
            log.warn("Token expirado")
            false
        } catch (ex: SignatureException) {
            log.warn("Assinatura JWT inválida")
            false
        } catch (ex: MalformedJwtException) {
            log.warn("Token JWT malformado")
            false
        } catch (ex: UnsupportedJwtException) {
            log.warn("Token JWT não suportado")
            false
        } catch (ex: IllegalArgumentException) {
            log.warn("Token JWT vazio ou nulo")
            false
        }
    }

    fun extractUserId(token: String): UUID =
        UUID.fromString(extractAllClaims(token).subject)

    fun extractEmail(token: String): String =
        extractAllClaims(token)["email"] as String

    fun extractRole(token: String): String =
        extractAllClaims(token)["role"] as String

    fun extractTokenType(token: String): String =
        extractAllClaims(token)["type"] as String

    fun isAccessToken(token: String): Boolean =
        extractTokenType(token) == "ACCESS"

    fun isRefreshToken(token: String): Boolean =
        extractTokenType(token) == "REFRESH"

    private fun buildToken(
        subject: String,
        claims: Map<String, Any>,
        expirationSeconds: Long,
    ): String {
        val now = Date()
        val expiration = Date(now.time + expirationSeconds * 1000)
        return Jwts.builder()
            .subject(subject)
            .claims(claims)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(signingKey)
            .compact()
    }

    private fun extractAllClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
