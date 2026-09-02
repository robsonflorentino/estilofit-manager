package br.com.estilofitudi.support

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Classe base para testes de integração.
 *
 * Sobe um PostgreSQL 15 real e efêmero via Testcontainers, garantindo que os testes
 * rodem contra o mesmo banco usado em produção (mesmas migrations Flyway, mesmos tipos).
 *
 * O container é estático e reutilizado entre as classes de teste (padrão singleton),
 * evitando o custo de subir um Postgres por classe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
abstract class IntegrationTest {

    companion object {
        @JvmStatic
        private val postgres = PostgreSQLContainer("postgres:15-alpine").apply {
            withDatabaseName("estilofit_manager_test")
            withUsername("test")
            withPassword("test")
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
