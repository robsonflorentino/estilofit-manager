package br.com.estilofitudi.shared.security

import br.com.estilofitudi.shared.exception.ErrorResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.time.LocalDateTime

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {

    companion object {
        val PUBLIC_ENDPOINTS = arrayOf(
            "/auth/login",
            "/auth/refresh",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
        )
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(*PUBLIC_ENDPOINTS).permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint { request, response, _ ->
                        writeJsonError(response, HttpStatus.UNAUTHORIZED,
                            "Autenticação necessária. Faça login para continuar.",
                            request.requestURI)
                    }
                    .accessDeniedHandler { request, response, _ ->
                        writeJsonError(response, HttpStatus.FORBIDDEN,
                            "Você não tem permissão para realizar esta ação.",
                            request.requestURI)
                    }
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    private fun writeJsonError(
        response: jakarta.servlet.http.HttpServletResponse,
        status: HttpStatus,
        message: String,
        path: String,
    ) {
        val error = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = path,
        )
        val mapper = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(mapper.writeValueAsString(error))
    }
}
