package br.com.estilofitudi.product.service

import br.com.estilofitudi.category.repository.CategoryRepository
import br.com.estilofitudi.product.domain.Product
import br.com.estilofitudi.product.dto.*
import br.com.estilofitudi.product.repository.ProductRepository
import br.com.estilofitudi.shared.dto.PageResponse
import br.com.estilofitudi.shared.exception.BusinessException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
) {

    fun findAll(
        name: String,
        categoryId: UUID?,
        active: Boolean?,
        pageable: Pageable,
    ): PageResponse<ProductSummaryResponse> {
        val page = productRepository.findAllWithFilters(name, categoryId, active, pageable)
        return PageResponse.from(page.map { it.toSummaryResponse() })
    }

    fun findById(id: UUID): ProductDetailResponse {
        val product = productRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Produto", id) }
        return product.toDetailResponse()
    }

    @Transactional
    fun create(request: CreateProductRequest): ProductDetailResponse {
        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { EntityNotFoundException("Categoria", request.categoryId) }

        // Regra: não permitir produto em categoria inativa (decisão 4 do tech design)
        if (!category.active) {
            throw BusinessException("Não é possível cadastrar produto em uma categoria inativa.")
        }

        val product = Product(
            name = request.name.trim(),
            description = request.description?.trim(),
            category = category,
        )
        return productRepository.save(product).toDetailResponse()
    }

    @Transactional
    fun update(id: UUID, request: CreateProductRequest): ProductDetailResponse {
        val product = productRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Produto", id) }

        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { EntityNotFoundException("Categoria", request.categoryId) }

        if (!category.active) {
            throw BusinessException("Não é possível vincular o produto a uma categoria inativa.")
        }

        product.name = request.name.trim()
        product.description = request.description?.trim()
        product.category = category

        return productRepository.save(product).toDetailResponse()
    }

    @Transactional
    fun updateStatus(id: UUID, active: Boolean): ProductDetailResponse {
        val product = productRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Produto", id) }
        product.active = active
        return productRepository.save(product).toDetailResponse()
    }
}
