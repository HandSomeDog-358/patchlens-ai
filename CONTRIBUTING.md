# Contributing to PatchLens AI

Thanks for helping make PatchLens AI better. The project is currently in Alpha, so clear bug reports, small focused pull requests, and real-world feedback are especially valuable.

## Development Setup

Requirements:

- JDK 17
- Maven 3.9+
- Node.js 20+
- Docker Desktop or Docker Engine

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run the backend:

```bash
cd backend
mvn spring-boot:run
```

Run the frontend:

```bash
cd frontend
npm install
npm run dev
```

The console runs at `http://localhost:5173`.

## Before Opening a Pull Request

Run:

```bash
cd backend
mvn -q -DskipTests compile

cd ../frontend
npm run build

cd ..
docker compose config
```

Also check that your change does not include:

- Real API keys, access tokens, cookies, private keys, or database dumps.
- Generated build output such as `backend/target`, `frontend/dist`, or `frontend/node_modules`.
- Unrelated formatting churn.

## Pull Request Guidelines

- Keep PRs focused and explain the user-facing behavior change.
- Include screenshots for UI changes.
- Mention platform scope when relevant: GitHub, Gitee, Gitea, or all.
- For backend changes, note whether migrations are required.
- For review logic changes, describe how false positives are controlled.

## Issue Guidelines

Helpful bug reports include:

- PatchLens AI version or commit.
- Platform provider: GitHub, Gitee, or Gitea.
- Whether the target is a PR or commit review.
- Sanitized error message and relevant backend logs.
- Steps to reproduce.

Never paste real tokens, API keys, private repository code, or proprietary diffs into public issues.
