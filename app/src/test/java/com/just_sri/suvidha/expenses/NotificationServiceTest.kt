package com.just_sri.suvidha.expenses

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationServiceTest {

    @Test
    fun transactionChannelIdIsConfigured() {
        assertEquals("suvidha_transactions_channel", NotificationService.TRANSACTION_CHANNEL_ID)
    }

    @Test
    fun nextNotificationIdGeneratesUniqueIds() {
        val id1 = NotificationService.nextNotificationId()
        val id2 = NotificationService.nextNotificationId()
        assertTrue(id2 > id1)
    }

    @Test
    fun chatNotificationIdIsStablePerGroupAndOutsideTransactionRange() {
        val id = NotificationService.chatNotificationId("group-a")
        assertEquals(id, NotificationService.chatNotificationId("group-a"))
        assertTrue(id != NotificationService.chatNotificationId("group-b"))
        assertTrue(id >= 1_000_000)
    }

    @Test
    fun typeAliasResolvesCorrectly() {
        assertEquals(NotificationService.TRANSACTION_CHANNEL_ID, TransactionNotificationService.TRANSACTION_CHANNEL_ID)
    }
}

