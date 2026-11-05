# EduNexus Recreation — Implementation Plan (Phase 5)

* **Architecture:** `recreation-architecture.md`.
* **Status:** PLAN ONLY. Nothing is created yet.
* **Phase 6 start gate:** open decisions OD1–OD6 must be confirmed (`recreation-architecture.md` §9).
* **Logging:** every milestone appends to `analysis/implementation-log.md`. Each milestone ends with its acceptance checks actually run and the results recorded.

## Consistency check (Phases 1–5)
| Item | Covered by |
|---|---|
| Every reference page/route | `phase-2-frontend-report.md` §3–4 → architecture §6 |
| Every frontend API call | `api-contract.md` §1 → architecture §3 (all live and latent calls mapped; README-only excluded by default) |
| Every entity/relationship | `database-schema.md` → architecture §4 |
| Every role rule | `role-permission-matrix.md` §5 → architecture §5 (OD1, OD2 open) |
| Every journey | `phase-4-end-to-end-flows.md` J1–J9 → acceptance criteria below |
| Deviations | architecture §8 (D1–D12) |

There are no unmapped items.

## Milestones

### M0 — Toolchain and approvals (no app code)
* **Tasks:**
  * Confirm OD1–OD6.
  * Create `EduNexus-Recreation/` with `.gitignore` and README skeleton; `git init` (separate repository).
  * Pin Spring Boot 3.x and frontend dependency versions; verify the Spring Boot version supports the installed JDK 25 runtime with `--release 21`.
  * Provision MongoDB per OD4.
* **Acceptance:**
  * `java -version` and `node -v` are recorded.
  * `./mvnw -v` works inside `backend/` (downloads Maven into the user's `.m2` wrapper cache).
  * MongoDB reachable, or the embedded test database is confirmed.
  * The original repository's `git status` is clean.

### M1 — Backend skeleton
* **Tasks:**
  * `pom.xml` (web, security, data-mongodb, validation, jjwt or spring-security-oauth2-resource-server's Nimbus — chosen at M0, springdoc, test).
  * `application.yml` with env placeholders.
  * `GlobalExceptionHandler` + `ApiError`.
  * `GET /api/health`.
  * Index creation (users.email unique; courses.adminId, courses.enrolledStudents).
* **Acceptance:**
  * `mvnw test` passes.
  * A context-load test passes.
  * `/api/health` returns 200 without auth.
  * An unknown route returns JSON 404.
  * No secrets in the repository (grep check).

### M2 — Authentication
* **Tasks:**
  * `User` model + repository.
  * `AuthController` (register, login, me), `JwtService`, `JwtAuthFilter`.
  * BCrypt; `@Valid` DTOs; role policy per OD1.
* **Acceptance (automated tests):**
  * Register → 201 `{token,user{id,email,firstName,lastName,roles[],enrolledCourses[],managedCourses[]}}`.
  * Duplicate email → 400 `{message}`.
  * Invalid fields → 400 `{message, errors}`.
  * Disallowed role → 400.
  * Login OK → 200; wrong password → 401 `{message}`.
  * `/me` without, with expired or with tampered token → 401 JSON; with a valid token → 200.
  * The password hash never appears in any response.

### M3 — Users / profile
* **Tasks:** `GET`/`PUT /api/users/profile`, `PUT /api/users/password`.
* **Acceptance:**
  * Update returns the full `UserDto`.
  * Duplicate email → 400.
  * Wrong current password → 400.
  * New password < 6 → 400.
  * Users can only touch their own profile.

### M4 — Courses and enrolment
* **Tasks:**
  * `Course` + embedded types.
  * List and detail (ids as strings, lowercase content types).
  * Create (instructor/admin; sets `adminId`, updates `managedCourses`).
  * Update with field whitelist (owner/TA/admin).
  * Delete (OD2; cleans both reference arrays).
  * Enrol (student; idempotent 400 if already enrolled; returns `CourseDto`).
* **Acceptance (permission tests per role):**
  * Student create → 403.
  * Instructor create → 201.
  * Non-owner instructor update → 403; TA update → 200.
  * Student enrol → 200, then again → 400.
  * Instructor enrol → 403.
  * Delete removes the course id from every user array.
  * Unauthenticated list → 401.

### M5 — Course content
* **Tasks:** POST announcements (owner/TA/admin), discussions and replies (enrolled/owner/TA/admin), server-generated subdocument ids, `createdAt`.
* **Acceptance:**
  * Each endpoint returns the updated `CourseDto` (or the created item — fixed at M5 and documented).
  * Permission matrix tests for all three.

### M6 — Frontend scaffold and auth screens
* **Tasks:**
  * Vite React TS project; same library majors as the reference.
  * `theme.ts` copied tokens; `App.tsx` routes (same table).
  * `Layout`, `PrivateRoute`, `PublicOnlyRoute`.
  * API client; `authSlice` with session restore; Login and Register pages.
* **Acceptance:**
  * `npm run build` succeeds.
  * Lint and typecheck are clean.
  * Unit tests: auth slice reducers and form validation.
  * Manual browser check: register → dashboard → reload keeps the user → logout → `/login`.

### M7 — Remaining pages and integration
* **Tasks:** Dashboard, CourseList (with link to detail), CourseDetail (tabs; enrol via thunk; error handling), Profile.
* **Acceptance:**
  * Journeys J1–J8 pass against the local backend (recorded per step in the log).
  * Responsive check at 375 px, 768 px and 1280 px widths.

### M8 — Safe demo data (opt-in)
* **Tasks:**
  * `DemoDataLoader`, active only with `--spring.profiles.active=dev` **and** `DEMO_DATA=true`.
  * Creates 1 admin, 1 instructor and 2 students with documented demo-only passwords printed in the README (not real credentials), plus 2–3 courses with modules, announcements and discussions.
* **Acceptance:**
  * Never runs by default.
  * Idempotent.
  * No real personal data.

### M9 → Phase 7 — Test and compare
* **Outputs:**
  * `analysis/test-report.md` (automated and manual results);
  * `analysis/ui-comparison.md` (each screen vs `phase-2-frontend-report.md`, differences classified as deliberate D# or defect);
  * high-priority defects fixed in the recreation only;
  * `analysis/final-summary.md`.

## Risks
| Risk | Mitigation |
|---|---|
| JDK 25 vs Spring Boot 3.x compatibility | Verify at M0; fall back to the newest Spring Boot line that supports JDK 25, or ask the user for a JDK 21 |
| No MongoDB | OD4 decision before M2 integration tests |
| Visual fidelity without reference screenshots (the reference never ran) | Compare against source-derived specs (component tree, `sx` values, theme). Optionally run the reference frontend alone in Phase 7 with user approval to capture screenshots of its static screens (Login/Register render without a backend). |
| Dependency supply chain | Pinned versions, lockfiles committed, no global installs |
