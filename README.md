# soundbox

**your sonic memory log.** Every user is a curator.

Soundbox is a Letterboxd-style app for music albums — but instead of just rating things, it's built around *logging a listen*: the mood you were in, where you were, whether it was your first time hearing it, the track that got you. Reviews and star ratings exist too, but the diary is the point. Over time your listen log becomes a record of how an album moved through your life, not just a score you gave it once.

## Screenshots

_Coming soon — the app is fully functional and manually verified end-to-end (see below), screenshots to follow._

## What you can do

- **Search** the MusicBrainz catalogue for any album, see cover art, full tracklist, and genres
- **Log a listen** in under ten seconds: star rating, mood (nostalgic, euphoric, melancholic...), context (driving, studying, heartbreak...), first-listen flag, favorite track, and a short note — everything optional except showing up
- **Write a review** — 0.5–5 stars with text, one per album per person, editable in place
- **Follow people** and see their reviews in a paginated feed; like the ones that land
- **Build lists** — curate albums into named, ordered, public-or-private shelves
- **Browse profiles** — your own or anyone else's: listen-log diary, reviews, and lists, all tabbed together with follower/following counts

## Stack

**Backend** — Spring Boot 3.4.3 (Java 17), PostgreSQL with Flyway migrations, Spring Security with stateless JWT auth, Spring Data JPA, WebClient for external HTTP, Lombok. 23 REST endpoints across 8 controllers, documented with an interactive Swagger UI (`/swagger-ui.html`) and RFC 7807 ProblemDetail error responses.

**Frontend** — Vite + React 18 + React Router 6 + Axios, hand-rolled CSS design system (no UI framework) — warm, diaristic, paper-and-ink.

**External data** — [MusicBrainz](https://musicbrainz.org/) is the metadata source of truth (search, tracklist, genres, release dates); the [iTunes Search API](https://performance-partners.apple.com/search-api) supplies album artwork, with [Cover Art Archive](https://coverartarchive.org/) as a fallback.

**Tests** — 59 tests: JUnit 5 + Mockito service-layer unit tests, plus Spring MockMvc integration tests against H2 covering the auth, review, listen-log, and follow/feed flows end-to-end (external HTTP mocked).

## Running it locally

### Quickest: Docker Compose

```bash
docker compose up --build   # app + Postgres + Redis, on http://localhost:8080
```

Swagger UI at `/swagger-ui.html`, health at `/actuator/health`.

### Or run the pieces yourself

You'll need Docker (or Postgres running some other way), a JDK, and Node.

```bash
# 1. Database
docker run -d --name soundbox-pg \
  -e POSTGRES_USER=soundbox -e POSTGRES_PASSWORD=soundbox -e POSTGRES_DB=soundbox \
  -p 5432:5432 postgres:16-alpine

# 2. Backend — Java 17 target; if your default JDK is newer, point JAVA_HOME
#    at a JDK ≤ 23 (Lombok's annotation processor breaks on JDK 26+ at present)
./mvnw spring-boot:run          # → http://localhost:8080

# 3. Frontend
npm install --prefix frontend
npm run dev --prefix frontend   # → http://localhost:5173, proxies /api to :8080
```

Open `http://localhost:5173`, register an account, and start logging.

Run the backend test suite with `./mvnw test` (uses an in-memory H2 database, no Docker needed).

## Architecture notes

- Auth identity flows from the JWT subject (the user's email) via `SecurityContextHolder`; controllers stay thin.
- `AlbumService` caches fetched albums for 7 days, re-resolving from MusicBrainz and re-checking iTunes for artwork on refresh — so browsing repeatedly doesn't hammer either API. Search results themselves are never persisted, only albums someone actually opens.
- A review is an upsert: reviewing an album you've already reviewed replaces it rather than creating a second row (unique constraint on user+album). A listen log, by contrast, is append-only — that's the diary.
- Ratings are constrained to 0.5–5.0 at both the database (`CHECK`) and API (bean validation) layers.

## Project layout

```
src/main/java/hr/andrijasevic/soundbox/
  controller/   REST endpoints
  service/      business logic
  domain/       JPA entities
  dto/          request/response records
  repository/   Spring Data repositories
  external/     MusicBrainz + iTunes API clients
  config/       security, CORS
  exception/    global error handling
frontend/src/
  pages/        route-level screens
  components/   shared UI (modals, cards, star rating, album cover)
  api/          Axios client + endpoint calls
  auth/         auth context, protected routes
```
