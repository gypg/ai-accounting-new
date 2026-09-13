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
        return try {
            AccountType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            AccountType.CASH
        }
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            TransactionType.EXPENSE
        }
    }

    @TypeConverter
    fun fromBudgetPeriod(value: BudgetPeriod): String {
        return value.name
    }

    @TypeConverter
    fun toBudgetPeriod(value: String): BudgetPeriod {
        return try {
            BudgetPeriod.valueOf(value)
        } catch (e: IllegalArgumentException) {
            BudgetPeriod.MONTHLY
        }
    }

    @TypeConverter
    fun fromConversationRole(value: ConversationRole): String {
        return value.name
    }

    @TypeConverter
    fun toConversationRole(value: String): ConversationRole {
        return try {
            ConversationRole.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ConversationRole.USER
        }
    }
}
