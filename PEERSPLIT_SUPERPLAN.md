# SUVIDHA PeerSplit Superplan

## Purpose

Evolve SUVIDHA from a single-device, offline expense splitter into a private, recoverable, multi-device sharing app without sacrificing the data users already have. Deliver value in small, verifiable steps. Keep Room as the local source of truth; make networking and cloud backup optional capabilities around it.

## Product decisions to lock before network code

1. **What does shared mean?** Initial target: one person creates a group and invites participants; every participant has their own device and identity. Friend/contact records already in SUVIDHA are local address-book-like records, not authenticated participants.
2. **Who may author shared actions?** A shared expense or settlement must identify its author and carry enough source data for every member to reproduce the same balance. No client sends an absolute balance.
3. **What happens to current data?** Existing personal expenses, friends, and splits remain local and retain their current behavior. Shared groups are a new domain; importing existing friend splits into a group is an explicit user action.
4. **What is the first release promise?** Recommended: encrypted invite-based group sync and manual encrypted export/import first. Do not promise automatic cross-device recovery until account linking, restore, key recovery, and delete/uninstall behavior are all proven.
5. **What metadata may relays see?** Encryption can hide payload contents, but relays still see transport metadata such as event times, sizes, connections, and any unencrypted tags. Do not claim “zero knowledge” or metadata privacy without a concrete protocol and threat model.

## Current foundation and gaps

- The Android project uses Kotlin, Compose, coroutines, and Room. `ExpenseData.kt` stores expenses, friends, and per-friend split transactions.
- `MainActivity.kt` currently calculates balances from unsettled rows and persists settlement by changing the original split row to `isSettled = true`. This works for one local device, but a shared replicated model should represent settlement as a separate immutable action rather than overwriting the earlier fact.
- The database is version 1, has no shared-group, participant, event-log, or sync-checkpoint tables, and currently uses destructive migration fallback after making a backup copy. Any sync rollout needs explicit migrations and a tested preservation/rollback path.
- The build has no networking, cryptography, WorkManager, Google sign-in/Drive, or serialization dependencies today.
- Keep any local categorization or ad-targeting feature outside the shared event contract. A local model may consume locally derived repository events, but its inputs, outputs, identifiers, and telemetry must never enter shared events, the outbox, relay filters, or sync diagnostics. The sync worker must not invoke the ad/categorization engine.

## Recommended target architecture

```text
Compose UI
  -> ViewModels / use cases
    -> Room repositories (authoritative local reads and writes)
      -> Local domain tables + append-only shared event log + projections
      -> SyncCoordinator (outbox, relay adapter, inbox, retry/checkpoint)
      -> BackupCoordinator (separate, optional encrypted export/Drive adapter)
```

All user actions commit to Room first. A committed shared action also writes an outbox record in the same Room transaction. Background sync only transports events; it never becomes the authority for what the UI displays. Incoming events are validated and deduplicated before they affect projections. Keep relay, identity, cryptography, and backup behind interfaces so no screen or DAO depends on a specific provider.

### Data model direction

Add new tables rather than changing the meaning of existing personal records:

- `shared_groups`: stable group ID, local display metadata, current epoch/key reference, created time, and lifecycle status.
- `group_members`: group ID, member public key, role/status, key epoch, and invite/acceptance metadata.
- `shared_events`: immutable event ID, group ID, author key, schema version, event type, canonical payload bytes, signature/envelope fields, parent/dependency references, received/created times, and validation status.
- `sync_outbox`: event ID, relay target/state, attempts, next retry time, last error class.
- `sync_cursors`: per group and relay cursor plus last successful reconciliation time. Timestamps are hints, never the only deduplication or correctness mechanism.
- `group_balances` or equivalent projection: derived state that can always be rebuilt from accepted events.

If local TF-IDF/vector categorization or ad targeting is developed, isolate it behind a local-only repository interface and local-only storage/projection. Do not use the shared event bus as its input surface. Keep local feature records out of backup/sync exports unless the user explicitly chooses a separately documented local backup option. Redact local model inputs and outputs from crash and sync logs.

Use integer minor currency units (for example, paise) for shared amounts, not `Double`. Include an ISO currency code and reject incompatible group currencies unless conversion is explicitly designed. Give each event a globally unique ID and schema version. Make DAO writes, event insertion, and projection updates transactional.

### Event semantics and conflict policy

Use domain actions such as `ExpenseAdded`, `ExpenseEdited` (references the prior expense/version), `ExpenseVoided`, `SettlementRecorded` (references the exact expense/debt items settled), `MemberInvited`, `MemberAccepted`, and `MemberRemoved` (with an effective key epoch). Preserve the original actions; corrections are new actions.

Do not adopt a generic “CRDT” label as a substitute for rules. Define invariants for each event type and deterministic ordering for causally related actions. Independent expense additions commute. Duplicate deliveries are idempotent. A settlement consumes only specifically identified unsettled items; two concurrent settlements cannot consume the same item twice. Conflicting edits to the same expense need a clear rule (recommended: retain both versions as a conflict requiring resolution, or use an explicit edit/revision protocol), never last-arrival-wins based on wall clock. Unknown schema versions remain stored but unapplied until supported.

