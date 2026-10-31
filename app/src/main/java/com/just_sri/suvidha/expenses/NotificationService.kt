package com.just_sri.suvidha.expenses

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicInteger

/**
 * Service managing system notifications for Suvidha transactions, sync, and backup.
 */
class NotificationService(private val context: Context) {

    fun notifyExpenseAdded(
        category: String,
        amount: Double,
        currency: String,
        note: String = "",
    ): Boolean = Companion.notifyExpenseAdded(context, category, amount, currency, note)

    fun notifySplitAdded(
        friendName: String,
        amount: Double,
        currency: String,
        note: String = "",
        isLent: Boolean,
    ): Boolean = Companion.notifySplitAdded(context, friendName, amount, currency, note, isLent)

    fun notifyGroupExpenseAdded(
        groupName: String,
        payerName: String,
        amountFormatted: String,
        note: String = "",
    ): Boolean = Companion.notifyGroupExpenseAdded(context, groupName, payerName, amountFormatted, note)

    fun notifySettlement(
        counterpartName: String,
        amountFormatted: String,
        isGroup: Boolean = false,
    ): Boolean = Companion.notifySettlement(context, counterpartName, amountFormatted, isGroup)

    companion object {
        const val TRANSACTION_CHANNEL_ID = "suvidha_transactions_channel"
        private const val TRANSACTION_CHANNEL_NAME = "Transactions"
        const val MESSAGE_CHANNEL_ID = "suvidha_messages_channel"
        private const val MESSAGE_CHANNEL_NAME = "Group messages"

        private val notificationIdCounter = AtomicInteger(2000)

        fun nextNotificationId(): Int = notificationIdCounter.getAndIncrement()

        fun ensureChannels(context: Context) {
            SyncNotificationHelper.ensureChannel(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
                val transactionChannel = NotificationChannel(
                    TRANSACTION_CHANNEL_ID,
                    TRANSACTION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts for new transactions, expenses, and splits"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(transactionChannel)
                val messageChannel = NotificationChannel(
                    MESSAGE_CHANNEL_ID,
                    MESSAGE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Chat messages from members of your shared groups"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(messageChannel)
            }
        }

        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }

        fun notifyExpenseAdded(
            context: Context,
            category: String,
            amount: Double,
            currency: String,
            note: String = "",
        ): Boolean {
            val symbol = currencySymbol(currency)
            val formattedAmount = "$symbol ${money(kotlin.math.abs(amount))}"
            val isIncome = isIncomeCategory(category)
            val title = if (isIncome) "Income Added · $formattedAmount" else "Expense Added · $formattedAmount"
            val message = if (note.isNotBlank()) "$category · $note" else category

            return showTransactionNotification(
                context = context,
                title = title,
                message = message,
                destination = "expenses"
            )
        }

        fun notifySplitAdded(
            context: Context,
            friendName: String,
            amount: Double,
            currency: String,
            note: String = "",
            isLent: Boolean,
        ): Boolean {
            val symbol = currencySymbol(currency)
            val formattedAmount = "$symbol ${money(kotlin.math.abs(amount))}"
            val title = if (isLent) {
                "Split Added · You lent $formattedAmount"
            } else {
                "Split Added · You owe $formattedAmount"
            }
            val message = if (note.isNotBlank()) "$friendName · $note" else "Split with $friendName"

            return showTransactionNotification(
                context = context,
                title = title,
                message = message,
                destination = "friends"
            )
        }

        fun notifyGroupExpenseAdded(
            context: Context,
            groupName: String,
            payerName: String,
            amountFormatted: String,
            note: String = "",
        ): Boolean {
            val title = "New Group Expense · $groupName"
            val message = if (note.isNotBlank()) {
                "$payerName added $amountFormatted for $note"
            } else {
                "$payerName added $amountFormatted"
            }

            return showTransactionNotification(
                context = context,
                title = title,
                message = message,
                destination = "groups"
            )
        }

        fun notifySettlement(
            context: Context,
            counterpartName: String,
            amountFormatted: String,
            isGroup: Boolean = false,
        ): Boolean {
            val title = "Payment Settled"
            val message = "Settled $amountFormatted with $counterpartName"

            return showTransactionNotification(
                context = context,
                title = title,
                message = message,
                destination = if (isGroup) "groups" else "friends"
            )
        }

        /** One notification per group, replaced by the latest message so a busy chat does not flood the shade. */
        fun notifyGroupChatMessage(
            context: Context,
            groupId: String,
            groupName: String,
            senderName: String,
            text: String,
        ): Boolean {
            return showTransactionNotification(
                context = context,
                title = groupName,
                message = "$senderName: $text",
                destination = "groups",
                notificationId = chatNotificationId(groupId),
                channelId = MESSAGE_CHANNEL_ID
            )
        }

        /** Stable per group and kept clear of the incrementing transaction ids. */
        fun chatNotificationId(groupId: String): Int = 1_000_000 + (groupId.hashCode() and 0x3FFFFFFF)

        fun showTransactionNotification(
            context: Context,
            title: String,
            message: String,
            destination: String = "home",
            notificationId: Int = nextNotificationId(),
            channelId: String = TRANSACTION_CHANNEL_ID,
        ): Boolean {
            ensureChannels(context)

            if (!hasNotificationPermission(context)) {
                return false
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("navigate_to", destination)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_transaction_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            return try {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
                true
            } catch (_: SecurityException) {
                false
            }
        }
    }
}

typealias TransactionNotificationService = NotificationService

