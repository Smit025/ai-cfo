# Bank sync

Finwise links accounts **read-only**. The product never moves money and never charges a linked card. This note is the quality bar for that link, and the seam a live Plaid source plugs into.

The Maya stub is still the data. The sync hooks around it are real.

## Quality bar

1. **Read-only.** `BankLinkSource` has one operation, `fetch`. There is no transfer, payment, or card-charge method. `readOnly` must be true. If it is not, `AiCfoController.refreshAccounts` does not call `fetch` and the UI shows a failure.
2. **Freshness.** Balances and transactions refresh on cold start, when the app returns to the foreground, and on pull-to-refresh (Accounts and Home). `lastSyncedAt` is persisted. Accounts and the Home wealth strip show a relative line such as “Updated 3m ago”. The line ages while the screen is open.
3. **No silent stale.** A failed refresh or an item that needs re-auth does not keep looking current. The line switches to a failure or “Reconnect to refresh balances”, with **Try again** or **Reconnect**. Yesterday’s figures can stay on screen, but they are marked stale.
4. **Idempotent ingest.** Transactions are stored by provider id (`TransactionLedger`). The same id updates that row. It does not insert a second copy. The stub uses fixed ids (`maya-tx-gympass-202610` and the rest).
5. **Token vault.** Link tokens stay in the Android Keystore or the iOS Keychain. Sync metadata (timestamp, error, re-auth flag, transaction rows) lives in the existing prefs store. `refreshAccounts` does not read the vault. Do not log tokens. `SafeLog.redact` still runs on any failure text before it is stored or shown.
6. **Stub now, live source later.** `MayaStubBankSource` delivers immediately. A source may also return from `fetch` and deliver once later, on the same thread. A newer refresh ignores an older delivery.

## What the apps call

`AiCfoController` is the only entry.

| Call | When |
| --- | --- |
| `refreshAccounts(ColdStart)` | Process start, once, when the first `ON_START` is observed |
| `refreshAccounts(Foreground)` | A later return to the foreground, after the app has left it |
| `refreshAccounts(PullToRefresh)` | Pull down on Home or Accounts |
| `refreshAccounts(Manual)` | Connect, **Try again**, and the reconnect path |
| `reconnectBank()` | Clears the re-auth flag and refreshes. Does not move money. |
| `debugSimulateNeedsReauth()` | Debug QA only (Settings → **Simulate bank reconnect**, testTag `qa_simulate_reconnect`). If nothing is linked yet, this links the read-only sample first, then marks NeedsReauth. Release builds ignore it and do not link. |
| `debugSimulateSyncFailure()` | Debug QA only (Settings → **Simulate sync failure**, testTag `qa_simulate_failure`). Same link-if-needed behavior, then Failed. Release builds ignore it and do not link. |

`SyncStatus` is `Idle`, `Syncing`, `Success(lastSyncedAt)`, `Failed(reason)`, or `NeedsReauth`. Home and Accounts read `freshnessLabel`, `syncCode`, `syncActionLabel`, and `syncStale` from the shared models. They do not format the relative time themselves.

The freshness line uses testTag `bank_freshness`. When there is an action, **Try again** or **Reconnect** uses testTag `bank_freshness_action`.

## QA order

An unlinked Accounts screen stays on “Nothing linked” and **Link read-only sample**. Cold start does not auto-link, in debug or release. Freshness, Reconnect, and Try again render only after the sample is linked. Assert them in this order:

1. Finish onboarding. **Skip for now** leaves Accounts unlinked. That empty state is expected until the next step.
2. Link the sample, either by tapping **Link read-only sample** on Accounts, or by using a debug simulate action. Simulate links the stub itself, then overlays a stale state. You do not need a prior tap on Link.
3. After a real link, Home’s wealth strip and Accounts show `bank_freshness` with a relative line such as “Updated just now”.
4. Settings → **Simulate bank reconnect** (`qa_simulate_reconnect`) shows NeedsReauth: “Reconnect to refresh balances” and a tappable **Reconnect** (`bank_freshness_action`) on Home and Accounts. Reconnect clears the flag and refreshes.
5. Settings → **Simulate sync failure** (`qa_simulate_failure`) shows Failed and **Try again** (`bank_freshness_action`) the same way. The line does not stay “Updated …” or the empty unlinked copy. Try again refreshes.

Release builds ignore both simulate actions. They do not link and they do not change a successful sync.

Prefs keys: `last_synced_at`, `sync_state`, `sync_error`, `sync_needs_reauth`, `synced_accounts`, `synced_transactions`.

Home’s dollar figures stay the locked October board. The freshness line reports the bank link. It does not recompute savings or net worth from the stub balances.

## Swapping the stub for Plaid

`LinkProvider` on the market pack (`plaid`, `uae-local-rails`) only names which aggregator a country uses. It does not fetch.

The fetch port is `BankLinkSource`. Implement it in the Android or iOS app (the SDK stays out of `shared`):

```kotlin
class PlaidBankSource(
    private val vault: TokenVault,
) : BankLinkSource {
    override val id: String = "plaid"
    override val readOnly: Boolean = true

    override fun fetch(nowMs: Long, deliver: (BankFetch) -> Unit) {
        // Read the access token from the vault inside this class.
        // Never log it. Never put it in LocalStore.
        // Map Plaid transaction_id -> ProviderTransaction.providerTransactionId.
        // Map ITEM_LOGIN_REQUIRED -> BankFetch.LoginRequired.
        // Deliver exactly once, on the thread that called fetch.
        // Do not add transfer, payment, or processor charge methods.
    }
}
```

Pass that source to the `AiCfoController` constructor that takes a `BankLinkSource`. The state machine, the ledger, and both UIs stay as they are.

When Plaid Link update-mode exists, present it from the **Reconnect** button, then call `reconnectBank()`. Today that button only clears the flag and refreshes, which is the right behavior for the stub.

Prefs are enough for Maya’s three transactions. A live item with a long history should move `TransactionLedger` to a database and keep the provider id as the primary key.

## Not in this change

No Plaid SDK, no Link phone / OTP flow, and no paywall work. The sample institutions are still Maya Chen’s October 2026 accounts.
