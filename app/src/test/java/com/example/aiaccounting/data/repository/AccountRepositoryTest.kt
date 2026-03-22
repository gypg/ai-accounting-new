package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.AccountDao
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.AccountType
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AccountRepositoryTest {

    @MockK
    private lateinit var accountDao: AccountDao

    @MockK
    private lateinit var transactionRepository: TransactionRepository

    private lateinit var repository: AccountRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = AccountRepository(accountDao, transactionRepository)
    }

    @Test
    fun `getAllAccounts should return flow from dao`() = runTest {
        // Given
        val accounts = listOf(
            Account(id = 1L, name = "Cash", type = AccountType.CASH, balance = 1000.0)
        )
        every { accountDao.getAllAccounts() } returns flowOf(accounts)

        // When
        val result = repository.getAllAccounts().first()

        // Then
        assertEquals(accounts, result)
        verify { accountDao.getAllAccounts() }
    }

    @Test
    fun `getAllAccountsList should return list from dao`() = runTest {
        // Given
        val accounts = listOf(
            Account(id = 1L, name = "Cash", type = AccountType.CASH, balance = 1000.0)
        )
        every { accountDao.getAllAccounts() } returns flowOf(accounts)

        // When
        val result = repository.getAllAccountsList()

        // Then
        assertEquals(accounts, result)
    }

    @Test
    fun `getAccountById should return account from dao`() = runTest {
        // Given
        val accountId = 1L
        val expectedAccount = Account(id = accountId, name = "Cash", type = AccountType.CASH, balance = 1000.0)
        coEvery { accountDao.getAccountById(accountId) } returns expectedAccount

        // When
        val result = repository.getAccountById(accountId)

        // Then
        assertEquals(expectedAccount, result)
        coVerify { accountDao.getAccountById(accountId) }
    }

    @Test
    fun `getAccountById should return null when not found`() = runTest {
        // Given
        val accountId = 999L
        coEvery { accountDao.getAccountById(accountId) } returns null

        // When
        val result = repository.getAccountById(accountId)

        // Then
        assertNull(result)
    }

    @Test
    fun `findAccountByName should find account with exact match`() = runTest {
        // Given
        val accounts = listOf(
            Account(id = 1L, name = "Cash", type = AccountType.CASH, balance = 1000.0),
            Account(id = 2L, name = "Bank", type = AccountType.BANK, balance = 5000.0)
        )
        every { accountDao.getAllAccounts() } returns flowOf(accounts)

        // When
        val result = repository.findAccountByName("Cash")

        // Then
        assertNotNull(result)
        assertEquals("Cash", result?.name)
    }

    @Test
    fun `findAccountByName should find account with case insensitive match`() = runTest {
        // Given
        val accounts = listOf(
            Account(id = 1L, name = "Cash", type = AccountType.CASH, balance = 1000.0)
        )
        every { accountDao.getAllAccounts() } returns flowOf(accounts)

        // When
        val result = repository.findAccountByName("CASH")

        // Then
        assertNotNull(result)
        assertEquals("Cash", result?.name)
    }

    @Test
    fun `findAccountByName should return null when no match`() = runTest {
        // Given
        val accounts = listOf(
            Account(id = 1L, name = "Cash", type = AccountType.CASH, balance = 1000.0)
        )
        every { accountDao.getAllAccounts() } returns flowOf(accounts)

        // When
        val result = repository.findAccountByName("NonExistent")

        // Then
        assertNull(result)
    }

    @Test
    fun `insertAccount should return id from dao`() = runTest {
        // Given
        val account = Account(name = "New Account", type = AccountType.CASH, balance = 0.0)
        val expectedId = 1L
        coEvery { accountDao.insertAccount(account) } returns expectedId

        // When
        val result = repository.insertAccount(account)

        // Then
        assertEquals(expectedId, result)
        coVerify { accountDao.insertAccount(account) }
    }

    @Test
    fun `updateAccount should call dao`() = runTest {
        // Given
        val account = Account(id = 1L, name = "Updated", type = AccountType.CASH, balance = 1000.0)
        coEvery { accountDao.updateAccount(account) } just Runs

        // When
        repository.updateAccount(account)

        // Then
        coVerify { accountDao.updateAccount(account) }
    }

    @Test
    fun `deleteAccount should delete transactions first then account`() = runTest {
        // Given
        val account = Account(id = 1L, name = "Cash", type = AccountType.CASH, balance = 1000.0)
        coEvery { transactionRepository.deleteTransactionsByAccount(account.id) } just Runs
        coEvery { accountDao.deleteAccount(account) } just Runs

        // When
        repository.deleteAccount(account)

        // Then
        coVerify { transactionRepository.deleteTransactionsByAccount(account.id) }
        coVerify { accountDao.deleteAccount(account) }
    }

    @Test
    fun `deleteAccountById should delete transactions first then account`() = runTest {
        // Given
        val accountId = 1L
        coEvery { transactionRepository.deleteTransactionsByAccount(accountId) } just Runs
        coEvery { accountDao.deleteAccountById(accountId) } just Runs

        // When
        repository.deleteAccountById(accountId)

        // Then
        coVerify { transactionRepository.deleteTransactionsByAccount(accountId) }
        coVerify { accountDao.deleteAccountById(accountId) }
    }

    @Test
    fun `archiveAccount should call dao`() = runTest {
        // Given
        val accountId = 1L
        coEvery { accountDao.archiveAccount(accountId) } just Runs

        // When
        repository.archiveAccount(accountId)

        // Then
        coVerify { accountDao.archiveAccount(accountId) }
    }

    @Test
    fun `updateBalance should call dao`() = runTest {
        // Given
        val accountId = 1L
        val amount = 100.0
        coEvery { accountDao.updateBalance(accountId, amount) } just Runs

        // When
        repository.updateBalance(accountId, amount)

        // Then
        coVerify { accountDao.updateBalance(accountId, amount) }
    }

    @Test
    fun `setDefaultAccount should call dao`() = runTest {
        // Given
        val accountId = 1L
        coEvery { accountDao.setDefaultAccount(accountId) } just Runs

        // When
        repository.setDefaultAccount(accountId)

        // Then
        coVerify { accountDao.setDefaultAccount(accountId) }
    }

    @Test
    fun `getDefaultAccount should return account from dao`() = runTest {
        // Given
        val expectedAccount = Account(id = 1L, name = "Default", type = AccountType.CASH, balance = 1000.0, isDefault = true)
        coEvery { accountDao.getDefaultAccount() } returns expectedAccount

        // When
        val result = repository.getDefaultAccount()

        // Then
        assertEquals(expectedAccount, result)
        coVerify { accountDao.getDefaultAccount() }
    }

    @Test
    fun `getTotalBalance should return sum from dao`() = runTest {
        // Given
        val expectedTotal = 6000.0
        coEvery { accountDao.getTotalBalance() } returns expectedTotal

        // When
        val result = repository.getTotalBalance()

        // Then
        assertEquals(expectedTotal, result, 0.01)
        coVerify { accountDao.getTotalBalance() }
    }

    @Test
    fun `getTotalBalance should return zero when null`() = runTest {
        // Given
        coEvery { accountDao.getTotalBalance() } returns null

        // When
        val result = repository.getTotalBalance()

        // Then
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun `getTotalAssets should return flow from dao`() = runTest {
        // Given
        val expectedTotal = 5000.0
        every { accountDao.getTotalAssets() } returns flowOf(expectedTotal)

        // When
        val result = repository.getTotalAssets().first()

        // Then
        assertEquals(expectedTotal, result, 0.01)
        verify { accountDao.getTotalAssets() }
    }

    @Test
    fun `getTotalLiabilities should return flow from dao`() = runTest {
        // Given
        val expectedTotal = -1000.0
        every { accountDao.getTotalLiabilities() } returns flowOf(expectedTotal)

        // When
        val result = repository.getTotalLiabilities().first()

        // Then
        assertEquals(expectedTotal, result, 0.01)
        verify { accountDao.getTotalLiabilities() }
    }

    @Test
    fun `getAccountCount should return count from dao`() = runTest {
        // Given
        val expectedCount = 5
        coEvery { accountDao.getAccountCount() } returns expectedCount

        // When
        val result = repository.getAccountCount()

        // Then
        assertEquals(expectedCount, result)
        coVerify { accountDao.getAccountCount() }
    }

    @Test
    fun `accountExists should return true when exists`() = runTest {
        // Given
        val name = "Cash"
        coEvery { accountDao.accountExists(name) } returns true

        // When
        val result = repository.accountExists(name)

        // Then
        assertTrue(result)
        coVerify { accountDao.accountExists(name) }
    }

    @Test
    fun `accountExists should return false when not exists`() = runTest {
        // Given
        val name = "NonExistent"
        coEvery { accountDao.accountExists(name) } returns false

        // When
        val result = repository.accountExists(name)

        // Then
        assertFalse(result)
        coVerify { accountDao.accountExists(name) }
    }

    @Test
    fun `updateAccountBalance should call dao`() = runTest {
        // Given
        val accountId = 1L
        val newBalance = 1500.0
        coEvery { accountDao.updateAccountBalance(accountId, newBalance) } just Runs

        // When
        repository.updateAccountBalance(accountId, newBalance)

        // Then
        coVerify { accountDao.updateAccountBalance(accountId, newBalance) }
    }

    @Test
    fun `adjustAccountBalance should call dao`() = runTest {
        // Given
        val accountId = 1L
        val amount = 100.0
        coEvery { accountDao.adjustAccountBalance(accountId, amount) } just Runs

        // When
        repository.adjustAccountBalance(accountId, amount)

        // Then
        coVerify { accountDao.adjustAccountBalance(accountId, amount) }
    }
}
