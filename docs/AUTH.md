# Finwise auth — session and device unlock

Account login and device unlock are two different layers. Biometrics and the PIN never sign someone in.

| Layer | What it is | When it runs |
| --- | --- | --- |
| Account login | Phone + OTP only | First launch, and again only after Log out, reinstall, or a cleared secure session |
| Device unlock | iOS Face ID / Touch ID, or the device passcode. Android biometrics or a local 6-digit PIN | Every cold start while a session is stored |

A release build always has an unlock path. There is no empty gate and no forever-skip.

## First launch

Existing onboarding intro, then account auth, then the existing trial step:

1. **Intro** — welcome, actions-not-charts, read-only connect (`OnboardingScreen` / `OnboardingView`, steps 0–2).
2. **Phone** — US `+1`, E.164. Continue stays disabled until 10 digits. Invalid numbers show a calm inline error.
3. **OTP** — 6 digits. Debug builds accept `000000` (and can fill that code). The session is written to secure storage only after a successful verify.
4. **Email** — monthly savings report. Skip is allowed. The address is not an account identity.
5. **Unlock setup** — Android: 6-digit PIN, with fingerprint as an alternate when the device has it. iOS: Face ID / Touch ID, with the device passcode as the fallback.
6. **Trial** — the existing “Start free 30-day trial” / “Maybe later” step.
7. **Home**, or the hard paywall if the trial was declined or has ended.

## Returning cold start

```
session present
  → device unlock (biometrics, or PIN / device passcode)
  → paywall if not entitled
  → main shell
```

Phone and OTP stay hidden until the account session is gone.

## Shared router

`AppGate.resolve` in `shared/.../auth/AppGate.kt`. `AiCfoController.gate()` is the only router the native apps switch on.

| Order | Condition | Gate |
| --- | --- | --- |
| 1 | Intro not finished (welcome / actions / connect) | `ONBOARDING` |
| 2 | No account session, or OTP succeeded but email / unlock setup is still open | `AUTH` |
| 3 | Trial step not finished | `ONBOARDING` |
| 4 | This process has not unlocked the device | `LOCK` |
| 5 | Not entitled (trial ended, or “Maybe later”) | `PAYWALL` |
| 6 | Otherwise | `APP` (main shell) |

Unlock is checked before the paywall, so a lapsed trial still asks for Face ID or PIN before the price screen.

Debug builds do not lock when the user has not chosen biometrics and has not set a PIN, so emulator QA can reach Home. Release builds lock anyway and, on Android, ask for a PIN if nothing else is configured. `unlockWithoutHardware()` is ignored in release.

## What is stored where

| Data | Store | Cleared by Log out |
| --- | --- | --- |
| `AuthSession` (`phoneE164`, issued time, token) | `SecureStore` — Android Keystore file `aicfo.session.cipher`, iOS Keychain service `com.aicfo.app.session` | Yes |
| Device PIN verifier (salt + SHA-256, not the digits) | Same `SecureStore` | Yes |
| Biometric toggle, PIN-set flag, passcode fallback, report email | `LocalStore` (prefs / UserDefaults) | Yes, so the next sign-in asks again |
| Link tokens | Existing `TokenVault` | No. Disconnect institutions still owns those |
| Trial clock, move status | `LocalStore` | No |

`TokenVault` still accepts only `link_…` tokens. The session does not go through that policy.

## OTP provider

`OtpAuthRepository`:

- `StubOtpAuthRepository` — debug builds. Sends succeed for a valid US number. Verify accepts `000000`. More than five sends in one process returns a rate-limit error.
- `UnconfiguredOtpAuthRepository` — release builds, until Firebase Phone Auth or Twilio Verify implements the same interface. It never accepts a code. No provider keys ship in this scaffold.

Debug skip on the phone screen persists a session and continues to email. It is not in the release UI, and `debugSkipPhone()` / `debugCompleteUnlockSetup()` no-op when `debugBuild` is false.

## Settings

- **Unlock with Face ID** (iOS) / **Unlock with biometrics** (Android). Device unlock only.
- **Lock now** — shown when biometrics, a PIN, or the device passcode is configured.
- **Log out** — deletes the account session and the local PIN. The next open is Phone. It does not restart the 30-day trial.

## Screens

Native UI only (Jetpack Compose and SwiftUI). Shared code does not draw these.

| Screen | Android | iOS |
| --- | --- | --- |
| Phone, OTP, email | `AuthFlowScreen` | `AuthFlowView` |
| Unlock setup | PIN pad in `AuthFlowScreen` | Face ID enable in `AuthFlowView` |
| Cold unlock | `LockScreen` | `LockView` |
| Settings auth block | `SettingsScreen` | `SettingsView` |

Soft-card tokens stay the app tokens: background `#F4F5F7`, white cards, Inter on Android, accent `#635BFF`, violet **F** mark.

## Design references

The session rules are in [`docs/design/auth/MARK-HANDOFF.md`](design/auth/MARK-HANDOFF.md).

Robolectric drew the Android screens below (no emulator on this machine). They are the debug build, so Phone shows **Debug skip** and OTP shows **Fill debug code**. iOS Face ID enable is SwiftUI only and is not in these captures.

| Screen | Implemented |
| --- | --- |
| Phone | [`docs/design/auth/implemented/01-phone.png`](design/auth/implemented/01-phone.png) |
| OTP | [`docs/design/auth/implemented/02-otp.png`](design/auth/implemented/02-otp.png) |
| Email | [`docs/design/auth/implemented/07-email.png`](design/auth/implemented/07-email.png) |
| PIN setup | [`docs/design/auth/implemented/08-pin-setup.png`](design/auth/implemented/08-pin-setup.png) |
| Cold unlock | [`docs/design/auth/implemented/05-cold-unlock.png`](design/auth/implemented/05-cold-unlock.png) |
| Settings auth | [`docs/design/auth/implemented/06-settings-auth.png`](design/auth/implemented/06-settings-auth.png) |

Sofia’s source boards (phone, OTP, email, PIN setup, Face ID enable, cold unlock, settings, collage) were attached to the review follow-up, but the PNG bytes were not on disk in this workspace — only the handoff markdown uploaded. Those originals are not in the tree. The handoff’s error states (`03-phone-error`, `04-otp-error`) were not in the attachment set.
