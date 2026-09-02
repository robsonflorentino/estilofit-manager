package br.com.estilofitudi.shared.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app")
class AppProperties {

    val jwt: JwtProperties = JwtProperties()
    val cors: CorsProperties = CorsProperties()
    val swagger: SwaggerProperties = SwaggerProperties()

    class JwtProperties {
        var secret: String = ""
        var accessTokenExpiration: Long = 28800
        var refreshTokenExpiration: Long = 604800
    }

    class CorsProperties {
        var allowedOrigins: String = "http://localhost:5173"
    }

    class SwaggerProperties {
        var enabled: Boolean = true
    }
}
