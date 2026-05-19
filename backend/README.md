# PatchLens API

## Run

```bash
export JAVA_HOME=/Users/00568717/Java/jdk17/Contents/Home
export PATH="$JAVA_HOME/bin:/Users/00568717/Java/apache-maven-3.9.11/bin:$PATH"
mvn spring-boot:run
```

## Useful APIs

Create a repository:

```bash
curl -X POST http://localhost:8080/api/repositories \
  -H 'Content-Type: application/json' \
  -d '{"provider":"GITEE","owner":"example-org","name":"example-service","defaultBranch":"main"}'
```

Run a mock review:

```bash
curl -X POST http://localhost:8080/api/repositories/1/reviews \
  -H 'Content-Type: application/json' \
  -d '{"prNumber":1,"commitSha":"local-demo"}'
```

List reviews:

```bash
curl http://localhost:8080/api/reviews
```

## Gitee Pull Request Context

To fetch real Gitee PR metadata and changed files, start the backend with:

```bash
export PATCHLENS_GITEE_ACCESS_TOKEN=your-gitee-token
export PATCHLENS_GITEE_API_BASE_URL=https://gitee.com/api/v5
mvn spring-boot:run
```

## Gitea Pull Request Context

For self-hosted Gitea:

```bash
export PATCHLENS_GITEA_API_BASE_URL=https://your-gitea-host/api/v1
export PATCHLENS_GITEA_ACCESS_TOKEN=your-gitea-token
mvn spring-boot:run
```

Webhook endpoint:

```text
POST /webhooks/gitea
```
