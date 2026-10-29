package com.just_sri.suvidha.expenses

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vitorpamplona.marmot.GroupEventResult
import com.vitorpamplona.marmot.WelcomeResult
import com.vitorpamplona.marmot.client.MarmotManager
import com.vitorpamplona.marmot.client.MarmotPublisher
import com.vitorpamplona.marmot.mip01Groups.MarmotGroupData
import com.vitorpamplona.quartz.marmot.mip02Welcome.WelcomeEvent
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.crypto.KeyPair
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl
import com.vitorpamplona.quartz.nip01Core.signers.NostrSignerInternal
import com.vitorpamplona.quartz.nip59Giftwrap.seals.SealEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Real MLS/key-store integration; transport ACKs are simulated and no public relay is contacted. */
@RunWith(AndroidJUnit4::class)
class PeerSplitMlsRoundTripTest {
    @Test
    fun twoIdentitiesJoinExchangeLedgerMessagesAndKeepSenderRatchetsAcrossRestart() = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(base.noBackupFilesDir, "test-mls-${UUID.randomUUID()}").apply { mkdirs() }
        fun isolated(name: String) = object : ContextWrapper(base) {
            override fun getNoBackupFilesDir(): File = File(directory, name).apply { mkdirs() }
        }
        val aliceContext = isolated("alice")
        val bobContext = isolated("bob")
        val aliceSigner = NostrSignerInternal(KeyPair())
        val bobSigner = NostrSignerInternal(KeyPair())
        fun manager(context: Context, signer: NostrSignerInternal): MarmotManager {
            val store = EncryptedMlsGroupStateStore(context)
            return MarmotManager(signer, store, keyPackageStore = EncryptedKeyPackageBundleStore(context),
                publisher = MarmotPublisher { _, _ -> true }, publishObligationStore = store.transportStore,
                ingestDedupStore = store.transportStore.ingestDedup)
        }
        val groupId = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "")
        val relays = listOf(NormalizedRelayUrl("wss://relay.damus.io"))
        try {
            var alice = manager(aliceContext, aliceSigner)
            var bob = manager(bobContext, bobSigner)
            val keyPackage = bob.generateKeyPackageEvent(relays, "test", false)
            bob.confirmKeyPackagePublished(keyPackage)
            alice.createGroup(groupId, MarmotGroupData(nostrGroupId = groupId, name = "Test", description = "suvidha.currency=INR", adminPubkeys = listOf(aliceSigner.pubKey), relays = relays.map { it.url }))
            val delivery = alice.addMember(groupId, keyPackage, relays).second ?: error("Missing Welcome")

            // Restore the recipient before consuming the Welcome: private KeyPackages must survive.
            bob = manager(bobContext, bobSigner)
            bob.restoreAll()
            val seal = delivery.giftWrapEvent.unwrapOrNull(bobSigner) as SealEvent
            val welcome = seal.unsealed(seal.unsealRumorThrowing(bobSigner)) as WelcomeEvent
            assertTrue(bob.processWelcome(welcome) is WelcomeResult.Joined)
            assertEquals(2, bob.memberCount(groupId))

            alice = manager(aliceContext, aliceSigner)
            alice.restoreAll()
            val expense = SharedLedgerEvent("expense", groupId, aliceSigner.pubKey, emptySet(), 10,
                SharedLedgerAction.ExpenseAdded("INR", listOf(SharedDebtUnit("debt", aliceSigner.pubKey, bobSigner.pubKey, 1250))), "Private test lunch")
            val encrypted = PeerSplitLedgerWire.encrypt(alice, expense)
            assertFalse(encrypted.content.contains(expense.description))
            val received = bob.processGroupEvent(encrypted) as GroupEventResult.ApplicationMessage
            assertEquals(expense, PeerSplitLedgerWire.decode(Event.fromJson(received.innerEventJson).content, groupId, aliceSigner.pubKey))

            val settlement = SharedLedgerEvent("settlement", groupId, bobSigner.pubKey, setOf("expense"), 11,
                SharedLedgerAction.SettlementRecorded(listOf(SharedSettlementAllocation("debt", 250))), "Payment")
            val encryptedSettlement = PeerSplitLedgerWire.encrypt(bob, settlement)
            alice = manager(aliceContext, aliceSigner)
            alice.restoreAll()
            val receivedSettlement = alice.processGroupEvent(encryptedSettlement) as GroupEventResult.ApplicationMessage
            assertEquals(settlement, PeerSplitLedgerWire.decode(Event.fromJson(receivedSettlement.innerEventJson).content, groupId, bobSigner.pubKey))

            // Bob still holds the receiver ratchet used for Alice's first message. Reusing
            // Alice's old sender generation after restore would fail this decryption.
            val secondExpense = expense.copy(eventId = "second", parentEventIds = setOf("expense", "settlement"),
                action = SharedLedgerAction.ExpenseAdded("INR", listOf(SharedDebtUnit("debt2", aliceSigner.pubKey, bobSigner.pubKey, 200))))
            val second = bob.processGroupEvent(PeerSplitLedgerWire.encrypt(alice, secondExpense)) as GroupEventResult.ApplicationMessage
            assertEquals(secondExpense, PeerSplitLedgerWire.decode(Event.fromJson(second.innerEventJson).content, groupId, aliceSigner.pubKey))
            assertEquals(-1200L, projectSharedLedger(groupId, listOf(secondExpense, settlement, expense), "INR").balanceByMemberId[bobSigner.pubKey])
        } finally { directory.deleteRecursively() }
    }
}
