# AGENTS.md — Studora

Conventions for any agent (AI or human) working in this repository.

## Agent skills

### Issue tracker

Issues and specs live as GitHub issues via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Domain docs

Single-context: one `GLOSSARY.md` at the repo root plus `docs/adr/`. See `docs/agents/domain.md`.

### Repo-local skills (`.opencode/skills/`)

- `android-ui-ux` — orchestrator for Compose UI work (MD3 primary, UUPM secondary, ADR + review gates).
- `material-3` — MD3 tokens, components, theming references.
- `ui-ux-pro-max` — design-system direction, Compose rules, pre-delivery checklist.
- `clean-architecture` — Dependency Rule reference for the review gate.
