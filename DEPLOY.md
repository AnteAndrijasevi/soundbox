# Deploying to Fly.io

The backend ships as a Docker image (`Dockerfile`) and deploys to [Fly.io](https://fly.io)
using [`fly.toml`](./fly.toml). This gives you a live API + interactive Swagger UI at
`https://<your-app>.fly.dev/swagger-ui.html`.

The base deploy runs on **PostgreSQL alone** — Redis (caching) and Kafka (events) are
optional extras you can add later. The app degrades gracefully without them.

## Prerequisites

- A [Fly.io account](https://fly.io/app/sign-up) and [`flyctl`](https://fly.io/docs/flyctl/install/) installed
- `fly auth login`

## 1. Launch (creates the app, doesn't deploy yet)

From the repo root:

```bash
fly launch --no-deploy --copy-config
```

Pick a unique app name when prompted; it rewrites `app = "..."` in `fly.toml`.

## 2. Provision Postgres and attach it

```bash
fly postgres create --name <your-app>-db --region fra
fly postgres attach <your-app>-db
```

`attach` sets a `DATABASE_URL` secret. Spring wants a JDBC URL and split credentials, so
translate it once (values are in the attach output / `fly postgres connect`):

```bash
fly secrets set \
  SPRING_DATASOURCE_URL="jdbc:postgresql://<host>:5432/<db>" \
  SPRING_DATASOURCE_USERNAME="<user>" \
  SPRING_DATASOURCE_PASSWORD="<password>"
```

Also set a strong JWT secret (≥ 256 bits) — `application.yml` reads `${JWT_SECRET}`:

```bash
fly secrets set JWT_SECRET="$(openssl rand -base64 48)"
```

## 3. Deploy

```bash
fly deploy
```

Then open it:

```bash
fly open /swagger-ui.html
```

Health is at `/actuator/health`; the Fly checks in `fly.toml` already watch it.

## Optional: caching (Redis) and events (Kafka)

**Redis** (Upstash, via Fly):

```bash
fly redis create           # note the connection URL
fly secrets set REDIS_HOST=<host> REDIS_PORT=<port> SPRING_CACHE_TYPE=redis
```

**Kafka** — point at any managed broker (Upstash Kafka, Redpanda Cloud, …):

```bash
fly secrets set \
  KAFKA_BOOTSTRAP_SERVERS=<broker:9092> \
  APP_EVENTS_ENABLED=true \
  SPRING_KAFKA_LISTENER_AUTO_STARTUP=true
```

## Optional: a separate frontend deploy

The `/frontend` build is static. Host it anywhere (Fly static, Netlify, …), point its API
base at your Fly URL, and allow its origin on the backend:

```bash
fly secrets set APP_CORS_ALLOWED_ORIGINS="https://<your-frontend-host>"
```
