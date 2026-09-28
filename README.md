# AI CFO

A US personal CFO **action coach**. The question it answers is “What should I do with my money this month?” — a short ranked list of specific moves, each with a why, the math, and a next step.

This repository is the MVP scaffold: shared Kotlin Multiplatform logic, a native Jetpack Compose Android app, and a native SwiftUI iOS app. The October 2026 sample is Maya Chen, a freelance product designer in Austin.

## Architecture

Native UIs call one shared controller. There is no Compose Multiplatform UI.

```
androidApp/   Jetpack Compose (Material 3 soft-card)
iosApp/       SwiftUI (soft-card)
shared/       KMP — domain, use cases, Maya stub, entitlement, security contracts
```

| Module | What lives here |
| --- | --- |
| `shared/src/commonMain` | Profile, accounts, moves, onboarding, 30-day trial, paywall, QA overrides, redaction, link-token policy, market packs. Entry point: `AiCfoController`. |
| `shared/src/androidMain` | Clock and time-zone actuals (`System.currentTimeMillis`, `java.time`). |
| `shared/src/iosMain` | Clock and time-zone actuals (`NSDate`, `NSTimeZone`). |
| `androidApp` | Compose screens, Android Keystore token vault, biometric prompt, fold / large-width split. |
| `iosApp` | SwiftUI screens, Keychain token vault, Face ID / Touch ID, large-width split. |

`AiCfoController` is constructed by each app with a platform `TokenVault`, `LocalStore`, and optional `LocalStrings`. The default market is the United States. The UIs render the models it returns. They do not reimplement ranking, trial math, or the Maya plan.

Not in this MVP: budgets, charts as the home story, P2P, tax filing, a free-form chat on Home, or live Plaid. Bank linking is a read-only stub. Canada, Europe, and the UAE are config stubs only — the shipped coach plan is still Maya in the US.

## Markets

New countries are a pack swap, not a rewrite. The shipped product is the US pack. `Markets.canada()`, `Markets.europe()`, and `Markets.uae()` exist so the seams are real, and `shipped` is false on those three.

| Seam | US (shipped) | CA / EU / AE (stubs) |
| --- | --- | --- |
| `MarketConfig` | `en-US`, USD, `America/Chicago` (Maya is in Austin) | `en-CA` / CAD / `America/Toronto`; EUR / `Europe/Berlin` (one stand-in zone — a launch would split member states); `en-AE` / AED / `Asia/Dubai` |
| `Money` | Minor units + ISO code. `$47 × 12` is `Money(4700, USD) * 12`, not string math. | Same type. CAD uses `CA$`, EUR uses `€`, AED uses `AED `. |
| `LinkProvider` | `plaid`, read-only, also tagged for CA and EU | UAE uses `uae-local-rails` (local open-banking, not Plaid). No network calls. |
| `RateMarket` | APR (`24.9% APR`) | UAE formats a profit rate. Do not treat APR as universal. |
| `TaxNiche` | `us.estimated-tax`, **off**. Estimated tax stays in TaxVault. | Off. No personal income-tax niche in the UAE stub. |
| `FeatureFlags` | Moves, bank link, investments, consumer debt, HYSA. Tax set-aside off. | UAE turns HYSA off until a savings product is specified. Idle-cash moves hide when that flag is off. |
| Calendar | October 2026 is the plan month in `America/Chicago`. | `MarketCalendar` uses the pack zone. A Dubai midnight can still be September in Chicago. |

Screen sentences for the US boards live in the shared English catalog (`CopyKey`). Android `res/values/strings.xml` and iOS `en.lproj/Localizable.strings` use the same keys (`reg.never_move` → `reg_never_move` on Android). A `values-ar` or `ar.lproj` folder overrides a key; a missing key falls through to the catalog. `AndroidLocalStrings` and `BundleLocalStrings` are the hooks the apps pass into `AiCfoController`.

Prices live on the pack (`Money`), and the paywall labels are formatted from that money. The US prices stay $9.99/month and $79/year.

## Screens

Floating pill nav: **Home · Moves · Accounts · Settings**.

