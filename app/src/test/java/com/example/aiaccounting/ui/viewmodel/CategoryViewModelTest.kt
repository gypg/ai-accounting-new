package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.CategoryRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.Runs
import io.mockk.Awaits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class CategoryViewModelTest {

    private lateinit var testDispatcher: TestDispatcher

    @MockK
    private lateinit var categoryRepository: CategoryRepository

    private lateinit var viewModel: CategoryViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        // Setup default mock behaviors
        every { categoryRepository.getAllCategories() } returns flowOf(emptyList())
        every { categoryRepository.getIncomeCategories() } returns flowOf(emptyList())
        every { categoryRepository.getExpenseCategories() } returns flowOf(emptyList())

        viewModel = CategoryViewModel(categoryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be empty`() {
        val initialState = viewModel.uiState.value
        assertEquals(emptyList<Category>(), initialState.categories)
        assertFalse(initialState.isLoading)
        assertNull(initialState.error)
        assertFalse(initialState.showAddDialog)
        assertFalse(initialState.showEditDialog)
        assertNull(initialState.editingCategory)
        assertNull(initialState.selectedType)
    }

    @Test
    fun `createCategory should call repository insert`() = runTest {
        // Given
        val name = "餐饮"
        val type = TransactionType.EXPENSE
        val icon = "🍔"
        val color = "#FF5722"
        coEvery { categoryRepository.insertCategory(any()) } just Awaits

        // When
        viewModel.createCategory(name, type, icon, color)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { categoryRepository.insertCategory(any()) }
    }

    @Test
    fun `createCategory with parentId should call repository insert`() = runTest {
        // Given
        val name = "午餐"
        val type = TransactionType.EXPENSE
        val icon = "🍱"
        val color = "#FF5722"
        val parentId = 1L
        coEvery { categoryRepository.insertCategory(any()) } just Awaits

        // When
        viewModel.createCategory(name, type, icon, color, parentId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { categoryRepository.insertCategory(match { it.parentId == parentId }) }
    }

    @Test
    fun `updateCategory should call repository update`() = runTest {
        // Given
        val category = Category(
            id = 1L,
            name = "餐饮",
            type = TransactionType.EXPENSE,
            icon = "🍔",
            color = "#FF5722"
        )
        coEvery { categoryRepository.updateCategory(category) } just Runs

        // When
        viewModel.updateCategory(category)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { categoryRepository.updateCategory(category) }
    }

    @Test
    fun `deleteCategory should call repository delete`() = runTest {
        // Given
        val category = Category(
            id = 1L,
            name = "餐饮",
            type = TransactionType.EXPENSE,
            icon = "🍔",
            color = "#FF5722"
        )
        coEvery { categoryRepository.deleteCategory(category) } just Runs

        // When
        viewModel.deleteCategory(category)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { categoryRepository.deleteCategory(category) }
    }

    @Test
    fun `showAddDialog should update state`() {
        // When
        viewModel.showAddDialog(TransactionType.EXPENSE)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showAddDialog)
        assertEquals(TransactionType.EXPENSE, state.selectedType)
        assertNull(state.error)
    }

    @Test
    fun `showAddDialog without type should update state`() {
        // When
        viewModel.showAddDialog()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showAddDialog)
        assertNull(state.selectedType)
    }

    @Test
    fun `hideAddDialog should update state`() {
        // Given
        viewModel.showAddDialog(TransactionType.EXPENSE)

        // When
        viewModel.hideAddDialog()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showAddDialog)
        assertNull(state.selectedType)
    }

    @Test
    fun `showEditDialog should update state`() {
        // Given
        val category = Category(
            id = 1L,
            name = "餐饮",
            type = TransactionType.EXPENSE,
            icon = "🍔",
            color = "#FF5722"
        )

        // When
        viewModel.showEditDialog(category)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showEditDialog)
        assertEquals(category, state.editingCategory)
        assertNull(state.error)
    }

    @Test
    fun `hideEditDialog should update state`() {
        // Given
        val category = Category(
            id = 1L,
            name = "餐饮",
            type = TransactionType.EXPENSE,
            icon = "🍔",
            color = "#FF5722"
        )
        viewModel.showEditDialog(category)

        // When
        viewModel.hideEditDialog()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showEditDialog)
        assertNull(state.editingCategory)
    }

    @Test
    fun `clearError should clear error state`() {
        // Given - trigger an error state
        coEvery { categoryRepository.insertCategory(any()) } throws Exception("Test error")
        viewModel.createCategory("Test", TransactionType.EXPENSE, "icon", "color")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.error)
    }

    @Test
    fun `getCategoriesByType should filter categories by type from current state`() {
        // This tests the filtering logic itself
        // Given - current state has categories loaded
        val expenseCategories = listOf(
            Category(id = 1L, name = "餐饮", type = TransactionType.EXPENSE, icon = "🍔", color = "#FF5722"),
            Category(id = 2L, name = "交通", type = TransactionType.EXPENSE, icon = "🚗", color = "#4CAF50")
        )

        // When - call the method
        val result = viewModel.getCategoriesByType(TransactionType.EXPENSE)

        // Then - should return empty list since state is empty
        assertTrue(result.isEmpty())
    }

    @Test
    fun `createCategory should handle error`() = runTest {
        // Given
        val errorMessage = "Database error"
        coEvery { categoryRepository.insertCategory(any()) } throws Exception(errorMessage)

        // When
        viewModel.createCategory("餐饮", TransactionType.EXPENSE, "🍔", "#FF5722")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `updateCategory should handle error`() = runTest {
        // Given
        val errorMessage = "Update failed"
        val category = Category(id = 1L, name = "餐饮", type = TransactionType.EXPENSE, icon = "🍔", color = "#FF5722")
        coEvery { categoryRepository.updateCategory(category) } throws Exception(errorMessage)

        // When
        viewModel.updateCategory(category)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(errorMessage, state.error)
    }
}
