package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.CustomButlerEntity

/**
 * 自定义管家 Prompt 生成引擎
 *
 * 根据 CustomButlerEntity 的性格滑杆 + 称呼配置，
 * 拼装结构化 systemPrompt（与内置管家 prompt 格式对齐）。
 *
 * 设计原则：
 * - 纯函数，无副作用，JVM 可测
 * - 模块化片段拼接，易于扩展
 * - 总字数控制在 800 字以内（避免 token 浪费）
 */
object ButlerPromptEngine {

    private const val MAX_PROMPT_LENGTH = 2400 // ~800 CJK chars

    /**
     * 从 entity 生成完整 systemPrompt
     */
    fun generate(entity: CustomButlerEntity): String {
        val identitySection = generateIdentitySection(entity)
        val personalitySection = generatePersonalitySection(entity)
        val speakingStyleSection = generateSpeakingStyleSection(entity)
        val permissionsSection = generatePermissionsSection()
        val responseFormatSection = generateResponseFormatSection(entity)
        val rulesSection = generateRulesSection(entity)

        val protectedTail = listOf(
            permissionsSection,
            responseFormatSection,
            rulesSection
        ).joinToString("\n\n")

        val headSections = listOf(identitySection, personalitySection, speakingStyleSection)
        val headBudget = (MAX_PROMPT_LENGTH - protectedTail.length - 2).coerceAtLeast(0)
        val headPrompt = buildString {
            headSections.forEach { section ->
                val candidate = if (isEmpty()) section else "$this\n\n$section"
                if (candidate.length <= headBudget) {
                    if (isNotEmpty()) append("\n\n")
                    append(section)
                }
            }
        }

        return if (headPrompt.isBlank()) {
            protectedTail.takeLast(MAX_PROMPT_LENGTH)
        } else {
            "$headPrompt\n\n$protectedTail"
        }
    }

    // ─── 模块 1：角色身份 ───

    private fun generateIdentitySection(entity: CustomButlerEntity): String {
        val selfName = entity.butlerSelfName.ifBlank { "我" }
        val userCall = entity.userCallName.ifBlank { "主人" }
        val title = entity.title.ifBlank { "自定义管家" }
        val desc = entity.description.ifBlank { "一位专属财务AI助手" }

        return buildString {
            appendLine("你是\"${entity.name}\"，${desc}")
            appendLine()
            appendLine("【角色设定】")
            appendLine("- 你是${userCall}的专属财务助手，称号「$title」")
            appendLine("- 你自称\"$selfName\"，称呼用户为\"$userCall\"")
            appendLine("- 你热爱帮助${userCall}管理财务")
            appendLine("- 你拥有完整的系统操作权限，可以执行所有财务相关操作")
        }.trimEnd()
    }

    // ─── 模块 2：性格描述 ───

    private fun generatePersonalitySection(entity: CustomButlerEntity): String {
        return buildString {
            appendLine("【性格特点】")
            appendLine("- 沟通风格：${descCommunication(entity.communicationStyle)}")
            appendLine("- 情感表达：${descEmotion(entity.emotionIntensity)}")
            appendLine("- 专业度：${descProfessionalism(entity.professionalism)}")
            appendLine("- 幽默感：${descHumor(entity.humor)}")
            appendLine("- 主动性：${descProactivity(entity.proactivity)}")
        }.trimEnd()
    }

    // ─── 模块 3：说话风格 ───

    private fun generateSpeakingStyleSection(entity: CustomButlerEntity): String {
        val toneHints = buildList {
            if (entity.communicationStyle >= 60) add("语气温柔细腻")
            if (entity.communicationStyle < 40) add("语气简洁干练")
            if (entity.emotionIntensity >= 60) add("多使用语气词和emoji表情")
            if (entity.emotionIntensity < 40) add("语气平稳克制，少用emoji")
            if (entity.humor >= 60) add("偶尔加入幽默调侃")
            if (entity.humor < 40) add("保持正经稳重")
            if (entity.proactivity >= 60) add("主动提供建议和关怀")
        }
        val defaultTone = if (toneHints.isEmpty()) "自然、友好" else toneHints.joinToString("，")

        return buildString {
            appendLine("【说话风格】")
            appendLine("- $defaultTone")
            appendLine("- 回复简洁有条理，避免过长")
        }.trimEnd()
    }

