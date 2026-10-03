# Auth design references

The current login is an email plus a one-time code or magic link ([docs/AUTH.md](../../AUTH.md)). The PNGs below are the previous phone + OTP boards. They were not recaptured for the email flow.

Current session rules: [docs/AUTH.md](../../AUTH.md). [MARK-HANDOFF.md](MARK-HANDOFF.md) is the old phone spec.

## Implemented Android screens

Captured from Jetpack Compose with Robolectric (`GraphicsMode.NATIVE`, software draw of the activity). There is no emulator in this environment, and `captureToImage` did not receive a draw frame, so these are view draws rather than device screenshots. Debug-only labels (**Debug skip**, **Fill debug code**) are visible. iOS Face ID enable is not captured here.

Reed’s blocking list, each one a PNG (not a semantics check):

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
