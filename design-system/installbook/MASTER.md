# InstallBook — Design System (Master)

Source of truth for every screen. A page file in `pages/` overrides this one where
they disagree; otherwise this file wins.

Generated with the **UI/UX Pro Max** skill
(`python3 .claude/skills/ui-ux-pro-max/scripts/search.py "<query>" --design-system --variance 3 --motion 2 --density 6 -p "InstallBook" --persist`)
and then reconciled by hand. The skill's own contract says to verify the returned
category and top result before using it, to retry once when the result is
off-topic, and never to persist unverified output — so the rows below record
which search each decision came from and, where a generated suggestion was
rejected, why.

---

## 1. Product

| | |
|---|---|
| **Type** | Professional tool. A field register, used one-handed on a phone. |
| **User** | An HVAC or plumbing installer, alone or with one employee. Not an office. |
| **Context** | A boiler room, a roof, a van, at the end of a job. Bright sun or bad light, dirty hands, patchy signal. |
| **Stack** | Laravel 12 + Blade, hand-written CSS, no Node, no build step (`--stack laravel`). |
| **Second audience** | The customer, who sees exactly one page: the warranty card. |

## 2. Style — Minimalism & Swiss Style

`--domain style` → `minimalism-and-swiss-style` (active). Its **Best For** row reads
"Enterprise apps, dashboards, documentation sites, SaaS platforms, professional
tools", and its accessibility risk is the lowest in the table. Its own design-system
variables are the ones applied here: `--border-radius: 0px`, `--shadow: none`,
grid-based layout, a single accent colour.

Read plainly: **this product is a logbook, so it is set like a well-printed form.**
Hairline rules, sharp corners, no shadows, no gradients, type carrying the
hierarchy. The pleasure, such as it is, comes from a serial number and a date being
legible at arm's length.

> **Rejected.** The first `--design-system` run returned *Claymorphism* with a pink
> palette, and the second returned the *Trust & Authority + Conversion* landing-page
> pattern with a Playfair Display body face. Both are verifiable rows in the
> database and both are wrong for this product: soft toy-like 3D for a trade tool,
> and an editorial serif for a screen that is mostly dates and part numbers. The
> skill's instruction to retry once and then judge the fit is what produced the
> Swiss result, which is the one used.

## 3. Colour tokens

