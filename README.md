# Social Media Dashboard

A multi-platform social media analytics dashboard. Users register, connect social
accounts, and explore followers, engagement, reach and post performance through
interactive charts, filterable post history and exportable reports. Goals track
progress against targets, and an insights engine surfaces what changed and why.

Demo login: **demo@socialdashboard.dev** / **demo12345**

## Stack

| Layer     | Choice                                                          |
| --------- | --------------------------------------------------------------- |
| Frontend  | React 18, Vite, React Router, Axios, Recharts                     |
| Backend   | Spring Boot 3.3, Spring Security, Spring Data JPA, JWT + BCrypt  |
| Database  | H2 (file, development) / MySQL (production)                      |
| Build     | Maven Wrapper 3.3, Node 20+                                      |

Analytics come from deterministic mock adapters behind a `PlatformAdapter`
interface, so the UI and API are fully functional without third-party
credentials. Swapping in real Graph/X APIs means adding one adapter
implementation and nothing else.

## Requirements

- Java 17+
- Node.js 18+ (20+ recommended)
- MySQL 8+ only if you want to run the `mysql` profile

## Getting started

Two terminals.

**1. Backend** (http://localhost:8080)

```bash
cd server
./mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run             # macOS / Linux
```

**2. Frontend** (http://localhost:5173)

```bash
cd client
npm install
npm run dev
```

Open http://localhost:5173 and log in with the demo account. The Vite dev server
proxies `/api` to port 8080, so there is no CORS in development and the browser
stays on a single origin.

## Scripts

Backend

| Command             | Purpose                       |
| ------------------- | ----------------------------- |
| `mvnw.cmd test`     | Run the integration test suite |
| `mvnw.cmd package`  | Build `target/social-dashboard-server.jar` |

Frontend

| Command           | Purpose                        |
| ----------------- | ------------------------------ |
| `npm run dev`     | Dev server with HMR            |
| `npm run build`   | Production bundle into `dist/` |
| `npm run preview` | Serve the production bundle    |
| `npm run lint`    | ESLint over `src/`             |

## Configuration

Server settings live in `server/src/main/resources/application.yml` and can be
overridden with environment variables.

| Variable                  | Default                  | Purpose                              |
| ------------------------- | ------------------------ | ------------------------------------ |
| `JWT_SECRET`              | dev placeholder          | HMAC signing key — set this in prod  |
| `JWT_EXPIRATION_MS`       | `86400000`               | Token lifetime (24h)                 |
| `CORS_ALLOWED_ORIGINS`    | `http://localhost:5173`  | Comma-separated allowed origins      |
| `SEED_ENABLED`            | `true`                   | Seed the demo account on boot        |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | H2 defaults | Used by the `mysql` profile |

Client configuration is optional; see `client/.env.example`.

| Variable             | Default                 | Purpose                            |
| -------------------- | ----------------------- | ---------------------------------- |
| `VITE_API_URL`       | `/api` (proxied)        | API base URL in production         |
| `VITE_PROXY_TARGET`  | `http://localhost:8080` | Dev proxy target                   |

## Database

`dev` (the default profile) uses a file-backed H2 database at
`server/data/socialdashboard.mv.db` and creates the schema automatically. To
reset the demo data, stop the server and delete that file.

`mysql` profile:

```bash
mysql -u root -p -e "CREATE DATABASE social_dashboard;"
cd server
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=mysql \
  -Dspring-boot.run.arguments="--DB_URL=jdbc:mysql://localhost:3306/social_dashboard --DB_USERNAME=root --DB_PASSWORD=secret"
```

## API

All routes are prefixed with `/api` and, except where noted, require
`Authorization: Bearer <token>`.

| Method   | Route                                | Purpose                              |
| -------- | ------------------------------------ | ------------------------------------ |
| `GET`    | `/health`                            | Liveness probe (public)              |
| `POST`   | `/auth/register`                     | Create account, returns a token      |
| `POST`   | `/auth/login`                        | Log in, returns a token              |
| `POST`   | `/auth/logout`                       | Client-side token discard            |
| `GET`    | `/auth/me`                           | Current user, accounts and post count |
| `PUT`    | `/auth/profile`                      | Update name, bio or password          |
| `GET`    | `/accounts`                          | Connected accounts with health status |
| `POST`   | `/accounts`                          | Connect an account                   |
| `PUT`    | `/accounts/{id}`                     | Rename or change account status      |
| `DELETE` | `/accounts/{id}`                     | Disconnect and delete its data       |
| `POST`   | `/accounts/{id}/sync`                | Re-pull one account's history        |
| `POST`   | `/accounts/sync`                     | Re-pull every account                |
| `GET`    | `/stats/overview`                    | Summary cards, breakdown, top posts  |
| `GET`    | `/stats/timeseries`                  | One point per day for the charts     |
| `GET`    | `/stats/reports`                     | Full report for a range              |
| `GET`    | `/reports/export.csv`                | CSV download                         |
| `GET`    | `/goals`                             | Goals with live progress             |
| `POST`   | `/goals`                             | Create a goal                        |
| `PUT`    | `/goals/{id}`                        | Update or deactivate a goal          |
| `DELETE` | `/goals/{id}`                        | Delete a goal                        |
| `GET`    | `/insights`                          | Ranked performance insights          |
| `GET`    | `/posts`                             | Paginated, filterable post history   |
| `GET`    | `/posts/hashtags`                    | Hashtags ranked by engagement        |
| `GET`    | `/notifications`                     | Paginated notification feed          |
| `GET`    | `/notifications/unread-count`        | Badge count                          |
| `PATCH`  | `/notifications/{id}/read`           | Mark one read                        |
| `POST`   | `/notifications/read-all`            | Mark all read                        |

Shared query parameters: `range` (`1d`, `7d`, `30d`, `90d`, `365d`, `all`),
`platform`, and for posts `type`, `sort`, `direction`, `page`, `size`.

## Testing

```bash
cd server && ./mvnw.cmd test
```

30 integration tests run against an in-memory database and cover registration,
login, JWT rejection, tenant isolation, account connect/sync/disconnect, sync
idempotency, overview/time-series consistency, platform and type filters,
pagination, hashtag analytics, reports, CSV export, goals (including the
multi-account follower baseline), insights and the HTTP
error mapping.

## Project layout

```
client/
  src/
    charts/      Recharts wrappers
    components/  cards, filters, layout, shared states
    context/     auth, theme, notifications
    hooks/       async data, media queries
    pages/       login, register, dashboard, accounts, posts,
                 reports, goals, notifications, profile
    services/    axios client and API modules
    utils/       formatting helpers
    styles/      design system
server/
  src/main/java/com/socialdashboard/
    adapter/     platform adapter interface, registry, mock implementations
    config/      security, CORS, demo data seeder
    controller/  REST endpoints
    dto/         request and response records
    entity/      User, SocialAccount, Post, MetricSnapshot, Goal, Notification
    exception/   typed API errors and the global handler
    generator/   deterministic metric and post generators
    repository/  Spring Data repositories
    security/    JWT filter, entry point, user details
    service/     auth, accounts, stats, posts, goals, insights, notifications
```

## Deployment

**Frontend (Vercel)** — import the repository, set the root to `client`, build
command `npm run build`, output directory `dist`, and set `VITE_API_URL` to the
deployed backend origin. Because the app uses client-side routing, add a rewrite
of `/*` to `/index.html` so deep links like `/posts` work.

**Backend (Render or Railway)** — use `server` as the root, build command
`./mvnw clean package`, and start `./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql`.
Set `JWT_SECRET`, `CORS_ALLOWED_ORIGINS` (the frontend origin), and the `DB_*`
variables for a managed MySQL instance.

## Notes and limitations

- Data is generated locally. There are no calls to Instagram, X or TikTok, so
  nothing here consumes real platform rate limits and no API keys are needed.
- Insights are produced by rules on the server, not by a language model. Each one
  is derived from the caller's own metrics and links to the page that explains it,
  but there is no free-text generation or external AI provider involved.
- Goal progress is derived from metric snapshots on every request rather than
  stored, so it reflects the latest sync. Goals default to a window that ends
  today; pass explicit `startDate` and `endDate` to plan a future period.
- Notifications are generated as a side effect of account activity; there is no
  WebSocket push, so the badge refreshes when the feed is polled.
- Report generation is on demand. There is no scheduler or email delivery yet.
- `JWT_SECRET` has a development placeholder and must be set before any real
  deployment.
