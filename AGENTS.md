# AGENTS.md — Studora

Conventions for any agent (AI or human) working in this repository.

## Working in this repo

- Verify changes with `./gradlew :app:check` (lint + unit tests); it must be green before handing off.
- Hard-to-reverse changes (Room schema, navigation structure, a new dependency) get a `docs/adr/` record before the slice.
- Review conventions live in `CODING_STANDARDS.md`.

## Agent skills

### Issue tracker

Issues and specs live as GitHub issues via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Domain docs

Single-context: one `GLOSSARY.md` at the repo root plus `docs/adr/`. See `docs/agents/domain.md`.

### Repo-local skills (machine-local, gitignored)

Handwritten skills live under `.agents/skills/`, beside the installed packs:

- `android-ui-ux` — orchestrator for Compose UI work (MD3 primary, UUPM secondary, ADR + review gates).
- `material-3` — MD3 tokens, components, theming references.
- `ui-ux-pro-max` — design-system direction, Compose rules, pre-delivery checklist.
- `clean-architecture` — Dependency Rule reference for the review gate.

### Skill format

A skill is a `<name>/SKILL.md` bundle whose YAML frontmatter requires a kebab-case
`name` and a `description`. Set `disable-model-invocation: true` to keep a skill out
of the model catalog and expose it for human invocation only.

DSH discovers `.dsh/skills/` (rank 100) and `.agents/skills/` (rank 200)
automatically; no restart is needed.

### Installed skill packs

`.agents/skills/` and `.dsh/skills/` are gitignored, so every skill below is
machine-local. The installers write to `.agents/skills/`, which DSH discovers
directly — no move step is needed.

| Pack | Count | Refresh with |
|---|---|---|
| Official Android skills | 25 | `android skills add --all` |
| `mattpocock/skills` | 38 | `npx skills@latest add mattpocock/skills` |
| Repo-local (handwritten) | 4 | — |

`skills-lock.json` at the repo root is written by `npx skills`, records each
skill's source and content hash, and is tracked. Restore with `npx skills experimental_install`.
