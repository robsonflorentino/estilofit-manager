package br.com.estilofitudi.product.service

import br.com.estilofitudi.product.repository.ProductVariantRepository
import org.springframework.stereotype.Component
import java.text.Normalizer

/**
 * Gera SKUs de variação no formato: {PREFIXO_CATEGORIA}-{SEQUENCIAL}-{TAMANHO}-{COR}
 * Ex: BLU-001-M-AZL
 *
 * Regras (ver docs/features/003-products-variants-design.md):
 * - PREFIXO: 3 primeiras letras da categoria, maiúsculas, sem acento
 * - SEQUENCIAL: contador por prefixo, 3 dígitos com zero à esquerda
 * - TAMANHO: como informado, maiúsculo
 * - COR: 3 primeiras letras da cor, maiúsculas, sem acento
 * - Em caso de colisão do SKU final, adiciona sufixo numérico (-2, -3, ...)
 */
@Component
class SkuGenerator(private val variantRepository: ProductVariantRepository) {

    fun generate(categoryName: String, size: String, color: String): String {
        val prefix = abbreviate(categoryName, 3)
        val sizePart = normalize(size).uppercase()
        val colorPart = abbreviate(color, 3)

        // Sequencial baseado na quantidade de variações já existentes com esse prefixo
        val nextSeq = variantRepository.countBySkuPrefix("$prefix-") + 1
        val seqPart = nextSeq.toString().padStart(3, '0')

        val base = "$prefix-$seqPart-$sizePart-$colorPart"

        // Rede de segurança contra colisão do SKU final
        if (!variantRepository.existsBySku(base)) return base

        var suffix = 2
        while (variantRepository.existsBySku("$base-$suffix")) {
            suffix++
        }
        return "$base-$suffix"
    }

    /** Remove acentos, mantém apenas letras e pega as N primeiras, em maiúsculas. */
    private fun abbreviate(text: String, length: Int): String {
        val cleaned = normalize(text).filter { it.isLetter() }.uppercase()
        return cleaned.take(length).ifEmpty { "XXX".take(length) }
    }

    /** Remove diacríticos (acentos): "Ação" -> "Acao". */
    private fun normalize(text: String): String =
        Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}
