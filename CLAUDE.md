# Soundbox

Letterboxd-style app for music albums: search albums, log listens with mood/context
("sonic memory log"), write reviews, follow people, curate lists.

## Stack

- **Backend**: Spring Boot 3.4.3, Java 17 target, PostgreSQL + Flyway, JWT auth (jjwt),
  WebClient for external APIs. Package root `hr.andrijasevic.soundbox`.
- **Frontend**: `/frontend` — Vite + React 18 + React Router 6 + Axios, plain CSS
  (design system in `src/index.css`; warm/diaristic, serif headings, terracotta accent).
- **External APIs**: MusicBrainz (metadata source of truth), Cover Art Archive
  (cover fallback), iTunes Search API (primary album artwork, `artworkUrl` on Album).

## Running locally

```bash
# PostgreSQL (docker via colima)
colima start && docker start soundbox-pg   # user/pass/db: soundbox

# Backend — needs JDK ≤ 23 (default JDK 26 breaks Lombok annotation processing)
JAVA_HOME=$(/usr/libexec/java_home -v 23) ./mvnw spring-boot:run   # :8080

# Frontend
npm run dev --prefix frontend   # :5173, /api proxied to :8080
```

Tests: `JAVA_HOME=$(/usr/libexec/java_home -v 23) ./mvnw test`. Service-layer unit tests
(Mockito) per service, plus `integration/` MockMvc tests covering auth, review
upsert/validation, listen log, follow/feed/like, and correlation-id flows (external HTTP
clients mocked via `@MockitoBean` in `BaseIntegrationTest`). Integration tests run against a
**real PostgreSQL via Testcontainers** (`TestcontainersConfiguration`, `@ServiceConnection`):
the real Flyway migrations run and Hibernate validates the schema — production parity, no H2.

- **CI / Docker Desktop**: `./mvnw test` works with no extra config.
- **Local colima**: put `docker.host=unix:///Users/<you>/.colima/default/docker.sock` in
  `~/.testcontainers.properties`, then run
  `DOCKER_API_VERSION=1.43 TESTCONTAINERS_RYUK_DISABLED=true ./mvnw test`
  (colima's Docker requires API ≥ 1.40, and Ryuk can't bind-mount the colima socket).

## Architecture notes

- Controllers are thin; auth identity comes from `SecurityContextHolder` (JWT subject = email).
- `AlbumService.getAlbum(mbid)` caches albums 7 days (`lastFetchedAt`), refreshes from
  MusicBrainz and looks up iTunes artwork on refresh. Search results are not persisted but
  are **Redis-cached** 1h (`@Cacheable` on `AlbumService.searchAlbums`); cache failures
  degrade to no-cache via a `CacheErrorHandler`, so the app runs even if Redis is down.
- `/api/auth/**` is rate-limited per IP (Bucket4j, `AuthRateLimitFilter`) → 429 over the
  limit; configurable via `app.rate-limit.*`, disabled in the test profile.
- `Album.tracklist` stores raw MusicBrainz media JSON (`[{tracks:[{number,title,length}]}]`);
  frontend parses it in `src/constants.js`.
- iTunes responds with `Content-Type: text/javascript` — `ITunesClient` fetches the body
  as String and parses with Jackson; don't switch back to `bodyToMono(dto)`.
- The cover-art WebClient follows Cover Art Archive's 307 redirects (`followRedirect(true)`),
  so `coverArtUrl` resolves. External calls (MusicBrainz search/getAlbum, iTunes artwork) are
  wrapped with Resilience4j `@CircuitBreaker` + `@Retry` and degrade to empty/null on outage.
  (This work supersedes the old `feat/backend-polish` branch entirely — Swagger, error
  handling, and the CAA fix are all now on the maturity-wave branches.)
- Frontend `AlbumCover` component falls back: `artworkUrl` → `coverArtUrl` → CAA by-mbid URL
  → placeholder. Review/log/list DTO mappers coalesce artwork (iTunes preferred).
