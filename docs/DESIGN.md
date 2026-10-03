# Finwise — Design one-pager

Product name is **Finwise**. The locked brand line is **Finwise — Your AI CFO** (descriptor “Your AI CFO”). Repo and bundle id are unchanged (`ai-cfo`, `com.aicfo.app`). The launcher name stays Finwise.

**Audience:** Sofia (product design) · Susmit review  
**Status:** Look pack **v1.2** — Home IA **locked by Susmit** (wealth context + spend/save snapshot + moves). Soft-card craft still v1.1. Onboarding pack v1.1 stays.  
**Sibling product:** TaxVault = quarterly estimated-tax set-aside only. This product = broader personal CFO **action coach**.

> **Mark note:** Home v1.2 is locked. The wealth strip, hope line, Needs · Wants · To save row, and three priority moves are the Home contract. Onboarding stays the 4-step v1.1 flow. Soft-card tokens are unchanged.

---

## Product one-liner

Answer **“What should I do with my money this month?”** with **specific actions** — not another budgeting / charts / net-worth dashboard.

KMP mobile (Android + iOS), US market first. Read-only connections to banks, cards, loans, investments, salary, rent, insurance. AI surfaces expensive debt, useless subscriptions, cash-flow risk, idle cash, bill timing, refinancing — **each with a concrete next step**.

---

## Job-to-be-done

| When | I want to | So I can |
|------|-----------|----------|
| It’s the start of the month / payday | Know the *few* highest-leverage money moves | Act without building a budget spreadsheet |

**Primary persona (US):** Maya Chen — freelance product designer, Austin TX. Chase checking/savings, Amex + Capital One, Nelnet student loan, rent $1,850, Spotify + Adobe + idle Gympass, ~$4.2k idle in 0.01% checking.

---

## Information architecture

```
Home          → Hello + hopeful wealth strip + hope line + this-month snapshot
                + 3–4 prioritized action cards (action-first still)
Moves         → ranked checklist for the month (To do / Done / Skipped)
Action detail → plain-English why + math + primary CTA + secondary remind/keep
Accounts      → connected institutions with Read-only badges
Settings      → profile, notifications, privacy, disconnect
```

Bottom nav: **Home · Moves · Accounts · Settings** (floating pill)

### Home IA v1.2 (Susmit OK)

Order above the fold:

1. **Hopeful wealth strip** — soft card with **Savings** + **Net worth** (calm green tint when up MoM). Optional hopeful subline. Runway folded in as a thin foot (no separate pulse clutter).
2. **Hope line** (when relevant) — e.g. “If unused Gympass stayed cancelled this year, you’d keep ~$564 more” — hopeful, not guilt.
3. **This month snapshot** — one soft row/card with three quiet amounts: **Needs** · **Wants** · **To save**. No pie charts, no income/expense KPI carousel, no spending line charts.
4. **This month’s moves** — 3 priority action cards + See all. **#1 tap remains the top move CTA** (action-first unchanged).

**Still not a budget app.** Wealth + snapshot are calm context so moves feel grounded — not the product hero. Rejected: pie/donut, income/expense KPI carousel, spending line charts, Cash Flow Health gauges, P2P quick-pay, card carousels.

**Action-first IA is unchanged.** Soft-card v1.1 craft is unchanged.

---

## Design tokens — v1.1 soft-card craft (kept in v1.2)

| Token | Value |
|-------|-------|
| Accent (CTAs / links) | `#635BFF` |
| Accent soft | `#EEF0FF` |
| Text | `#0F172A` |
| Muted | `#8B93A7` |
| App background | `#F4F5F7` |
| Card surface | `#FFFFFF` |
| Soft shadow | multi-layer soft elevation (no hard Stripe borders) |
| Nav active pill | soft black `#1A1D26` |
| Success | `#059669` |
| Warning | `#D97706` |
| Danger | `#DC2626` |
| Font | Inter / SF-like sans |
| Radius | ~20–24px cards, pill nav, 44px phone bezel |
| Frame | ~390×844 content, iPhone-sized |

**v1.1 craft (from Susmit refs):** Soft white cards on light gray, airy spacing, large corner radii, floating pill bottom nav with dark active pill. Premium soft elevation — not flat border-only.

**Rejected from refs (IA):** Total Income/Expenses KPI hero, spending line charts, Cash Flow Health gauges, Quick Payment / P2P avatars, Top Up / virtual card carousel.

---

## Screen map (look pack)

