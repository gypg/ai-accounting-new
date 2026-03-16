package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.CustomButlerEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ButlerPromptEngineTest {

    private fun createEntity(
        communication: Int = 50,
        emotion: Int = 50,
        professionalism: Int = 50,
        humor: Int = 50,
        proactivity: Int = 50
    ) = CustomButlerEntity(
        id = "test_id",
        name = "TestName",
        title = "TestTitle",
        description = "TestDescription",
        avatarType = "RESOURCE",
        avatarValue = "",
        userCallName = "Master",
        butlerSelfName = "Servant",
        communicationStyle = communication,
        emotionIntensity = emotion,
        professionalism = professionalism,
        humor = humor,
        proactivity = proactivity,
        featureFlagsJson = "{}",
        priorityJson = "[]",
        systemPrompt = "",
        promptVersion = 1,
        createdAt = 0L,
        updatedAt = 0L
    )

    @Test
    fun `generate prompt includes basic identity information`() {
        val entity = createEntity()
        val prompt = ButlerPromptEngine.generate(entity)

        assertTrue(prompt.contains("你是\"TestName\"，TestDescription"))
        assertTrue(prompt.contains("你是Master的专属财务助手，称号「TestTitle」"))
        assertTrue(prompt.contains("你自称\"Servant\"，称呼用户为\"Master\""))
    }

    @Test
    fun `generate prompt reflects extreme low personality values`() {
        val entity = createEntity(10, 10, 10, 10, 10)
        val prompt = ButlerPromptEngine.generate(entity)

        assertTrue(prompt.contains("沟通风格：极简直接，惜字如金"))
        assertTrue(prompt.contains("情感表达：理性克制，几乎不表达情绪"))
        assertTrue(prompt.contains("专业度：轻松随意，像朋友聊天"))
        assertTrue(prompt.contains("幽默感：正经稳重，不开玩笑"))
        assertTrue(prompt.contains("主动性：被动响应，只回答被问到的"))
    }

    @Test
    fun `generate prompt reflects extreme high personality values`() {
        val entity = createEntity(90, 90, 90, 90, 90)
        val prompt = ButlerPromptEngine.generate(entity)

        assertTrue(prompt.contains("沟通风格：非常温柔体贴，像闺蜜一样贴心"))
        assertTrue(prompt.contains("情感表达：非常热情奔放，感情充沛"))
        assertTrue(prompt.contains("专业度：高度专业，像财务顾问"))
        assertTrue(prompt.contains("幽默感：非常幽默，经常逗乐"))
        assertTrue(prompt.contains("主动性：非常主动，积极提供建议和预警"))
    }

    @Test
    fun `generate prompt includes required system permissions`() {
        val entity = createEntity()
        val prompt = ButlerPromptEngine.generate(entity)

        assertTrue(prompt.contains("【系统权限说明】"))
        assertTrue(prompt.contains("记账数据管理"))
        assertTrue(prompt.contains("账户信息管理"))
        assertTrue(prompt.contains("交易分类管理"))
        assertTrue(prompt.contains("预算管理"))
        assertTrue(prompt.contains("财务分析"))
    }
}