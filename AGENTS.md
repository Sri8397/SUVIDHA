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
  - Any member can leave the group with an explicit warning dialog: *"Leave group? You will be removed from this group. You must settle your balance first."*
  - Leaving is blocked (`own_balance_settled`) while the member owes or is owed any open amount in the group, in either direction.
  - The self-remove is only a request until the group admin commits it. `confirm_self_removal` re-reads the roster and fails the leave with "waiting for the group admin to confirm" while the member is still listed, so the app never reports "left" for a member who is still in the group.
- **Admin Member Removal**:
  - Only the group owner can remove members.
  - **Zero-Balance Requirement**: Removal is strictly disallowed if the member has outstanding balances or active debts (borrowed or owed). Dues must be settled first.
  - On removal, an activity notice `"<Member> was removed by group admin"` is recorded in the group timeline.
- **Owner Exit (Transfer or Close)**:
  - An owner cannot leave while other members remain (`self_exit` denies with `owner_must_transfer_or_close`). The leave action shows a dialog offering **Transfer ownership** or **Close group**.
  - **Transfer ownership** (`transfer_ownership`): the new owner must be another active member; they become the sole MLS admin and the previous owner becomes a regular member who may then leave.
  - **Close group** (`close_group`): owner only, and strictly disallowed while any transaction has an outstanding amount (`no_open_transactions`). It removes every other member, then the owner, then syncs. Former members keep read-only history.
- **Group Rename**: Only an owner/admin can rename. It is relayed to all members as an encrypted `GroupRenamed` ledger event (latest admin rename wins) and sends no notification.
- **Post-Departure Restrictions**:
  - Inactive/former members cannot post messages or initiate splits.
  - Historical messages and settled transactions are preserved with appropriate former-member attribution.

### Rule Engine Workflow (Required for Group Workflows)

Every new group workflow (membership, ownership, closing, invitations, etc.) MUST be modelled with the rule engine, never as ad-hoc logic in a screen or ViewModel.

1. **Define the rule** in [`app/rules/group_membership.yaml`](file:///Users/srikant/Documents/srikant/suvidha/frontend/app/rules/group_membership.yaml): `actor`, `conditions`, `confirm`, `actions`, and `message`. Update this file first; it is the source of truth.
2. **Register the actor, conditions and actions**: the actor (`active_member`, `owner_only`, `user`) is enforced by the engine before any condition (`ConditionRegistry.registerActor`, and `RuleEngine(requireActors = true)` rejects rules whose actor is not registered). Register conditions (guards that return `ValidationResult.Allowed` or `Denied(code, reason)`) and **actions** (`register` / `registerUnit`) in `service/GroupMembershipService.kt`. Every name in the YAML must be registered; the engine validates this at startup (`validateRegistries = true`). Use `declarative(...)` only for steps already covered by another action.
3. **Put protocol work in `PeerSplitService`** (MLS, ledger events, roster); actions only delegate to it. Do NOT repeat permission or validation checks there: the YAML actor and conditions are the single source of those checks. The ledger projection and MLS still reject invalid events from other devices.
4. **Expose it through `AppViewModel`** with `runRule("<rule_id>", groupContext(...))` and call it from the UI via `model.perform(...)`. Screens must never call `model.peers` membership methods directly. Use `canExecuteRule` to enable/disable UI actions or to explain why something is blocked, instead of re-implementing the checks.
5. **Current rules**: `self_exit`, `admin_removal`, `transfer_ownership`, `close_group`, `rename_group`, `invite_member`, `cancel_split`, `delete_debt`, `invitation_enabled`, `invitation_disabled`, `create_group`, `post_message`, `add_split`, `record_payment`. Ledger writes (messages, splits, payments) are rules too, so former members are denied by the `active_member` actor. Do not add placeholder rules or no-op actions: if a rule does not run, delete it.
6. **Document the rule** in the Group Membership section above and add tests for each condition and action.

### Testing Standards

- Follow the **Arrange-Act-Assert (AAA)** convention for all tests.
- Name test variables clearly (`inputX`, `mockX`, `actualX`, `expectedX`).
- Write unit tests for public business logic, projection algorithms, and repositories.
- Use test doubles or in-memory Room databases for persistence testing.
- Write acceptance and integration tests following the **Given-When-Then** pattern.
