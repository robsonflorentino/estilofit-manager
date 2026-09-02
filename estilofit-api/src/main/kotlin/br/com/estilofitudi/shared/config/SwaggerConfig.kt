package br.com.estilofitudi.shared.config

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token JWT obtido via POST /auth/login. Cole aqui sem o prefixo 'Bearer '.",
)
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("EstiloFit Manager API")
                .version("1.0.0")
                .description(
                    """
                    API REST do sistema de gestão de estoque e vendas da loja **EstiloFit — Moda Fitness**.
                    
                    ## Como autenticar
                    1. Execute `POST /auth/login` com email e senha
                    2. Copie o `accessToken` da resposta
                    3. Clique em **Authorize** (cadeado) e cole o token
                    
                    ## Perfis de Acesso
                    - 🔴 **ADMIN** — Administrador do sistema
                    - 🟡 **MANAGER** — Gestor / Proprietária da loja  
                    - 🟢 **SELLER** — Vendedor
                    """.trimIndent()
                )
                .contact(Contact().name("EstiloFit Manager"))
        )
        .addServersItem(Server().url("/api/v1").description("Servidor atual"))
}
