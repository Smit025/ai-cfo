# Finwise auth — email code and device unlock

Account login and device unlock are two different layers. Biometrics and the PIN never sign someone in.

| Layer | What it is | When it runs |
| --- | --- | --- |
| Account login | Any email, then a one-time code or a magic link | First launch, and again only after Log out, reinstall, or a cleared secure session |
| Device unlock | iOS Face ID / Touch ID, or the device passcode. Android biometrics or a local 6-digit PIN | Every cold start while a session is stored |

A release build always has an unlock path. There is no empty gate and no forever-skip.

Phone number is optional profile data. It is not the session, and it is not required to enter the app. There is no SMS login and no Sign in with Google.

## First launch

Existing onboarding intro, then account auth, then the existing trial step:

1. **Intro** — welcome, actions-not-charts, read-only connect (`OnboardingScreen` / `OnboardingView`, steps 0–2).
2. **Email** — Gmail, Outlook, Apple Mail, or any other address. Continue does not write a session.
3. **Code** — 6 digits. A magic-link token for that same challenge is enough on its own; the Android screen uses the code. Debug builds accept `000000` (and can fill that code) and do not send mail. The session is written to secure storage only after a successful verify.
4. **Unlock setup** — Android: 6-digit PIN, with fingerprint as an alternate when the device has it. iOS: Face ID / Touch ID, with the device passcode as the fallback.
5. **Trial** — the existing “Start free 30-day trial” / “Maybe later” step.
6. **Home**, or the hard paywall if the trial was declined or has ended.

The verified address is also stored for a future monthly savings report. This build does not send that report, and the address is not a second login.

## Returning cold start

```
session present
  → device unlock (biometrics, or PIN / device passcode)
  → paywall if not entitled
  → main shell
```

The email field stays hidden until the account session is gone.

## Shared router

`AppGate.resolve` in `shared/.../auth/AppGate.kt`. `AiCfoController.gate()` is the only router the native apps switch on.

| Order | Condition | Gate |
| --- | --- | --- |
| 1 | Intro not finished (welcome / actions / connect) | `ONBOARDING` |
| 2 | No account session, or the code succeeded but unlock setup is still open | `AUTH` |
| 3 | Trial step not finished | `ONBOARDING` |
| 4 | This process has not unlocked the device | `LOCK` |
| 5 | Not entitled (trial ended, or “Maybe later”) | `PAYWALL` |
| 6 | Otherwise | `APP` (main shell) |

Unlock is checked before the paywall, so a lapsed trial still asks for Face ID or PIN before the price screen.

Debug builds do not lock when the user has not chosen biometrics and has not set a PIN, so emulator QA can reach Home. Release builds lock anyway and, on Android, ask for a PIN if nothing else is configured. `unlockWithoutHardware()` is ignored in release.

## What is stored where

| Data | Store | Cleared by Log out |
| --- | --- | --- |
| `AuthSession` (email, issued time, token) | `SecureStore` — Android Keystore file `aicfo.session.cipher`, iOS Keychain service `com.aicfo.app.session` | Yes |
| Report email (same address, for a future monthly note) | `LocalStore` | No |
| Optional profile phone | `LocalStore` | No |
| Device PIN verifier (salt + SHA-256, not the digits) | Same `SecureStore` | No |
| Biometric toggle, PIN-set flag, passcode fallback | `LocalStore` | No |
| Link tokens | Existing `TokenVault` | No. Disconnect institutions still owns those |
| Trial clock, move status | `LocalStore` | No |

`TokenVault` still accepts only `link_…` tokens. The session does not go through that policy. A stored phone number or report address is not a session. An old phone-OTP record does not decode.

## Email sender

`EmailAuthRepository`:

- `StubEmailAuthRepository` — debug builds. A request succeeds for a valid address and does not send mail. Verify accepts `000000` only after that request. Magic links are not issued. More than five sends in one process returns a rate-limit error.
- `UnconfiguredEmailAuthRepository` — release builds, until a human implements a sender. It never accepts a code or a link. The email button is disabled and the screen says sign-in is not configured.
- Tests pass a fake. The fake is not in the app.

No API key, client secret, or OAuth file is in git. `email.local.properties.example` shows the gitignored file a human would fill in. The app does not read that file.

Before a real code or magic link can be sent, someone has to:

1. Choose a transactional email provider that can deliver to any inbox.
2. Put the provider name and API key in gitignored `email.local.properties` (see the example). Do not commit them.
3. Implement `EmailAuthRepository` so `requestChallenge` sends either a 6-digit code or a magic link, and `verifyCode` / `verifyMagicLink` checks that challenge.
4. Pass that implementation into `AiCfoController`. Leave it unset and release keeps failing closed.

Either the code or the link is enough. This Android build verifies the code. `verifyMagicLink` is on the shared controller for a sender that issues a token; nothing in this repo sends the link or registers an app link.

## Settings

- **Unlock with Face ID** (iOS) / **Unlock with biometrics** (Android). Device unlock only.
- **Phone number** — optional profile field. Skip it and the app still opens.
- **Lock now** — shown when biometrics, a PIN, or the device passcode is configured.
- **Log out** — deletes the account session only. The next open is the email field. It does not restart the 30-day trial, and it does not clear the PIN, biometrics, profile phone, or bank link tokens.

## Screens

Native UI only (Jetpack Compose and SwiftUI). Shared code does not draw these. Android is the build that was verified. iOS calls the same session model; it was not compiled here and does not send mail.

| Screen | Android | iOS |
| --- | --- | --- |
| Email, code | `AuthFlowScreen` | `AuthFlowView` |
| Unlock setup | PIN pad in `AuthFlowScreen` | Face ID enable in `AuthFlowView` |
| Cold unlock | `LockScreen` | `LockView` |
| Settings auth block | `SettingsScreen` | `SettingsView` |

Soft-card tokens stay the app tokens: background `#F4F5F7`, white cards, Inter on Android, accent `#635BFF`, violet **F** mark.

## Older phone boards

`docs/design/auth/MARK-HANDOFF.md` and the PNGs under `docs/design/auth/implemented/` describe the previous phone + OTP boards. They are not the current login. Do not build SMS from them.
