# EduNexus — Unified API Contract (Phase 3, Task 4)

This catalogue merges five sources. It records **observed** behaviour only. Where an endpoint is missing, the "expected" shape is taken from how the frontend *consumes* it, quoted with file:line, and is never invented.

| Tag | Source |
|---|---|
| **SB** | Spring Boot controllers — `backend/src/main/java/com/edunexus/controller/*` |
| **EX** | Express routes and controllers mounted by `backend/src/server.ts` (the intended entry point) |
| **EX-idx** | Express stub routes mounted by `backend/src/index.ts` |
| **FE** | Live frontend callers — `frontend/src/store/slices/*`, `pages/*` |
| **FE-dead** | `frontend/src/services/api.ts` (never imported) |
| **RM** | `README.md` "API Endpoints" section (claims only) |

**Effective base URLs:**
* SB `http://localhost:8083/api` + controller path → `/api/api/...` (doubled prefix).
* EX `http://localhost:8080/api/...`.
* FE Redux client `http://localhost:8083/api`; FE raw `fetch` `http://localhost:8080/api`.

**Status values:** Implemented · Partial · Broken · Stub · Missing (see `phase-3-backend-report.md`).

---

## 1. Summary catalogue

| # | Endpoint | Method | Purpose | Auth | Role (declared) | Implemented in | Status | Referenced by |
|---|---|---|---|---|---|---|---|---|
| A1 | `/api/auth/register` | POST | Create account, return token | No | — | SB `AuthController.java:28`; EX `routes/auth.ts:23` | SB **Broken** (startup, path); EX **Partial** | FE `authSlice.ts:53`; FE-dead `api.ts:41`; RM |
| A2 | `/api/auth/login` | POST | Authenticate, return token | No | — | SB `AuthController.java:55`; EX `routes/auth.ts:24` | SB **Broken**; EX **Implemented** | FE `authSlice.ts:33`; FE-dead `api.ts:36`; RM |
| A3 | `/api/auth/me` | GET | Current user | Yes | any | — | **Missing** | FE-dead `api.ts:46`; RM |
| U1 | `/api/users/profile` | GET | Current user profile | Yes | any | EX `routes/user.ts:10`; EX-idx stub `routes/users.ts:7` | EX **Implemented**; EX-idx **Stub** | FE `authSlice.ts:67` (thunk never dispatched) |
| U2 | `/api/users/profile` | PUT | Update own name/email | Yes | any | EX `routes/user.ts:11` | **Partial** | FE `Profile.tsx:37` |
| U3 | `/api/users/password` | PUT | Change own password | Yes | any | EX `routes/user.ts:12` | **Implemented** (no strength rules) | none |
| U4 | `/api/users` | GET | List all users | Yes | admin | — | **Missing** | RM |
| U5 | `/api/users/{id}` | GET | Get user by id | Yes | — | — | **Missing** | RM |
| U6 | `/api/users/{id}` | PUT | Update user | Yes | — | — | **Missing** | RM |
| U7 | `/api/users/{id}` | DELETE | Delete user | Yes | — | — | **Missing** | RM |
| C1 | `/api/courses` | GET | List courses | EX: No / EX-idx: Yes | — | EX `routes/course.ts:15`; EX-idx stub `routes/courses.ts:7` | EX **Implemented**; EX-idx **Stub** | FE `courseSlice.ts:23` (CourseList); FE-dead `api.ts:53`; RM |
| C2 | `/api/courses/{id}` | GET | Course detail | No | — | EX `routes/course.ts:16` | **Implemented** (shape mismatch) | FE `CourseDetail.tsx:66,142`; FE `courseSlice.ts:35` (dead page); RM |
| C3 | `/api/courses` | POST | Create course | Yes | instructor, admin | EX `routes/course.ts:19` | **Broken** (always 403) | FE `courseSlice.ts:47` (no UI); RM |
| C4 | `/api/courses/{id}` | PUT | Update course | Yes | instructor, admin + owner/TA | EX `routes/course.ts:20` | **Broken** (403) | FE `courseSlice.ts:59` (no UI); RM |
| C5 | `/api/courses/{id}` | DELETE | Delete course | Yes | admin + owner | EX `routes/course.ts:21` | **Broken** (403) | FE `courseSlice.ts:71` (no UI); RM |
| C6 | `/api/courses/{id}/enroll` | POST | Enrol current user | Yes | student | EX `routes/course.ts:22` | **Broken** (403) | FE `CourseList.tsx:17`, `CourseDetail.tsx:135`, `courseSlice.ts:83` |
| C7 | `/api/courses/{id}/announcements` | POST | Post announcement | Yes | (unspecified) | — | **Missing** | FE-dead `api.ts:83` |
| C8 | `/api/courses/{id}/discussions` | POST | Start discussion | Yes | (unspecified) | — | **Missing** | FE-dead `api.ts:88` |
| C9 | `/api/courses/{id}/discussions/{did}/replies` | POST | Reply to discussion | Yes | (unspecified) | — | **Missing** | FE-dead `api.ts:93-96` |
| H1 | `/health` | GET | Liveness | No | — | EX `server.ts:32`, `index.ts:26` | **Implemented** | none |
| H2 | `/api/health` (actual `/api/api/health`) | GET | Liveness | **Yes** (not in permitAll) | — | SB `HealthController.java:9-15` | **Partial** (requires auth, wrong path) | none |
| D1 | `/api-docs`, `/swagger-ui.html` (actual `/api/...`) | GET | OpenAPI docs | **Yes** | — | SB config `application.properties:19-20` | **Partial** (behind auth) | none |

