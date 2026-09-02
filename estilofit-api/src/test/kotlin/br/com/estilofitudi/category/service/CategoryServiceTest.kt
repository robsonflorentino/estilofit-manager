package br.com.estilofitudi.category.service

import br.com.estilofitudi.category.domain.Category
import br.com.estilofitudi.category.dto.CategoryRequest
import br.com.estilofitudi.category.repository.CategoryRepository
import br.com.estilofitudi.shared.exception.DataConflictException
import br.com.estilofitudi.shared.exception.EntityNotFoundException
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class CategoryServiceTest {

    @MockK
    lateinit var categoryRepository: CategoryRepository

    @InjectMockKs
    lateinit var categoryService: CategoryService

    @Test
    fun `findAll com onlyActive true retorna apenas categorias ativas`() {
        every { categoryRepository.findAllByActiveTrueOrderByNameAsc() } returns
            listOf(Category(name = "Blusas"), Category(name = "Calças"))

        val result = categoryService.findAll(onlyActive = true)

        assertEquals(2, result.size)
        assertEquals("Blusas", result[0].name)
        verify(exactly = 1) { categoryRepository.findAllByActiveTrueOrderByNameAsc() }
    }

    @Test
    fun `findAll com onlyActive false retorna todas as categorias`() {
        every { categoryRepository.findAllByOrderByNameAsc() } returns
            listOf(Category(name = "Blusas"), Category(name = "Inativa", active = false))

        val result = categoryService.findAll(onlyActive = false)

        assertEquals(2, result.size)
        verify(exactly = 1) { categoryRepository.findAllByOrderByNameAsc() }
    }

    @Test
    fun `create salva categoria quando nome nao existe`() {
        val slot = slot<Category>()
        every { categoryRepository.existsByNameIgnoreCase("Macacões") } returns false
        every { categoryRepository.save(capture(slot)) } answers { slot.captured }

        val result = categoryService.create(CategoryRequest(name = "Macacões"))

        assertEquals("Macacões", result.name)
        assertTrue(result.active)
        verify { categoryRepository.save(any()) }
    }

    @Test
    fun `create faz trim do nome antes de salvar`() {
        val slot = slot<Category>()
        every { categoryRepository.existsByNameIgnoreCase("Macacões") } returns false
        every { categoryRepository.save(capture(slot)) } answers { slot.captured }

        categoryService.create(CategoryRequest(name = "  Macacões  "))

        assertEquals("Macacões", slot.captured.name)
    }

    @Test
    fun `create lanca conflito quando nome ja existe`() {
        every { categoryRepository.existsByNameIgnoreCase("Blusas") } returns true

        val ex = assertThrows<DataConflictException> {
            categoryService.create(CategoryRequest(name = "Blusas"))
        }
        assertTrue(ex.message!!.contains("já existe"))
        verify(exactly = 0) { categoryRepository.save(any()) }
    }

    @Test
    fun `rename lanca not found quando categoria nao existe`() {
        val id = UUID.randomUUID()
        every { categoryRepository.findById(id) } returns Optional.empty()

        assertThrows<EntityNotFoundException> {
            categoryService.rename(id, CategoryRequest(name = "Novo Nome"))
        }
    }

    @Test
    fun `rename lanca conflito quando novo nome ja existe em outra categoria`() {
        val id = UUID.randomUUID()
        every { categoryRepository.findById(id) } returns Optional.of(Category(name = "Blusas"))
        every { categoryRepository.existsByNameIgnoreCaseAndIdNot("Calças", id) } returns true

        assertThrows<DataConflictException> {
            categoryService.rename(id, CategoryRequest(name = "Calças"))
        }
    }

    @Test
    fun `updateStatus altera o campo active`() {
        val id = UUID.randomUUID()
        val category = Category(name = "Blusas", active = true)
        every { categoryRepository.findById(id) } returns Optional.of(category)
        every { categoryRepository.save(any()) } answers { firstArg() }

        val result = categoryService.updateStatus(id, active = false)

        assertFalse(result.active)
    }
}
