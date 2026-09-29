# Mark handoff — Finwise auth (phone + OTP)

**Product:** Finwise — Your AI CFO  
**From:** Sofia look pack (Susmit / Elon session + security bar)  
**To:** Mark (eng) — visual direction + session / unlock model; HOLD deep UI until Susmit likes core pack

---

## Two layers (LOCKED)

| Layer | What | When |
|-------|------|------|
| **Account login** | Phone + OTP only (no email/password) | First auth; again **only** after Log out / reinstall / cleared session |
| **Device unlock** | iOS Face ID / Touch ID · Android biometric **or** PIN | Every cold start while session present. **Not** account login. |

**Release bar:** Every build must have an unlock path (bio **or** PIN/passcode). **No empty gate.**

Biometric / PIN = **device unlock only**. They never replace phone+OTP for account identity.

---

## Session model (Rocket Money style)

**Persist logged-in session after first successful Phone → OTP.**

| Launch | What user sees |
|--------|----------------|
| **First launch / no session** | Onboarding (4) → **Phone** → **OTP** → **Email** (reports) → **Biometric / PIN setup** → Trial (04) → Home |
| **Returning cold start (session present)** | **Biometric or PIN unlock only** → Home **or** paywall if trial ended. **Do NOT** show Phone/OTP. |
| **Bio fail / off** | Passcode (iOS) or PIN (Android) fallback → Home/Paywall. Still no Phone/OTP. |
| **Show Phone + OTP again ONLY when** | **Log out**, **reinstall**, or **cleared session** |

### Settings (required)

- **Unlock with Face ID / biometrics** — toggle (device unlock).
- **PIN** (Android) / passcode fallback — always an unlock path.
- **Log out** — clears persisted **account** session; next open → Phone + OTP.

### Soft-launch rules

- Phone **required** (no Skip on production boards).
- Email for monthly savings reports — skippable; **not** a login method.
- Tiny **Debug skip** on Phone mock only — **not** on production boards.
- US-first **+1** · E.164 · 6-digit OTP · ~30s resend · SMS provider **TBD**.

---

## Flow diagrams

```
FIRST LAUNCH (no session)
─────────────────────────
Onboarding 01–03
  → Phone → OTP          ※ account + session persisted after OTP
  → Email (reports)      ※ optional skip; not login
  → Unlock setup         ※ iOS: Face ID enable · Android: bio or PIN
  → Trial 04 → Home      ※ Pro clock per product rule

RETURNING COLD START (session present)
──────────────────────────────────────
App open → Biometric OR PIN unlock → Home / Paywall
         ✗ never Phone/OTP unless session cleared
         ✗ bio/PIN is device unlock, not re-login

RE-AUTH (account)
─────────────────
Settings → Log out → clear session → Phone → OTP → …
Reinstall / cleared keychain → Phone path
```

---

## File index

| Path | Screen |
|------|--------|
| `auth/01-phone.png` | Enter mobile · +1 · empty/disabled Continue |
| `auth/02-otp.png` | 6 boxes · ····1234 · Resend 0:30 · Verify |
| `auth/03-phone-error.png` | Invalid number · calm red inline |
| `auth/04-otp-error.png` | Wrong / expired · Resend available |
| `auth/05-biometric.png` | **Cold-start device unlock** (returning) |
| `auth/06-settings-auth.png` | Biometric toggle + Log out |
| `auth/07-email.png` · `email.png` | Email for monthly savings reports |
| `auth/08-pin-setup.png` · `pin-setup.png` | Android 6-digit PIN setup (fallback) |
| `auth/09-face-id-prompt.png` · `face-id-prompt.png` | iOS Face ID **enable** (first-run setup) |
| `auth/auth-board.png` | Collage: Phone · OTP · Email · Face ID enable · Cold unlock |
| `auth/*.html` + `screenshot.mjs` | `cd auth && node screenshot.mjs` |

---

## States for Mark

| Screen | Happy | Error / empty |
|--------|-------|----------------|
| Phone | Valid E.164 → send OTP | Empty = disabled CTA; invalid/rate-limit = `03` |
| OTP | Verify → **persist session** → Email | Wrong/expired = `04` |
| Email | Save → Unlock setup | Skip OK · not used for auth |
| Face ID enable (iOS) | Enable → system prompt → Trial | “Use device passcode” = still an unlock path |
| PIN setup (Android) | 6-digit × confirm → Trial | Fingerprint alternate; PIN required if no bio |
| Cold unlock | Bio/PIN success → Home/Paywall | Fail → passcode/PIN retry; never OTP |
| Settings | Toggle bio; Log out clears **account** session | — |

---

## Craft tokens (soft-card v1.1)

- Bg `#F4F5F7` · cards white ~22px · soft elevation · Inter · accent `#635BFF`
- Violet **F** squircle mark — **no gold coin**
- Calm danger `#DC2626` / soft `#FEF2F2`

---

## Eng notes

1. Persist account session in secure storage after OTP (Keychain / Keystore). SMS provider TBD.
2. Cold-start router: `session == null` → Phone (or Onboarding); `session != null` → **device unlock gate** (bio/PIN) → Home. **Never** Phone/OTP while session valid.
3. Unlock gate is mandatory for release — if bio unavailable/declined, force PIN (Android) or device passcode (iOS). No empty gate.
4. Email is profile/reports only — do not use as auth identity.
5. Log out = revoke + clear local session → Phone. Bio/PIN settings are local device prefs, cleared or re-prompted as appropriate after re-login.
6. KMP: BiometricPrompt (Android) · LocalAuthentication (iOS); store unlock preference separately from account tokens.
