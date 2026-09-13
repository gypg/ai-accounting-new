package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.TransactionTemplateDao
import com.example.aiaccounting.data.local.entity.TransactionTemplate
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 记账模板仓库
 */
@Singleton
class TransactionTemplateRepository @Inject constructor(
    private val templateDao: TransactionTemplateDao
) {

    fun getAllTemplates(): Flow<List<TransactionTemplate>> {
        return templateDao.getAllTemplates()
    }

    suspend fun getAllTemplatesList(): List<TransactionTemplate> {
        return templateDao.getAllTemplatesList()
    }

    suspend fun getTemplateById(id: Long): TransactionTemplate? {
        return templateDao.getTemplateById(id)
    }

    suspend fun insertTemplate(template: TransactionTemplate): Long {
        return templateDao.insertTemplate(template)
    }

    suspend fun updateTemplate(template: TransactionTemplate) {
        templateDao.updateTemplate(template)
    }

    suspend fun deleteTemplate(template: TransactionTemplate) {
        templateDao.deleteTemplate(template)
    }

    suspend fun deleteTemplateById(id: Long) {
        templateDao.deleteTemplateById(id)
    }

    suspend fun createDefaultTemplates() {
        val count = templateDao.getTemplateCount()
        if (count == 0) {
            // 创建默认模板
            val defaultTemplates = listOf(
                TransactionTemplate(
                    name = "早餐",
                    amount = 10.0,
                    type = com.example.aiaccounting.data.local.entity.TransactionType.EXPENSE,
                    categoryId = 1,
                    accountId = 1,
                    note = "早餐",
                    icon = "🍞",
                    sortOrder = 0
                ),
                TransactionTemplate(
                    name = "午餐",
                    amount = 20.0,
                    type = com.example.aiaccounting.data.local.entity.TransactionType.EXPENSE,
                    categoryId = 1,
                    accountId = 1,
                    note = "午餐",
                    icon = "🍱",
                    sortOrder = 1
                ),
                TransactionTemplate(
                    name = "晚餐",
                    amount = 25.0,
                    type = com.example.aiaccounting.data.local.entity.TransactionType.EXPENSE,
                    categoryId = 1,
                    accountId = 1,
                    note = "晚餐",
                    icon = "🍜",
                    sortOrder = 2
                ),
                TransactionTemplate(
                    name = "地铁",
                    amount = 5.0,
                    type = com.example.aiaccounting.data.local.entity.TransactionType.EXPENSE,
                    categoryId = 2,
                    accountId = 1,
                    note = "地铁通勤",
                    icon = "🚇",
                    sortOrder = 3
                ),
                TransactionTemplate(
                    name = "工资",
                    amount = 5000.0,
                    type = com.example.aiaccounting.data.local.entity.TransactionType.INCOME,
                    categoryId = 3,
                    accountId = 1,
                    note = "月工资",
                    icon = "💰",
                    sortOrder = 4
                ),
                TransactionTemplate(
                    name = "咖啡",
                    amount = 15.0,
                    type = com.example.aiaccounting.data.local.entity.TransactionType.EXPENSE,
                    categoryId = 1,
                    accountId = 1,
                    note = "下午茶",
                    icon = "☕",
                    sortOrder = 5
                )
            )
            defaultTemplates.forEach { templateDao.insertTemplate(it) }
        }
    }
}
