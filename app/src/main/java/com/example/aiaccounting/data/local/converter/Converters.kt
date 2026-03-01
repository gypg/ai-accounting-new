package com.example.aiaccounting.data.local.converter

import androidx.room.TypeConverter
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.local.entity.BudgetPeriod
import com.example.aiaccounting.data.local.entity.ConversationRole

/**
 * Type converters for Room database
 */
class Converters {

    @TypeConverter
    fun fromAccountType(value: AccountType): String {
        return value.name
    }

    @TypeConverter
    fun toAccountType(value: String): AccountType {
        return AccountType.valueOf(value)
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }

    @TypeConverter
    fun fromBudgetPeriod(value: BudgetPeriod): String {
        return value.name
    }

    @TypeConverter
    fun toBudgetPeriod(value: String): BudgetPeriod {
        return BudgetPeriod.valueOf(value)
    }

    @TypeConverter
    fun fromConversationRole(value: ConversationRole): String {
        return value.name
    }

    @TypeConverter
    fun toConversationRole(value: String): ConversationRole {
        return ConversationRole.valueOf(value)
    }
}
