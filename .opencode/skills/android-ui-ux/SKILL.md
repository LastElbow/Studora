---
name: android-ui-ux
description: Use when building Android UI with Compose.
version: 0.1.0
author: Rheniel Penional, Hermes Agent
license: MIT
metadata:
  hermes:
    tags: [android, compose, material3, ui-ux]
    related_skills: [android-compose-apps, delegating-to-coding-agents]
---

# Android UI/UX

MD3 is primary for Compose tokens/components; UI-UX-Pro-Max is secondary for design-system and accessibility polish. Both are advisory — repo `AGENTS.md` and constitution always win.

## When to Use

- Building or changing Compose screens (Studora timer, heatmap; HabitTracker screens).
- Choosing MD3 components, theme, layout, navigation, accessibility.
- Generating or reviewing a design system for a new screen.
- Don't use for: pure domain logic, Gradle/build files, backend/API work.

## Prerequisites

- `android-compose-apps` skill (toolchain, gates) and the repo's `AGENTS.md`.
- Repo-local skills (loaded via the `skill` tool — no GitHub URLs as load paths):
  - `material-3` — `SKILL.md` plus `references/` (color-system, component-catalog, layout-and-responsive, navigation-patterns, theming-and-dynamic-color, typography-and-shape).
  - `ui-ux-pro-max` — `SKILL.md`, `references/pro-rules.md`, `references/quick-reference.md`, and `data/stacks/jetpack-compose.csv` (52 rules). Read the CSV/MD directly; no `scripts/search.py` invocation (the script is not vendored).
  - `clean-architecture` — Dependency Rule reference for the review gate below. Studora's `ui → domain ← data` layering already implements its Dependency Rule — use it for the review gate below, not as a process. Do NOT adopt obra/superpowers (295k stars): it duplicates the pocock grill + slice flow already owned by standing rule; two process owners produce spec-theater.

## How to Run

- MD3: load the `material-3` skill (`SKILL.md`) plus one needed reference (component-catalog for a component, theming-and-dynamic-color for a theme, layout-and-responsive for a scaffold). Follow its decision tree (component vs theme vs scaffold vs audit). Invoke pattern: component/theme/scaffold/audit + description.
- UUPM: load the `ui-ux-pro-max` skill; for a new screen derive a design direction from `references/quick-reference.md` plus the Compose rules in `data/stacks/jetpack-compose.csv`; for a focused concern read the matching section of `references/quick-reference.md` (ux, color, typography, icons, chart). One dominant intent per lookup, 2-5 terms. A lookup with no match: retry once narrower, then label the fallback as defaults — never present no-match output as data.
- Priority: MD3 tokens and component structure first; UUPM design direction and accessibility checklist second. On conflict, MD3 wins for components, repo rules win for everything.

## Procedure

1. Read repo `AGENTS.md` plus `android-compose-apps` constraints (Compose-only, single `:app`, theme tokens, 48dp targets, loading/empty/content/error).
2. MD3 pass: pick components from the catalog, theme from tokens (`MaterialTheme.colorScheme/typography/shapes`), layout from size-class guidance. No literal colors/dimensions outside `ui/theme/`, no hardcoded strings.
3. UUPM pass: generate or refresh the screen's design direction; run the vendored `pro-rules.md` Pre-Delivery Checklist (icons, touch feedback, light/dark contrast, safe areas, accessibility).
4. Reconcile with repo rules (see Pitfalls) before writing the slice prompt.
5. ADR gate: hard-to-reverse decisions (Hilt adoption, Room schema, navigation structure, new module, new dependency) get a `docs/adr/NNNN-*.md` record BEFORE the slice prompt — use the repo's ADR template (HabitTracker `docs/adr/0000-template.md`; if the repo has none, write a minimal Context/Decision/Consequences note). Reversible choices skip the ADR.
6. Delegate code via `delegating-to-coding-agents` + `opencode`; verify with `assembleDebug` + `check`, parse test XML, report per `Task-Report-Format.md`.

## Pitfalls

- UUPM `jetpack-compose.csv` is adopted as standing rules (best modern practice), with two STAGED items, not rejected. Rule 38 (Hilt): adopt at the first ViewModel slice unless that slice justifies a manual container for a single VM; never mix Hilt and manual containers. Rules 44-45 (feature modules): stay single `:app` until a real trigger (build time, team boundary, reusable feature) — Google modularizes to scale, not prematurely. Record either decision in `docs/adr/` (hard-to-reverse, real tradeoff).
- UUPM is web-heavy (GSAP, Tailwind, 22 stacks). Ignore non-Android stacks. Never add an app dependency from either skill without a `libs.versions.toml` reason.
- MD3 skill is large. Load one reference per task, not all six. Quote token names (`primary-container`, `surface-container-high`) instead of pasting whole tables into prompts.
- Accessibility is a gate, not polish: contrast 4.5:1, 48dp targets, semantics on every control, state never by color alone (AGENTS.md P5/P6).
- Rheniel runs the app himself from Android Studio. Get the build green, hand over run steps, never claim to have seen it render.

## Verification

- `./gradlew :app:assembleDebug` and `./gradlew :app:check` green in my own run.
- Test XML parsed (`tests=`/`failures=`/`errors=`); no `NO-SOURCE` pass.
- MD3 audit categories (color, typography, shape, elevation, components, layout, navigation, motion, accessibility, theming) addressed or explicitly deferred.
- UUPM Pre-Delivery Checklist run or explicitly deferred with a reason.
- Architecture review gate (every 3-4 slices, or before any hard-to-reverse slice): score `domain/` against the wondelai Dependency Rule diagnostic 0-10 (7 rows: dependency direction, entities/use-case separation, adapters confine frameworks, component cohesion, no cycles, SOLID at class level, inner circles framework-free). Report the score + failed rows + the inversion fixing each. Under 9 blocks the next slice until fixed.
