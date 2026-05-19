# Open Source Release Checklist

Use this checklist before publishing PatchLens AI to a public repository.

## Required

- [ ] Confirm `.env`, local database dumps, logs, build output, and IDE files are not committed.
- [ ] Run a secret scan against tracked files and git history.
- [ ] Rotate any API key or platform token that was ever pasted into chat, logs, screenshots, or commits.
- [ ] Change all production deployment defaults:
  - `PATCHLENS_ADMIN_PASSWORD`
  - `PATCHLENS_SECRET_KEY`
  - `POSTGRES_PASSWORD`
  - platform webhook secrets
- [ ] Verify `LICENSE`, `SECURITY.md`, `CONTRIBUTING.md`, and `CODE_OF_CONDUCT.md`.
- [ ] Run CI locally:

```bash
cd backend
mvn -q -DskipTests compile

cd ../frontend
npm run build

cd ..
docker compose config
```

## Recommended

- [ ] Add screenshots to `docs/screenshots/` and reference them from `README.md`.
- [ ] Create an `alpha` release tag.
- [ ] Enable private vulnerability reporting on the repository host.
- [ ] Protect the default branch and require CI for pull requests.
- [ ] Add a short roadmap issue for:
  - pgvector code context retrieval
  - line-level PR comments
  - team-rule learning from feedback
  - GitLab support

## Positioning

Suggested public label:

```text
PatchLens AI is an Alpha / Developer Preview project.
It is suitable for local trials, team pilots, and community contribution.
It is not yet a production SLA-backed code review system.
```