**Totals:** 22 catalogue entries.
* Implemented in at least one backend: **9** (A1, A2, U1, U2, U3, C1, C2, H1, H2).
* Of those, actually usable by the frontend as wired: **0**.
* Missing everywhere: **8** (A3, U4-U7, C7-C9).
* Broken by role checks: **4** (C3-C6).

---

## 2. Endpoint details

### A1 `POST /api/auth/register`
* **Request body:**
  * SB `RegisterRequest` (`dto/RegisterRequest.java:10-26`): `{ email, password, firstName, lastName, role }`. The annotations would require role non-blank and password ≥ 6, but are **unenforced** (no `@Valid`).
  * EX (`routes/auth.ts:8-16`): `{ email (isEmail), password (≥6), firstName, lastName, role? ∈ {STUDENT, ADMIN} }`.
  * FE sends (`Register.tsx:53-59` → `authSlice.ts:53`): `{ email, password, firstName, lastName, role: 'student'|'instructor'|'admin' }`.
* **Response:**
  * SB `200` `AuthenticationResponse { token, user: <User entity> }` (`AuthController.java:46-49`). The user serialises with `role` (singular).
  * SB errors: `400` plain text `"Email already exists"` or `"Registration failed: …"`.
  * EX `201 { token, user: { id, email, firstName, lastName, roles } }` (`controllers/auth.ts:39-48`).
  * EX errors: `400 { errors: [...] }`, `400 { message: 'User already exists' | 'Email already exists' }`, `500 { message: 'Server error' }`.
* **FE consumption:** reads `response.data.token`, `response.data.user` (`authSlice.ts:54-56,121-123`); error `response.data.message` (`:58`).
* **Broken / missing:**
  * SB: unreachable path, startup failure, any role accepted, plain-text errors.
  * EX: lowercase FE roles fail validation → 400; `instructor` cannot be registered; self-registration as `ADMIN` is possible.

### A2 `POST /api/auth/login`
* **Request:** `{ email, password }` — SB `AuthenticationRequest.java:12-15`; EX `routes/auth.ts:18-21`; FE `Login.tsx:49`.
* **Response:**
  * SB `200 { token, user }`; failure `400` plain text `"Authentication failed: …"` (`AuthController.java:70-72`).
  * EX `200 { token, user: { id, email, firstName, lastName, roles } }`; failure `400 { message: 'Invalid credentials' }` (`controllers/auth.ts:69-77`).
* **FE consumption:** same as A1 (`authSlice.ts:34-36,106-110`).
* **Token:** SB HS256 `{sub: email, iat, exp}`, 24 h; EX `{userId}`, `expiresIn: '24h'`. Neither contains roles.
* **Broken / missing:** SB path and startup; EX port mismatch with FE's Redux client (8083 vs 8080); FE and EX lack a lockout/rate limit.

### A3 `GET /api/auth/me` — **Missing**
* Referenced only by the dead `services/api.ts:45-48`. The live FE uses `GET /users/profile` for the same purpose (`authSlice.ts:67`) but never dispatches it.
* **Expected shape (from FE use):** the `User` object assigned to `state.auth.user` (`authSlice.ts:134-137`), i.e. `authSlice.ts:5-13` fields.

