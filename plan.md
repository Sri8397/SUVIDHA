# Plan: user-selectable relay servers (Advanced settings)

Status: planning. A partial draft exists in the working tree (see "Draft state") and does not compile yet.

## Why

- `relay.damus.io` was unreliable in testing (HTTP 522 or timeouts), which caused "some relays did not respond" and "No relay responded" messages. Defaults are now `wss://nos.lol` and `wss://relay.primal.net`.
- A fixed list needs an app release whenever a relay degrades. Letting the user choose removes that dependency, and lets privacy-minded users run their own relay.

## Analysis

**Main risk: relay overlap.** An invitation only arrives if inviter and invitee share at least one relay. The inviter looks up the invitee's key package on the inviter's relays and sends the welcome there. The invitee reads only their own relays. Disjoint lists mean the invite silently never arrives.

Mitigations, in order:
1. Keep the defaults, and let the user add to or edit the list. The default stays the common ground.
2. Say so in the settings screen: friends must share at least one relay.
3. Later: publish the key package to the whole list, and look it up on the union of both people's relays. This needs a per-user relay list (NIP-65, kind 10002) in the invite flow.

**Existing groups.** Each group stores its relay list in its MLS group data when it is created, so existing groups are unaffected. A new list affects future groups, key-package publishing, and the user's own sync.

**Security and privacy.**
- Accept `wss://` only. Never `ws://`.
- Cap the list at 5 relays; each extra relay adds a connection and a failure source.
- Never allow an empty list.
- A relay sees the IP address and sync timing, never the contents.

**Behaviour on change.** Applies at the next sync, no restart. Relays are read from preferences on each call.

## Design

| Piece | Responsibility |
| --- | --- |
| `RelaySettings` | Saved list in the `expense_settings` preferences (key `relay_urls`, newline-separated). Empty means defaults. `normalize()` validates and canonicalises. `add` / `remove` / `reset`. Max 5, min 1. |
| `PeerSplitRelaySync` | Takes a relay provider. `relayUrls()` and all default parameters read from it. `probe(url)` returns round-trip ms or null. |
| `PeerSplitService`, `RefreshService` | Build `PeerSplitRelaySync` with the provider, so the worker, manual refresh and invitations all use the same list. |
| `AppViewModel` | Exposes `relays`, `relaysCustomized`, `addRelay`, `removeRelay`, `resetRelays`, `probeRelay`. |
| `RelaysScreen` | Route `relays`, reached from Profile → Advanced → Relay servers. Add field, per-relay status, Remove, Test connections, Restore defaults. |

Validation rules for `normalize()`: trim, strip trailing `/`, require `wss://`, require a host containing a dot, reject user-info, query and fragment, lowercase the host, max 200 characters.

## Draft state

Written, uncommitted, not compiling:
- New: `RelaySettings.kt`, `ui/RelaysScreen.kt`.
- Edited: `PeerSplitRelaySync.kt`, `PeerSplitService.kt`, `service/RefreshService.kt`, `ui/AppViewModel.kt`, `ui/SuvidhaApp.kt`, `ui/ProfileScreen.kt`.
- Build error: `Muted` and `Danger` are unresolved in `RelaysScreen.kt`. `ui/theme/Color.kt` only has `MutedLight` / `DangerLight` and dark variants, so the screen must use the same theme colors as the other screens.

## Remaining work

1. Fix the two color references in `RelaysScreen.kt`; rebuild.
2. Unit tests for `RelaySettings.normalize` and the add/remove/reset rules (duplicates, limit of 5, last relay cannot be removed, `ws://` rejected).
3. Check the key-package publish path (`publishInviteKey`) and `createGroup` use the configured list.
4. Manual test on a phone, on Indian mobile data and Wi-Fi: add a relay, test connections, remove one, restore defaults, then invite a friend on default relays.
5. Update `architecture.md` to say the relay list is user-configurable, with the defaults.

## Out of scope / later

- NIP-65 relay lists and union lookup for invitations.
- Per-group relay selection.
- Third default relay (`offchain.pub` was flaky under concurrent connections).
- Self-hosted relay guide.

## Open decisions

- Keep the draft and finish it, or revert it and implement from this plan.
- Whether to show the overlap warning only when the list differs from the defaults.
