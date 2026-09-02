package br.com.estilofitudi.report.controller

import br.com.estilofitudi.report.dto.ChannelProfitResponse
import br.com.estilofitudi.report.dto.DailyRevenueResponse
import br.com.estilofitudi.report.dto.PurchaseSuggestionReportResponse
import br.com.estilofitudi.report.dto.ReportSummaryResponse
import br.com.estilofitudi.report.dto.RevenueSliceResponse
import br.com.estilofitudi.report.dto.SalesTargetResponse
import br.com.estilofitudi.report.dto.SellerRankingResponse
import br.com.estilofitudi.report.dto.TopProductResponse
import br.com.estilofitudi.report.service.ReportService
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
@RequestMapping("/reports")
@Tag(name = "Relatórios", description = "Relatórios de vendas por período")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
class ReportController(private val reportService: ReportService) {

    @GetMapping("/summary")
    @Operation(summary = "Resumo do período", description = "🟡 Admin + Gestor")
    fun summary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): ResponseEntity<ReportSummaryResponse> =
        ResponseEntity.ok(reportService.summary(startDate, endDate))

    @GetMapping("/revenue-by-day")
    @Operation(summary = "Faturamento por dia", description = "🟡 Admin + Gestor")
    fun revenueByDay(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): ResponseEntity<List<DailyRevenueResponse>> =
        ResponseEntity.ok(reportService.revenueByDay(startDate, endDate))

    @GetMapping("/top-products")
    @Operation(summary = "Produtos mais vendidos", description = "🟡 Admin + Gestor")
    fun topProducts(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam(defaultValue = "10") limit: Int,
    ): ResponseEntity<List<TopProductResponse>> =
        ResponseEntity.ok(reportService.topProducts(startDate, endDate, limit))

    @GetMapping("/by-channel")
    @Operation(summary = "Vendas por canal", description = "🟡 Admin + Gestor")
    fun byChannel(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): ResponseEntity<List<RevenueSliceResponse>> =
        ResponseEntity.ok(reportService.byChannel(startDate, endDate))

    @GetMapping("/by-payment")
    @Operation(summary = "Vendas por forma de pagamento", description = "🟡 Admin + Gestor")
    fun byPayment(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): ResponseEntity<List<RevenueSliceResponse>> =
        ResponseEntity.ok(reportService.byPayment(startDate, endDate))

    @GetMapping("/sales-target")
    @Operation(
        summary = "Meta de vendas para o pró-labore",
        description = "🟡 Admin + Gestor — faturamento necessário por mês para o pró-labore desejado vs. realizado",
    )
    fun salesTarget(
        @RequestParam(defaultValue = "6") months: Int,
    ): ResponseEntity<SalesTargetResponse> =
        ResponseEntity.ok(reportService.salesTarget(months))

    @GetMapping("/profit-by-channel")
    @Operation(
        summary = "Lucratividade por canal",
        description = "🟡 Admin + Gestor — faturamento, custo, lucro e margem por canal no período",
    )
    fun profitByChannel(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): ResponseEntity<List<ChannelProfitResponse>> =
        ResponseEntity.ok(reportService.profitByChannel(startDate, endDate))

    @GetMapping("/seller-ranking")
    @Operation(
        summary = "Ranking de vendedores",
        description = "🟡 Admin + Gestor — vendedores ordenados por faturamento no período",
    )
    fun sellerRanking(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): ResponseEntity<List<SellerRankingResponse>> =
        ResponseEntity.ok(reportService.sellerRanking(startDate, endDate))

    @GetMapping("/purchase-suggestion")
    @Operation(
        summary = "Sugestão de compra do próximo lote",
        description = "🟡 Admin + Gestor — quantidade sugerida por variação com base nas vendas e no estoque",
    )
    fun purchaseSuggestion(
        @RequestParam(defaultValue = "30") days: Int,
    ): ResponseEntity<PurchaseSuggestionReportResponse> =
        ResponseEntity.ok(reportService.purchaseSuggestion(days))
}
