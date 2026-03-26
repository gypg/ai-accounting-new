package com.example.aiaccounting.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.example.aiaccounting.data.local.converter.Converters
import com.example.aiaccounting.data.local.dao.AccountDao
import com.example.aiaccounting.data.local.dao.CategoryDao
import com.example.aiaccounting.data.local.dao.TransactionDao
import com.example.aiaccounting.data.local.dao.BudgetDao
import com.example.aiaccounting.data.local.dao.AIConversationDao
import com.example.aiaccounting.data.local.dao.TransactionTemplateDao
import com.example.aiaccounting.data.local.dao.ChatSessionDao
import com.example.aiaccounting.data.local.dao.ChatMessageDao
import com.example.aiaccounting.data.local.dao.ChatMemoryDao
import com.example.aiaccounting.data.local.dao.TagDao
import com.example.aiaccounting.data.local.dao.CustomButlerDao
import com.example.aiaccounting.data.local.dao.AIOperationTraceDao
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.Budget
import com.example.aiaccounting.data.local.entity.AIConversation
import com.example.aiaccounting.data.local.entity.TransactionTemplate
import com.example.aiaccounting.data.local.entity.ChatSession
import com.example.aiaccounting.data.local.entity.ChatMessageEntity
import com.example.aiaccounting.data.local.entity.ChatMemory
import com.example.aiaccounting.data.local.entity.Tag
import com.example.aiaccounting.data.local.entity.TransactionTag
import com.example.aiaccounting.data.local.entity.CustomButlerEntity
import com.example.aiaccounting.data.local.entity.AIOperationTrace
import com.example.aiaccounting.security.SecurityManager
import com.example.aiaccounting.data.storage.StorageManager
import com.example.aiaccounting.data.storage.ExternalSharedPreferences
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
        AIConversation::class,
        TransactionTemplate::class,
        ChatSession::class,
        ChatMessageEntity::class,
        ChatMemory::class,
        com.example.aiaccounting.data.local.entity.AIPermissionLog::class,
        Tag::class,
        TransactionTag::class,
        com.example.aiaccounting.data.local.entity.CustomButlerEntity::class,
        AIOperationTrace::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun aiConversationDao(): AIConversationDao
    abstract fun transactionTemplateDao(): TransactionTemplateDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatMemoryDao(): ChatMemoryDao
    abstract fun aiPermissionLogDao(): com.example.aiaccounting.data.local.dao.AIPermissionLogDao
    abstract fun aiOperationTraceDao(): AIOperationTraceDao
    abstract fun tagDao(): TagDao
    abstract fun customButlerDao(): CustomButlerDao

    companion object {
        const val DATABASE_NAME = "ai_accounting.db"

        /**
         * Migration from version 4 to 5: Add tags and transaction_tags tables
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tags` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` TEXT NOT NULL DEFAULT '#2196F3',
                        `icon` TEXT,
                        `createdAt` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `transaction_tags` (
                        `transactionId` INTEGER NOT NULL,
                        `tagId` INTEGER NOT NULL,
                        PRIMARY KEY(`transactionId`, `tagId`)
                    )
                """)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_parentId` ON `categories` (`parentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_type` ON `categories` (`type`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_butlers` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `avatarType` TEXT NOT NULL,
                        `avatarValue` TEXT NOT NULL,
                        `userCallName` TEXT NOT NULL,
                        `butlerSelfName` TEXT NOT NULL,
                        `communicationStyle` INTEGER NOT NULL,
                        `emotionIntensity` INTEGER NOT NULL,
                        `professionalism` INTEGER NOT NULL,
                        `humor` INTEGER NOT NULL,
                        `proactivity` INTEGER NOT NULL,
                        `featureFlagsJson` TEXT NOT NULL,
                        `priorityJson` TEXT NOT NULL,
                        `systemPrompt` TEXT NOT NULL,
                        `promptVersion` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_permission_logs` (
                        `id` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `operationType` TEXT NOT NULL,
                        `permissionLevel` TEXT NOT NULL,
                        `riskScore` REAL NOT NULL,
                        `confidence` REAL NOT NULL,
                        `granted` INTEGER NOT NULL,
                        `reason` TEXT NOT NULL,
                        `requiresHumanIntervention` INTEGER NOT NULL,
                        `interventionReason` TEXT,
                        `humanApproved` INTEGER,
                        `humanApprovalReason` TEXT,
                        `humanApprovalTime` INTEGER,
                        `executed` INTEGER NOT NULL,
                        `executionSuccess` INTEGER,
                        `executionResult` TEXT,
                        `userId` TEXT NOT NULL,
                        `deviceTrusted` INTEGER NOT NULL,
                        `networkSecure` INTEGER NOT NULL,
                        `operationDetails` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `aiSourceType` TEXT NOT NULL DEFAULT 'MANUAL'")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `aiTraceId` TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_operation_traces` (
                        `id` TEXT NOT NULL,
                        `traceId` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `actionType` TEXT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `entityId` TEXT,
                        `relatedTransactionId` INTEGER,
                        `summary` TEXT NOT NULL,
                        `details` TEXT,
                        `success` INTEGER NOT NULL,
                        `errorMessage` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_operation_traces_traceId` ON `ai_operation_traces` (`traceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_operation_traces_entityType_entityId` ON `ai_operation_traces` (`entityType`, `entityId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_operation_traces_sourceType` ON `ai_operation_traces` (`sourceType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_operation_traces_actionType` ON `ai_operation_traces` (`actionType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_operation_traces_timestamp` ON `ai_operation_traces` (`timestamp`)")
            }
        }
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
            return database ?: throw IllegalStateException("Database cache missing after initialization")
        }

        // Derive database key from PIN
        val salt = getOrCreateSalt()
        val dbKey = securityManager.deriveDatabaseKey(pin, salt)

        // Create SQLCipher support factory
        // IMPORTANT: derived key bytes are arbitrary; never roundtrip through UTF-8 String (lossy)
        val factory = SupportFactory(dbKey)

        // Build database with internal storage path (more stable)
        database = androidx.room.Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigrationFrom(1, 2, 3)
            .addMigrations(AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8)
            .addCallback(DatabaseCallback())
            .build()

        currentPin = pin
        return database ?: throw IllegalStateException("Database initialization failed")
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
     * 使用外部存储的 SharedPreferences
     */
    private fun getOrCreateSalt(): ByteArray {
        val saltKey = "db_salt"
        val prefs = ExternalSharedPreferences.getInstance(context, "db_prefs")
        val saltBase64 = prefs.getString(saltKey, null)

        if (saltBase64 != null) {
            return android.util.Base64.decode(saltBase64.toByteArray(), android.util.Base64.DEFAULT)
        }

        // Generate new salt
        val salt = securityManager.generateSalt()
        val saltBase64New = android.util.Base64.encodeToString(salt, android.util.Base64.DEFAULT)
        prefs.edit()
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
