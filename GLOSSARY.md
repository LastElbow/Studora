# GLOSSARY

Studora's domain vocabulary. Single-context repo: one glossary at the root, with decisions recorded under [`docs/adr/`](docs/adr/). Use these terms in code, issues, and tests.

## Sessions

- **Session** — one run of the focus timer, from start to a terminal state. Modelled by `FocusSession`.
- **Completed session** — a session whose countdown reached zero. Only completed sessions are recorded; it is the unit the heatmap counts. Modelled as `SessionRecord`.
- **In-progress session** — the single session persisted so an unfinished run survives process death. At most one exists at a time. Modelled as `InProgress`.
- **Discarded session** — a running or paused session the user abandoned. It leaves no record and never shades the heatmap.

## Heatmap

- **Grid** (or **heatmap**) — the GitHub-style calendar of day cells on the History tab, one cell per day.
- **Shade** — the colour of a cell, derived from that day's completed-session count: 0 / 1 / 2–3 / 4–5 / 6+. Modelled as `ShadeLevel`.
- **Window** — the fixed 52-week (53-column), Monday-first block of days the grid renders. A grid is one window.
- **Current window** (window `0`) — the trailing block ending in the week containing today. It is the default view and rolls forward with the date.
- **Past window** (window `N`, `N > 0`) — the block ending in the week containing `today.minusYears(N)`. Windows are consecutive and non-overlapping; back navigation is bounded by the earliest year that has data.

## Data

- **Clear all data** — the Settings action that deletes every completed session and the in-progress snapshot, and resets a live session to idle. There is no undo.
