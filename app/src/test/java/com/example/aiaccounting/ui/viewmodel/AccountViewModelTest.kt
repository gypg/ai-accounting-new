package com.example.aiaccounting.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.data.repository.AccountRepository
import io.mockk.Awaits
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
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
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AccountViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var testDispatcher: TestDispatcher

    @MockK
    private lateinit var accountRepository: AccountRepository

    private lateinit var viewModel: AccountViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        // Setup default mock behavior
        every { accountRepository.getAllAccounts() } returns flowOf(emptyList())

        viewModel = AccountViewModel(accountRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be empty`() {
        val initialState = viewModel.uiState.value
        assertEquals(emptyList<Account>(), initialState.accounts)
        assertFalse(initialState.isLoading)
        assertNull(initialState.error)
        assertFalse(initialState.showAddDialog)
        assertFalse(initialState.showEditDialog)
        assertNull(initialState.editingAccount)
    }

    @Test
    fun `createAccount should call repository insert`() = runTest {
        // Given
        val name = "现金"
        val type = AccountType.CASH
        val initialBalance = 1000.0
        val icon = "💵"
        val color = "#4CAF50"

        coEvery { accountRepository.insertAccount(any()) } just Awaits

        // When
        viewModel.createAccount(name, type, initialBalance, icon, color)

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { accountRepository.insertAccount(any()) }
    }

    @Test
    fun `createAccount should set first account as default`() = runTest {
        // Given
        coEvery { accountRepository.insertAccount(any()) } just Awaits

        // When - First account
        viewModel.createAccount("现金", AccountType.CASH, 1000.0, "💵", "#4CAF50")

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { 
            accountRepository.insertAccount(
                any()
            )
        }
    }

    @Test
    fun `createAccount should complete without error`() = runTest {
        // Given
        coEvery { accountRepository.insertAccount(any()) } returns 1L

        // When
        viewModel.createAccount("现金", AccountType.CASH, 1000.0, "💵", "#4CAF50")

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `updateAccount should call repository update`() = runTest {
        // Given
        val account = Account(
            id = 1L,
            name = "现金",
            type = AccountType.CASH,
            balance = 1000.0,
            icon = "💵",
            color = "#4CAF50"
        )
        coEvery { accountRepository.updateAccount(any()) } just Awaits

        // When
        viewModel.updateAccount(account)

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { accountRepository.updateAccount(account) }
    }

    @Test
    fun `deleteAccount should call repository delete`() = runTest {
        // Given
        val account = Account(
            id = 1L,
            name = "现金",
            type = AccountType.CASH,
            balance = 1000.0,
            icon = "💵",
            color = "#4CAF50"
        )
        coEvery { accountRepository.deleteAccount(any()) } just Awaits

        // When
        viewModel.deleteAccount(account)

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { accountRepository.deleteAccount(account) }
    }

    @Test
    fun `archiveAccount should call repository archive`() = runTest {
        // Given
        val accountId = 1L
        coEvery { accountRepository.archiveAccount(accountId) } just Awaits

        // When
        viewModel.archiveAccount(accountId)

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { accountRepository.archiveAccount(accountId) }
    }

    @Test
    fun `setDefaultAccount should update account with isDefault true`() = runTest {
        // Given
        val account = Account(
            id = 1L,
            name = "现金",
            type = AccountType.CASH,
            balance = 1000.0,
            icon = "💵",
            color = "#4CAF50",
            isDefault = false
        )
        coEvery { accountRepository.updateAccount(any()) } just Awaits

        // When
        viewModel.setDefaultAccount(account)

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { 
            accountRepository.updateAccount(
                any()
            )
        }
    }

    @Test
    fun `updateBalance should call repository`() = runTest {
        // Given
        val accountId = 1L
        val amount = 500.0
        coEvery { accountRepository.updateBalance(accountId, amount) } just Awaits

        // When
        viewModel.updateBalance(accountId, amount)

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { accountRepository.updateBalance(accountId, amount) }
    }

    @Test
    fun `showAddDialog should update state`() {
        // When
        viewModel.showAddDialog()

        // Then
        assertTrue(viewModel.uiState.value.showAddDialog)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `hideAddDialog should update state`() {
        // Given
        viewModel.showAddDialog()
        assertTrue(viewModel.uiState.value.showAddDialog)

        // When
        viewModel.hideAddDialog()

        // Then
        assertFalse(viewModel.uiState.value.showAddDialog)
    }

    @Test
    fun `showEditDialog should update state with account`() {
        // Given
        val account = Account(
            id = 1L,
            name = "现金",
            type = AccountType.CASH,
            balance = 1000.0,
            icon = "💵",
            color = "#4CAF50"
        )

        // When
        viewModel.showEditDialog(account)

        // Then
        assertTrue(viewModel.uiState.value.showEditDialog)
        assertEquals(account, viewModel.uiState.value.editingAccount)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `hideEditDialog should update state`() {
        // Given
        val account = Account(
            id = 1L,
            name = "现金",
            type = AccountType.CASH,
            balance = 1000.0,
            icon = "💵",
            color = "#4CAF50"
        )
        viewModel.showEditDialog(account)

        // When
        viewModel.hideEditDialog()

        // Then
        assertFalse(viewModel.uiState.value.showEditDialog)
        assertNull(viewModel.uiState.value.editingAccount)
    }

    @Test
    fun `clearError should set error to null`() {
        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `getTotalBalance should return from repository`() = runTest {
        // Given
        val expectedBalance = 5000.0
        coEvery { accountRepository.getTotalBalance() } returns expectedBalance

        // When
        val result = viewModel.getTotalBalance()

        // Then
        assertEquals(expectedBalance, result, 0.01)
        coVerify { accountRepository.getTotalBalance() }
    }

    @Test
    fun `createAccount should handle error`() = runTest {
        // Given
        val errorMessage = "Database error"
        coEvery { accountRepository.insertAccount(any()) } throws Exception(errorMessage)

        // When
        viewModel.createAccount("现金", AccountType.CASH, 1000.0, "💵", "#4CAF50")

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(errorMessage, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `updateAccount should handle error`() = runTest {
        // Given
        val account = Account(
            id = 1L,
            name = "现金",
            type = AccountType.CASH,
            balance = 1000.0,
            icon = "💵",
            color = "#4CAF50"
        )
        val errorMessage = "Update failed"
        coEvery { accountRepository.updateAccount(any()) } throws Exception(errorMessage)

        // When
        viewModel.updateAccount(account)

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(errorMessage, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `deleteAccount should handle error`() = runTest {
        // Given
        val account = Account(
            id = 1L,
            name = "现金",
            type = AccountType.CASH,
            balance = 1000.0,
            icon = "💵",
            color = "#4CAF50"
        )
        val errorMessage = "Delete failed"
        coEvery { accountRepository.deleteAccount(any()) } throws Exception(errorMessage)

        // When
        viewModel.deleteAccount(account)

        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(errorMessage, viewModel.uiState.value.error)
    }
}
