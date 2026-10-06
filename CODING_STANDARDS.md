# CODING_STANDARDS

Review-time conventions for Studora. Read during review, not implementation. This file holds the **judgement calls** no guardrail can decide; mechanical rules belong in lint (`.github/workflows/check.yml` runs `:app:check`) or a test. Where a rule below is mechanical but has no cheap check yet, treat it as a review prompt, not a hard gate.

## Structure

- `domain/` stays framework-free: no Android, Room, or Compose imports. Frameworks enter only through the `data/` and `ui/` adapters.
- Time is injected: read the wall clock through `Clock`, never `System.currentTimeMillis()` outside the composition root; pass time zones explicitly.
- Date, bucketing, and window logic is pure and top-level so it is unit-testable without Android.
- A Compose screen keeps a stateful route (ViewModel + `collectAsStateWithLifecycle`) delegating to a stateless `…Content` so previews stay ViewModel-free.

## Presentation

- User-facing text comes from `strings.xml` — including content descriptions and legend labels. No hardcoded UI strings.
- Colors and dimensions come from `MaterialTheme` tokens. Color literals live only in `ui/theme/Color.kt` (the heat ramp is the one deliberate exception; icon path fills are not theme tokens).
- Interactive targets are at least 48dp and carry semantics; state is never conveyed by color alone.

## Tests

- Tests verify behavior through a seam — the `SessionRepository` interface, or a pure top-level function — never private internals or rendering.
- Each suite owns one in-memory fake and constructs the ViewModel directly; Hilt and Room are not exercised in JVM tests.
- Expected values come from an independent source (a worked example, a known-good literal), never recomputed the way the code under test does.

## Changes

- Hard-to-reverse changes — Room schema, navigation structure, a new dependency — get a `docs/adr/` record before the slice.
- Public APIs and non-obvious contracts carry KDoc, kept accurate as behavior changes.
