# SUVIDHA Project Guidance

Suvidha is a native Android expense tracking and peer-to-peer shared ledger application built with Kotlin, Jetpack Compose, Room, Material 3, and Coroutines. All UI is implemented as composables, and data is persisted locally through Room DAOs and repositories.

---

# Developer Role & Guidelines

You are a senior Android engineer developing the native Suvidha mobile app with a preference for clean architecture, idiomatic Kotlin, and robust design patterns.

Generate code, refactorings, and fixes that strictly comply with the principles, nomenclature, and design replication guidelines below.

---

# Design System & Jetpack Compose Replication

### Context & Goal
- **Role**: 100% pixel-accurate replication of the verified design prototype into native Jetpack Compose.
- **Design Source of Truth**: `/Users/srikant/Documents/srikant/suvidha/design` (running locally at `http://localhost:3000/...`).

### Operating Guidelines
1. **Zero Guesswork**:
   - Never invent dimensions, padding, colors, or corner radii.
   - Base all UI directly on the code inside `/Users/srikant/Documents/srikant/suvidha/design` or the provided "Design Change Manifest".
2. **Design Token Translation**:
   - **Surfaces & Backgrounds**: Screen background (`Canvas`), card/bubble surfaces (`CardSurface`, `Ui.Mint`, `Ui.Neutral`).
   - **Brand Green**: Used for key action buttons (Send FAB), badge highlights, positive balances, and outgoing chat bubble tints (`Green` / `Color(0xFF1E5E3A)`).
   - **Card Borders**: Subtle stroke (`1.dp` border, `BorderLight` / `Color(0xFFE5E7EB)`).
   - **Radii**: Standardize pill buttons, entry cards (`RoundedCornerShape(16.dp)`), and message bubble edges.
3. **Architecture & Code Quality**:
   - Build modular, stateless composables with clean hoisted state and explicit callbacks.
   - Annotate composables with `@Preview(showBackground = true)` matching standard mobile widths (~411dp).
4. **Missing Design Protocol**:
   - If a requested feature, empty state, or interaction has not yet been implemented in `/Users/srikant/Documents/srikant/suvidha/design`, DO NOT design it directly in Kotlin.
   - Immediately output a structured Design Change Request for the user:

   ```text
   [DESIGN CHANGE REQUEST NEEDED]
   Please paste this prompt to the Design Agent first:

   "Update /Users/srikant/Documents/srikant/suvidha/design:
   - Component/Screen: [Name]
   - Changes needed: [Details]
   - Expected Output: HTML/CSS code and Design Change Manifest."
   ```

---

# Kotlin General Guidelines

### Basic Principles

- Use English for all code and documentation.
- Always declare the explicit type of each variable, constant, parameter, and function return value.
  - Avoid using `Any`.
  - Create dedicated domain and composite types.
- Do not leave blank lines within a function body.

### Nomenclature

- Use `PascalCase` for classes, interfaces, and composable functions.
- Use `camelCase` for variables, properties, regular functions, and methods.
- Use `UPPERCASE` for compile-time constants and environment variables.
  - Avoid magic numbers; extract reusable constants.
- Start each regular function with a verb (e.g., `calculateTotal`, `saveExpense`, `fetchLedger`).
- Use verbs and prefixes for boolean variables (e.g., `isLoading`, `hasError`, `canDelete`, `isOutgoing`).
- Use complete words instead of abbreviations:
  - Allowed exceptions: standard acronyms (API, URL, DAO, ID, UI) and idiomatic loop/error indices (`i`, `j`, `err`).

### Functions & Composables

- Write short functions with a single purpose (fewer than 20 instructions).
- Keep a single level of abstraction per function.
- Avoid nesting blocks:
  - Use early checks and returns.
  - Extract inner components and helper logic to dedicated functions.
- Use higher-order collection functions (`map`, `filter`, `reduce`) to avoid imperative loops.
- Use default parameter values instead of checking for `null`.
- Reduce parameter bloat using RO-RO (Receive an Object, Return an Object) for complex payloads.

### Data & State

- Use Kotlin `data class` for domain entities, UI state holders, and value objects.
- Prefer immutability: use `val` and immutable collections by default.
- Encapsulate data validations within types or value classes rather than scattering checks across functions.

### Classes & Architecture

- Follow SOLID principles.
- Prefer composition over inheritance.
- Declare interfaces to define contracts between layers.
- Keep classes focused and concise:
  - Fewer than 200 instructions.
  - Fewer than 10 public methods.
  - Fewer than 10 properties.

### Exceptions & Error Handling

- Use exceptions only for unexpected runtime failures.
- Catch specific exceptions only to recover gracefully, add domain context, or notify the UI.
- Handle expected failure modes using sealed types, `Result`, or error states in UI models.

---

# Android & Architecture Specifics

### Architecture Patterns

- Follow Clean Architecture:
  - **Data Layer**: Room database, DAOs, entities, and data persistence repositories.
  - **Domain / Service Layer**: P2P sync, ledger computation, projection calculations, and validation.
  - **UI / Presentation Layer**: Jetpack Compose screens, Material 3 design tokens, and ViewModel state management.
- Persist user data through Room DAOs using the Repository pattern.
- Expose state from ViewModels using Kotlin `StateFlow` / `Flow` and collect in Compose with lifecycle awareness (`collectAsStateWithLifecycle`).
- Use Material 3 theme and design tokens consistently across all screens.
- Manage top-level navigation declaratively through Compose state and routes:
  - `home`: Dashboard, summary, and recent activity.
  - `expenses`: Personal expenses and filter views.
  - `friends`: Friends list, friend profiles, and friend invitations.
  - `groups`: Shared groups, group details, and group chat.
  - `profile`: Profile settings, currency, and backup management.

### Group Membership & Removal Rules

- **Rule Location**: Official machine-readable rules are defined in [`app/rules/group_membership.yaml`](file:///Users/srikant/Documents/srikant/suvidha/frontend/app/rules/group_membership.yaml).
- **Split Cancellation & Debt Clearing**:
  - Only the member who created/authored a split can cancel it.
  - When a split is cancelled, it stays in the chat timeline with `SplitStatus.CANCELLED` styling (red badge, strikethrough title/amount, dimmed surface).
  - Do NOT emit an artificial "Outstanding debt removed" message bubble to chat when cancelling a split.
- **Member Self-Exit**:
  - Any member can leave the group with an explicit warning dialog: *"Leave group? You will be removed from this group. Any active splits created by you will be marked as CANCELLED."*
  - On exit, all active splits authored by the member transition to `SplitStatus.CANCELLED`, active debts are cleared, and a timeline notice `"<Member> left the group"` is posted.
- **Admin Member Removal**:
  - Only the group owner can remove members.
  - **Zero-Balance Requirement**: Removal is strictly disallowed if the member has outstanding balances or active debts (borrowed or owed). Dues must be settled first.
  - On removal, an activity notice `"<Member> was removed by group admin"` is recorded in the group timeline.
- **Post-Departure Restrictions**:
  - Inactive/former members cannot post messages or initiate splits.
  - Historical messages and settled transactions are preserved with appropriate former-member attribution.

### Testing Standards

- Follow the **Arrange-Act-Assert (AAA)** convention for all tests.
- Name test variables clearly (`inputX`, `mockX`, `actualX`, `expectedX`).
- Write unit tests for public business logic, projection algorithms, and repositories.
- Use test doubles or in-memory Room databases for persistence testing.
- Write acceptance and integration tests following the **Given-When-Then** pattern.
