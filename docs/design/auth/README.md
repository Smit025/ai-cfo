# Auth design references

The current login is an email plus a one-time code or magic link ([docs/AUTH.md](../../AUTH.md)). [MARK-HANDOFF.md](MARK-HANDOFF.md) is the old phone spec.

## Email login (Compose fallback)

These three PNGs are the email login. They are **not** device screenshots. An Android 15 emulator was started on this machine (KVM was available), but the guest never finished booting and `adb` stayed offline, so nothing was captured from a running Android UI. Each file is a Jetpack Compose draw of the real `AuthFlowScreen` / `LockScreen` through Robolectric (`GraphicsMode.NATIVE`, software draw of the activity window). Debug copy is visible, including the code `000000`. No mail is sent. iOS was not compiled, and these boards are not iOS.

| # | Screen | File |
| --- | --- | --- |
| 1 | Email entry, before a session exists | [email-login/01-email-entry.png](email-login/01-email-entry.png) |
| 2 | One-time code entry, after a valid email was submitted and before a session exists | [email-login/02-code-entry.png](email-login/02-code-entry.png) |
| 3 | Signed-in lock on a cold start with a saved session. This capture has no biometric hardware, so the lock is the PIN pad | [email-login/03-cold-unlock.png](email-login/03-cold-unlock.png) |

## Previous phone + OTP boards

The PNGs in `implemented/` are the old phone flow. They are not this login.

## Old phone boards (not this login)

These files are the previous phone + OTP flow. Debug-only labels (**Debug skip**, **Fill debug code**) are from that flow. Do not present them as the email login.

Reed’s earlier phone list, each one a PNG:

| # | Required | File |
| --- | --- | --- |
| 1 | Phone | [implemented/01-phone.png](implemented/01-phone.png) |
| 2 | OTP | [implemented/02-otp.png](implemented/02-otp.png) |
| 3 | Email | [implemented/07-email.png](implemented/07-email.png) |
| 4 | Unlock setup (Android PIN) | [implemented/08-pin-setup.png](implemented/08-pin-setup.png) |
| 5 | Cold unlock | [implemented/05-cold-unlock.png](implemented/05-cold-unlock.png) |
| 6 | Settings logout | [implemented/06-settings-auth.png](implemented/06-settings-auth.png) |

## Sofia boards

The follow-up named these files: `01-phone.png`, `02-otp.png`, `05-biometric.png`, `06-settings-auth.png`, `07-email.png`, `08-pin-setup.png`, `09-face-id-prompt.png`, `auth-board.png`. The PNG bytes were not present on the workspace filesystem (the handoff markdown was). They are not committed. Error boards `03-phone-error.png` and `04-otp-error.png` were not attached.
