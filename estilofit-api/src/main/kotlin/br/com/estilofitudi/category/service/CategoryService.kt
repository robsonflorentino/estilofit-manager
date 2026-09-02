package br.com.estilofitudi.category.service

import br.com.estilofitudi.category.domain.Category
import br.com.estilofitudi.category.dto.CategoryRequest
import br.com.estilofitudi.category.dto.CategoryResponse
import br.com.estilofitudi.category.dto.toResponse
import br.com.estilofitudi.category.repository.CategoryRepository
import br.com.estilofitudi.shared.exception.DataConflictException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class CategoryService(
    private val categoryRepository: CategoryRepository,
) {

    fun findAll(onlyActive: Boolean): List<CategoryResponse> {
        val categories = if (onlyActive) {
            categoryRepository.findAllByActiveTrueOrderByNameAsc()
        } else {
            categoryRepository.findAllByOrderByNameAsc()
        }
        return categories.map { it.toResponse() }
    }

    @Transactional
    fun create(request: CategoryRequest): CategoryResponse {
        val name = request.name.trim()
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw DataConflictException("Categoria '$name' já existe")
        }
        val category = Category(name = name)
        return categoryRepository.save(category).toResponse()
    }

    @Transactional
    fun rename(id: UUID, request: CategoryRequest): CategoryResponse {
        val category = categoryRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Categoria", id) }

        val name = request.name.trim()
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw DataConflictException("Categoria '$name' já existe")
        }

        category.name = name
        return categoryRepository.save(category).toResponse()
    }

    @Transactional
    fun updateStatus(id: UUID, active: Boolean): CategoryResponse {
        val category = categoryRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Categoria", id) }
        category.active = active
        return categoryRepository.save(category).toResponse()
    }
}
