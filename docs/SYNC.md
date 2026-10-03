# Bank sync

Finwise links accounts **read-only**. The product never moves money and never charges a linked card. This note is the quality bar for that link, and the seam a live Plaid source plugs into.

Android can open a read-only Plaid **sandbox** Link session. The Maya sample remains a debug QA path. It does not unlock Home figures. See **Plaid sandbox** below.

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

An unlinked Accounts screen stays on “Nothing linked” and **Connect securely**. Cold start does not auto-link, in debug or release. Debug builds also show **Link read-only sample**. That sample is not the default path. Freshness, Reconnect, and Try again render only after something is linked. Assert them in this order:

1. Finish onboarding. **Skip for now** leaves Accounts unlinked. That empty state is expected until the next step.
2. For QA without Plaid keys, link the sample from Accounts → **Link read-only sample**, or use a debug simulate action. Simulate links the stub itself, then overlays a stale state. You do not need a prior tap on Link. The sample does not fill Home’s dollar figures.
3. After a link, Home’s wealth strip and Accounts show `bank_freshness` with a relative line such as “Updated just now”. Home dollar figures stay “—” until a Plaid sandbox sync succeeds.
4. Settings → **Simulate bank reconnect** (`qa_simulate_reconnect`) shows NeedsReauth: “Reconnect to refresh balances” and a tappable **Reconnect** (`bank_freshness_action`) on Home and Accounts. Reconnect clears the flag and refreshes.
5. Settings → **Simulate sync failure** (`qa_simulate_failure`) shows Failed and **Try again** (`bank_freshness_action`) the same way. The line does not stay “Updated …” or the empty unlinked copy. Try again refreshes.

Release builds ignore both simulate actions and **Link read-only sample**. They do not link and they do not change a successful sync. They also do not embed Plaid credentials.

Prefs keys: `last_synced_at`, `sync_state`, `sync_error`, `sync_needs_reauth`, `synced_accounts`, `synced_transactions`.

Home keeps the same layout: savings, net worth, needs / wants / to save, and the ranked move cards. Dollar figures on that strip stay “—” until one successful sync from the Plaid source (`bank_link_kind` = `plaid`, `real_bank_sync` = true). A later failed or stale sync keeps those last figures and marks them not current. Cold start does not invent balances. The Maya sample never sets `real_bank_sync`. Ranked move cards stay the October coach plan. The three Home boards are in `docs/plaid-review`. The empty board is a Galaxy S23 FE device screenshot from Sauce session `12516cda-cff6-4f47-949f-7c9f7f4af563`. The synced and stale boards are Compose captures; those device states were not reached.

## Plaid sandbox

Android Connect opens Plaid Link against `https://sandbox.plaid.com` only. Products requested: `transactions`. No transfer, payment, or auth product. The public token is exchanged in memory. The access token is stored with the Android Keystore vault (`plaid.access_token`). It is not written to `AndroidLocalStore`, logs, or git.

Credentials are not in the repo. Copy `local.properties.example` to `local.properties` (gitignored) and set `PLAID_CLIENT_ID` and `PLAID_SECRET` from the Plaid **Sandbox** secret. In the dashboard, allow Android package `com.aicfo.app`. Rebuild the **debug** app. Release builds compile those fields as empty, so a release APK cannot open Link and does not contain the secret.

If the values are blank, Connect says **Plaid is not configured** and does not fall back to the Maya sample.

After a successful sandbox sync, Home savings is the sum of savings accounts, net worth is cash plus investments minus cards and loans, and needs / wants / left-to-save come from this month’s categorized transactions. Accounts lists the linked rows with a read-only badge.

A Plaid item may stay while the 25-day Pro trial is running, or while `subscribed_plan` is `MONTHLY` or `YEARLY`. That plan flag is a simulated purchase. Play billing is not wired, so a finished trial with no subscription is unpaid. On the next open (`gate`, Home, or Accounts), Finwise deletes the access token from the vault and asks sandbox `POST /item/remove` when credentials are present. Home dollar figures go back to “—”. Connecting again is refused until `purchaseMonthly` or `purchaseYearly` sets that simulated plan. There is no production Plaid application in this change. If sandbox credentials are missing, the local token is still deleted and `/item/remove` is not called.

iOS shares the controller but reports **Not available on iOS yet**. This environment does not compile the iOS app.

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

`PlaidBankSource` in `androidApp` is that source. The state machine and the ledger stay in `shared`. Android passes it to the `AiCfoController` constructor that takes a `BankLinkSource`.

**Reconnect** on a Plaid item opens Link update mode, then `reconnectBank()`. The Maya sample still only clears the flag and refreshes.

Prefs are enough for the sample’s three transactions. A long Plaid history should move `TransactionLedger` to a database and keep the provider id as the primary key.

## Not in this change

No phone OTP, device unlock, paywall, real SMS, monthly savings email, or Play billing work. iOS Link is not implemented. Production Plaid is refused.
