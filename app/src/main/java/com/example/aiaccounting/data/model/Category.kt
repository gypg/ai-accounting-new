package com.example.aiaccounting.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val type: TransactionType,
    val icon: String = "",
    val color: String = "",
    val sortOrder: Int = 0,
    val createdAt: Date = Date()
)

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER_IN,
    TRANSFER_OUT,
    BORROW_IN,
    BORROW_OUT
}
