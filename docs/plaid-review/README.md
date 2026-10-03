# Plaid Home captures

iOS was not compiled.

## Empty Home — device frame

`home-before-plaid-sync.png` is a **device frame**, not a Compose capture.

It is from a Galaxy S23 FE on Android 16, Sauce session [12516cda-cff6-4f47-949f-7c9f7f4af563](https://app.saucelabs.com/tests/12516cda-cff6-4f47-949f-7c9f7f4af563), keyed debug APK of `5bd9dc20d019794684b0d2a1ac92447ab646eec0`. The frame is from the scrcpy recording of that session while Home was on screen, before Connect securely (536×1168). The separate full-resolution still was not on disk in this environment.

The screen shows **Hello, Maya**, dashes for savings, net worth, needs, wants, and to save, and **Balances show after a bank sync**. Ranked move cards are still there.

## After sync and stale — Compose captures only

Those two device states were **not reached**. Tapping Connect securely stayed on Accounts. No Plaid Link UI appeared. There are no synced or stale device screenshots.

`home-after-sync.png` and `home-stale.png` are Robolectric Compose draws of `HomeScreen` plus the bottom pill at 1080×2400. An Android 15 emulator was started here earlier (Pixel 6, API 35). Hardware acceleration faulted in `kvm_arch_vcpu_create`, and a software-CPU boot ANR'd System UI before Home could be drawn. The dollars are a stand-in fetch whose id is `plaid`, not a live sandbox institution. The Maya sample is not used.

| File | Screen | What to check |
| --- | --- | --- |
| `home-before-plaid-sync.png` | Device Home before any successful Plaid sync | **Hello, Maya**. Savings, net worth, needs, wants, and to save are dashes. **Balances show after a bank sync**. |
| `home-after-sync.png` | Compose capture after one successful read-only sync | **$1,000** savings, **$1,300** net worth, needs **$25**, wants **$18**, to save **$457**. **From your linked accounts**. **Updated just now**. Not a device screenshot. |
| `home-stale.png` | Compose capture after that sync fails | The same dollar figures stay. **Couldn't refresh. Last update just now.** with **Try again**. Not a device screenshot. |

Regenerate only the Compose boards when `HOME_CAPTURE_DIR` is set. That test does not write the empty Home file. A normal unit-test run does not write files.

```bash
HOME_CAPTURE_DIR=docs/plaid-review ./gradlew :androidApp:testDebugUnitTest --tests com.aicfo.app.ui.HomeComposeCaptureTest
```
