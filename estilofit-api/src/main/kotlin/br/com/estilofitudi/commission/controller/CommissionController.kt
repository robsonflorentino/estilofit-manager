package br.com.estilofitudi.commission.controller

import br.com.estilofitudi.commission.dto.CommissionReportResponse
import br.com.estilofitudi.commission.service.CommissionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/commissions")
@Tag(name = "Comissões", description = "Comissões a pagar por vendedor")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
class CommissionController(private val commissionService: CommissionService) {

    @GetMapping
    @Operation(
        summary = "Comissões a pagar",
        description = "🟡 Admin + Gestor — comissão por vendedor no período (snapshot por venda)",
    )
    fun report(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): ResponseEntity<CommissionReportResponse> =
        ResponseEntity.ok(commissionService.report(startDate, endDate))
}