### U1 `GET /api/users/profile`
* **Auth:** Bearer token (`routes/user.ts:8`).
* **Response EX:** `200 <User document minus password>` with `enrolledCourses` and `managedCourses` populated with `{_id, title, description}` (`controllers/user.ts:8-17`). Note `_id`, not `id`. `404 {message:'User not found'}`. `500`.
* **EX-idx stub:** `200 { message: 'User profile route' }` (`routes/users.ts:7-9`).
* **FE:** `getCurrentUser` thunk (`authSlice.ts:63-73`) — **never dispatched**, which is the root of the Phase 2 reload bug.
* **Gap:** the FE expects `id` and string arrays; EX returns `_id` and populated objects.

### U2 `PUT /api/users/profile`
* **Body:** `{ firstName?, lastName?, email? }` (`controllers/user.ts:27`; FE `Profile.tsx:43`).
* **Response EX:** `200 { id, email, firstName, lastName, roles }` (`:41-47`); `404`; `500` (including duplicate email).
* **FE:** replaces `auth.user` with the response (`Profile.tsx:47-48`), which loses `enrolledCourses`/`managedCourses`.
* **Gap:** no validation, wrong status for duplicate email, response missing course arrays, FE calls port 8080 while the Redux client uses 8083.

### U3 `PUT /api/users/password`
* **Body:** `{ currentPassword, newPassword }` (`controllers/user.ts:57`).
* **Response:** `200 { message: 'Password updated successfully' }`; `400 { message: 'Current password is incorrect' }`; `404`; `500`.
* **FE:** no caller, no UI.
* **Gap:** no password policy.

### U4–U7 Admin user management — **Missing**
README claims only (`README.md:233-237`). No route, controller or FE caller exists. No response shape can be derived.

### C1 `GET /api/courses`
* **EX:** public. `200 [Course]` with `adminId` and `teachingAssistants` populated as `{_id, firstName, lastName, email}`; `enrolledStudents` as ids (`controllers/course.ts:5-15`).
* **EX-idx:** auth required, `200 { message: 'Courses route' }`.
* **FE:** `fetchCourses` (`courseSlice.ts:19-29`) → `state.courses.courses`. Each item is rendered with `course.id`, `title`, `description` (`CourseList.tsx:51-64`).
* **Gap:** `_id` vs `id` (React keys and enrol URL become `undefined`); no pagination, search or filter; SB missing entirely.

### C2 `GET /api/courses/{id}`
* **EX:** public. `200 Course` with `adminId`, `teachingAssistants` and `enrolledStudents` all populated (`controllers/course.ts:17-33`). `404 { message: 'Course not found' }`; invalid id → `500`.
* **FE (`CourseDetail.tsx`):**
  * reads `title`, `description`, `enrolledStudents.includes(user.id)`, `adminId === user.id`, `teachingAssistants.includes(user.id)` (`:116-117`);
  * reads `modules[].{id,title,description,contents[0].type}`, `announcements[].{id,title,content,createdAt}`, `discussions[].{id,title,content,createdAt}`.
* **Gap:**
  * Populated objects vs string-id comparisons → `isEnrolled`/`isInstructor` are always false.
  * `_id` vs `id` on subdocuments.
  * Content `type` casing.
  * FE does not check `response.ok`.

### C3 `POST /api/courses`
* **Body:** `{ title, description, modules? }` (`controllers/course.ts:37`). `adminId` is forced to the requester.
* **Response:** `201 Course`; `500`.
* **Declared guard:** `auth` + `checkRole(['instructor','admin'])` → **always 403** (E1/E2).
* **FE:** `createCourse` thunk only; no form.

### C4 `PUT /api/courses/{id}`
* **Body:** any — applied with `$set: req.body` (`controllers/course.ts:75-79`).
* **Response:** `200 Course (updated)`; `403 { message: 'Not authorized' }` if not owner/TA; `404`; `500`.
* **Declared guard:** `checkRole(['instructor','admin'])` → always 403.
* **Gap:** mass assignment; no field whitelist; no validation.

### C5 `DELETE /api/courses/{id}`
* **Response:** `200 { message: 'Course deleted successfully' }`; `403`; `404`; `500`.
* **Guard:** `checkRole(['admin'])` + owner check (`controllers/course.ts:97-100`) → always 403.
* **Gap:** `remove()` is version-dependent; students' `enrolledCourses` are not cleaned.

