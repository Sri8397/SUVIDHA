# SUVIDHA 💸

[![CI](https://github.com/Sri8397/SUVIDHA/actions/workflows/ci.yml/badge.svg)](https://github.com/Sri8397/SUVIDHA/actions/workflows/ci.yml)
[![Deploy to Play Store (Internal Testing)](https://github.com/Sri8397/SUVIDHA/actions/workflows/deploy-play-store.yml/badge.svg)](https://github.com/Sri8397/SUVIDHA/actions/workflows/deploy-play-store.yml)

**SUVIDHA** is a native Android app for tracking personal expenses and splitting bills with friends and groups. Your personal data lives in a local Room database on your device. Shared groups sync through end-to-end encrypted messages over public Nostr relays, so the app works offline and sends changes when a connection is available.

---

## ✨ Features

### 1. 📊 Expense Tracking
- **Quick logging:** Add an expense with amount, category, date, and note.
- **Save & add another:** Log several expenses in a row without leaving the screen.
- **Expenses and history:** Browse past expenses with filters and period-based views.
- **Categories:** Use the built-in categories or add your own.

### 2. 👥 Friends & Split Bills
- **Add friends by public ID:** No device contacts permission is needed. QR codes are scanned with Google's code scanner.
- **Balances:** See who owes you and who you owe.
- **Split expenses:** Split a bill between you and one or more friends.
- **Settle up:** Record a payment to clear a balance.
- **Friend ledger:** See the full history with a specific friend.

### 3. 🧑‍🤝‍🧑 Shared Groups
- **Create groups and invite members:** Invitations are delivered as encrypted welcome messages.
- **Group splits and payments:** Add splits, record payments, and cancel your own splits. Cancelled splits stay in the timeline, marked as cancelled.
- **Group chat:** Messages in each group, with former members shown with their history.
- **Membership rules:** Owners and members can leave, remove members with zero balance, transfer ownership, or close a group. These rules are defined in [`app/rules/group_membership.yaml`](app/rules/group_membership.yaml) and enforced by the rule engine.
- **Encrypted history:** Group data is encrypted with MLS (Messaging Layer Security) using the Marmot protocol.

### 4. 🔒 Local-First & Private
- **Personal data stays on the device:** Expenses, friends, and categories are stored only in the local Room database.
- **Relays see metadata, not content:** Nostr relays carry encrypted events. They can see timestamps, pubkeys, and routing tags, but not amounts, notes, or chat text.
- **Encrypted backups:** Export an encrypted backup and recover from it on another device.
- **Minimal permissions:** The app asks only for internet access and, on Android 13+, notification permission.

### 5. ⚙️ Customization
- **Currency and timezone:** Choose a currency symbol and use the auto-detected timezone or set your own.
- **Relays:** Use the default relays (`wss://nos.lol`, `wss://relay.primal.net`) or add your own.
- **Themes:** Pick an appearance from the theme selection screen.
- **Profile:** Set your display name, share your public ID or QR code, and manage backups.

---

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/) with Coroutines and Flow
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Local database:** [Room](https://developer.android.com/training/data-storage/room) (SQLite)
- **Background work:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **Group encryption:** [Marmot](https://github.com/vitorpamplona/marmot) (MLS over Nostr), via `marmot-jvm`
- **Transport:** Nostr relays over OkHttp WebSockets
- **Architecture:** MVVM with `StateFlow`, a repository layer, and a declarative rule engine for group workflows

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Ladybug or newer recommended)
- **JDK 17**
- **Android SDK:** compile SDK 37, target SDK 36, minimum SDK 26

### Build

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Sri8397/SUVIDHA.git
   cd SUVIDHA
   ```

2. **Build:**
   - **Debug APK:**
     ```bash
     ./gradlew :app:assembleDebug
     ```
   - **Release app bundle (AAB):** requires the signing variables listed in [Continuous Integration](#-continuous-integration-github-actions).
     ```bash
     ./gradlew :app:bundleRelease
     ```
   - Or open the project in **Android Studio** and click **Run** (▶).

### Verify

Run the same checks as CI:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

Device tests include `com.just_sri.suvidha.expenses.ui.UiRewriteTest` and need a connected device or emulator, so run them with `./gradlew :app:connectedDebugAndroidTest`.

---

## 📂 Project Structure

```text
frontend/
├── app/
│   ├── rules/
│   │   ├── group_membership.yaml       # Group workflow rules (source of truth)
│   │   └── refresh.yaml                # Background refresh rules
│   └── src/main/java/com/just_sri/suvidha/expenses/
│       ├── MainActivity.kt             # Entry point
│       ├── ExpenseData.kt              # Room entities, DAOs and database
│       ├── ExpenseRepository.kt        # Personal expense data access
│       ├── SharedGroupRepository.kt    # Shared group data access
│       ├── SharedLedger.kt             # Ledger events and projections
│       ├── SharedLedgerCodec.kt        # Ledger event serialization
│       ├── PeerSplitService.kt         # MLS groups, ledger mutation, outbox
│       ├── PeerSplitRelaySync.kt       # Relay connections, publish and query
│       ├── PeerSplitSyncScheduler.kt   # WorkManager scheduling
│       ├── BackupService.kt            # Encrypted export and recovery
│       ├── engine/                     # Rule engine: actors, conditions, actions
│       ├── service/                    # GroupMembershipService and RefreshService
│       ├── constants/                  # App constants
│       ├── utils/                      # Helpers
│       └── ui/                         # Compose screens, design system and view model
├── docs/UI_REWRITE_PLAN.md             # UI rewrite plan and verification steps
├── architecture.md                     # Node ↔ relay protocol specification
├── AGENTS.md                           # Contributor and coding guidelines
└── README.md
```

---

## 🧩 Group Membership Rules

Group workflows are modelled as rules in `app/rules/group_membership.yaml`. Each rule declares an actor, conditions, confirmation, actions, and a message. The rule engine checks the actor and conditions before any action runs.

| Rule | Purpose |
|---|---|
| `create_group` | Create a new group |
| `post_message` | Send a chat message (active members only) |
| `add_split` | Add a split (active members only) |
| `record_payment` | Record a payment (active members only) |
| `cancel_split` | Cancel a split. Only its author can cancel it. |
| `delete_debt` | Delete a debt |
| `self_exit` | Leave a group. Requires a settled balance. Owners must transfer ownership or close the group first. |
| `admin_removal` | Owner removes a member. Requires a zero balance. |
| `transfer_ownership` | Owner hands ownership to another active member |
| `close_group` | Owner closes the group. Requires no open transactions. |
| `rename_group` | Owner or admin renames the group |
| `invite_member` | Invite a member |
| `invitation_enabled` / `invitation_disabled` | Turn group invitations on or off |

---

## 🔄 Background Sync

Shared groups use encrypted Nostr relay events with a persistent local outbox.

- **Saving commits locally first.** Saving a group expense, payment, deletion, or chat message writes to the local database and queues delivery without waiting for a relay.
- **Scheduled sync.** WorkManager runs a network-connected sync every 15 minutes, and again when the app resumes.
- **Retries.** Background jobs use exponential backoff, up to five attempts per run. Pending invitations and restored outboxes retry automatically.
- **Single pending sync.** Only one immediate sync and one follow-up can be queued at a time. Foreground polling pauses when the screen is not resumed.
- **Shared lock.** Services, workers, backups, and database resets share one protocol lock, so encrypted state never changes concurrently.

This is eventual sync, not instant delivery. Android may delay periodic work to save power, and force-stopping the app suspends background work until it is opened again. Relay pagination and recovery of history beyond the current query limits are not finished yet.

---

## 🔄 Continuous Integration (GitHub Actions)

Two workflows live in [`.github/workflows/`](.github/workflows/):

| Workflow | Trigger | What it does |
|---|---|---|
| `ci.yml` (CI) | Pull requests, pushes to `main` / `develop`, manual | Runs `assembleDebug`, `testDebugUnitTest`, `lintDebug` and `assembleDebugAndroidTest` on JDK 17. Uploads test and lint reports when a run fails. |
| `deploy-play-store.yml` | Pushes to `develop`, manual | Builds a signed release AAB and uploads it to the Google Play internal-testing track. |

To skip the Play Store deploy for a push, include `[skip deploy]` in the head commit message. CI still runs. To skip everything, use `[skip ci]`.

```bash
git commit -m "docs: update README [skip deploy]"
```

The CI workflow needs no secrets, since it only builds debug variants. The deploy workflow requires these repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, and `PLAY_SERVICE_ACCOUNT_JSON`.

---

## 🧪 Testing

- **Unit tests** cover ledger projections, rule conditions and actions, repositories, and services. Run them with `./gradlew :app:testDebugUnitTest`.
- **Device tests** cover the UI and persistence. Run them with `./gradlew :app:connectedDebugAndroidTest`.
- Tests follow the Arrange-Act-Assert pattern. Acceptance tests follow Given-When-Then.

---

## 📄 License

This project is personal and open source for learning and development.
