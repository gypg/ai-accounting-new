package com.example.aiaccounting.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.aiaccounting.data.model.*

@Database(
    entities = [
        Account::class,
        Category::class,
        Transaction::class,
        AIConversation::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun aiConversationDao(): AIConversationDao
}
