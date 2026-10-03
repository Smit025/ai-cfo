# Auth design references

The current login is an email plus a one-time code or magic link ([docs/AUTH.md](../../AUTH.md)). [MARK-HANDOFF.md](MARK-HANDOFF.md) is the old phone spec.

## Device screenshots (Galaxy S23 FE)

These three PNGs are the full-resolution stills (1080×2340) of the email login on a real phone. Galaxy S23 FE, Android 16, Sauce Labs session [e862d7ed-6c66-4ea1-b3fa-3aab6098d8e1](https://app.saucelabs.com/tests/e862d7ed-6c66-4ea1-b3fa-3aab6098d8e1). The installed app is commit `0ae985b01f401c38ca1457c1bac583575e74e6ed`. They are device screenshots, not Compose captures, and they replace the earlier frames taken from the session recording. iOS was not compiled.

| # | Screen | File |
| --- | --- | --- |
| 1 | Email entry, before a session. "Email me a code" is still on screen | [device/01-email-entry.png](device/01-email-entry.png) |
| 2 | One-time code entry after a valid email. No mail was sent | [device/02-code-entry.png](device/02-code-entry.png) |
| 3 | Cold-start PIN lock with a saved session. Device unlock only. No biometric prompt | [device/03-cold-unlock.png](device/03-cold-unlock.png) |

## Email login (Compose fallback, not these shots)

The PNGs in `email-login/` are an earlier Robolectric draw of the same screens. They are not device screenshots and they are not the shots above. An Android 15 emulator was started on the build machine, but the guest never finished booting, so those files were drawn from Compose instead. Debug copy is visible, including the code `000000`. No mail is sent. They are not iOS.

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
