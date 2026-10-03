# Sync review captures

Android debug build (`com.aicfo.app`) on an Android 15 emulator, after the read-only sample link. Home numbers are the locked October board. The freshness line is the only addition.

| File | Screen | What to check |
| --- | --- | --- |
| `home-success.png` | Home | Wealth strip says **Updated just now** |
| `accounts-success.png` | Accounts | Same success line under the title |
| `home-failed.png` | Home | **Couldn't refresh. Last update 1m ago.** and **Try again** |
| `accounts-failed.png` | Accounts | Same failure line and **Try again** |
| `home-needs-reauth.png` | Home | **Reconnect to refresh balances.** and **Reconnect** |
| `accounts-needs-reauth.png` | Accounts | Same re-auth line and **Reconnect** |

Failed and NeedsReauth come from the debug Settings actions **Simulate sync failure** and **Simulate bank reconnect**. Release builds ignore both.

## Unlinked, then simulate

These five are the QA order from `docs/SYNC.md` when nothing is linked yet. They are Compose draws of the debug UI at 1080×2400 (Robolectric native graphics, same phone size as the boards above). Simulate links the read-only sample, then overlays the stale line. The empty state is before either action.

| File | Screen | What to check |
| --- | --- | --- |
| `accounts-unlinked.png` | Accounts | **Nothing linked · Maya Chen** and **Link read-only sample**. No freshness line. |
| `home-simulate-reconnect.png` | Home | **Reconnect to refresh balances. Last update just now.** and tappable **Reconnect** |
| `accounts-simulate-reconnect.png` | Accounts | Same Reconnect line, accounts linked (not the empty copy) |
| `home-simulate-failure.png` | Home | **Couldn't refresh. Last update just now.** and tappable **Try again** |
| `accounts-simulate-failure.png` | Accounts | Same failure line and **Try again** |
