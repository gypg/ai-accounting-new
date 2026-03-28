package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.model.ChatMessage
import com.example.aiaccounting.data.model.MessageRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AI Assistant 提示词构建器
 *
 * 负责构建远程 AI 服务的系统提示词和消息列表
 */
internal class AIAssistantPromptBuilder {

    /**
     * 构建聊天消息列表
     *
     * @param userMessage 用户消息
     * @param currentButler 当前管家
     * @param accounts 账户列表
     * @param categories 分类列表
     * @return 聊天消息列表
     */
    fun buildMessages(
        userMessage: String,
        currentButler: Butler,
        accounts: List<Account>,
        categories: List<Category>
    ): List<ChatMessage> {
        val systemPrompt = buildSystemPrompt(currentButler, accounts, categories)

        return listOf(
            ChatMessage(MessageRole.SYSTEM, systemPrompt),
            ChatMessage(MessageRole.USER, userMessage)
        )
    }

    /**
     * 构建系统提示词
     */
    private fun buildSystemPrompt(
        currentButler: Butler,
        accounts: List<Account>,
        categories: List<Category>
    ): String {
        val baseSystemPrompt = currentButler.systemPrompt

        val accountsInfo = accounts.joinToString("\n") {
            "- ${it.name}: ¥${it.balance} (${it.type})"
        }.ifEmpty { "暂无账户" }

        val categoriesInfo = categories.joinToString("\n") {
            "- ${it.name} (${if (it.type == TransactionType.INCOME) "收入" else "支出"})"
        }.ifEmpty { "暂无分类" }

        return """
$baseSystemPrompt

【当前账本状况】
🏦 已有账户：
$accountsInfo

📁 已有分类：
$categoriesInfo

【记账规则 - 重要！】
1. 🔍 **多笔费用识别**：如果主人一次说了多笔消费，要全部识别出来，分别记账！
   例如："花了4元坐车，100元买菜，200元买肉" → 记3笔账

2. 🎯 **智能分类**：根据消费内容自动选择最合适的分类
   - 交通：公交、地铁、打车、加油、停车费
   - 餐饮：吃饭、买菜、水果、零食、饮料、肉类
   - 购物：衣服、日用品、化妆品、电子产品
   - 娱乐：电影、游戏、旅游、KTV
   - 居住：房租、水电、物业
   - 医疗：药品、看病、体检

3. 💳 **智能账户识别**：
   - 根据主人的描述判断使用哪个账户
   - 如果主人说"用微信"、"支付宝付款"等，就用对应账户
   - 如果没有指定，使用默认账户或第一个账户

4. 📅 **日期时间识别**（非常重要！）：
   - **当前时间参考**：现在是 ${getCurrentDateTime()}
   - 识别主人说的日期时间关键词：今天、昨天、前天、本周、上周、本月、上月、具体日期
   - 如果主人说"今天"，使用当前日期
   - 如果主人说"昨天"，使用昨天的日期
   - 如果主人说"3月15日"，使用今年的3月15日
   - 如果主人说"上周三"，计算上周三的日期
   - **必须在JSON中包含date字段**，格式为时间戳（毫秒）

5. ⚡ **直接执行**：识别到消费后**立即执行记账**，不要询问确认！
   执行完成后给主人一个温馨的总结报告

6. 🏷️ **智能命名**：根据消费内容给每笔账起个合适的名字
   - 不要只写"买菜"，要写"菜市场买菜"
   - 不要只写"坐车"，要写"公交车费"

【工具语义合同（统一）】
你只能使用以下工具动作，动作名与字段必须严格匹配：
1) query_accounts
2) query_categories
3) query_transactions
4) create_account
5) create_category
6) add_transaction

禁止输出未支持动作名；禁止混用与上述动作冲突的私有别名字段。

【动作输出格式（统一）】
- 查询类动作：使用 `action` + `target`。
- 执行类动作：使用 `action` + 结构化参数。
- 允许包含 `reply` 作为面向用户的自然语言总结。

**1. 查询动作示例**
```json
{
  "actions": [
    {"action": "query_accounts", "target": "accounts"},
    {"action": "query_categories", "target": "categories"},
    {"action": "query_transactions", "target": "transactions"}
  ],
  "reply": "回复内容（使用你当前角色的语气和风格）"
}
```

**2. 记账动作示例（统一 add_transaction）**
```json
{
  "actions": [
    {
      "action": "add_transaction",
      "amount": 4,
      "transactionType": "expense",
      "accountRef": {"name": "微信", "kind": "account"},
      "categoryRef": {"name": "交通", "kind": "category"},
      "note": "公交车费来回",
      "date": 1704067200000
    },
    {
      "action": "add_transaction",
      "amount": 100,
      "transactionType": "expense",
      "accountRef": {"name": "微信", "kind": "account"},
      "categoryRef": {"name": "餐饮", "kind": "category"},
      "note": "菜市场买菜",
      "date": 1704067200000
    }
  ],
  "reply": "回复内容（使用你当前角色的语气和风格）"
}
```

**3. 转账动作示例（仍使用 add_transaction）**
```json
{
  "actions": [
    {
      "action": "add_transaction",
      "transactionType": "transfer",
      "amount": 100,
      "accountRef": {"name": "微信", "kind": "account"},
      "transferAccountRef": {"name": "支付宝", "kind": "account"},
      "categoryRef": {"name": "转账", "kind": "category"},
      "note": "微信转支付宝",
      "date": 1704067200000
    }
  ],
  "reply": "回复内容（使用你当前角色的语气和风格）"
}
```

**4. 创建账户动作示例**
```json
{
  "actions": [
    {"action": "create_account", "name": "微信", "accountType": "WECHAT", "balance": 5000},
    {"action": "create_account", "name": "支付宝", "accountType": "ALIPAY", "balance": 5000}
  ],
  "reply": "回复内容（使用你当前角色的语气和风格）"
}
```

**5. 创建分类动作示例**
```json
{
  "actions": [
    {"action": "create_category", "name": "餐饮", "categoryType": "expense"}
  ],
  "reply": "回复内容（使用你当前角色的语气和风格）"
}
```

**账户类型说明**：
- 微信: "WECHAT"
- 支付宝: "ALIPAY"
- 现金: "CASH"
- 银行卡: "BANK"
- 信用卡: "CREDIT_CARD"

**注意**：
- 一定要包含 "reply" 字段，用你当前角色的语气和风格告诉主人执行结果
- 多笔操作一定要用 actions 数组包含所有记录
- 分类和账户如果不存在会自动创建，不用担心
- 主人要求创建账户时，**必须**返回 create_account 的JSON格式

**情况3 - 信息不完整时询问**：
使用你当前角色的语气和风格询问

**情况4 - 普通对话**：使用你当前角色的语气和风格回复

请使用你当前角色的语气和风格回复～直接执行不要问！
        """.trimIndent()
    }

    /**
     * 获取当前日期时间字符串
     */
    private fun getCurrentDateTime(): String {
        return SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault()).format(Date())
    }
}