| Screen | Behavior |
| --- | --- |
| Onboarding | Welcome → actions, not charts → read-only connect → 30-day Pro trial. |
| Home | Hello Maya, then a wealth strip (savings + net worth, runway in the foot), a hope line when Gympass is still open or cancelled, Needs · Wants · To save, and up to three priority moves. See all opens Moves. The first card is the top move. No charts, no chat. |
| Moves | October 2026 ranked list with To do / Done / Skipped. |
| Action detail | Why, math, primary CTA, secondary remind / keep. |
| Accounts | Connected institutions with **Read-only** badges. |
| Settings | Profile, notifications, biometric lock, privacy, disconnect, QA tools. |
| Lock | Biometric gate on a cold start when the lock is on. |
| Paywall | Hard stop when the trial is over. $9.99/month or $79/year. No forever-free plan. |

Onboarding follows Sofia’s v1.1 boards (`OnboardingScreen` / `OnboardingView`). Copy lives in the shared controller. **Connect securely** links the read-only Maya sample; **Skip for now** continues without linking. **Start free 30-day trial** starts the clock. **Maybe later** finishes onboarding without a trial, so the hard paywall shows.

## Maya Chen stub

Austin, TX. Freelance product designer. Chase checking ($4,812 at 0.01% APY) and savings, Amex Blue Cash, Capital One Quicksilver ($3,840 at 24.9% APR), Nelnet federal student loan, Fidelity brokerage. Rent $1,850. Spotify and Adobe look active. Gympass does not — last check-in July 5, $47/month, $564/year if cancelled. About $4,200 of checking cash is idle.

Home v1.2 context (the locked board, not a sum of those balances): savings $8,420 (up $340 this month), net worth $42.1k (up 2.1% MoM), 47 days of runway, Needs $2,840 · Wants $620 · To save $890. Keeping Gympass hides the hope line. Cancelling it confirms the $564 stays with her.

October moves, in rank order:

1. Cancel unused Gympass (+$47/mo)
2. Pay $400 extra on Capital One (save about $180, ~4 months faster)
3. Move idle cash to a HYSA (+$18/mo vs 0.01% checking; Ally 4.20% APY in the copy)
4. Pay rent before Oct 1 (already done)
5. Refinance check on Nelnet (skipped — federal protections)

Home opens on the first still-open move. Marking Gympass done or skipped promotes Capital One.

## Trial, paywall, and the QA flip

- **Start free 30-day trial** starts a **30-day full Pro trial**. **Maybe later** does not — the hard paywall is next, so there is no forever-free path.
- When that clock runs out, the app shows a **hard paywall**. There is no free tier after the trial.
- Prices: **$9.99/month** or **$79/year**. Purchase buttons in this build are simulated and mark the account Pro.
- Replaying onboarding does **not** restart the 30 days.

QA controls are in **Settings → QA · trial / paywall** (always on in this scaffold, `Qa.toolsEnabled`):

| Control | Effect |
| --- | --- |
| Show paywall | Force the hard paywall, even inside an active trial. |
| Restore trial | Force the trial presentation (30 days left) without waiting. |
| Simulate Pro | Force a Pro entitlement. |
| Clear QA override | Drop the override and use the real clock / simulated subscription. |
| Replay onboarding | Show the four steps again. The original trial start is kept. |

The paywall itself has **QA: return to trial**, which is the same as Restore trial.

The trial start, QA override, subscription flag, and move statuses persist (Android `SharedPreferences`, iOS `UserDefaults`). Link tokens persist in the Keystore / Keychain. The in-memory unlock flag resets when the process dies, so the biometric gate shows again on the next cold start when the lock is enabled.

## Security foundations

- Linking is read-only. The product never moves money.
- Raw bank passwords are rejected. The vault only accepts `link_…` tokens (`LinkPolicy`).
- Android stores those tokens with an Android Keystore AES-GCM key. iOS stores them in the Keychain (`kSecAttrAccessibleWhenUnlockedThisDeviceOnly`).
- Cleartext HTTP is off (`network_security_config.xml`, `TlsPolicy.cleartextAllowed = false`).
- `TlsPolicy.spkiPins` is the certificate-pinning hook. The pin is a placeholder and must be replaced before a real API host is called. This scaffold makes no network calls and does not request `INTERNET`.
- `SafeLog.redact` strips link tokens, `password=` / `token=` assignments, emails, and 13–19 digit numbers. The logger does not forward the original line.
- Accounts render a Read-only badge. Stored account data is a mask (last four) plus a display balance — no full account numbers.
- Biometric lock gates app open. With no fingerprint or Face ID, the lock screen explains that and offers **Continue without biometrics**. Settings → **Lock now** shows the gate without reinstalling.

