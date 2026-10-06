# Studora Design System — MASTER

Single source of truth for Studora's visual language. Page-level overrides
would live in `design-system/studora/pages/<page>.md` and take precedence over
this file; none exist yet.

- **Slice:** 3 (theme only)
- **Code of record:** `app/src/main/java/com/bustedelbow/studora/ui/theme/`
- **Status:** implemented — light + dark schemes, heat ramp, full type scale

## Product type

Studora is a **student productivity focus tool**: a one-tap focus timer whose
only other surface is a GitHub-style daily consistency heatmap. The interface is
intentionally spare — "nothing else competes for attention in v1" (issue #1).
The design system therefore optimizes for calm, low-chrome surfaces and a single
high-salience signal (heat), not for broad component variety.

## Color

### Seed and why Ink Indigo

- **Seed:** `#4758A9` — "Ink Indigo".
- **Why indigo over teal/amber:** indigo reads as focused, academic and calm;
  it is the conventional color of study/notes and pairs with the subdued
  near-neutral surfaces a consistency tool needs. Teal skews clinical/medical
  and competes with the GitHub-green heat ramp; amber skews warning/attention
  and would fight the "calm focus" intent. Indigo stays distinct from the heat
  greens, so the heatmap remains the one loud thing on screen.

### Generation source

Both schemes were generated from the seed with
**`@material/material-color-utilities` 0.4.0**, using **`SchemeTonalSpot`** at
standard contrast (`contrastLevel = 0`) — the same tonal-spot algorithm and role
mapping used by **Material Theme Builder**'s default "Tonal spot" variant and by
the `md_theme_*` Compose export. No hand-tuning was applied to the scheme.
Dynamic color (wallpaper) is intentionally **off**: the app is locked to this
seed.

### Key roles (light / dark)

| Role | Light | Dark |
|---|---|---|
| primary | `#505B92` | `#B9C3FF` |
| onPrimary | `#FFFFFF` | `#212C61` |
| primaryContainer | `#DDE1FF` | `#384379` |
| onPrimaryContainer | `#384379` | `#DDE1FF` |
| secondary | `#5A5D72` | `#C3C5DD` |
| secondaryContainer | `#DFE1F9` | `#434659` |
| tertiary | `#76546E` | `#E5BAD8` |
| tertiaryContainer | `#FFD7F2` | `#5C3C55` |
| error | `#BA1A1A` | `#FFB4AB` |
| errorContainer | `#FFDAD6` | `#93000A` |
| background / surface | `#FBF8FF` | `#121318` |
| onBackground / onSurface | `#1B1B21` | `#E3E1E9` |
| surfaceVariant | `#E3E1EC` | `#45464F` |
| onSurfaceVariant | `#45464F` | `#C6C5D0` |
| outline | `#767680` | `#90909A` |
| outlineVariant | `#C6C5D0` | `#45464F` |
| inverseSurface | `#303036` | `#E3E1E9` |
| inverseOnSurface | `#F2F0F7` | `#303036` |
| inversePrimary | `#B9C3FF` | `#505B92` |
| scrim | `#000000` | `#000000` |

### Full role coverage

Beyond primary/secondary/tertiary, the schemes define **all** MD3 roles exposed
by Compose Material3 1.4.0 `ColorScheme`:

- `onPrimary/onSecondary/onTertiary/onError` and their `*Container` +
  `on*Container` variants
- `background`/`onBackground`
- `surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`, `surfaceTint`
- **all surface-container roles:** `surfaceContainerLowest`, `surfaceContainerLow`,
  `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest`
- `surfaceDim`/`surfaceBright`
- **inverse roles:** `inverseSurface`, `inverseOnSurface`, `inversePrimary`
- `outline`, `outlineVariant`, `scrim`
- **fixed roles:** `primaryFixed`, `primaryFixedDim`, `onPrimaryFixed`,
  `onPrimaryFixedVariant`, and the same quartet for secondary and tertiary.

### Contrast (WCAG AA)

Measured against the generated values. All `on*` text pairs clear the **4.5:1**
normal-text threshold in **both** themes:

| Pair | Light | Dark |
|---|---|---|
| onPrimary / primary | 6.46:1 | 7.71:1 |
| onPrimaryContainer / primaryContainer | 7.25:1 | 7.25:1 |
| onSecondary / secondary | 6.47:1 | 7.76:1 |
| onSecondaryContainer / secondaryContainer | 7.20:1 | 7.20:1 |
| onTertiary / tertiary | 6.43:1 | 7.75:1 |
| onTertiaryContainer / tertiaryContainer | 7.28:1 | 7.28:1 |
| onError / error | 6.46:1 | 7.72:1 |
| onErrorContainer / errorContainer | 7.24:1 | 7.24:1 |
| onSurface / surface | 16.30:1 | 14.32:1 |
| onSurfaceVariant / surfaceVariant | 7.25:1 | 5.49:1 |
| onBackground / background | 16.30:1 | 14.32:1 |
| inverseOnSurface / inverseSurface | 11.60:1 | 10.12:1 |
| primary / surface (links, emphasis) | 6.14:1 | 10.88:1 |

Non-text roles:

- `outline / surface` is **4.27:1** (light) and **5.87:1** (dark). Both clear the
  **3:1** WCAG non-text minimum that applies to borders/control outlines;
  `outline` is not a text role. `outlineVariant` (decorative dividers) is
  intentionally lower contrast by spec.
- Dark-mode contrast was checked independently from light mode (never inferred).

## Heat ramp (intentional non-MD3 exception)

The heatmap uses **fixed GitHub-contribution greens**, hard-coded as semantic
tokens independent of the seed or any future dynamic color:

| Token | Light | Dark |
|---|---|---|
| `HeatNone` (0 sessions) | `#EBEDF0` | `#161B22` |
| `HeatLight` (1) | `#9BE9A8` | `#0E4429` |
| `HeatMedium` (2–3) | `#40C463` | `#006D32` |
| `HeatDark` (4–5) | `#30A14E` | `#26A641` |
| `HeatIntense` (6+) | `#216E39` | `#39D353` |

**Why this breaks the "MD3 tokens only" rule:** the heatmap's whole contract is
"darker means more focus", learned from GitHub. If the ramp were derived from
the seed, a wallpaper/seed change could re-order luminance and silently break
that reading. The ramp is therefore pinned: the light ramp darkens monotonically
as intensity rises (relative luminance 0.845 → 0.118), and the dark ramp
brightens monotonically (0.011 → 0.481) so the busiest days stay most salient on
a dark surface.

Tokens are exposed with `isSystemInDarkTheme()`-aware accessors in `Color.kt`:
the five composable properties above plus `heatmapRamp(): List<Color>` ordered
`none → intense` for indexing by shade level.

## Typography

- **Roboto for everything** via `FontFamily.Default` (Android's platform
  typeface). No font asset or dependency is added.
- The **full MD3 type scale** is declared explicitly in `Type.kt` — all 15
  roles (display/headline/title/body/label × L/M/S) with MD3 sizes, line
  heights, tracking and weights, mirroring Material3's `TypeScaleTokens`
  (v0_103). Not just `bodyLarge`.
- **Deferred:** tabular-figure numerals for the countdown. The timer needs
  non-jittering digits, which is a font-feature/tabular setting rather than a
  different family; it ships with the timer slice.

## Shape and elevation

- Shapes stay at the MD3 default `Shapes()` — **not customized** in this slice.
- Elevation follows MD3 tonal-surface guidance; no custom shadow tokens.

## Deferred (explicitly out of Slice 3)

- **Timer screen** (countdown UI, start/pause/resume/discard controls).
- **Heatmap grid** (cell layout, month/week axes, tooltips/legend).
- **Page overrides** under `design-system/studora/pages/`.
- **Tabular-numeral countdown** typography (see above).
- Dynamic color / wallpaper theming (deliberately rejected).
- Custom shapes, motion tokens, and spacing tokens (repo uses 4/8dp rhythm
  once screens exist; no spacing tokens added in a theme-only slice).

## File map

| File | Holds |
|---|---|
| `ui/theme/Color.kt` | the MD3 theme + heat tokens (theme and heat color literals; icon path fills live in `ui/components/AppIcons.kt`) |
| `ui/theme/Theme.kt` | `StudoraTheme(darkTheme, content)`; light/dark schemes; default shapes |
| `ui/theme/Type.kt` | full Roboto MD3 type scale |
| `MainActivity.kt` | themed root `Scaffold` + bottom-tab navigation (Timer / History / Settings) |