| File | Screen | Purpose |
|------|--------|---------|
| `ai-cfo-home.png` | Home **v1.2** | Hello Maya · wealth strip · hope line · Needs/Wants/To save · priority moves |
| `ai-cfo-moves.png` | Moves | Ranked October 2026 checklist for Maya |
| `ai-cfo-action-debt.png` | Action · debt | 24.9% APR card — math + Schedule / Remind |
| `ai-cfo-action-sub.png` | Action · sub | Unused Gympass — last used, cancel / keep |
| `ai-cfo-accounts.png` | Accounts | Read-only connected accounts strip |
| `ai-cfo-board.png` | Collage | Home + Moves + Debt side by side |
| `onboarding/01-welcome.png` | Onboarding · Welcome | Brand moment · tagline · Continue |
| `onboarding/02-what-we-do.png` | Onboarding · Value | Actions-not-charts · 3 example moves |
| `onboarding/03-connect.png` | Onboarding · Connect | Read-only trust · Bank/Cards/Loans/Investments |
| `onboarding/04-trial.png` | Onboarding · Trial | 25-day Pro · no charge today · paywall after |
| `onboarding/onboarding-board.png` | Onboarding collage | All 4 onboarding screens side by side |

HTML sources in this folder (`home.html`, etc.) can be re-shot via `node screenshot.mjs`.
Onboarding HTML + PNGs live in `onboarding/`; re-shot via `cd onboarding && node screenshot.mjs`.

---

## Onboarding (v1.1 look pack)

**Flow (4 screens, implementable for Mark):**

```
01 Welcome  → brand + sharp line + Continue
02 Value    → actions not charts (example moves mini-cards) + Continue
03 Connect  → Read-only trust + account-type chips + Connect securely / Skip
04 Trial    → 25-day Pro included list + Start trial / Maybe later
```

**Copy notes (locked tone — Maya / calm premium)**

| Screen | Primary copy | CTA |
|--------|--------------|-----|
| Welcome | Mark is a **violet F squircle** (the gold coin on the board is rejected). Brand lines **FINWISE** / **Your AI CFO** (tagline Finwise — Your AI CFO). “Your money, what to do next” | Continue |
| Value | “We tell you what to do this month” · explicit: we don’t lead with budgets/charts/net-worth | Continue |
| Connect | “We never move money without you” · **Read-only** badge · generic chips (Bank · Cards · Loans · Investments) — no trademarked logos | Connect securely · Skip for now |
| Trial | Full moves · unlimited actions · all accounts · why+math · **No charge today · Cancel anytime · Then paywall** | Start free 25-day trial · Maybe later |

**Monetization:** 25-day full Pro trial → then paywall. Do not imply free forever on Maybe later; keep secondary simple.

**Trust:** Connect is read-only aggregation (Plaid-class implication without naming Plaid unless already in product). Never auto-move money.

**Craft:** Same soft-card v1.1 tokens as core look pack (`#F4F5F7`, white ~22px cards, soft elevation, Inter, `#635BFF` CTAs). No bottom nav on onboarding screens — progress dots only.

---

## Do / Don’t

**Do**
- Lead with **actions** and concrete CTAs (“Cancel · save $47/mo”)
- Show calm wealth context + Needs/Wants/To save as quiet grounding (v1.2 Home)
- Use hope framing (“you’d keep ~$X more”), not guilt
- Show plain-English *why* + simple math ($X extra → save $Y)
- Mark every connected account **Read-only**
- Keep priority badges sparse (P1 / P2 / P3)
- Soft elevated cards + floating pill nav; `#635BFF` for CTAs

**Don’t**
- Hero pie / donut / rainbow charts or Cash Flow Health gauges
- Income/expense KPI carousel or spending line charts on Home
- Dense Workday-style admin tables
- Net-worth / income-expense dashboards as the *home story* (wealth strip is context, not hero)
- “Budget categories” as the primary IA
- Auto-move money (product is coach + deep links / schedules user controls)

---

## Mark HOLD note

> Home v1.2 is signed off. Wealth, hope, and the Needs · Wants · To save snapshot are in the app as calm context above the moves.  
> Next pass can add interaction, empty, and error states for those blocks. Onboarding flow mocks stay in `onboarding/`.

---

## Differentiator vs TaxVault

| | TaxVault | Finwise |
|--|----------|--------|
| Scope | Quarterly estimated-tax set-aside | Full-month personal CFO actions |
| Hero | Tax buffer progress | Prioritized moves with CTAs |
| Success | “Am I set aside for the quarter?” | “What should I do with my money *this month*?” |
