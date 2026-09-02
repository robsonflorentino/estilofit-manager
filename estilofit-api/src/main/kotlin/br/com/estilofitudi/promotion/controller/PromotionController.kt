package br.com.estilofitudi.promotion.controller

import br.com.estilofitudi.promotion.dto.StalePromotionResponse
import br.com.estilofitudi.promotion.service.PromotionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/promotions")
@Tag(name = "Alertas de Promoção", description = "Produtos parados (sem venda há X dias)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
class PromotionController(private val promotionService: PromotionService) {

    @GetMapping("/stale")
    @Operation(
        summary = "Produtos parados",
        description = "🟡 Admin + Gestor — variações ativas com estoque sem venda há mais de X dias " +
            "(X padrão vem de PROMOTION_ALERT_DAYS). Inclui as que nunca venderam.",
    )
    fun stale(
        @RequestParam(required = false) days: Int?,
    ): ResponseEntity<StalePromotionResponse> =
        ResponseEntity.ok(promotionService.stale(days))
}
