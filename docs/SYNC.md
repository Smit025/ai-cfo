# Bank sync

Finwise links accounts **read-only**. The product never moves money and never charges a linked card. Android opens Plaid Link when sandbox keys are present. The Maya sample is a debug-only button, not the Connect path. iOS does not open Link yet.

## Quality bar

1. **Read-only.** `BankLinkSource` has one operation, `fetch`. There is no transfer, payment, or card-charge method. `readOnly` must be true. If it is not, `AiCfoController.refreshAccounts` does not call `fetch` and the UI shows a failure.
2. **Freshness.** Balances and transactions refresh on cold start, when the app returns to the foreground, and on pull-to-refresh (Accounts and Home). `lastSyncedAt` is persisted. Accounts and the Home wealth strip show a relative line such as “Updated 3m ago”. The line ages while the screen is open.
3. **No silent stale.** A failed refresh or an item that needs re-auth does not keep looking current. The line switches to a failure or “Reconnect to refresh balances”, with **Try again** or **Reconnect**. Yesterday’s figures can stay on screen, but they are marked stale.
4. **Idempotent ingest.** Transactions are stored by provider id (`TransactionLedger`). The same id updates that row. It does not insert a second copy. The stub uses fixed ids (`maya-tx-gympass-202610` and the rest).
5. **Token vault.** Link tokens stay in the Android Keystore or the iOS Keychain. Sync metadata (timestamp, error, re-auth flag, transaction rows) lives in the existing prefs store. `refreshAccounts` does not read the vault. Do not log tokens. `SafeLog.redact` still runs on any failure text before it is stored or shown.
6. **One delivery.** `MayaStubBankSource` delivers immediately. Plaid returns from `fetch` and delivers once later, on the same thread. A newer refresh ignores an older delivery.

## What the apps call

`AiCfoController` is the only entry.

| Call | When |
| --- | --- |
| `refreshAccounts(ColdStart)` | Process start, once, when the first `ON_START` is observed |
| `refreshAccounts(Foreground)` | A later return to the foreground, after the app has left it |
| `refreshAccounts(PullToRefresh)` | Pull down on Home or Accounts |
| `refreshAccounts(Manual)` | Connect, **Try again**, and the reconnect path |
| `reconnectBank()` | Clears the re-auth flag and refreshes. Does not move money. |
| `debugSimulateNeedsReauth()` | Debug QA only (Settings → **Simulate bank reconnect**). Release builds ignore it. |
| `debugSimulateSyncFailure()` | Debug QA only (Settings → **Simulate sync failure**). Release builds ignore it. |

`SyncStatus` is `Idle`, `Syncing`, `Success(lastSyncedAt)`, `Failed(reason)`, or `NeedsReauth`. Home and Accounts read `freshnessLabel`, `syncCode`, `syncActionLabel`, and `syncStale` from the shared models. They do not format the relative time themselves.

Prefs keys: `last_synced_at`, `sync_state`, `sync_error`, `sync_needs_reauth`, `synced_accounts`, `synced_transactions`.

Home’s dollar figures stay the locked October board. The freshness line reports the bank link. It does not recompute savings or net worth from the stub balances.

## Android Plaid

`BankLinkSource` stays the seam. `PlaidBankSource` in `androidApp` implements `fetch`. The Plaid Link SDK and the HTTPS calls (`/link/token/create`, `/item/public_token/exchange`, `/accounts/balance/get`, `/transactions/sync`) stay in the Android app. `shared` does not depend on the SDK.

Connect and the Accounts empty state open Plaid Link. A successful Link exchanges the public token, stores the access token in the Android Keystore, and refreshes. Accounts then show that institution’s name, mask, type, balance, and the read-only mark. Transactions are stored by Plaid `transaction_id`, so a second sync updates the row. `ITEM_LOGIN_REQUIRED` is `NeedsReauth`. **Reconnect** opens Link in update mode, then `reconnectBank()`. **Try again** only refreshes. There is no transfer, payment, or card-charge call. `readOnly` stays true.

Home’s October dollar figures stay the authored plan. This change does not recompute net worth from Plaid balances.

Checking and savings are Plaid depository subtypes. A debit card is the card on the checking account — Plaid has no separate debit subtype. Credit cards use the `credit card` subtype. The link token asks only for the `transactions` product and those account filters.

### Sandbox keys

Copy `local.properties.example` to `local.properties` (gitignored) and set:

```
PLAID_CLIENT_ID=your_sandbox_client_id
PLAID_SECRET=your_sandbox_secret
PLAID_ENV=sandbox
```

Leave the id or secret blank and Connect says **Plaid is not configured**. It does not fall back to the Maya sample.

In the Plaid dashboard, allow the Android package name `com.aicfo.app`. Then:

```bash
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Open Connect. In Link, use Plaid’s sandbox institution (First Platypus Bank, user `user_good`, password `pass_good`) and select checking, savings, and a credit card. The access token stays in the Keystore. Do not log it, do not put it in prefs, and do not commit it. `PLAID_SECRET` is compiled into the debug app from `local.properties` so the phone can exchange tokens. That file is gitignored. Do not ship a production secret in the app.

The SDK on this app is `com.plaid.link:sdk-core:5.5.3` (Link `FastOpenPlaidLink`). It matches compileSdk 35.

Debug builds can still **Link Maya sample** from Settings. That label means October demo accounts, not the user’s bank. Release builds ignore it.

Prefs still hold the sync cursor and the ledger. A long history should move `TransactionLedger` to a database and keep `transaction_id` as the primary key.

## iOS

Plaid Link is not in the iOS app. Connect says it is not available yet, and the screen notes that adding it needs a Mac. The button does not pretend a link succeeded and does not load the Maya sample. A debug build can still use **Link Maya sample**.

## Not in this change

No money movement, no iOS Link SDK, and no recomputed Home net worth. Paywall purchases stay simulated.
