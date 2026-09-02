package br.com.estilofitudi.dashboard.controller

import br.com.estilofitudi.dashboard.dto.DashboardKpisResponse
import br.com.estilofitudi.dashboard.service.DashboardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Indicadores do painel")
@SecurityRequirement(name = "bearerAuth")
class DashboardController(private val dashboardService: DashboardService) {

    @GetMapping("/kpis")
    @Operation(
        summary = "KPIs do mês corrente",
        description = "🟢 Qualquer role — vendedor recebe apenas os próprios indicadores de venda",
    )
    fun kpis(authentication: Authentication): ResponseEntity<DashboardKpisResponse> =
        ResponseEntity.ok(dashboardService.kpis(authentication.name))
}
