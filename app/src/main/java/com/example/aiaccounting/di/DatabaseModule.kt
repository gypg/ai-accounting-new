package com.example.aiaccounting.di

import android.content.Context
import com.example.aiaccounting.data.local.dao.AccountDao
import com.example.aiaccounting.data.local.dao.AIConversationDao
import com.example.aiaccounting.data.local.dao.BudgetDao
import com.example.aiaccounting.data.local.dao.CategoryDao
import com.example.aiaccounting.data.local.dao.TransactionDao
import com.example.aiaccounting.data.local.database.AppDatabase
import com.example.aiaccounting.data.local.repository.AccountRepository
import com.example.aiaccounting.data.local.repository.AIConversationRepository
import com.example.aiaccounting.data.local.repository.BudgetRepository
import com.example.aiaccounting.data.local.repository.CategoryRepository
import com.example.aiaccounting.data.local.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Database module for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAccountDao(database: AppDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(database: AppDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    @Singleton
    fun provideAIConversationDao(database: AppDatabase): AIConversationDao {
        return database.aiConversationDao()
    }

    @Provides
    @Singleton
    fun provideAccountRepository(accountDao: AccountDao): AccountRepository {
        return AccountRepository(accountDao)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(categoryDao: CategoryDao): CategoryRepository {
        return CategoryRepository(categoryDao)
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(transactionDao: TransactionDao): TransactionRepository {
        return TransactionRepository(transactionDao)
    }

    @Provides
    @Singleton
    fun provideBudgetRepository(budgetDao: BudgetDao): BudgetRepository {
        return BudgetRepository(budgetDao)
    }

    @Provides
    @Singleton
    fun provideAIConversationRepository(aiConversationDao: AIConversationDao): AIConversationRepository {
        return AIConversationRepository(aiConversationDao)
    }
}
