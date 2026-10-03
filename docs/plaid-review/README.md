# Plaid Home captures

Compose draws of the debug Home screen (`HomeScreen` plus the bottom pill) at 1080×2400. These are **not** device screenshots, and iOS was not compiled.

An Android 15 emulator was started here (Pixel 6 system image, API 35, 1080×2400). Hardware acceleration did not run the guest: the host kernel faulted in `kvm_arch_vcpu_create` (`kvm_spurious_fault`), and that process stayed idle with adb `offline`. A second boot with `-accel off` did reach `sys.boot_completed`, but System UI and the app were killed or ANR'd before Home could be drawn (`System UI isn't responding`, `com.aicfo.app` failed to complete startup). There is no physical device on this machine. The boards below are the fallback: Robolectric native graphics, software draw of the same composable, same phone size as `docs/sync-review`.

Sandbox client id and secret are not in this environment, so Plaid Link was not opened. The synced and stale boards go through the controller's read-only success path (`completeExternalReadOnlyLink`, then a failed refresh) with a stand-in fetch whose id is `plaid`. The dollars are that fetch, not a live sandbox institution. The layout is the current Home layout. The Maya sample is not used, and it still does not unlock these figures.

| File | Screen | What to check |
| --- | --- | --- |
| `home-before-plaid-sync.png` | Home before any successful Plaid sync | Savings, net worth, needs, wants, and to save are **—**. Runway says **Balances show after a bank sync**. No freshness line. Ranked move cards are still there. |
| `home-after-sync.png` | Home after one successful read-only sync | **$1,000** savings, **$1,300** net worth, needs **$25**, wants **$18**, to save **$457**. Runway says **From your linked accounts**. Freshness says **Updated just now**. Ranked moves stay. |
| `home-stale.png` | Home after that sync fails | The same dollar figures stay. The line is **Couldn't refresh. Last update just now.** with **Try again**. It does not say **Updated**. |

Regenerate when `HOME_CAPTURE_DIR` is set. A normal unit-test run does not write these files.

```bash
HOME_CAPTURE_DIR=docs/plaid-review ./gradlew :androidApp:testDebugUnitTest --tests com.aicfo.app.ui.HomeComposeCaptureTest
```
