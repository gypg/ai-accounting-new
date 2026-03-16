package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AICategoryInferrerTest {

    private val inferrer = AICategoryInferrer()

    @Test
    fun inferCategory_matchesSubCategoryFirst() {
        val result = inferrer.inferCategory("中午吃火锅", TransactionType.EXPENSE)
        assertEquals("火锅", result)
    }

    @Test
    fun inferCategory_matchesParentCategory() {
        val result = inferrer.inferCategory("淘宝买了日用品", TransactionType.EXPENSE)
        assertEquals("购物", result)
    }

    @Test
    fun parseSubCategoryIntent_inXBelowCreateY() {
        val (parent, child) = inferrer.parseSubCategoryIntent("在餐饮下面创建火锅")!!
        assertEquals("餐饮", parent)
        assertEquals("火锅", child)
    }

    @Test
    fun parseCategoryName_createsValidName() {
        assertEquals("通勤", inferrer.parseCategoryName("创建通勤分类"))
    }

    @Test
    fun parseCategoryName_rejectsLongName() {
        assertNull(inferrer.parseCategoryName("创建一个特别特别特别特别特别长的分类"))
    }
}