### C6 `POST /api/courses/{id}/enroll`
* **Body:** none.
* **Response:** `200 { message: 'Successfully enrolled in course' }`; `400 { message: 'Already enrolled in this course' }`; `404`; `500`.
* **Guard:** `checkRole(['student'])` → always 403.
* **FE:**
  * `CourseList.tsx:17-30` treats non-OK as `alert(data.message)`, so the user sees "Access denied".
  * `CourseDetail.tsx:135-148` ignores the result.
  * The `enrollInCourse` thunk expects a `Course` back (`courseSlice.ts:79-89,183-192`), but EX returns only `{message}`.
* **Gap:** no unenrol endpoint; response shape mismatch with the thunk.

### C7–C9 Announcements, discussions, replies — **Missing**
* FE-dead signatures:
  * `addAnnouncement(courseId, data)` (`api.ts:82-85`);
  * `addDiscussion(courseId, data)` (`:87-90`);
  * `addDiscussionReply(courseId, discussionId, data)` (`:92-98`).
* `data` is typed `any`, so no request shape can be derived. Storage targets exist as embedded arrays (`models/Course.ts:127-128`).
* No GET-by-sub-resource endpoints are referenced; the FE reads these arrays from C2.

### H1 / H2 / D1
* H1 `GET /health` → `200 { status: 'ok' }` (EX).
* H2 `GET /api/api/health` → `200 "Service is healthy"` (plain text, SB), but only with a valid token (`SecurityConfig.java:40`).
* D1: springdoc UI and docs are behind authentication.

---

## 3. Phase 2 expectation vs backend reality

| Phase 2 call site (frontend) | URL as sent | SB result (static reading) | EX result (static reading) | Match? |
|---|---|---|---|---|
| `authSlice.ts:33` login | POST `:8083/api/auth/login` | No handler at that path; blocked → 403 (inferred) | Port 8080, so connection refused at 8083 | ✗ |
| `authSlice.ts:53` register | POST `:8083/api/auth/register` | as above | as above; also lowercase role → 400 | ✗ |
| `courseSlice.ts:23` list | GET `:8083/api/courses` | No handler | wrong port | ✗ |
| `CourseList.tsx:17` enrol | POST `:8080/api/courses/{id}/enroll` | n/a (wrong port) | 403 `Access denied`; `{id}` is `undefined` because the list items carry `_id` | ✗ |
| `CourseDetail.tsx:66` detail | GET `:8080/api/courses/{id}` | n/a | 200 — but populated refs break `isEnrolled`/`isInstructor` | ◐ |
| `CourseDetail.tsx:135` enrol | POST `:8080/api/courses/{id}/enroll` | n/a | 403 | ✗ |
| `Profile.tsx:37` update | PUT `:8080/api/users/profile` | n/a | 200 `{id,email,firstName,lastName,roles}`. Works if a token from the 8080 backend exists — impossible, since login goes to 8083. | ◐ (isolated) |
| `authSlice.ts:67` current user (unused) | GET `:8083/api/users/profile` | No handler | wrong port | ✗ |

**Conclusion:** no frontend journey completes against either backend as committed.

---

## 4. Contract conflicts to resolve in the recreation

| Conflict | SB | EX | FE | Resolution needed |
|---|---|---|---|---|
| Base path | `/api/api` | `/api` | `/api` | single `/api` |
| Port | 8083 | 8080 | both | single port |
| Id key | `id` | `_id` | `id` | `id` |
| Role field | `role` | `roles[]` | `roles[]` | choose one (FE implies `roles[]`) |
| Role values | lower `student/instructor/admin` | upper `STUDENT/ADMIN` | lower, incl. `instructor` | one vocabulary incl. instructor |
| Error envelope | plain text | `{message}` / `{errors[]}` | `{message}` | `{message, errors?}` |
| Register status | 200 | 201 | any 2xx | 201 |
| Ref fields in course detail | ids (strings) | populated objects | ids (strings) for comparisons | ids, plus optional separate display fields |
| Enrol response | — | `{message}` | `Course` (thunk) / ignored (pages) | decide |
| Content type casing | UPPER | UPPER | lower | one casing |
| Author field | `authorId` | `authorId` | `author` | decide |
