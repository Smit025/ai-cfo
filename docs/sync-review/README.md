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
