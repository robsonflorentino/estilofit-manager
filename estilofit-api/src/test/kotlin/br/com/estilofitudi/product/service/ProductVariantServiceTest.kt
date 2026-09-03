package br.com.estilofitudi.product.service

import br.com.estilofitudi.category.domain.Category
import br.com.estilofitudi.inventory.service.SettingsReader
import br.com.estilofitudi.product.domain.Product
import br.com.estilofitudi.product.domain.ProductVariant
import br.com.estilofitudi.product.dto.CreateVariantRequest
import br.com.estilofitudi.product.dto.UpdateVariantRequest
import br.com.estilofitudi.product.repository.ProductRepository
import br.com.estilofitudi.product.repository.ProductVariantRepository
import br.com.estilofitudi.shared.exception.BusinessException
import br.com.estilofitudi.shared.exception.DataConflictException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.*

class ProductVariantServiceTest {

    private val productRepository = mockk<ProductRepository>()
    private val variantRepository = mockk<ProductVariantRepository>()
    private val skuGenerator = mockk<SkuGenerator>()
    private val settingsReader = mockk<SettingsReader>()
    private val service = ProductVariantService(productRepository, variantRepository, skuGenerator, settingsReader)

    init {
        // Margem global padrão usada no cálculo do preço sugerido.
        every { settingsReader.defaultProfitMargin() } returns BigDecimal("100")
    }

    private fun product(active: Boolean = true): Product {
        val category = Category(name = "Blusas")
        return Product(name = "Blusa Listrada", category = category, active = active)
    }

    @Test
    fun `create gera SKU e salva variacao`() {
        val prod = product()
        every { productRepository.findById(prod.id) } returns Optional.of(prod)
        every {
            variantRepository.existsByProductIdAndSizeIgnoreCaseAndColorIgnoreCase(prod.id, "M", "Azul")
        } returns false
        every { skuGenerator.generate("Blusas", "M", "Azul") } returns "BLU-001-M-AZL"
        every { variantRepository.save(any()) } answers { firstArg() }

        val result = service.create(prod.id, CreateVariantRequest(size = "M", color = "Azul"))

        assertEquals("BLU-001-M-AZL", result.sku)
        assertEquals(0, result.stockQuantity)
        assertNull(result.salePrice)
    }

    @Test
    fun `create bloqueia variacao em produto inativo`() {
        val prod = product(active = false)
        every { productRepository.findById(prod.id) } returns Optional.of(prod)

        val ex = assertThrows<BusinessException> {
            service.create(prod.id, CreateVariantRequest(size = "M", color = "Azul"))
        }
        assertTrue(ex.message!!.contains("inativo"))
    }

    @Test
    fun `create lanca conflito quando tamanho+cor ja existe`() {
        val prod = product()
        every { productRepository.findById(prod.id) } returns Optional.of(prod)
        every {
            variantRepository.existsByProductIdAndSizeIgnoreCaseAndColorIgnoreCase(prod.id, "M", "Azul")
        } returns true

        assertThrows<DataConflictException> {
            service.create(prod.id, CreateVariantRequest(size = "M", color = "Azul"))
        }
    }

    @Test
    fun `create lanca not found quando produto nao existe`() {
        val id = UUID.randomUUID()
        every { productRepository.findById(id) } returns Optional.empty()

        assertThrows<EntityNotFoundException> {
            service.create(id, CreateVariantRequest(size = "M", color = "Azul"))
        }
    }

    @Test
    fun `update altera margem e preco mas nao o SKU`() {
        val prod = product()
        val variant = ProductVariant(
            product = prod, sku = "BLU-001-M-AZL", size = "M", color = "Azul",
        )
        every { variantRepository.findById(variant.id) } returns Optional.of(variant)
        every { variantRepository.save(any()) } answers { firstArg() }

        val result = service.update(
            prod.id, variant.id,
            UpdateVariantRequest(profitMargin = BigDecimal("120.00"), salePrice = BigDecimal("99.90")),
        )

        assertEquals("BLU-001-M-AZL", result.sku) // SKU inalterado
        assertEquals("M", result.size)
        assertEquals(BigDecimal("99.90"), result.salePrice)
    }

