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
- **Track Balances:** Clearly see who owes you money and who you owe.
- **Split Expenses:** Split bills with one or more friends easily.
- **One-Tap Settle:** Mark balances as settled once someone pays back.
- **Friend Ledger:** View the complete payment history for any specific friend.

### 3. 🔒 100% Offline & Private
- **Zero Cloud Dependence:** Works anywhere without an internet connection.
- **Instant Performance:** No network lag or syncing errors.
- **Welcome Onboarding:** Sleek introduction to key features on first launch.

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
│   │   │   ├── WelcomeScreen.kt       # Onboarding & feature introduction
│   │   │   ├── ExpenseScreens.kt      # Expenses, Analytics & Settings UI
│   │   │   ├── ExpenseFlowScreen.kt   # Add Expense flow
│   │   │   ├── SplitFlowScreen.kt     # Bill split flow
│   │   │   ├── ContactsScreen.kt      # Contact picker for friends
│   │   │   ├── ExpenseData.kt         # Room entities and DAOs
│   │   │   └── ExpenseUiTheme.kt      # Colors, typography & styling
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
└── README.md
```

---

## 📄 License

This project is personal and open source for learning and development.
