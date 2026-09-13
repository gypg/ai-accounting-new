package com.example.aiaccounting.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelIdCategorizerTest {

    @Test
    fun categorizeModelId_vendorPrefixMappings() {
        assertEquals("OpenAI", ModelIdCategorizer.categorizeModelId("openai/gpt-oss-120b"))
        assertEquals("Claude", ModelIdCategorizer.categorizeModelId("anthropic/claude-3-5-sonnet"))
        assertEquals("Gemini", ModelIdCategorizer.categorizeModelId("google/gemini-2.0-flash"))
        assertEquals("Llama", ModelIdCategorizer.categorizeModelId("meta-llama/llama-3.1-70b-instruct"))
        assertEquals("Mistral", ModelIdCategorizer.categorizeModelId("mistralai/mistral-large"))
        assertEquals("通义千问", ModelIdCategorizer.categorizeModelId("qwen/qwen2.5-72b-instruct"))
        assertEquals("DeepSeek", ModelIdCategorizer.categorizeModelId("deepseek/deepseek-r1"))
        assertEquals("ChatGLM", ModelIdCategorizer.categorizeModelId("zhipu/glm-4"))
        assertEquals("Yi", ModelIdCategorizer.categorizeModelId("01-ai/yi-34b"))
        assertEquals("其他", ModelIdCategorizer.categorizeModelId("unknownvendor/foo"))
        assertEquals("ChatGLM", ModelIdCategorizer.categorizeModelId("2api/glm-4.7-think"))
    }

    @Test
    fun categorizeModelId_fallbackDoesNotMisclassifyTinyAsYi() {
        assertEquals("其他", ModelIdCategorizer.categorizeModelId("tiny"))
        assertEquals("Yi", ModelIdCategorizer.categorizeModelId(" yi "))
    }
}
