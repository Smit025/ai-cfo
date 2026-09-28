# Auth design references

Session and unlock rules: [MARK-HANDOFF.md](MARK-HANDOFF.md). Product behavior is summarized in [docs/AUTH.md](../../AUTH.md).

## Implemented Android screens

Captured from Jetpack Compose with Robolectric (`GraphicsMode.NATIVE`, software draw of the activity). There is no emulator in this environment, and `captureToImage` did not receive a draw frame, so these are view draws rather than device screenshots. Debug-only labels (**Debug skip**, **Fill debug code**) are visible. iOS Face ID enable is not captured here.

| File | Screen |
| --- | --- |
| [implemented/01-phone.png](implemented/01-phone.png) | Phone |
| [implemented/02-otp.png](implemented/02-otp.png) | OTP |
| [implemented/07-email.png](implemented/07-email.png) | Email for reports |
| [implemented/08-pin-setup.png](implemented/08-pin-setup.png) | Android PIN setup |
| [implemented/05-cold-unlock.png](implemented/05-cold-unlock.png) | Cold biometric unlock |
| [implemented/06-settings-auth.png](implemented/06-settings-auth.png) | Settings auth |

## Sofia boards

The follow-up named these files: `01-phone.png`, `02-otp.png`, `05-biometric.png`, `06-settings-auth.png`, `07-email.png`, `08-pin-setup.png`, `09-face-id-prompt.png`, `auth-board.png`. The PNG bytes were not present on the workspace filesystem (the handoff markdown was). They are not committed. Error boards `03-phone-error.png` and `04-otp-error.png` were not attached.