## Adaptive layout

Moves + action detail share the screen when there is room:

- Android: width ≥ 600dp, or a **separating vertical fold** reported by `WindowInfoTracker`. The hinge width is left as a gap so content does not sit on the crease (Galaxy Z Fold book posture).
- iOS: width ≥ 720pt (large iPhone landscape, iPad). `Navigation` stays the floating pill; the Moves tab becomes the two-pane layout.

Home, Accounts, and Settings stay a centered phone column on large widths so the cards do not stretch. Lists are short and use static shadows (no live blur) to stay cheap to draw.

## Visual tokens (soft-card v1.1)

| Token | Value |
| --- | --- |
| Accent | `#635BFF` |
| Accent soft | `#EEF0FF` |
| Text | `#0F172A` |
| Muted | `#8B93A7` |
| Background | `#F4F5F7` |
| Cards | `#FFFFFF` |
| Nav active | `#1A1D26` |
| Success / warning / danger | `#059669` / `#D97706` / `#DC2626` |

Cards use about a 24px radius and a soft shadow. Android sets Inter (SIL OFL, see `third_party/inter/OFL.txt`). iOS uses the system sans (SF). The look-pack one-pager is in `docs/DESIGN.md`.

## Run Android

Requirements: JDK 17 or 21, Android SDK 35 (platform + build-tools 35.0.0).

```bash
cp local.properties.example local.properties
# edit sdk.dir, or export ANDROID_HOME

./gradlew :shared:testDebugUnitTest
./gradlew :androidApp:assembleDebug
```

Debug APK:

```
androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Install on a device or emulator:

```bash
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

The Linux agent that produced this scaffold ran `:shared:testDebugUnitTest`, `:androidApp:assembleDebug`, and `:androidApp:bundleRelease`. It did not boot an emulator, so the Compose layout was not exercised on a device here.

## Build an APK or AAB for QA

| Task | Output | Use |
| --- | --- | --- |
| `:androidApp:assembleDebug` | `androidApp/build/outputs/apk/debug/androidApp-debug.apk` | Sideload QA. |
| `:androidApp:assembleRelease` | `androidApp/build/outputs/apk/release/androidApp-release.apk` | Release-shaped APK. |
| `:androidApp:bundleRelease` | `androidApp/build/outputs/bundle/release/androidApp-release.aab` | Play App Bundle. |

`release` is **signed with the debug keystore** so the tasks run without a secret in git. Replace `signingConfig` in `androidApp/build.gradle.kts` with an upload keystore before Play Console. Minify is off.

Version in this scaffold: `versionName` 0.1.0, `versionCode` 1, application id `com.aicfo.app`.

## iOS and TestFlight

iOS archive needs a Mac with Xcode. This Linux environment cannot produce an `.ipa`.

1. Install JDK 17+ and the Android SDK (API 35). The Xcode build phase runs Gradle (`:shared:embedAndSignAppleFrameworkForXcode`) to compile the static `Shared` framework, and that build still configures the Android Gradle plugin.
2. `cp local.properties.example local.properties` and set `sdk.dir`, or export `ANDROID_HOME`.
3. Open `iosApp/AICFO.xcodeproj`.
4. Select the AICFO target → Signing & Capabilities → your Team. Bundle id `com.aicfo.app`.
5. Run on an iPhone simulator or device (deployment target iOS 16). Large-width: rotate a Plus / Max / iPad, or use a wide window.
6. TestFlight: Product → Archive, then Distribute App → App Store Connect → Upload. Create the app record for `com.aicfo.app` in App Store Connect first. Face ID usage text is in `iosApp/AICFO/Info.plist`.

The shared scheme is `AICFO`. User script sandboxing is off so Gradle can write the framework under `shared/build/xcode-frameworks`.

## Tests

`shared/src/commonTest` covers the 30-day cliff, the QA override, “no forever free”, Gympass $47 × 12 = $564, home ranking, cancel / keep, link-token policy, and log redaction.

```bash
./gradlew :shared:testDebugUnitTest
```