    @Test
    fun `update com preco manual preserva a margem desejada (nao deriva do preco)`() {
        val prod = product()
        val variant = ProductVariant(
            product = prod, sku = "BLU-001-M-AZL", size = "M", color = "Azul",
            profitMargin = BigDecimal("100.00"), // margem desejada configurada
            averageCost = BigDecimal("40.00"),
        )
        every { variantRepository.findById(variant.id) } returns Optional.of(variant)
        every { variantRepository.save(any()) } answers { firstArg() }

        // define preço manual 80 (que "equivaleria" a 100%); margem desejada NÃO muda
        val result = service.update(
            prod.id, variant.id,
            UpdateVariantRequest(salePrice = BigDecimal("80.00")),
        )

        assertEquals(0, BigDecimal("80.00").compareTo(result.salePrice))
        assertTrue(result.priceOverride)
        // a margem desejada permanece 100% (não é sobrescrita pela derivação do preço)
        assertEquals(0, BigDecimal("100.00").compareTo(result.profitMargin))
    }

    @Test
    fun `update com preco manual ativa priceOverride e expoe preco sugerido`() {
        val prod = product()
        val variant = ProductVariant(
            product = prod, sku = "BLU-001-M-AZL", size = "M", color = "Azul",
            averageCost = BigDecimal("40.00"), // sugerido pela margem global 100% = 80,00
        )
        every { variantRepository.findById(variant.id) } returns Optional.of(variant)
        every { variantRepository.save(any()) } answers { firstArg() }

        // vende ACIMA do sugerido (95 > 80)
        val result = service.update(
            prod.id, variant.id,
            UpdateVariantRequest(salePrice = BigDecimal("95.00")),
        )

        assertTrue(result.priceOverride)
        assertEquals(0, BigDecimal("95.00").compareTo(result.salePrice))
        // preço sugerido continua sendo exibido como referência (custo 40 * margem efetiva)
        assertNotNull(result.suggestedPrice)
    }

    @Test
    fun `update abaixo do custo e permitido (priceOverride) sem bloqueio`() {
        val prod = product()
        val variant = ProductVariant(
            product = prod, sku = "BLU-001-M-AZL", size = "M", color = "Azul",
            averageCost = BigDecimal("40.00"),
        )
        every { variantRepository.findById(variant.id) } returns Optional.of(variant)
        every { variantRepository.save(any()) } answers { firstArg() }

        // vende ABAIXO do custo (30 < 40) — caso raro, permitido
        val result = service.update(
            prod.id, variant.id,
            UpdateVariantRequest(salePrice = BigDecimal("30.00")),
        )

        assertTrue(result.priceOverride)
        assertEquals(0, BigDecimal("30.00").compareTo(result.salePrice))
    }

    @Test
    fun `update com resetToSuggested volta ao automatico e recalcula pela margem`() {
        val prod = product()
        val variant = ProductVariant(
            product = prod, sku = "BLU-001-M-AZL", size = "M", color = "Azul",
            profitMargin = BigDecimal("100.00"),
            salePrice = BigDecimal("95.00"),
            averageCost = BigDecimal("40.00"),
            priceOverride = true,
        )
        every { variantRepository.findById(variant.id) } returns Optional.of(variant)
        every { variantRepository.save(any()) } answers { firstArg() }

        val result = service.update(
            prod.id, variant.id,
            UpdateVariantRequest(resetToSuggested = true),
        )

        assertFalse(result.priceOverride)
        // custo 40 * (1 + 100/100) = 80,00
        assertEquals(0, BigDecimal("80.00").compareTo(result.salePrice))
    }

    @Test
    fun `update lanca not found quando variacao nao pertence ao produto`() {
        val prod = product()
        val outroProduto = product()
        val variant = ProductVariant(
            product = outroProduto, sku = "BLU-002-G-RSA", size = "G", color = "Rosa",
        )
        every { variantRepository.findById(variant.id) } returns Optional.of(variant)

        assertThrows<EntityNotFoundException> {
            service.update(prod.id, variant.id, UpdateVariantRequest(salePrice = BigDecimal("50")))
        }
    }
}
