package com.example.aiaccounting.data.importer

/**
 * 账单导入类型
 */
enum class ImportType {
    ALIPAY,  // 支付宝账单
    WECHAT,  // 微信账单
    CSV      // 通用CSV格式
}

/**
 * 导入结果
 */
sealed class ImportResult {
    data class Success(val importedCount: Int, val skippedCount: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