## Protocol and security corrections

- Nostr NIP-01 event identities and signatures use Schnorr on secp256k1, not Ed25519. If SUVIDHA uses Nostr events, implement the Nostr key/signature requirements as specified and keep Android Keystore protections for local secrets where possible. Avoid claiming that an Ed25519 key is directly a Nostr identity.
- NIP-44 is a versioned encryption format for keypair-based communication; it is not a group shared-key protocol. Its current version uses secp256k1 ECDH and ChaCha20/HMAC, and NIP-44 explicitly leaves group messaging/event kinds to other protocols. Do not combine “NIP-44” with a custom AES group envelope and call the result standard NIP-44.
- Before choosing transport, prototype a group protocol with membership changes and key rotation. Options include adopting a maintained Nostr group protocol/library, or specifying a deliberately small custom encrypted group envelope with independent cryptographic review. Do not invent cryptographic primitives or key distribution.
- A QR/invite containing the group secret is a bearer credential: anyone who gets it can read (and perhaps write) group data. Make invitations short-lived and single-use where feasible, show a clear warning, and provide membership revocation with key rotation. Never put private keys or raw group keys in logs, analytics, crash reports, clipboard history, or ordinary preferences.
- Model shared encryption keys as **epochs**. On membership removal, create a new epoch key and distribute it only to remaining members over the selected authenticated key-distribution protocol. Every event identifies its epoch; retain prior epoch key references needed to decrypt and verify historical events for remaining members. A removed member may retain data and keys already received, so rotation prevents access to future events but cannot make the member forget past plaintext. Test that no post-removal event is encrypted to or accepted from the removed member.
- A signed event proves control of a signing key, not that the author is an authorized active group member. Validate membership, group epoch, event type, payload limits, signature, encryption authentication, dependencies, and replay/deduplication before applying it.
- Relay timestamps may be coarse sync hints, but use event IDs and per-group event ancestry/checkpoints for deduplication and gap detection. Allow multiple relays and partial success; a relay is an untrusted mailbox, not durable storage.

## Delivery roadmap

### Phase 0 — Scope, invariants, and threat model

Write down the participant model, author permissions, data visibility, supported group size, supported currency, membership removal behavior, expected retention, and what happens after reinstall/uninstall. Decide whether interoperable Nostr events are a real requirement or merely one possible transport. Threat-model lost phones, stolen invite links, compromised member keys, malicious relays, replayed events, corrupted backups, and account loss.

**Exit:** product decisions and event/balance invariants are documented; protocol choice has named libraries and license review; no protocol-specific schema is committed prematurely.

### Phase 1 — Local domain foundation (no networking)

Move balance calculation and write operations out of `MainActivity` into repository/use-case boundaries. Introduce shared-group data classes, integer money, immutable action semantics, and deterministic balance projection. Keep existing friend ledger behavior intact. Enable Room schema export now and commit the version 1 schema snapshot. Before adding shared tables, implement and verify an explicit `Migration(1, 2)` against a copy of a real version 1 database; increment versions and add explicit migrations for each later schema change. Remove destructive fallback as a normal upgrade path, because it can silently discard user data when a migration is missing. Keep the existing pre-migration database backup as an additional recovery measure, not a replacement for migrations.

**Exit:** a version 1 install upgrades through the explicit migration with all existing rows preserved; the exported schema is committed and reviewed; a shared group can be created and replayed locally; projections rebuild deterministically; malformed or duplicate actions cannot corrupt balances.

### Phase 2 — Local event log and recovery export

Persist every shared action and its derived projection atomically. Build a versioned encrypted export/import format containing only the app's required records, with a user-chosen recovery secret processed using a vetted password KDF and authenticated encryption. Include integrity/version checks, atomic restore into a staging database, and a preview before replacing current data. Keep export/import manual and test across app versions.

**Exit:** restore on a clean install reproduces groups, keys, history, and balances; wrong passwords and truncated/tampered files fail safely; restore never silently overwrites current data.

### Phase 3 — Identity, membership, and local invite UX

Implement the selected identity scheme behind an interface, secure key lifecycle, group creation, invitation creation/acceptance, membership state, and key epochs. Removing a member advances the group epoch, rotates the shared encryption key, and distributes the new key only to remaining members. Keep historical epoch keys available to authorized remaining members so old events remain readable; never re-encrypt history as if the removed member could be made to forget plaintext already seen. QR payloads should avoid leaking more than needed, expire, and be redacted from app logs. Add UI for identity backup, member list, invite expiry, and lost-device recovery. Exercise group removal and rotation before any real transport.

**Exit:** two app instances can join a group in a controlled local/dev flow; unauthorized, expired, replayed, and wrong-epoch actions are rejected; revocation prevents future decryption after rotation.

### Phase 4 — Relay sync MVP

