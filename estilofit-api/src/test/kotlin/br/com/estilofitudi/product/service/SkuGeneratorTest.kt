package br.com.estilofitudi.product.service

import br.com.estilofitudi.product.repository.ProductVariantRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SkuGeneratorTest {

    private val variantRepository = mockk<ProductVariantRepository>()
    private val skuGenerator = SkuGenerator(variantRepository)

    @Test
    fun `gera SKU no formato PREFIXO-SEQ-TAMANHO-COR`() {
        every { variantRepository.countBySkuPrefix("BLU-") } returns 0
        every { variantRepository.existsBySku(any()) } returns false

        // "Azul" -> 3 primeiras letras -> AZU
        val sku = skuGenerator.generate("Blusas", "M", "Azul")

        assertEquals("BLU-001-M-AZU", sku)
    }

    @Test
    fun `sequencial incrementa com base na contagem existente`() {
        every { variantRepository.countBySkuPrefix("BLU-") } returns 4
        every { variantRepository.existsBySku(any()) } returns false

        val sku = skuGenerator.generate("Blusas", "G", "Rosa")

        assertEquals("BLU-005-G-ROS", sku)
    }

    @Test
    fun `remove acentos da categoria e da cor`() {
        every { variantRepository.countBySkuPrefix("CAL-") } returns 0
        every { variantRepository.existsBySku(any()) } returns false

        // "Calças" -> CAL, "Salmão" -> SAL
        val sku = skuGenerator.generate("Calças", "P", "Salmão")

        assertEquals("CAL-001-P-SAL", sku)
    }

    @Test
    fun `tamanho é normalizado para maiusculas`() {
        every { variantRepository.countBySkuPrefix("TOP-") } returns 0
        every { variantRepository.existsBySku(any()) } returns false

        val sku = skuGenerator.generate("Tops", "gg", "Preto")

        assertEquals("TOP-001-GG-PRE", sku)
    }

    @Test
    fun `adiciona sufixo quando o SKU base ja existe`() {
        every { variantRepository.countBySkuPrefix("BLU-") } returns 0
        // base colide (BLU-001-M-AZU), base-2 livre
        every { variantRepository.existsBySku("BLU-001-M-AZU") } returns true
        every { variantRepository.existsBySku("BLU-001-M-AZU-2") } returns false

        val sku = skuGenerator.generate("Blusas", "M", "Azul")

        assertEquals("BLU-001-M-AZU-2", sku)
    }

    @Test
    fun `prefixo com categoria de menos de 3 letras usa o que houver`() {
        every { variantRepository.countBySkuPrefix(any()) } returns 0
        every { variantRepository.existsBySku(any()) } returns false

        val sku = skuGenerator.generate("Kit", "U", "Verde")

        assertEquals("KIT-001-U-VER", sku)
    }
}
