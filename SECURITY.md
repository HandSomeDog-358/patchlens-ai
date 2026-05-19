# Security Policy

PatchLens AI is an AI code review tool that connects to source hosting platforms and model providers. Treat it as security-sensitive software.

## Supported Versions

The project is currently in Alpha. Security fixes are applied to the main branch until formal releases are introduced.

## Reporting a Vulnerability

Do not open a public issue for suspected vulnerabilities.

Until a dedicated security email is published, please report privately through your repository host's private vulnerability reporting feature, or contact the maintainers through a private channel.

Please include:

- Affected component: backend, frontend, Docker deployment, webhook handling, platform integration, model integration, or authentication.
- Reproduction steps.
- Impact assessment.
- Relevant sanitized logs.

Do not include real API keys, access tokens, private source code, or private model prompts.

## Security Notes for Operators

- Change `PATCHLENS_ADMIN_PASSWORD`, `PATCHLENS_SECRET_KEY`, and `POSTGRES_PASSWORD` before any shared or public deployment.
- Use strong, random webhook secrets for GitHub, Gitee, and Gitea.
- Prefer HTTPS behind a reverse proxy for non-local deployments.
- Store platform tokens with the minimum required scopes.
- Rotate any token that was pasted into logs, chat, screenshots, or commits.
- Keep `.env` out of version control.

## Secret Handling

Model API keys, platform access tokens, and webhook secrets are stored encrypted at rest when configured through the UI. They are not returned in plaintext by API responses.

Development defaults such as `dev-secret` and `patchlens-admin` are for local use only.
