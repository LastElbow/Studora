# ADR-0002: Room for session persistence

- **Status:** Accepted (2026-10-06)
- **Context:** Slice 5 needs completed-session history (heatmap) + in-progress restore (kill-recovery). Choice: Room vs DataStore vs files.
- **Decision:** Room 2.8.5 only. `completed_sessions(startMillis, endMillis)` + `in_progress(id=1, startMillis, durationMinutes)` single row. DB v1, no migration (first schema). Repository interface in `domain/`, impl in `data/`.
- **Consequences:** KSP already wired (slice 4). DAO compile verified by assemble; DAO behavior verified instrumented later (needs device) — stated, not hidden. JVM tests cover mapper + VM restore logic via fake.
