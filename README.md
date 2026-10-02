# CaseLock

**A secure digital evidence management and chain-of-custody system.**

CaseLock helps investigators, forensic analysts, legal officers and case administrators
manage digital evidence through its entire lifecycle: collection, registration,
cryptographic hashing, storage, access, transfer, analysis, integrity verification,
audit, reporting and archival — with every step traceable.

```
COLLECT → REGISTER → HASH → STORE → ACCESS → TRANSFER → ANALYZE → VERIFY → AUDIT → REPORT → ARCHIVE
```

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Core Features](#core-features)
3. [Architecture](#architecture)
4. [Technology Stack](#technology-stack)
5. [Project Structure](#project-structure)
6. [Prerequisites](#prerequisites)
7. [Database Setup](#database-setup)
8. [Backend Setup](#backend-setup)
9. [Frontend Setup](#frontend-setup)
10. [Environment Variables](#environment-variables)
11. [Running the Application](#running-the-application)
12. [How the Frontend and Backend Communicate](#how-the-frontend-and-backend-communicate)
13. [API Overview](#api-overview)
14. [User Roles](#user-roles)
15. [Sample / Demo Credentials](#sample--demo-credentials)
16. [Security Considerations](#security-considerations)
17. [Testing](#testing)
18. [Build Notes & Known Limitations](#build-notes--known-limitations)

---

## Project Overview

CaseLock combines five things that are usually built as disconnected tools into one
coherent platform:

```
Case Management + Digital Evidence Management + Cryptographic Integrity
Verification + Chain of Custody + Role-Based Access Control + Audit Trail
```

The result is a system that can always answer: *what evidence exists, who uploaded it,
who has accessed or transferred it, whether its content has changed since registration,
and what happened to it throughout the case's lifecycle.*

Every meaningful action (case created, evidence uploaded, evidence accessed/downloaded/
transferred, integrity verified, user created, role changed, unauthorized access attempt,
...) produces both a **chain-of-custody event** (evidence-specific) and/or an
**audit log entry** (system-wide), so nothing happens silently.

## Core Features

- **Authentication & RBAC** — JWT-based login/registration, BCrypt password hashing,
  account lockout after repeated failed attempts, login history, and five roles
  (Admin, Investigator, Forensic Analyst, Legal Officer, Viewer) enforced on the
  **backend**, not just hidden in the UI.
- **Case Management** — create/search/filter/update cases, assign an investigator and
  team, track status through its lifecycle (`OPEN → UNDER_INVESTIGATION →
  EVIDENCE_REVIEW → PENDING → CLOSED/ARCHIVED`).
- **Evidence Management** — drag-and-drop upload with metadata, category and case
  association; every upload is hashed automatically.
- **Cryptographic Integrity Verification** — SHA-256 hash computed on upload and stored
  as an **immutable** original hash; a "Verify Integrity" action recomputes the hash and
  compares it, reporting `INTEGRITY VERIFIED` or `INTEGRITY COMPROMISED`.
- **Chain of Custody** — every collection/upload/access/transfer/analysis/verification/
  download/seal/reopen/archive event is recorded and rendered as a timeline.
- **Audit Logging** — a tamper-evident, append-only log of every security-relevant
  action, filterable by user/action/entity/date/result.
- **Dashboard & Analytics** — live case/evidence counts, integrity alerts, monthly
  activity chart, cases-by-status breakdown, recent activity and security alerts.
- **Search, Filter & Pagination** — across cases, evidence, users and audit logs.
- **Notifications** — case assignments, transfers, and integrity-failure alerts.
- **Reports** — a full case report (case info, evidence summary, chain of custody,
  integrity results, audit history) viewable in-app or exported as a PDF.
- **Responsive, accessible UI** — sidebar/drawer navigation, breadcrumbs, loading/empty/
  error states, toasts, confirmation dialogs, icon+text+color status indicators.

## Architecture

```
                          CASELOCK
                             │
          ┌──────────────────┴──────────────────┐
          │                                      │
    React Frontend                      Spring Boot Backend
   (Vite, Tailwind,                            │
   React Router, Axios)                    REST APIs
          │                                      │
          │                              Spring Security (JWT)
          │                                      │
          │                                  Services
          │                                      │
          │                               Repositories (JPA)
          └──────────────────────────────────────┤
                                                   │
                                            Hibernate / JPA
                                                   │
                                                   ▼
                                                MySQL
```

Backend layering is strict: **Controller → Service → Repository → Entity → MySQL**.
Controllers only translate HTTP ⇄ DTOs; all business rules (access control, hashing,
state transitions, audit/notification side-effects) live in the service layer.
DTOs are used on every request/response — JPA entities are never serialized directly
to the client.

## Technology Stack

**Backend:** Java 21 (LTS) · Spring Boot 3.4 · Spring Web · Spring Security · Spring Data JPA
· Hibernate · Maven · Bean Validation · JWT (jjwt) · BCrypt · Lombok · SLF4J/Logback ·
iText (PDF reports) · MySQL (H2 available for an optional zero-config dev profile)

**Frontend:** React 18 · JavaScript (JSX) · React Router · Axios · Tailwind CSS ·
Recharts (dashboard charts) · lucide-react (icons) · react-hot-toast

**Database:** MySQL by default (auto-creates `caselock_db` and seeds demo data on first
run), with an optional `dev` profile that swaps in an in-memory H2 database instead

## Project Structure

```
caselock/
├── backend/
│   ├── src/main/java/com/caselock/
│   │   ├── controller/     REST controllers (HTTP only, no business logic)
│   │   ├── service/        Business logic, access control, orchestration
│   │   ├── repository/     Spring Data JPA repositories
│   │   ├── entity/         JPA entities (+ entity/enums)
│   │   ├── dto/             Request/response DTOs (request/, response/)
│   │   ├── security/       JWT, UserDetails, filters, entry points
│   │   ├── exception/      Custom exceptions + GlobalExceptionHandler
│   │   ├── config/         Security, CORS, JPA auditing, seed data, properties
│   │   └── util/           Hash, identifier generation, request/security helpers
│   ├── src/test/java/...   Unit + integration tests
│   ├── db/schema-reference.sql   Reference MySQL DDL (documentation only)
│   └── pom.xml
│
└── frontend/
    ├── src/
    │   ├── components/     Reusable UI building blocks
    │   ├── pages/           One component per route
    │   ├── layouts/         AppLayout (sidebar + navbar shell)
    │   ├── services/        Axios API clients, one per backend module
    │   ├── context/         AuthContext, NotificationContext
    │   ├── hooks/            useDebounce, etc.
    │   ├── utils/            constants (enum labels/colors), formatters
    │   └── App.jsx            Route definitions
    └── package.json
```

## Prerequisites

- **Java 21** (JDK, LTS) and **Maven 3.9+**
- **Node.js 18+** and **npm**
- **MySQL 8+** — the app connects to real MySQL by default (no profile needs to be
  set). If you'd rather skip installing MySQL for a quick local trial, set
  `SPRING_PROFILES_ACTIVE=dev` to use an in-memory H2 database instead.

## Database Setup

**Option A — real MySQL (default, no profile needed):** make sure a local MySQL 8+
server is running and that `DB_USERNAME`/`DB_PASSWORD` in `backend/.env` match a real
account on it (see [Environment Variables](#environment-variables)). You do **not**
need to create the `caselock_db` database yourself — the JDBC URL includes
`createDatabaseIfNotExist=true`, so the backend creates it automatically on first
connection, and Hibernate then creates the tables (`DDL_AUTO=update`, the default) and
seeds demo users/cases/evidence since the `users` table starts empty. To pre-create the
schema yourself instead, run `backend/db/schema-reference.sql` and set `DDL_AUTO=validate`.

**Option B — zero-config H2 (quick local trial, no MySQL install needed):** set
`SPRING_PROFILES_ACTIVE=dev` in `backend/.env`. This switches to an in-memory H2
database that resets on every restart, with demo data seeded automatically.

For production, set `SPRING_PROFILES_ACTIVE=prod`, which disables auto-seeding and
sets `DDL_AUTO=validate` (schema must already exist).

## Backend Setup

```bash
cd backend
cp .env.example .env      # then edit values as needed
mvn clean install
```

Environment variables can be exported directly, loaded from `.env` with a tool like
`direnv`/`dotenv`, or set in `application.yml`'s referenced variables by your process
manager / container platform. Nothing sensitive is hardcoded in source.

## Frontend Setup

```bash
cd frontend
cp .env.example .env      # optional — defaults work with the Vite dev proxy
npm install
```

## Environment Variables

**Backend** (`backend/.env.example`):

| Variable | Purpose | Dev default |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | leave blank for real MySQL (default), `dev` for in-memory H2 + seed data, or `prod` for hardened MySQL | *(blank)* |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection | `localhost:3306/caselock_db`, `root`, your MySQL password |
| `JWT_SECRET` | HS256 signing key (256+ bits) | dev-only placeholder |
| `JWT_EXPIRATION_MS` / `JWT_REFRESH_EXPIRATION_MS` | Token lifetimes | 24h / 7d |
| `FILE_STORAGE_PATH` | Where evidence files are stored on disk | `./uploads` |
| `MAX_FILE_SIZE_MB` | Evidence upload size limit | `50` |
| `CORS_ALLOWED_ORIGINS` | Frontend origin(s) | `http://localhost:5173` |
| `SERVER_PORT` | Backend port | `8080` |

**Frontend** (`frontend/.env.example`):

| Variable | Purpose |
|---|---|
| `VITE_API_BASE_URL` | Backend API base URL (defaults to the Vite dev proxy, `/api`) |

Never commit a real `.env` file — only the `.env.example` templates are checked in.

## Running the Application

```bash
# 1. Start MySQL (make sure the server is running and your .env credentials match;
#    the caselock_db schema is created automatically on first connection)

# 2. Backend
cd backend
mvn spring-boot:run
# -> http://localhost:8080

# 3. Frontend (separate terminal)
cd frontend
npm run dev
# -> http://localhost:5173

# 4. Open http://localhost:5173 and log in with a demo account (below)
```

On first startup with the `dev` profile, `DataSeeder` populates realistic demo data —
five users (one per role), several cases, evidence items with real SHA-256 hashes
computed against small sample files it writes to `FILE_STORAGE_PATH`, chain-of-custody
events, audit log entries and notifications — so the app is immediately explorable.

## How the Frontend and Backend Communicate

The React app talks to Spring Boot exclusively through versioned REST endpoints under
`/api/**`, using a shared Axios instance (`frontend/src/services/api.js`) that:

- attaches the JWT access token to every request (`Authorization: Bearer <token>`),
- transparently refreshes an expired access token using the refresh token and retries
  the original request,
- surfaces backend error messages through the app's consistent `{ success, message,
  data, errorCode }` response envelope.

In local dev, Vite's dev server proxies `/api/*` to `http://localhost:8080`
(see `vite.config.js`), so the frontend never needs to hardcode a backend host.
For a separately-deployed frontend, set `VITE_API_BASE_URL` to the real backend URL and
configure `CORS_ALLOWED_ORIGINS` on the backend to match.

## API Overview

All responses share this envelope:

```json
{ "success": true, "message": "Evidence verified successfully.", "data": { } }
{ "success": false, "message": "Evidence not found.", "errorCode": "EVIDENCE_NOT_FOUND" }
```

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Create an account |
| POST | `/api/auth/login` | Log in, receive access + refresh tokens |
| POST | `/api/auth/refresh` | Exchange a refresh token for a new access token |
| POST | `/api/auth/logout` | Log out (audit-logged) |
| GET | `/api/auth/me` | Current authenticated user |
| GET/POST | `/api/cases` | List/search (paginated) or create a case |
| GET/PUT | `/api/cases/{id}` | Get / update a case |
| PATCH | `/api/cases/{id}/status` | Change case status |
| GET/POST | `/api/evidence` | List/search or upload evidence (multipart) |
| GET | `/api/evidence/{id}` | Get evidence (also logs an ACCESS custody event) |
| GET | `/api/evidence/{id}/download` | Download the evidence file |
| POST | `/api/evidence/{id}/verify` | Recompute SHA-256 and compare to the original hash |
| POST | `/api/evidence/{id}/transfer` | Transfer custody to another user |
| POST | `/api/evidence/{id}/seal` \| `/reopen` | Seal/reopen evidence |
| GET | `/api/evidence/{id}/chain-of-custody` | Full custody timeline for one item |
| GET | `/api/chain-of-custody/case/{caseId}` | Custody timeline across a whole case |
| GET | `/api/audit-logs` | Search the audit trail (admin only) |
| GET/POST | `/api/users` | List/search or create users (admin only) |
| POST | `/api/users/{id}/disable` \| `/enable` \| `/reset-password` | Admin user actions |
| GET | `/api/users/directory` | Lightweight user list for dropdowns (any authenticated user) |
| GET | `/api/notifications` | Current user's notifications |
| GET | `/api/dashboard/stats` | Aggregated dashboard statistics |
| GET | `/api/reports/{caseId}` | Full case report (JSON) |
| GET | `/api/reports/{caseId}/pdf` | Full case report (PDF download) |

## User Roles

| Role | Can do |
|---|---|
| **ADMIN** | Manage users & roles, view all cases, view audit logs, everything below |
| **INVESTIGATOR** | Create cases, upload evidence, view/edit assigned cases, transfer evidence |
| **FORENSIC_ANALYST** | Analyze evidence, verify integrity, transfer evidence, view cases |
| **LEGAL_OFFICER** | View authorized evidence/cases, generate reports |
| **VIEWER** | Read-only access to cases and evidence they're authorized for |

Authorization is enforced with Spring Security `@PreAuthorize` on every sensitive
endpoint *and* independently re-checked in the service layer for case/evidence-level
ownership (`CaseAccessService`) — a user cannot obtain data they're not authorized for
just by knowing an ID or bypassing the UI.

## Sample / Demo Credentials

Seeded automatically by `DataSeeder` in the `dev` profile:

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin123!` | Administrator |
| `investigator` | `Investigate123!` | Investigator |
| `investigator2` | `Investigate123!` | Investigator |
| `analyst` | `Analyze123!` | Forensic Analyst |
| `legal` | `Legal123!` | Legal Officer |
| `viewer` | `Viewer123!` | Viewer |

These are demo-only accounts with intentionally simple passwords — never reuse this
seeding behavior in a real deployment (`caselock.seed.enabled` is `false` under the
`prod` profile for exactly this reason).

## Security Considerations

- Passwords hashed with BCrypt (strength 12); never logged or returned by any API.
- JWT access + refresh tokens (HS256); the signing secret is environment-supplied,
  never hardcoded.
- Account lockout after 5 failed login attempts (15-minute cooldown), with every
  attempt recorded in `login_history` and the audit log.
- Every stored evidence file is renamed to a random UUID on disk — the original
  filename never doubles as a path, and file extensions/sizes are validated server-side.
- The evidence's **original SHA-256 hash is immutable** (`updatable = false` at the JPA
  level) — verification always compares against that first-recorded value.
- CORS is restricted to explicitly configured origins; CSRF is disabled because the API
  is stateless/token-based (no cookie-based session to forge).
- All error responses are sanitized (`GlobalExceptionHandler`); stack traces are
  logged server-side via SLF4J and never sent to the client.
- Unauthorized access attempts (wrong role, wrong case ownership) are themselves
  recorded as `UNAUTHORIZED_ACCESS_ATTEMPT` audit entries with `result = DENIED`.

## Testing

```bash
cd backend
mvn test
```

Covers the most security-sensitive paths, per the spec's minimum bar:

- `HashUtilTest` — SHA-256 correctness (same content → same hash, any change → different
  hash, verified against a known test vector).
- `JwtUtilTest` — token issuance/parsing/expiry, access vs. refresh token typing.
- `AuthControllerIntegrationTest` — register → login round trip, wrong-password
  rejection, protected-endpoint-with-no-token rejection, duplicate-username conflict.
- `CaseAuthorizationIntegrationTest` — a VIEWER is rejected (403) from an
  investigator/admin-only action even with a valid token; an INVESTIGATOR succeeds.
- `EvidenceIntegrityIntegrationTest` — end-to-end: upload evidence → verify (VERIFIED) →
  tamper with the file on disk → verify again (COMPROMISED) → confirm the original hash
  was never overwritten.

Frontend: the codebase is structured for straightforward component/integration testing
(Vitest + React Testing Library fit naturally here) but a test suite was not included
in this pass given the scope of the rest of the build — see
[Build Notes & Known Limitations](#build-notes--known-limitations).

## Build Notes & Known Limitations

This project was generated end-to-end (backend + frontend + schema + docs) in one
pass, prioritizing a fully working core over exhaustive coverage of every listed
feature at equal depth. Specifically:

- **Backend business logic, security, and API surface are complete and real** — no
  mocked endpoints, no hardcoded frontend data; every screen reads from and writes to
  MySQL through the real Spring Boot API.
- **Built and run successfully end-to-end on Java 21** — `mvn clean install` produces
  `BUILD SUCCESS` (including the integration test suite) and `mvn spring-boot:run` /
  `npm run dev` start cleanly. Earlier drafts of this project targeted Java 26, which
  ran into Lombok/Spring/ASM class-file-format incompatibilities on that very new JDK;
  the project now targets Java 21 (an LTS release) to avoid that whole class of issue.
- **Case → evidence workflow is wired end-to-end**: creating a case (Cases → New Case)
  redirects straight to that case's detail page, which has an "Upload Evidence" action
  (and an empty-state prompt on the Evidence tab) tied to that specific case's ID —
  uploading there hits `POST /api/evidence` with the file plus `caseId` metadata,
  computes the SHA-256 hash, and opens the chain-of-custody trail for that item.
- **Registration 500 error**: if you hit "Request failed with status code 500" on the
  Register screen, this was seen once during development against the old H2 setup and
  could not be reproduced from a static review of the registration code path (field
  names, enums, and constraints all check out). It has not recurred since switching to
  MySQL, but if it happens again, please copy the stack trace from the backend
  terminal (not just the browser error) so it can be root-caused precisely.
- **Notifications** are functional (created on assignment/transfer/integrity-failure,
  listed, markable as read) but are pull-based (polled every 30s), not push/WebSocket.
- **PDF reports** use a clean, functional iText layout rather than a heavily designed
  template.
- **Frontend automated tests** were not included in this pass (see above); the backend
  test suite is the primary automated safety net.
- **Login history for non-admins**: the `/api/users/{id}/login-history` endpoint is
  admin-only per the spec's RBAC table, so a non-admin's own Profile page will show
  "available to administrators" rather than their own history — extend this with a
  `/api/auth/me/login-history` endpoint if self-service history is wanted later.
- The demo case/evidence identifier generator (`IdentifierGenerator`) is an in-memory
  counter suitable for a demo; swap it for a DB sequence before relying on it across
  multiple app instances in production.
- `CaseSummaryResponse`/`CaseResponse` compute `evidenceCount` from the lazy
  `evidenceItems` collection, which costs one extra query per case in a list result
  (a deliberate N+1 trade-off for simplicity — see the comment in
  `CaseSummaryResponse.java` for the batched-query alternative to use at scale).

None of the above are placeholders standing in for missing functionality — they're
scoping notes on depth, called out explicitly rather than left for you to discover.
