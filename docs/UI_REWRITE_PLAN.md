# Native UI rewrite — 30 October 2026

## Source of truth

Use the current HTML, shared styles and interaction scripts in `../design/`, including their latest CSS overrides. Screenshots are secondary references. Rebuild with native Jetpack Compose; do not embed the prototype or copy its sample people, balances or public keys into live data.

## Implementation order

1. Replace the old UI files with a new `ui/` presentation layer. Keep Room schema v4, repositories, encrypted backups, integer shared-money calculations and MLS transport.
2. Build reusable primitives: theme, responsive page shell, header, avatar, connected list, list row, search, segmented control, chips, buttons, fields, confirmation dialog, keypad and amount flow.
3. Rebuild navigation: Home has the wordmark, profile entry, four quick actions and recent friends/groups. All five main screens expose Home, Expenses, Friends, Groups and Profile navigation. Details/forms have their own back navigation.
4. Rebuild Expenses and the shared add-expense flow with period selection, chart, category filters, details and deletion.
5. Rebuild Friends, add-by-public-ID, friend history and local splits. Save public identity links atomically through Room. Remove READ_CONTACTS, the contact picker and every device-address-book action; preserve legacy stored records.
6. Rebuild Groups, group creation, saved-friend invitations, member management, chronological activity, net amounts and shared split/settlement flows. Reuse the ledger and MLS operations through a UI-independent service. Show transport failures and pending writes without discarding local data.
7. Rebuild Profile, currency/timezone/category preferences and backup export/restore with the new primitives. Display the actual device identity and editable local profile name.
8. Verify compilation, unit tests, lint and instrumentation tests on an emulator. Review fresh-install and populated screens at phone size, verify navigation and financial writes, and inspect the merged manifest for contact permission.

## Completion criteria

- No old composables or contact-reading code remain in the app.
- Shared UI patterns live in reusable components, and screens compose those components.
- Existing financial data and migrations are retained; sample design values never become user data.
- The debug APK builds and the critical flows work with no contact permission.
- Record checks and any protocol limitations below before handoff.

## Validation

- Replaced the previous screen/component files, `FlowUi.kt` and chart composables with `ui/` screens and shared primitives. Date/period calculations remain in `ExpensePeriods.kt`.
- Bundled licensed Inter and Manrope fonts for offline rendering.
- `assembleDebug`, 17 unit tests, `lintDebug` and `assembleDebugAndroidTest` pass. Lint also uncovered an existing API-31-only BigInteger conversion; the bounded conversion now works at minSdk 26 and the existing Long.MAX_VALUE allocation test passes.
- Merged manifest contains no READ_CONTACTS permission; no ContactsContract queries or contact-permission requests remain.
- Emulator screenshots reviewed for Home, numeric amount entry and add-friend. Public-ID validation and atomic friend/link persistence pass on device.
- Final UI instrumentation rerun pending after aligning test libraries to the app’s resolved Compose UI 1.9.0 (the previous BOM selected test 1.7.8).
- Physical phone wireless ADB discovery works, but connection to its advertised address returns “No route to host.” No phone app/data was removed or reset.
- The group chat is fully implemented matching the design: free-form text messages and split expense cards are displayed chronologically with date dividers, custom incoming/outgoing bubbles, and split detail dialogues. Messages are encrypted and transmitted peer-to-peer using MLS ratchet keys (RFC 9420) over Nostr relays via `SharedLedgerAction.ChatMessage`. Unit and instrumented wire serialization tests pass.
