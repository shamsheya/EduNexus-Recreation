# EduNexus — Recreation

A separate, from-scratch recreation of the EduNexus Learning Management System
(reference: https://github.com/kbhujbal/EduNexus-Full-Stack-Learning-Management-System).
It reproduces the reference UI and its declared API surface, but this time they actually work.
The full reverse-engineering analysis lives in `../analysis/`.

| Layer | Stack |
|---|---|
| Frontend | React 18, TypeScript, MUI 5, Redux Toolkit 1.9, React Router 6, Formik + Yup, axios, Vite |
| Backend | Spring Boot 3.5 (Java 21 target), Spring Security, Spring Data MongoDB, JJWT |
| Database | MongoDB (`users`, `courses` with embedded modules, announcements and discussions) |

## Run locally (no MongoDB install required)

Requirements: JDK 21+ and Node 18+. Maven is provided by the wrapper.

```bash
# 1. Backend on :8083 with an embedded MongoDB and fictional demo data
cd backend
./mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--app.demo-data=true

# 2. Frontend on :3000
cd frontend
npm install
npm run dev
```

Open http://localhost:3000.

To use a real MongoDB instead, set `MONGODB_URI` and run `./mvnw spring-boot:run`.

### Demo accounts (dev profile + `DEMO_DATA=true` only)
All demo accounts use the demo-only password `demo1234`. They are fictional and not real credentials.

| Email | Role |
|---|---|
| admin@demo.edunexus.test | admin |
| instructor@demo.edunexus.test | instructor (owns 2 courses) |
| ta@demo.edunexus.test | instructor (TA on "Introduction to Web Development") |
| student@demo.edunexus.test | student (enrolled in 1 course) |
| student2@demo.edunexus.test | student |

## Configuration (environment variable names; see `*/.env.example`)
* **Backend:**
  * `PORT` (8083), `MONGODB_URI`, `CORS_ORIGIN` (http://localhost:3000), `DEMO_DATA`;
  * `JWT_SECRET` (≥ 32 bytes; if unset, a random per-process key is used);
  * `JWT_EXPIRATION_MS` (24 h).
* **Frontend:** `VITE_API_URL` (http://localhost:8083/api).

## Tests
```bash
cd backend && ./mvnw test        # 19 integration tests against an embedded MongoDB
cd frontend && npm test          # 12 unit/component tests
cd frontend && npm run build     # typecheck + production build
```

## API
| Method | Path | Auth / role |
|---|---|---|
| POST | `/api/auth/register` | public (roles: student, instructor; admin is rejected) |
| POST | `/api/auth/login` | public |
| GET | `/api/auth/me` | authenticated |
| GET / PUT | `/api/users/profile` | authenticated (self) |
| PUT | `/api/users/password` | authenticated (self) |
| GET | `/api/courses`, `/api/courses/{id}` | authenticated |
| POST | `/api/courses` | instructor, admin |
| PUT | `/api/courses/{id}` | instructor/admin **and** owner, TA or admin |
| DELETE | `/api/courses/{id}` | instructor/admin **and** owner or admin |
| POST | `/api/courses/{id}/enroll` | student |
| POST | `/api/courses/{id}/announcements` | owner, TA or admin |
| POST | `/api/courses/{id}/discussions` | enrolled student, owner, TA or admin |
| POST | `/api/courses/{id}/discussions/{discussionId}/replies` | enrolled student, owner, TA or admin |
| GET | `/api/health` | public |

Errors always use `{ "message": "...", "errors"?: { "field": "reason" } }`. OpenAPI docs are served at `/swagger-ui.html`.

## Deliberate differences from the reference
See `../analysis/recreation-architecture.md` §8 (D1–D12). In short:
* Vite instead of CRA.
* Login password minimum 6 (matching registration).
* Session restore after a reload.
* Signed-in users are redirected away from `/login`.
* Courses require authentication.
* Course cards link to the course detail page.
* One API base URL.
* JSON errors.
* Real role enforcement.
* No committed secrets.
* Lowercase content types.
* Delete cleans every reference.
