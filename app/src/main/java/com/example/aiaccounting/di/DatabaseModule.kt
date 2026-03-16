package com.example.aiaccounting.di

import android.content.Context
import androidx.room.Room
import com.example.aiaccounting.data.exporter.CsvExporter
import com.example.aiaccounting.data.local.dao.AccountDao
import com.example.aiaccounting.data.local.dao.AIConversationDao
import com.example.aiaccounting.data.local.dao.BudgetDao
import com.example.aiaccounting.data.local.dao.CategoryDao
import com.example.aiaccounting.data.local.dao.ChatMemoryDao
import com.example.aiaccounting.data.local.dao.ChatMessageDao
import com.example.aiaccounting.data.local.dao.ChatSessionDao
import com.example.aiaccounting.data.local.dao.TransactionDao
import com.example.aiaccounting.data.local.dao.TransactionTemplateDao
import com.example.aiaccounting.data.local.dao.CustomButlerDao
import com.example.aiaccounting.data.local.database.AppDatabase
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.AIConversationRepository
import com.example.aiaccounting.data.repository.BudgetRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.ChatSessionRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import com.example.aiaccounting.data.repository.TransactionTemplateRepository
import com.example.aiaccounting.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

/**
 * Database module for dependency injection
 * Provides encrypted database using SQLCipher
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        securityManager: SecurityManager
    ): AppDatabase {
        // Generate or retrieve database encryption key
        val dbKey = getOrCreateDatabaseKey(securityManager)
        
        // Create SQLCipher factory with the encryption key
        val factory = SupportFactory(dbKey.toByteArray())
        
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7)
            .fallbackToDestructiveMigration() // For development only
            .build()
    }
    
    /**
     * Get existing database key or create a new one
     */
    private fun getOrCreateDatabaseKey(securityManager: SecurityManager): String {
        val keyPref = "database_encryption_key"
        var key = securityManager.getEncryptedString(keyPref)
        
        if (key == null) {
            // Generate a new random key
            val salt = securityManager.generateSalt()
            val randomKey = securityManager.generateSalt().joinToString("") { "%02x".format(it) }
            key = randomKey + salt.joinToString("") { "%02x".format(it) }
            securityManager.storeEncryptedString(keyPref, key)
        }
        
        return key
    }

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
    fun provideTransactionTemplateDao(database: AppDatabase): TransactionTemplateDao {
        return database.transactionTemplateDao()
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        transactionDao: TransactionDao,
        @ApplicationContext context: Context
    ): TransactionRepository {
        return TransactionRepository(transactionDao, context)
    }

    @Provides
    @Singleton
    fun provideAccountRepository(
        accountDao: AccountDao,
        transactionRepository: TransactionRepository
    ): AccountRepository {
        return AccountRepository(accountDao, transactionRepository)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(
        categoryDao: CategoryDao,
        transactionRepository: TransactionRepository
    ): CategoryRepository {
        return CategoryRepository(categoryDao, transactionRepository)
    }

    @Provides
    @Singleton
    fun provideBudgetRepository(
        budgetDao: BudgetDao,
        transactionDao: TransactionDao
    ): BudgetRepository {
        return BudgetRepository(budgetDao, transactionDao)
    }

    @Provides
    @Singleton
    fun provideAIConversationRepository(aiConversationDao: AIConversationDao): AIConversationRepository {
        return AIConversationRepository(aiConversationDao)
    }

    @Provides
    @Singleton
    fun provideTransactionTemplateRepository(transactionTemplateDao: TransactionTemplateDao): TransactionTemplateRepository {
        return TransactionTemplateRepository(transactionTemplateDao)
    }

    // Chat Session DAOs
    @Provides
    @Singleton
    fun provideChatSessionDao(database: AppDatabase): ChatSessionDao {
        return database.chatSessionDao()
    }

    @Provides
    @Singleton
    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }

    @Provides
    fun provideChatMemoryDao(database: AppDatabase): ChatMemoryDao {
        return database.chatMemoryDao()
    }

    @Provides
    fun provideAIPermissionLogDao(database: AppDatabase): com.example.aiaccounting.data.local.dao.AIPermissionLogDao {
        return database.aiPermissionLogDao()
    }

    @Provides
    @Singleton
    fun provideTagDao(database: AppDatabase): com.example.aiaccounting.data.local.dao.TagDao {
        return database.tagDao()
    }

    @Provides
    @Singleton
    fun provideCustomButlerDao(database: AppDatabase): CustomButlerDao {
        return database.customButlerDao()
    }

    @Provides
    @Singleton
    fun provideChatSessionRepository(
        sessionDao: ChatSessionDao,
        messageDao: ChatMessageDao,
        memoryDao: ChatMemoryDao
    ): ChatSessionRepository {
        return ChatSessionRepository(sessionDao, messageDao, memoryDao)
    }

    // CSV Exporter
    @Provides
    @Singleton
    fun provideCsvExporter(
        @ApplicationContext context: Context,
        accountRepository: AccountRepository,
        categoryRepository: CategoryRepository
    ): CsvExporter {
        return CsvExporter(context, accountRepository, categoryRepository)
    }
}
