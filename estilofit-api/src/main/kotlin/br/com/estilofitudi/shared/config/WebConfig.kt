package br.com.estilofitudi.shared.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(private val appProperties: AppProperties) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        val origins = appProperties.cors.allowedOrigins
            .split(",")
            .map { it.trim() }
            .toTypedArray()

        registry.addMapping("/**")
            .allowedOrigins(*origins)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }
}