Build a transport interface and one relay adapter. Publish committed outbox events, retry with bounded exponential backoff, handle acknowledgements/timeouts, and periodically fetch since a cursor with overlap plus ID deduplication. Verify and decrypt before Room ingestion. Persist inbox checkpoints only after a successful transaction. Support reconnect catch-up and relay failure independently; expose simple sync state without blocking local use. Use WorkManager for deferrable persistent sync and user-visible foreground work only when the operation truly needs it; background scheduling is best-effort under Android constraints, not a real-time guarantee.

**Exit:** airplane mode, app process death, duplicate delivery, out-of-order delivery, one relay outage, slow network, and event gap all converge without duplicate or lost local actions. User can see pending/failed sync and retry.

### Phase 5 — Multi-relay anti-entropy and conflict handling

Add a second relay and reconcile event IDs/causal heads across sources. Fetch missing dependencies and quarantine invalid or unsupported events. Define bounded pagination and payload limits. Add explicit UX for genuine same-record conflicts. Prefer relay-based reconciliation first; direct device-to-device gossip adds NAT, background execution, discovery, and privacy complexity and should wait for a demonstrated need.

**Exit:** pruning or a missed subscription is repaired when another configured source has the event; missing ancestors are detected; concurrent scenarios produce documented outcomes and balance invariants hold.

### Phase 6 — Optional Google Drive backup

Treat Drive as a separate backup adapter, not as the sync source of truth. Use the narrow `drive.appdata` scope only after confirming its recovery tradeoffs: appData is hidden and app-specific, but is deleted when the app is uninstalled from the user's Drive and cannot be shared like normal files. Use client-side encryption with a recovery secret that is not stored beside the backup. Maintain numbered/versioned snapshots and an atomic manifest or equivalent so an interrupted upload cannot replace the last good backup. Provide last backup time, account status, restore preview, and manual backup/restore controls before scheduling periodic work.

**Exit:** upload interruption and API quota/auth failures preserve the previous valid snapshot; fresh-device restore works with only Drive access and the recovery secret; account disconnection and deleted appData are handled honestly.

### Phase 7 — Harden, release, and operate

Review cryptographic implementation and protocol interoperability with an independent reviewer before describing the product as end-to-end encrypted. Add structured redacted diagnostics, database/event schema compatibility policy, event size/rate limits, key rotation and compromise procedures, privacy copy, and a migration/rollback release checklist. Validate older databases and large histories on real devices; document relay configuration, backup boundaries, and known limitations.

**Exit:** release criteria are met, recovery steps are usable by non-developers, and every security/privacy claim maps to a tested property.

## Risk register and mitigations

| Risk | Why it matters | Mitigation / gate |
|---|---|---|
| Cryptographic protocol mismatch | Nostr signatures, identities, and encryption are not interchangeable with the PDF's Ed25519/AES group sketch. | Select maintained implementation and protocol before implementing transport; security review before production. |
| Irrecoverable identity loss | Local-only private keys can make a group inaccessible after device loss. | Recovery export first; make recovery key setup explicit; never promise automatic recovery without a tested path. |
| Unsafe migrations | Current database opening permits destructive fallback after backup. | Export and commit schema snapshots; write and validate explicit per-version Room migrations against real version 1 data; retain pre-upgrade backup and test recovery; avoid destructive fallback for production upgrades. |
| Incorrect settlements/concurrency | Absolute balance overwrites or mutable settled flags do not merge reliably. | Immutable settlement references and invariant-based deterministic projection. |
| Relay pruning / unavailable peers | Relay storage is not guaranteed and phones are rarely simultaneously reachable. | Multiple relays plus export/backup; do not depend on direct P2P for recovery. |
| Privacy overclaim or local feature leakage | Encryption does not hide all relay metadata; local ad/categorization inputs can become sensitive if they cross the event/outbox boundary. | Document metadata exposure; isolate local categorization from shared events and transport; redact diagnostics; avoid “zero knowledge” claims. |
| Background sync expectations | Android may defer constrained work; periodic work is not immediate. | UI shows sync status; foreground sync on app use; WorkManager only for eventual retry. |
| Scope and dependency creep | Current app has none of the proposed network/security/backup stack. | Deliver phases independently; keep optional adapters out of the local core. |

## First implementation slice

Start with Phase 0 and Phase 1 only: document the shared-event and settlement invariants, then extract balance/write logic from `MainActivity` and build an additive, local-only group/event model with deterministic projections. Keep the app usable as it is today. This creates a stable base for a later transport decision and exposes data-model mistakes before networking makes them expensive.

## Reference material

- Source analyses: `PeerSplit Architecture Analysis and Implementation Plan.pdf` and `PeerSplit Sync Logic, Network Issues, and Solutions.pdf` (user-provided documents).
- Nostr NIP-01: https://github.com/nostr-protocol/nips/blob/master/01.md
- Nostr NIP-44: https://github.com/nostr-protocol/nips/blob/master/44.md
- Google Drive appData: https://developers.google.com/workspace/drive/api/guides/appdata
- Android WorkManager: https://developer.android.com/reference/androidx/work/WorkManager
