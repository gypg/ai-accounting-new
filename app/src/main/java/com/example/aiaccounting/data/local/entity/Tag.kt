package com.example.aiaccounting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 标签实体
 */
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: String = "#2196F3", // 默认蓝色
    val icon: String? = null, // 可选的图标
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 交易标签关联表
 */
@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transactionId", "tagId"]
)
data class TransactionTag(
    val transactionId: Long,
    val tagId: Long
)

/**
 * 预设标签颜色
 */
object TagColors {
    val RED = "#F44336"
    val PINK = "#E91E63"
    val PURPLE = "#9C27B0"
    val DEEP_PURPLE = "#673AB7"
    val INDIGO = "#3F51B5"
    val BLUE = "#2196F3"
    val LIGHT_BLUE = "#03A9F4"
    val CYAN = "#00BCD4"
    val TEAL = "#009688"
    val GREEN = "#4CAF50"
    val LIGHT_GREEN = "#8BC34A"
    val LIME = "#CDDC39"
    val YELLOW = "#FFEB3B"
    val AMBER = "#FFC107"
    val ORANGE = "#FF9800"
    val DEEP_ORANGE = "#FF5722"
    val BROWN = "#795548"
    val GREY = "#9E9E9E"
    val BLUE_GREY = "#607D8B"

    val PRESET_COLORS = listOf(
        RED, PINK, PURPLE, DEEP_PURPLE, INDIGO,
        BLUE, LIGHT_BLUE, CYAN, TEAL, GREEN,
        LIGHT_GREEN, LIME, YELLOW, AMBER, ORANGE,
        DEEP_ORANGE, BROWN, GREY, BLUE_GREY
    )
}

/**
 * 预设标签
 */
object PresetTags {
    val TAGS = listOf(
        Tag(name = "重要", color = TagColors.RED),
        Tag(name = "日常", color = TagColors.BLUE),
        Tag(name = "工作", color = TagColors.PURPLE),
        Tag(name = "娱乐", color = TagColors.GREEN),
        Tag(name = "旅行", color = TagColors.ORANGE),
        Tag(name = "餐饮", color = TagColors.YELLOW),
        Tag(name = "购物", color = TagColors.PINK),
        Tag(name = "交通", color = TagColors.TEAL),
        Tag(name = "医疗", color = TagColors.CYAN),
        Tag(name = "教育", color = TagColors.INDIGO)
    )
}
