# SUVIDHA 💸

**SUVIDHA** is a simple, fast, and modern Android app to track your daily expenses and split bills with friends. 

It works completely offline, keeping 100% of your data private and local on your device using a fast Room database.

---

## ✨ Features

### 1. 📊 Expense Tracking
- **Quick Logging:** Add expenses quickly with amount, category, date, and note.
- **Save & Add Another:** Log multiple expenses in a row without leaving the screen.
- **Analytics & History:** View all your past expenses sorted by date, with clear breakdowns.
- **Custom Categories:** Add, remove, or customize categories (Food, Travel, Bills, Shopping, etc.).

### 2. 👥 Split Bills with Friends
- **Public IDs:** Add friends by name and public ID; no device contact permission is required.
- **Track Balances:** Clearly see who owes you money and who you owe.
- **Split Expenses:** Split bills with one or more friends easily.
- **One-Tap Settle:** Mark balances as settled once someone pays back.
- **Friend Ledger:** View the complete payment history for any specific friend.

### 3. 🔒 100% Offline & Private
- **Zero Cloud Dependence:** Works anywhere without an internet connection.
- **Instant Performance:** No network lag or syncing errors.
- **Home:** Quick actions and recent friends and groups, using your actual local data.

### 4. ⚙️ Easy Customization
- **Currency:** Choose your preferred currency symbol (₹, $, €, £, and more).
- **Timezone:** Use auto-detected timezone or set a custom one.
- **Manage Friends & Categories:** Edit your friend list and expense categories anytime.

---

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Local Database:** [Room Database](https://developer.android.com/training/data-storage/room) (SQLite)
- **Architecture:** MVVM, StateFlow, Coroutines, Edge-to-Edge Design

---

## 🚀 How to Run the App Locally

### Prerequisites
- **Android Studio** (Ladybug or newer recommended)
- **JDK 17**
- **Android SDK** (API 35 supported, minimum API 26)

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Sri8397/SUVIDHA.git
   cd SUVIDHA
   ```

2. **Build and install:**
   - **Debug APK:**
     ```bash
     ./gradlew assembleDebug
     ```
   - **Release App Bundle (AAB):**
     ```bash
     ./gradlew :app:bundleRelease
     ```
   - Or open the project in **Android Studio** and click **Run** (▶).

---

## 📂 Project Structure

```text
SUVIDHA/
├── app/
│   ├── src/main/
│   │   ├── java/com/just_sri/suvidha/expenses/
│   │   │   ├── MainActivity.kt        # Main entry point & tab navigation
│   │   │   ├── ui/                   # New Compose screens and reusable design system
│   │   │   ├── PeerSplitService.kt    # Serialized group, sync and ledger operations
│   │   │   ├── BackupService.kt       # Encrypted export and atomic recovery
│   │   │   ├── ExpenseData.kt         # Room entities and DAOs
│   │   │   └── ExpenseUiTheme.kt      # Colors, typography & styling
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
└── README.md
```

---

## 📄 License

This project is personal and open source for learning and development.

## Native UI rewrite

The Compose presentation layer is rebuilt from the HTML reference in `../design/`. Shared components live in `ui/DesignSystem.kt`; all expense and split modes share `ui/MoneyFlow.kt`. Group transport and backup recovery are independent of screen code. See [the implementation and verification plan](docs/UI_REWRITE_PLAN.md).

Verify with `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest`. Device tests include `com.just_sri.suvidha.expenses.ui.UiRewriteTest`.

## Background group sync

Shared groups use encrypted Nostr relay events with a persistent local outbox. WorkManager schedules a network-connected sync every 15 minutes and a catch-up when the app resumes. Saving a group expense, payment, deletion, or chat message commits locally and queues delivery without waiting for a relay. Pending invitations and restored outboxes also retry automatically.

Only one immediate sync and one follow-up can be pending at a time. Foreground group polling waits for each pass and pauses when the screen is no longer resumed. All service instances, workers, backups, and local database resets share the protocol lock so encrypted state cannot be changed concurrently.

Background jobs use exponential backoff, with up to five attempts per run; a later periodic pass can try again. Android may delay periodic work for power management, and force-stopping the app suspends background work until it is reopened. This is eventual sync, not guaranteed instant delivery. Automatic sync stays quiet. Relay pagination and recovery of history beyond the existing query limits remain incomplete.
