package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.AccountType
import org.junit.Assert.assertEquals
import org.junit.Test

class AITransactionEntityResolverTest {

    @Test
    fun inferAccountType_mapsPaymentChannelsAndCustomCashAccounts() {
        assertEquals(AccountType.WECHAT, AITransactionEntityResolver.inferAccountType("微信"))
        assertEquals(AccountType.ALIPAY, AITransactionEntityResolver.inferAccountType("支付宝"))
        assertEquals(AccountType.BANK, AITransactionEntityResolver.inferAccountType("银行代发"))
        assertEquals(AccountType.CASH, AITransactionEntityResolver.inferAccountType("现金"))
        assertEquals(AccountType.CASH, AITransactionEntityResolver.inferAccountType("私房钱"))
    }
}