- Ratings are 0.5–5.0 (DB CHECK + bean validation). Reviews upsert per (user, album).
- Listen logs are append-only (the diary). "Then vs Now" reads them via
  `GET /api/users/{id|me}/albums/{mbid}/history` (oldest-first); the frontend
  `RelistenHistory` component compares the earliest and latest and hides itself below 2 logs.
- Follow/like endpoints are blind toggles; DTOs don't report isFollowing/likedByMe yet.
- Logging a listen publishes a `ListenLoggedEvent` to Kafka (`event/` package, topic
  `soundbox.listen-logged`). A `@KafkaListener` consumer fans it out to the actor's
  followers as `notifications` rows (V4). Publishing is fire-and-forget and disabled via
  `app.events.enabled=false` (tests); the consumer/publisher are unit-tested, and the flow
  is exercised end-to-end through `docker compose` (which includes a KRaft Kafka).
- Errors are RFC 7807 `ProblemDetail` (`application/problem+json`). Services throw a typed
  `ApiException` subclass (`exception/` package) — `ResourceNotFoundException` (404),
  `ForbiddenException` (403), `ConflictException` (409), `UnauthorizedException` (401),
  `BadRequestException` (400) — which `GlobalExceptionHandler` renders with the right status.
  Validation failures add an `errors` map; unexpected exceptions log server-side and return a
  bland 500. Frontend reads the `detail` field via `apiError()` in `frontend/src/api/index.js`.

## Conventions

- One feature per `feat/*` branch, small commits, merged via PR (see git history).
- Flyway migrations `V<N>__description.sql` in `src/main/resources/db/migration`;
  `ddl-auto: validate` — every entity change needs a migration.
- After each phase: `./mvnw test` green and the app boots.

## Roadmap (agreed priorities)

1. ~~Frontend + iTunes cover art~~ (done: `feat/itunes-cover-art`, `feat/frontend`)
2. ~~Service-layer unit tests + MockMvc integration tests~~ (done: `feat/tests`)
3. ~~README~~ (done: `feat/readme` — screenshots deliberately left as a TODO; the preview
   tooling here has no way to save a rendered screenshot to disk, so real screenshots need
   to be captured from a real browser and dropped into a `docs/screenshots/` the user creates)
4. ~~"Then vs Now" — relisten history endpoint + album-page UI diff~~ (done: `feat/relisten-history`)
5. Relisten nudge (logs ~365 days old) + profile surface

## Backend-maturity wave (in progress)

1. ~~RFC 7807 ProblemDetail + typed exceptions + DTO validation~~ (done: `feat/problem-details`)
2. ~~springdoc-openapi / Swagger UI~~ (done: `feat/swagger` — UI at `/swagger-ui.html`,
   spec at `/api-docs`, JWT Authorize button, `@Tag`-grouped controllers)
3. ~~Actuator + structured JSON logs + correlation/trace ID~~ (done: `feat/observability` —
   `/actuator/health`+`/info` public, metrics/prometheus authed; `logback-spring.xml` JSON
   under the `json` profile; `CorrelationIdFilter` → `X-Correlation-Id` in MDC + response)
4. ~~Testcontainers (real Postgres) for integration tests~~ (done: `feat/testcontainers`)
5. ~~Resilience4j (circuit breaker + retry) around MusicBrainz/iTunes + CAA redirect fix~~ (done: `feat/resilience`)
6. ~~Redis cache (Spring Cache) for search + Bucket4j rate limiting on auth~~ (done: `feat/cache-ratelimit`)
7. ~~Multi-stage Dockerfile + docker-compose (app + Postgres + Redis)~~ (done: `feat/docker`;
   `docker compose up --build`. Kafka added with the event feature.)
8. ~~GitHub Actions CI (build + test)~~ (done: `feat/ci` — `.github/workflows/ci.yml`,
   `mvnw verify` on JDK 21 + Docker image build; Testcontainers runs in CI with no extra config)
9. ~~Kafka event-driven: listen logged → event → consumer builds notifications~~ (done: `feat/kafka`)
10. Fly.io deploy config + instructions (final authenticated deploy is the user's)
