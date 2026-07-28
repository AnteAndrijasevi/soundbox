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

# Backend — use JDK 21 (matches CI; pom targets 17).
# Avoid JDK 24+: Lombok's annotation processing breaks.
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run   # :8080

# Frontend
npm run dev --prefix frontend   # :5173, /api proxied to :8080
```

Tests: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw verify`. Service-layer unit tests
(Mockito) per service, plus `integration/` MockMvc tests covering auth, review
upsert/validation, listen log, follow/feed/like, and correlation-id flows (external HTTP
clients mocked via `@MockitoBean` in `BaseIntegrationTest`). Integration tests run against a
**real PostgreSQL via Testcontainers** (`TestcontainersConfiguration`, `@ServiceConnection`):
the real Flyway migrations run and Hibernate validates the schema — production parity, no H2.

There is no failsafe plugin and no surefire excludes, so `./mvnw test` runs the integration
tests too. `verify` is the documented command because CI uses it and it is a superset.

- **CI / Docker Desktop**: `./mvnw verify` works with no extra config.
- **Local colima**: `DOCKER_API_VERSION` does **not** work — docker-java ignores the env var
  and still negotiates API 1.32, which colima rejects. Pass the system property instead:

  ```bash
  export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
  export TESTCONTAINERS_RYUK_DISABLED=true   # Ryuk can't bind-mount the colima socket
  ./mvnw -B verify -DargLine="-Dapi.version=1.43"
  ```

  `DOCKER_HOST` replaces `~/.testcontainers.properties`; there is no need for both.

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
- Review DTOs go through one shared `ReviewMapper` (`mapper/` package). **Never reintroduce a
  private `mapToDto` in a service** — there used to be two byte-identical copies in
  `ReviewService` and `FollowService`, each counting likes per row (N+1).
  `ReviewMapper.toDtoPage(Page<Review>)` resolves a whole page's like counts with one grouped
  aggregate (`LikeRepository.countLikesForReviews` → `ReviewLikeCount` projection), and the
  page-returning `ReviewRepository` finders carry `@EntityGraph({"user", "album"})` so both
  `@ManyToOne` sides come back on the same join, pagination still in the database.
  `FollowFeedIntegrationTest` pins the prepared-statement count via Hibernate `Statistics`
  (`hibernate.generate_statistics` is enabled in `application-test.yml`).
- **`spring.jpa.open-in-view` is `true` and the app currently depends on it.** Almost nothing
  in the service layer is `@Transactional`, so without OSIV every entity detaches the moment a
  repository call returns. Confirmed broken with `open-in-view: false`:
  `AlbumService.mapAlbumEntityToDto` (Artist proxy, cache-hit path),
  `ListenLogService.mapToDto` (Album + Artist proxies), `UserListService.mapToDto`
  (User proxy) — about seven endpoints in total. `GET /api/feed` stays clean thanks to the
  entity graph above. Turning OSIV off requires `@Transactional(readOnly = true)` on the read
  paths of those three services first.
- **`BaseIntegrationTest` is `@Transactional`.** The test's transaction holds one persistence
  context open across the whole request. Two consequences: lazy loads get deduped, so query
  counts measured in tests are lower than in production; and these tests are structurally
  incapable of detecting an OSIV dependency — `open-in-view: false` makes the entire suite
  pass while production endpoints break. Verify any OSIV change with a throwaway
  non-transactional probe test, not the suite.
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
- After each phase: `./mvnw verify` green and the app boots.

## Roadmap (agreed priorities)

1. ~~Frontend + iTunes cover art~~ (done: `feat/itunes-cover-art`, `feat/frontend`)
2. ~~Service-layer unit tests + MockMvc integration tests~~ (done: `feat/tests`)
3. ~~README~~ (done: `feat/readme` — screenshots deliberately left as a TODO; the preview
   tooling here has no way to save a rendered screenshot to disk, so real screenshots need
   to be captured from a real browser and dropped into a `docs/screenshots/` the user creates)
4. ~~"Then vs Now" — relisten history endpoint + album-page UI diff~~ (done: `feat/relisten-history`)
5. ~~Relisten nudge (logs ~365 days old) + profile surface~~ (done: `feat/relisten-nudge` —
   `GET /api/users/me/relisten-nudges`, deduped by album; "One year ago" card on own profile)

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
10. ~~Fly.io deploy config + instructions~~ (done: `feat/deploy` — `fly.toml` + `DEPLOY.md`;
    runs on Postgres alone, Redis/Kafka optional. Final `fly deploy` is the user's to run.)
11. ~~N+1 on review pages and the follow feed~~ (done: shared `ReviewMapper`, batched like
    counts, `@EntityGraph`; feed page cost is now constant in row count, pinned by a test)

### Next up

- `@Transactional(readOnly = true)` on the read paths of `AlbumService`, `ListenLogService`
  and `UserListService`, then flip `spring.jpa.open-in-view: false`.
- `FollowService.getFeed` re-queries the user by email even though `JwtAuthFilter` already
  holds the `User` as the security principal — one redundant query per request.
- `listen_logs.mood` / `.context` are unconstrained `VARCHAR(50)` while `rating` has a DB
  CHECK. Either add a CHECK migration or commit to free-form descriptors — currently neither.
