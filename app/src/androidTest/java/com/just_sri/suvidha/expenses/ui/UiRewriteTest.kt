package com.just_sri.suvidha.expenses.ui

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.just_sri.suvidha.expenses.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class UiRewriteTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: ExpenseDatabase
    private lateinit var model: AppViewModel
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(app, ExpenseDatabase::class.java).build()
        compose.runOnUiThread { model = AppViewModel(app, db) }
    }
    @After fun close() { db.close() }
    private fun launch(route: String = "home") { compose.setContent { SuvidhaApp(model, route) } }
    private fun snapshot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val file = File(app.getExternalFilesDir(null), "ui-review/$name.png").apply { parentFile!!.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun freshHomeUsesReusableNavigationAndAddsAnExpense() {
        launch()
        compose.onNodeWithText("Quick actions").assertIsDisplayed()
        snapshot("home")
        compose.onNodeWithText("Add expense").performClick()
        compose.onNodeWithText("1").performClick()
        compose.onNodeWithText("2").performClick()
        compose.onNodeWithText("5").performClick()
        snapshot("amount")
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("What’s this for?").performTextInput("Lunch")
        compose.onNodeWithText("Food", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Save & exit", substring = true).performClick()
        compose.waitUntil(10_000) { runBlocking { db.expenses().getAll().size == 1 } }
        val expense = runBlocking { db.expenses().getAll().single() }
        assertEquals("Lunch", expense.description)
        assertEquals("Food", expense.category)
        assertEquals(125.0, expense.amount, 0.001)
        compose.onAllNodesWithText("Expenses").onLast().performClick()
        compose.onNodeWithText("Lunch").performScrollTo().assertIsDisplayed()
        snapshot("expenses")
    }

    @Test fun publicIdFriendValidationAndAtomicSaveNeedNoContactAccess() {
        launch("new-friend")
        compose.onNodeWithText("Add friend").let { /* Header and action share a label. */ }
        compose.onNodeWithText("How you know them").performTextInput("Nikhil")
        compose.onAllNodesWithText("Add friend").onLast().performScrollTo().performClick()
        compose.onNodeWithText("Enter a full 64-character public ID using a–f and 0–9.").assertExists()
        compose.onNodeWithText("Paste their full public ID").performScrollTo().performTextInput("b".repeat(64))
        snapshot("add-friend")
        compose.onAllNodesWithText("Add friend").onLast().performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { db.friends().getAll().size == 1 } }
        runBlocking {
            val friend = db.friends().getAll().single()
            assertEquals("Nikhil", friend.name)
            assertNull(friend.contactId)
            assertNull(friend.phoneNumber)
            assertEquals(friend.id, db.sharedMemberFriendLinks().getAll().single().friendId)
            assertEquals("b".repeat(64), db.sharedMemberFriendLinks().getAll().single().memberId)
        }
        val requested = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse(requested.contains("android.permission.READ_CONTACTS"))
    }

    @Test fun duplicateIdentityCannotCreateAnUnlinkedFriend() = runBlocking {
        val repo = ExpenseRepository(db)
        repo.addFriend("Nikhil", "b".repeat(64), "a".repeat(64))
        assertTrue(runCatching { repo.addFriend("Duplicate", "b".repeat(64), "a".repeat(64)) }.isFailure)
        assertEquals(1, db.friends().getAll().size)
        assertEquals(1, db.sharedMemberFriendLinks().getAll().size)
        assertTrue(runCatching { repo.addFriend("Me", "a".repeat(64), "a".repeat(64)) }.isFailure)
        assertEquals(1, db.friends().getAll().size)
    }

    @Test fun groupAmountsIncludeTheCurrentUsersRealNetBalance() = runBlocking {
        val own = model.peers.publicId()
        val peer = "b".repeat(64)
        val repo = SharedGroupRepository(db)
        repo.createGroup("review-group", "Trip to Goa", "INR", own, "You")
        repo.addInvitedMember("review-group", peer)
        repo.createFriendForMember("review-group", peer, "Nikhil")
        repo.appendLocalEvent(SharedLedgerEvent("e", "review-group", own, emptySet(), 1,
            SharedLedgerAction.ExpenseAdded("INR", listOf(SharedDebtUnit("d", own, peer, 180000))), "Dinner"))
        compose.runOnUiThread { model.updateProfileName("Srikant Agrawal") }
        launch("group/review-group")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Dinner").fetchSemanticsNodes().isNotEmpty() }
        snapshot("group-activity")
        compose.onNodeWithText("Amount", substring = false).performClick()
        compose.onNodeWithText("+₹ 1800.00").assertExists()
        compose.onNodeWithText("−₹ 1800.00").assertExists()
        snapshot("group-amounts")
    }

    @Test fun friendsSummariesDoNotMixCurrencies() = runBlocking {
        val own = model.peers.publicId()
        val peer = "b".repeat(64)
        val repo = SharedGroupRepository(db)
        val friend = model.repository.addFriend("Nikhil", peer, own)
        db.splitTransactions().save(SplitTransaction("p", friend.id, 100.0, "Chai", 1))
        repo.createGroup("dollars", "US trip", "USD", own, "You")
        repo.addInvitedMember("dollars", peer)
        repo.appendLocalEvent(SharedLedgerEvent("e", "dollars", own, emptySet(), 1,
            SharedLedgerAction.ExpenseAdded("USD", listOf(SharedDebtUnit("d", own, peer, 2500)))))
        launch("friends")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Nikhil").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("₹ 100").onFirst().assertExists()
        snapshot("friends")
        compose.onNodeWithText("USD", substring = false).performClick()
        compose.onAllNodesWithText("$ 25").onFirst().assertExists()
        compose.onNodeWithText("₹ 125").assertDoesNotExist()
    }
}