`--domain color` → the **Invoice & Billing Tool** palette ("Navy professional + paid
green"). It fits for the same reason it fits an invoice: this is a record of work
done, and the only two things in it worth celebrating are "covered" and "serviced".

| Role | Light | Dark | Used for |
|---|---|---|---|
| `--primary` | `#1e3a5f` | `#9fc0e8` | Brand, primary buttons, the rule on an emphasised panel |
| `--on-primary` | `#ffffff` | `#0b1420` | Text on primary |
| `--primary-tint` | `#e8edf4` | `#1a2433` | Selected type, info tag |
| `--link` | `#1d4ed8` | `#8ab4f8` | Links, focus ring |
| `--bg` / `--surface` | `#f4f6f8` / `#ffffff` | `#0e1116` / `#161a21` | Page, panels |
| `--surface-sunken` | `#eef1f4` | `#1e232c` | Message drafts, glyph wells, checkbox blocks |
| `--ink` / `--ink-muted` / `--ink-faint` | `#0f172a` / `#475569` / `#5b6878` | `#e9edf2` / `#a5b0bd` / `#8c98a6` | Body / secondary / micro-labels |
| `--line` / `--line-strong` | `#d8dee5` / `#b6c0cb` | `#2a313b` / `#3d4756` | Rules, input borders |
| `--ok` | `#046c4e` on `#e3f3ec` | `#5fd4a0` on `#13291f` | Covered, done, sent |
| `--warn` | `#92400e` on `#fdf1e0` | `#f0b45e` on `#2b2011` | Due soon, no consent |
| `--danger` | `#b42318` on `#fdeceb` | `#f28b82` on `#2d1616` | Overdue, window closed, erase |
| `--whatsapp` | `#1f8a4c` | `#4cd07d` | Only the button that opens WhatsApp |

Dark mode is a **separate palette, not an inversion**: the tints are desaturated and
each status colour is lifted until it clears 4.5:1 on its own tint. Every foreground
and background pair in both themes was checked against 4.5:1, which is why
`--ink-faint` is `#5b6878` and not the usual `#64748b` — the lighter value falls to
4.2:1 on the warning tint, where the registration panel puts a micro-label.

**No screen defines a colour.** `tests/Unit/InterfaceDisciplineTest.php` fails the
build on a raw hex in a Blade template.

## 4. Typography — Minimal Swiss

`--domain typography` → **Minimal Swiss**: Inter alone, hierarchy from weight, "Best
For: Dashboards, admin panels, documentation, enterprise apps, design systems".
Paired with **JetBrains Mono** for every serial, date, count and price, because
tabular figures stop a column of dates from dancing and stop a changing badge from
shoving the layout sideways (`number-tabular`).

Scale, a fifth apart: **11 · 13 · 15 · 16 · 17 · 21 · 27 · 34**. Body never below
16px on a phone. Micro-labels are 11px, uppercase, `letter-spacing: .09em`, and are
the only place small type is allowed.

Both faces load with `display=swap` behind a complete system fallback: this app is
used in basements, and text waiting for a font it will never receive is worse than
text in the wrong font.

## 5. Spacing, grid and radius

- 4px rhythm: `--s-1` 4 → `--s-8` 64 (`--density 6`).
- `--radius: 0`. Everything is rectangular, including status tags.
- `--rule: 1px` hairline, `--rule-accent: 3px` for the coloured edge of a panel.
- `--tap: 48px` floor on every control, enforced by test.
- Page 880px, narrow 480px, card 560px. Mobile first at 375 / 420 / 560 / 640 / 720.

## 6. Icons — Phosphor, regular weight

`--domain icons` recommends Phosphor. There is no build step here, so the ~97 glyphs
actually referenced are copied from `@phosphor-icons/core` (MIT) into
`resources/icons.php` as raw path data and rendered by `<x-icon name="…" />`.
Regenerate with `scripts/build-icons.py`.

**Emoji are banned and the ban is tested.** They are drawn by whatever font the
device carries, render at a size nobody chose, differ between Android and iOS, and
cannot take a colour from a token — so the glyph that has to mean "gas" or "overdue"
could not be relied on to look like anything.

Decorative by default (`aria-hidden`), because nearly every icon sits beside its own
visible word; `:label` turns one into an image with a name when it is carrying the
meaning alone.

## 7. Motion — `--motion 2`, subtle

Colour, background and border transitions at 120ms on a shared easing token. A 1px
press on buttons. Nothing animates on arrival: nothing here is worth waiting to
read. `prefers-reduced-motion: reduce` switches all of it off, and that is tested.

## 8. Rules this interface keeps

Drawn from the skill's Quick Reference, priorities 1–5 first.

- **Accessibility.** Skip link; focus ring never removed (3px, offset); `aria-current`
  on the active tab and filter; headings in order; a count announced as a sentence
  (`role="status"`), never as a bare number; status meaning carried by icon + word
  before colour.
- **Touch.** 48px minimum, 8px apart. No hover-only affordance.
- **Forms** (`--stack laravel`). A visible label on every field; `old()` repopulation;
  the error under the field it belongs to, tied with `aria-describedby` and
  `aria-invalid`; **and** a focusable summary at the top linking to each bad field —
  the summary never replaces the inline message. Advanced fields live behind a
  disclosure. Erasure lives at the bottom, behind a disclosure, away from everything
  else.
- **Layout.** Mobile-first, `min-height: 100dvh`, no horizontal scroll at 375px,
  `env(safe-area-inset-bottom)` under the tab bar, long serials and card URLs
  reflow with `overflow-wrap: anywhere`.
- **Navigation.** Five bottom destinations, each with an icon *and* a word.

## 9. Anti-patterns, from the database and from this codebase

- Emoji as icons *(was true here; now tested against)*
- Raw hex in a component *(tested against)*
- Colour as the only signal *(every tag carries an icon)*
- Placeholder-as-label
- Errors dumped in one block at the top with no field-level message
- Small text — nothing below 11px, and 11px only for uppercase labels
- AI purple/pink gradients
- Mixing icon families or stroke weights
