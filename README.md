# SUVIDHA 💸

**SUVIDHA** is a simple, fast, and modern Android app to track your daily expenses and split bills with friends. 

It works completely offline and automatically backs up your data to the cloud when you sign in with your phone number.

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

### 3. ☁️ Offline First & Cloud Sync
- **Works Offline:** All your data is saved locally on your phone first using a local database.
- **Phone Sign-In:** Simple sign-in using your mobile number and an SMS OTP.
- **Cloud Backup:** Automatically syncs your data with Firebase Firestore so you never lose your records.

### 4. ⚙️ Easy Customization
- **Currency:** Choose your preferred currency symbol (₹, $, €, £, and more).
- **Timezone:** Use auto-detected timezone or set a custom one.
- **Manage Friends & Categories:** Edit your friend list and expense categories anytime.

---

## 📱 Screenshots

| Home & Analytics |
|:---:|
| <img src="suvidha-screenshots/suvidha-20261025-192918.png" width="300" alt="Suvidha App Screenshot"/> |

---

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Local Database:** [Room Database](https://developer.android.com/training/data-storage/room) (SQLite)
- **Backend & Auth:** [Firebase Authentication](https://firebase.google.com/docs/auth) (Phone OTP) & [Cloud Firestore](https://firebase.google.com/docs/firestore)
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

2. **Setup Firebase (Required for Cloud Sync):**
   - Create a project on [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with package name `com.just_sri.suvidha.expenses`.
   - Download the `google-services.json` file and place it inside the `app/` folder.
   - Enable **Phone Authentication** and **Cloud Firestore** in your Firebase project.
   - For detailed instructions and Firestore security rules, see [`FIREBASE_SETUP.md`](FIREBASE_SETUP.md).

3. **Build and install:**
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
│   │   │   ├── ExpenseScreens.kt      # Expenses, Analytics & Settings UI
│   │   │   ├── ExpenseFlowScreen.kt   # Add Expense flow
│   │   │   ├── SplitFlowScreen.kt     # Bill split flow
│   │   │   ├── ExpenseData.kt         # Room entities and DAOs
│   │   │   ├── FirebaseSync.kt        # Cloud sync engine
│   │   │   ├── PhoneSignIn.kt         # OTP phone authentication UI
│   │   │   └── ExpenseUiTheme.kt      # Colors, typography & styling
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── google-services.json
├── docs/                               # Architecture and data model specifications
│   ├── README.md                       # Data architecture overview
│   ├── shared_splits.md                # Peer-to-peer mailbox specification
│   ├── friends.md                      # User friends directory (no stored balance)
│   └── split_transactions.md           # Private ledger (single source of truth)
├── firestore.rules                     # Security rules for Firebase Firestore
└── README.md
```

---

## 🏛️ Architecture & Data Model

For full details on our offline-first data model, check the documentation:
- **[Data Architecture Overview](file:///Users/srikant/Documents/srikant/SUVIDHA/docs/README.md)**
- **[Shared Splits (Peer Mailbox)](file:///Users/srikant/Documents/srikant/SUVIDHA/docs/shared_splits.md)**
- **[Friends Directory](file:///Users/srikant/Documents/srikant/SUVIDHA/docs/friends.md)**
- **[Split Transactions (Private Ledger)](file:///Users/srikant/Documents/srikant/SUVIDHA/docs/split_transactions.md)**

---

## 📄 License

This project is personal and open source for learning and development.
