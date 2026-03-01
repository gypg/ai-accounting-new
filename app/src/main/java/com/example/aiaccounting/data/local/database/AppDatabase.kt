package com.example.aiaccounting.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.example.aiaccounting.data.local.converter.Converters
import com.example.aiaccounting.data.local.dao.AccountDao
import com.example.aiaccounting.data.local.dao.CategoryDao
import com.example.aiaccounting.data.local.dao.TransactionDao
import com.example.aiaccounting.data.local.dao.BudgetDao
import com.example.aiaccounting.data.local.dao.AIConversationDao
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.Budget
import com.example.aiaccounting.data.local.entity.AIConversation
import com.example.aiaccounting.security.SecurityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main encrypted database using SQLCipher
 */
@Database(
    entities = [
        Account::class,
        Category::class,
        Transaction::class,
        Budget::class,
        AIConversation::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun aiConversationDao(): AIConversationDao

    companion object {
        const val DATABASE_NAME = "ai_accounting.db"
    }
}

/**
 * Database factory for creating encrypted database instances
 */
@Singleton
class DatabaseFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityManager: SecurityManager
) {

    private var database: AppDatabase? = null
    private var currentPin: String? = null

    /**
     * Initialize database with PIN
     * Must be called before accessing database
     */
    fun initializeDatabase(pin: String): AppDatabase {
        if (database != null && currentPin == pin) {
            return database!!
        }

        // Derive database key from PIN
        val salt = getOrCreateSalt()
        val dbKey = securityManager.deriveDatabaseKey(pin, salt)
        val passphrase = String(dbKey, Charsets.UTF_8)

        // Create SQLCipher support factory
        val factory = SupportFactory(passphrase.toByteArray())

        // Build database
        database = androidx.room.Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration() // For development only
            .addCallback(DatabaseCallback())
            .build()

        currentPin = pin
        return database!!
    }

    /**
     * Get current database instance
     * Throws if not initialized
     */
    fun getDatabase(): AppDatabase {
        return database ?: throw IllegalStateException("Database not initialized. Call initializeDatabase() first.")
    }

    /**
     * Check if database is initialized
     */
    fun isInitialized(): Boolean {
        return database != null
    }

    /**
     * Reinitialize database with new PIN (for PIN change)
     */
    fun reinitializeDatabase(oldPin: String, newPin: String): Boolean {
        if (!isInitialized() || currentPin != oldPin) {
            return false
        }

        // Close old database
        database?.close()
        database = null
        currentPin = null

        // Initialize with new PIN
        initializeDatabase(newPin)
        return true
    }

    /**
     * Get or create salt for key derivation
     */
    private fun getOrCreateSalt(): ByteArray {
        val saltKey = "db_salt"
        val saltBase64 = context.getSharedPreferences("db_prefs", Context.MODE_PRIVATE)
            .getString(saltKey, null)

        if (saltBase64 != null) {
            return android.util.Base64.decode(saltBase64, android.util.Base64.DEFAULT)
        }

        // Generate new salt
        val salt = securityManager.generateSalt()
        val saltBase64New = android.util.Base64.encodeToString(salt, android.util.Base64.DEFAULT)
        context.getSharedPreferences("db_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(saltKey, saltBase64New)
            .apply()

        return salt
    }

    /**
     * Database callback for initialization
     */
    private class DatabaseCallback : androidx.room.RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Pre-populate default categories
            prepopulateCategories(db)
        }

        private fun prepopulateCategories(db: SupportSQLiteDatabase) {
            val defaultCategories = listOf(
                // Income categories
                "INSERT INTO categories (id, name, type, icon, color, isDefault, parentId) VALUES (1, '工资', 'INCOME', '💰', '#4CAF50', 1, NULL)",
                "INSERT INTO categories (id, name, type, icon, color, isDefault, parentId) VALUES (2, '奖金', 'INCOME', '🎁', '#8BC34A', 1, NULL)",
                "INSERT INTO categories (id, name, type, icon, color, isDefault, parentId) VALUES (3, '投资', 'INCOME', '📈', '#CDDC39', 1, NULL)",
                "INSERT INTO categories (id, name, type, icon, color, isDefault, parentId) VALUES (4, '兼职', 'INCOME', '💼', '#FFC107', 1, NULL)",
                "INSERT INTO categories (id, name, type, icon, color, isDefault, parentId) VALUES (5, '其他收入', 'INCOME', '💵', '#FF9800', 1, NULL)",
                
                // Expense categories
                "INSERT INTO categories (id, name, type, icon, color, isDefault, parentId) VALUES (6, '餐饮', 'EXPENSE', '🍔', '#F44336', 1, NULL)",
                "INSERT INTO categories (id, name, type, icon, color, isDefault, parentId) VALUES (7, '交通', 'EXPENSE', '🚗', '#E91E63', 1, NULL)",
                "INSERT INTO categories (id, name, type, icon, color, isDefault, parentId) VALUES (8, '购物', 'EXPENSE', '🛒', '#9C27B0', 1, NULL)",
                "INSERT INTO categories (id, name, type, icon, color, isDefault, parentId) VALUES (9, '娱乐', 'EXPENSE', '🎮', '#673AB7', 1, NULL)",
                "INSERT INTO categories (id, name, type, icon, color, isDefault, parentId) VALUES (10, '医疗', 'EXPENSE', '💊', '#3F51B5', 1, NULL)",
                "INSERT INTO categories (id, name, type, icon, color, isDefault, parentId) VALUES (11, '教育', 'EXPENSE', '📚', '#2196F3', 1, NULL)",
                "INSERT INTO categories (id, name, type, icon, color, isDefault, parentId) VALUES (12, '住房', 'EXPENSE', '🏠', '#03A9F4', 1, NULL)",
                "INSERT INTO categories (id, name, type, icon, color, isDefault, parentId) VALUES (13, '通讯', 'EXPENSE', '📱', '#00BCD4', 1, NULL)",
                "INSERT INTO categories (id, name, type, icon, color, isDefault, parentId) VALUES (14, '其他支出', 'EXPENSE', '📦', '#009688', 1, NULL)"
            )

            defaultCategories.forEach { query ->
                try {
                    db.execSQL(query)
                } catch (e: Exception) {
                    // Ignore errors if categories already exist
                }
            }
        }
    }
}
