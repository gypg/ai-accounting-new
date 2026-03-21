package com.example.aiaccounting.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.importer.BillImporter
import com.example.aiaccounting.data.importer.ImportResult
import com.example.aiaccounting.data.importer.ImportType
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val transactionRepository: TransactionRepository,
  private val billImporter: BillImporter
) : ViewModel() {

  private val _uiState = MutableStateFlow(ImportUiState())
  val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

  data class ImportUiState(
    val selectedImportType: ImportType = ImportType.CSV,
    val isImporting: Boolean = false,
    val importResult: ImportResult? = null,
    val previewTransactions: List<Transaction> = emptyList(),
    val showPreview: Boolean = false,
    val fileName: String = ""
  )

  fun selectImportType(type: ImportType) {
    _uiState.update { it.copy(
      selectedImportType = type,
      importResult = null,
      previewTransactions = emptyList(),
      showPreview = false,
      fileName = ""
    ) }
  }

  fun previewFile(uri: Uri, fileName: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isImporting = true, fileName = fileName) }

      try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
          val content = inputStream.bufferedReader().use { it.readText() }

          val transactions = when (_uiState.value.selectedImportType) {
            ImportType.ALIPAY -> billImporter.previewAlipayBill(content)
            ImportType.WECHAT -> billImporter.previewWechatBill(content)
            ImportType.CSV -> billImporter.previewCsvBill(content)
          }

          _uiState.update { it.copy(
            isImporting = false,
            previewTransactions = transactions,
            showPreview = true
          ) }
        } ?: run {
          _uiState.update { it.copy(
            isImporting = false,
            importResult = ImportResult.Error("无法读取文件")
          ) }
        }
      } catch (e: Exception) {
        _uiState.update { it.copy(
          isImporting = false,
          importResult = ImportResult.Error("解析失败: ${e.message}")
        ) }
      }
    }
  }

  fun confirmImport() {
    viewModelScope.launch {
      _uiState.update { it.copy(isImporting = true) }

      var importedCount = 0
      var skippedCount = 0

      _uiState.value.previewTransactions.forEach { transaction ->
        try {
          transactionRepository.insertTransaction(transaction)
          importedCount++
        } catch (e: Exception) {
          skippedCount++
        }
      }

      _uiState.update { it.copy(
        isImporting = false,
        importResult = ImportResult.Success(importedCount, skippedCount),
        showPreview = false,
        previewTransactions = emptyList()
      ) }
    }
  }

  fun cancelPreview() {
    _uiState.update { it.copy(
      showPreview = false,
      previewTransactions = emptyList(),
      fileName = ""
    ) }
  }

  fun clearResult() {
    _uiState.update { it.copy(importResult = null) }
  }

  fun getImportTypeDescription(type: ImportType): String {
    return when (type) {
      ImportType.ALIPAY -> "支付宝账单"
      ImportType.WECHAT -> "微信账单"
      ImportType.CSV -> "通用CSV"
    }
  }

  fun getImportInstructions(type: ImportType): String {
    return when (type) {
      ImportType.ALIPAY -> """
        支付宝账单导入说明：
        1. 打开支付宝APP
        2. 我的 → 账单 → 右上角设置 → 开具交易流水证明
        3. 选择"用于个人对账"，导出CSV格式
        4. 将文件保存到手机后选择导入
      """.trimIndent()
      ImportType.WECHAT -> """
        微信账单导入说明：
        1. 打开微信APP
        2. 我 → 服务 → 钱包 → 账单 → 常见问题
        3. 下载账单 → 用于个人对账
        4. 输入邮箱接收账单，下载后选择导入
      """.trimIndent()
      ImportType.CSV -> """
        CSV格式要求：
        第一行：日期,类型,金额,分类,账户,备注
        示例：
        2024-01-15,支出,35.50,餐饮,支付宝,午餐
        2024-01-16,收入,5000.00,工资,银行卡,月薪
      """.trimIndent()
    }
  }
}