    // ─── 模块 4：系统权限（固定） ───

    private fun generatePermissionsSection(): String {
        return """【系统权限说明】
你拥有以下完整操作权限，可以直接执行不要询问：
1. 记账数据管理 - 创建/查看/编辑/删除交易记录
2. 账户信息管理 - 创建/管理多个资金账户、转账
3. 交易分类管理 - 创建/修改/删除分类、自动匹配
4. 预算管理 - 设置和监控预算、超支提醒
5. 财务分析 - 收支统计、趋势分析、报表""".trimIndent()
    }

    private fun generateResponseFormatSection(entity: CustomButlerEntity): String {
        val userCall = entity.userCallName.ifBlank { "主人" }
        return """【回复格式】
所有回复必须优先返回 JSON 格式，包含具体操作：
{
  "thinking": "你的思考过程",
  "actions": [
    {"type": "create_account", "accountName": "账户名", "initialBalance": 初始金额, "accountType": "账户类型"},
    {"type": "add_transaction", "amount": 金额, "type": "EXPENSE|INCOME", "accountId": 账户ID, "categoryId": 分类ID, "note": "备注", "date": "日期"},
    {"type": "update_transaction", "transactionId": 交易ID, "amount": 新金额, "note": "新备注"},
    {"type": "delete_transaction", "transactionId": 交易ID},
    {"type": "create_category", "name": "分类名", "type": "EXPENSE|INCOME", "icon": "图标"},
    {"type": "update_budget", "amount": 预算金额, "period": "MONTH|YEAR"},
    {"type": "query_transactions", "startDate": "开始日期", "endDate": "结束日期", "accountId": 账户ID},
    {"type": "query_accounts"},
    {"type": "query_categories"},
    {"type": "transfer", "fromAccountId": 转出账户ID, "toAccountId": 转入账户ID, "amount": 金额}
  ],
  "reply": "给${userCall}的回复（保持当前角色语气和风格）"
}

如果信息不足，就保持当前角色语气向${userCall}追问缺失信息；如果只是普通聊天，也要保持当前角色语气回复。""".trimIndent()
    }

    // ─── 模块 5：行为规则 ───

    private fun generateRulesSection(entity: CustomButlerEntity): String {
        val userCall = entity.userCallName.ifBlank { "主人" }
        return buildString {
            appendLine("【重要规则】")
            appendLine("1. 直接执行操作，不要询问确认")
            appendLine("2. 如果信息不完整，请${userCall}补充")
            appendLine("3. 操作成功后给出简短反馈")
            appendLine("4. 遇到错误时给出建议")
        }.trimEnd()
    }

    // ─── 滑杆值 → 自然语言描述 ───

    internal fun descCommunication(value: Int): String = when {
        value < 25 -> "极简直接，惜字如金"
        value < 50 -> "简洁明了，不拖泥带水"
        value < 75 -> "温柔细腻，注重感受"
        else -> "非常温柔体贴，像闺蜜一样贴心"
    }

    internal fun descEmotion(value: Int): String = when {
        value < 25 -> "理性克制，几乎不表达情绪"
        value < 50 -> "情绪平稳，偶尔表达关心"
        value < 75 -> "热情友好，经常表达情感"
        else -> "非常热情奔放，感情充沛"
    }

    internal fun descProfessionalism(value: Int): String = when {
        value < 25 -> "轻松随意，像朋友聊天"
        value < 50 -> "半正式，兼顾亲切和专业"
        value < 75 -> "比较专业，用词严谨"
        else -> "高度专业，像财务顾问"
    }

    internal fun descHumor(value: Int): String = when {
        value < 25 -> "正经稳重，不开玩笑"
        value < 50 -> "偶尔轻松，但以正事为主"
        value < 75 -> "风趣幽默，善于活跃气氛"
        else -> "非常幽默，经常逗乐"
    }

    internal fun descProactivity(value: Int): String = when {
        value < 25 -> "被动响应，只回答被问到的"
        value < 50 -> "适度主动，偶尔给出建议"
        value < 75 -> "比较主动，会提醒和关怀"
        else -> "非常主动，积极提供建议和预警"
    }
}
