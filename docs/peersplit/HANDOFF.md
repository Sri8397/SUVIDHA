# Saved checkpoint — 30 October 2026

Work stopped at the user's request. Resume from this checkpoint; do not restart the implementation or repeat completed checks without a new reason.

## Implemented and checked

- Normal and preview apps use the same feature sources and separate application IDs. Both latest APKs were installed in place on the physical OnePlus CPH2869. No emulator was used for these checks.
- Room schema v4 has explicit migrations 1 -> 2 -> 3 -> 4 and exported schema files. The normal app's original **27 expenses, 3 friends, and 52 personal split records** survived the upgrades with identical row digests; SQLite integrity checks returned `ok`.
- In-app Nostr identity display/copy, invite-key publication, group creation, public-key invitations, Welcome delivery/acceptance, and joined-group persistence work.
- Encrypted financial messages use Marmot/MLS. Ledger actions and their exact encrypted outbox payload commit together in Room. Welcomes have a separate persistent outbox; pending MLS commit obligations and ingest markers are encrypted on disk.
- Live relay checks exchanged expenses and partial/full payment records between the normal identity (`42d72230…`) and preview identity (`53c69688…`) on the same physical phone. A slow relay no longer cancels the healthy relay's results.
- Shared groups now contain the amount keypad and Equal / Exact / By shares member picker. Amount calculations use integer minor units and deterministic remainder allocation. Equal ₹80.02 -> ₹40.01 each and 2:1 shares -> ₹53.35 / ₹26.67 were inspected on-device. An Exact ₹40.01 split saved successfully.
- A group member can link to an existing friend, a selected phone contact, or a newly named local friend. The synthetic `Relay Test Peer` link survived an app restart and appeared in the shared split picker. The local contact picker opened successfully; no real contact was assigned to the synthetic test identity.
- The Friends screen was renamed and its + split action removed. Existing personal history remains intact. New shared splits are entered inside a group.
- Android back navigation and full-screen flow system-bar styling were checked. Member names/contact details stay outside the relay envelope.

## Latest verification

Build command:

```sh
./gradlew --max-workers=2 :app:assembleDeviceTestDebug :app:assembleStandardDebug :app:testDeviceTestDebugUnitTest :app:assembleDeviceTestDebugAndroidTest
```

Result: **BUILD SUCCESSFUL; 12 unit tests passed**.

Physical device instrumentation:

```sh
adb -s adb-RK4LZTKNYXDM8HIZ-Rw8lFU._adb-tls-connect._tcp shell am instrument -w -r com.just_sri.suvidha.expenses.peersplitpreview.test/androidx.test.runner.AndroidJUnitRunner
```

Latest result: **OK (14 tests)**. Coverage includes all three migrations, friend upsert/link/delete/restore behavior, currency precision, sender/group binding, rejection of fractional/string minor units, causal replay, outbox deduplication, encrypted protocol recovery, and a real two-identity MLS exchange across store recreation.

## Exact resume point

The synthetic group is **RelayCheck_Oct30**. Additional test expense entry occurred while the user was exploring the phone, so compare actual event sets rather than assuming a fixed count from the original script.

The last preview-side UI check recorded payments of **₹38.67 and ₹40.01**, showed **All settled / ₹0.00**, and retained the `Relay Test Peer` link. Its test group then had seven financial events. **The final normal-app relay catch-up and byte/domain event-set comparison were not completed before stopping.** Do not report that final comparison as passed. Pending transport state is durable; use Sync now to catch up and retry if required.

Next useful steps, only when the user resumes:

1. Coordinate an idle period on the phone; the user was actively navigating during earlier ADB checks.
2. Sync the preview and normal test group, compare immutable event IDs/payloads and projected balances, and check duplicate delivery does not add records.
3. Record that final result in the migration/status log. Avoid adding more synthetic financial actions unless needed.

## Remaining planned / incomplete scope

- Relay pagination/cursors and automatic gap repair. Background scheduling/retry is now implemented; see README and ADR-003 for its behavior and limits.
- Member removal/rekeying UI and revoked-member epoch tests.
- Recovery/reinvite for interruption between a successful membership change and queuing the returned Welcome.
- Complete clean-install encrypted-backup restoration with real identity/MLS state; store/format/Room recovery checks pass, but that full UI scenario remains unverified.
- Optional Google Drive AppData backup and release/security hardening.
- A real second-phone / different-network test. The completed live checks used two independent app sandboxes and identities on one OnePlus.

## Local artifacts

- APKs archived during cleanup: `artifacts/apks/suvidha-normal-debug.apk` and `artifacts/apks/suvidha-peersplit-test-debug.apk`; copy checksums are in `artifacts/apks/SHA256SUMS`. These are local debug APKs. Later source edits may require a new build.
- Retained device/build logs, unit-test XML reports and row-digest summaries: `artifacts/verification/`. This local artifacts directory is Git-ignored. Temporary database copies and ADB helper scripts under `/tmp` were removed after retaining the verification records.
- UI captures remain in `suvidha-screenshots/`, including `shared-group-exact-20261030.png`, `shared-linked-member-20261030.png`, and `shared-group-settled-20261030.png`. Some captures precede the final visual polish; retain the source/build as the current implementation.
- Sync notification helper/icon files were also present from concurrent workspace work. They are preserved in the checkpoint; their notification behavior was not verified in this task.

No code was pushed or published.

## Workspace cleanup

Generated `app/build/`, root `build/`, project `.gradle/` and `.kotlin/` caches, and the tracked `.DS_Store` were removed. Latest APKs were copied and checksum-checked before deleting build output. Source code, migration schemas, tests, project/IDE settings, local SDK configuration, database recovery backups and screenshot references were retained. No application source was changed or tests rerun for this file cleanup.

## Background group sync — 30 October 2026

WorkManager background sync, automatic outbox delivery, app-resume catch-up, shared protocol locking, and lifecycle-aware polling are implemented. See README and ADR-003 for timing, retry behavior, and remaining transport limits.

Validation: debug APK build, all 24 local unit tests, lint (no errors), and Android test APK compilation passed. The initial Android run passed 10 worker tests and six existing encryption/backup checks; the scheduler suite initially had a JUnit return-type declaration issue, which was corrected. Emulator testing was then stopped at the user's request before the scheduler rerun completed. No completed emulator scheduler or background process-restart validation is claimed. The temporary emulator and its profile were removed. No physical device was modified.
