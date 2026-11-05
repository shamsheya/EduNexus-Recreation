# EduNexus Recreation — Architecture (Phase 5)

| | |
|---|---|
| **Target folder** | `EduNexus-Recreation` (not yet created) |
| **Basis** | Phases 1–4 reports in `analysis/` |
| **Guiding principle** | Reproduce the reference's **intended** product — its UI exactly as designed and its API surface as declared — because the reference's observed runtime behaviour is non-functional (`phase-4-end-to-end-flows.md`). Every departure from reference behaviour is listed in §8 as a deliberate difference. |

---

## 1. Decision: one coherent stack

| Layer | Choice | Evidence-based reasoning |
|---|---|---|
| Frontend | **React 18 + TypeScript + MUI 5 + Redux Toolkit + React Router 6 + Formik/Yup + axios** | The same libraries and major versions as the reference (`frontend/package.json`), so visual and behavioural fidelity comes from using the same components and the same theme tokens. |
| Frontend tooling | **Vite** instead of CRA *(deliberate difference D1)* | CRA `react-scripts` 5 is unmaintained. The build tool is invisible to users and does not affect UI fidelity. |
| Backend | **Spring Boot 3 + Spring Security + Spring Data MongoDB (Java)** | (a) README names Spring Boot as the backend (`README.md:58-63`). (b) The frontend's primary client targets Spring's port 8083 (`utils/axios.ts:6`) and Spring's JSON conventions: `id` and lowercase `student/instructor/admin` (`types.ts:1`, `User.java:86-90`). (c) Spring's `Course` model is a field-for-field port of the Mongoose model, indicating Spring was the destination of an unfinished migration (`phase-3-backend-report.md` §4). (d) It has a buildable toolchain (Maven wrapper); Express has none. |
| API behaviour source | **Express controllers + frontend call sites** | Express holds the only implemented course/profile logic (`controllers/course.ts`, `controllers/user.ts`), and its owner/TA checks are correct. Its routes mirror the frontend's endpoint list. This logic is ported, not copied verbatim. |
| Database | **MongoDB**, collections `users` and `courses`, with embedded modules/contents/announcements/discussions/replies | Same model in both reference backends (`database-schema.md`). The embedded design is kept for fidelity. |
| Java version | Compile for **Java 21** (`--release 21`) using the installed JDK 25 | The reference targets 17. 21 is the nearest LTS supported by Spring Boot 3.x. The exact Spring Boot 3.x patch version is confirmed at milestone M0 against JDK 25 (not assumed). |
| Lombok | **Not used** (Java records/plain classes) | Avoids annotation-processor compatibility risk on JDK 25. Invisible to users. |

**Alternative considered — Express/Node backend.** Single-language stack and directly reusable logic. It was rejected because the reference never had a buildable Express project, the README and frontend conventions point to Spring, and the Mongoose `_id` convention conflicts with the frontend's `id` expectation. If the user prefers Node, the API contract in §3 is backend-agnostic and still applies.

## 2. Runtime topology (development)
```
Browser ──► Vite dev server :3000 ──(axios, VITE_API_URL=http://localhost:8083/api)──► Spring Boot :8083 ──► MongoDB :27017 / db edunexus
```
* Port 3000 matches the reference CORS origin.
* Port 8083 matches the reference's primary client and README.
* **No servlet context path**: controllers map `/api/...` directly, fixing the doubled-prefix defect.

## 3. API modules (contract)

All bodies are JSON. Errors use the envelope `{ "message": string, "errors"?: { field: message } }`. Ids are strings named `id`. Roles are lowercase.

| Module | Endpoint | Method | Auth | Permission | Source in reference |
|---|---|---|---|---|---|
| **auth** | `/api/auth/register` | POST | public | — | SB `AuthController:28`, EX `routes/auth.ts:23` |
| | `/api/auth/login` | POST | public | — | SB `:55`, EX `:24` |
| | `/api/auth/me` | GET | JWT | any | FE-dead `api.ts:46`, README `:231` |
| **users** | `/api/users/profile` | GET | JWT | self | EX `routes/user.ts:10` |
| | `/api/users/profile` | PUT | JWT | self | EX `:11`, FE `Profile.tsx:37` |
| | `/api/users/password` | PUT | JWT | self | EX `:12` |
| **courses** | `/api/courses` | GET | JWT | any authenticated | EX `routes/course.ts:15` (*D5: public → authenticated*) |
| | `/api/courses/{id}` | GET | JWT | any authenticated | EX `:16` |
| | `/api/courses` | POST | JWT | instructor, admin | EX `:19` |
| | `/api/courses/{id}` | PUT | JWT | owner/TA or admin; field whitelist | EX `:20` + `controllers/course.ts:70-73` |
| | `/api/courses/{id}` | DELETE | JWT | owner or admin | EX `:21` + `controllers/course.ts:97-100` (*open decision OD2*) |
| | `/api/courses/{id}/enroll` | POST | JWT | student | EX `:22` |
| **course-content** | `/api/courses/{id}/announcements` | POST | JWT | owner/TA or admin | FE-dead `api.ts:83` |
| | `/api/courses/{id}/discussions` | POST | JWT | enrolled, owner/TA or admin | FE-dead `api.ts:88` |
| | `/api/courses/{id}/discussions/{did}/replies` | POST | JWT | enrolled, owner/TA or admin | FE-dead `api.ts:93` |
| **health** | `/api/health` | GET | public | — | SB `HealthController`, EX `/health` |

**Out of scope by default** (README-only or inferred; optional extras only if approved): admin user CRUD (`/api/users`, `/api/users/{id}`), unenrol, TA management, module sub-resource CRUD, assignments, grades, progress, categories, analytics.

### 3.1 Response shapes (derived from frontend consumption)
* **AuthResponse:** `{ token, user: UserDto }` (`authSlice.ts:34-36,106-110`).
* **UserDto:** `{ id, email, firstName, lastName, roles: string[], enrolledCourses: string[], managedCourses: string[] }` (`authSlice.ts:5-13`).
* **CourseSummaryDto:** `{ id, title, description }`, plus the full detail fields if cheap (`CourseList.tsx:52-60`).
* **CourseDto:**
  * `{ id, title, description, adminId, enrolledStudents: string[], teachingAssistants: string[], modules[], announcements[], discussions[] }`;
  * `modules[]: { id, title, description, dueDate, contents[]: { id, title, type (lowercase), content, required } }`;
  * `announcements[]: { id, title, content, authorId, createdAt }`;
  * `discussions[]: { id, title, content, authorId, isAnonymous, createdAt, replies[] }`.
  * Source: `CourseDetail.tsx:116-246`, `database-schema.md` §2.
* **Enrol response:** `CourseDto`. This satisfies the `enrollInCourse` thunk (`courseSlice.ts:183-192`); pages ignore the body anyway.
* **Register status:** `201`. Login: `200`.

## 4. Database entities
As in `database-schema.md`, with these normalisations:

| Entity | Fields (recreation) | Change vs reference |
|---|---|---|
| `users` | `id, email (unique index), passwordHash, firstName, lastName, roles: [student\|instructor\|admin], enrolledCourses: [courseId], managedCourses: [courseId], createdAt, updatedAt` | `roles[]` lowercase (FE shape); explicit unique index creation |
| `courses` | `id, title, description, adminId, enrolledStudents[], teachingAssistants[], modules[], announcements[], discussions[], createdAt, updatedAt` | `updatedAt` maintained; indexes on `adminId`, `enrolledStudents` |
| embedded | `CourseModule{id,title,description,dueDate,contents[]}`, `ModuleContent{id,title,type,content,required}`, `Announcement{id,title,content,authorId,createdAt}`, `Discussion{id,title,content,authorId,isAnonymous,replies[],createdAt}`, `DiscussionReply{id,content,authorId,isAnonymous,createdAt}` | Subdocument ids generated server-side |

Bidirectional references (`enrolledStudents` ↔ `enrolledCourses`, `adminId` ↔ `managedCourses`) are maintained by the service layer, with deletes cleaning both sides.

## 5. Authentication and permission design
* **Passwords:** BCrypt (strength 10, as in the reference).
* **Tokens:** a single `JwtService`, HS256.
  * Claims: `sub` = user id, `email`, `roles`, `iat`, `exp`. Lifetime 24 h, as in the reference.
  * The secret comes from the `JWT_SECRET` environment variable. There is **no committed secret**. In the dev profile a random secret is generated at startup when the variable is unset; tokens are then invalidated on restart, which is acceptable for demos.
* **Filter:** validates the token and converts every JWT failure into a JSON `401 {message}`. Authorities are `ROLE_STUDENT`, `ROLE_INSTRUCTOR`, `ROLE_ADMIN`.
* **Global roles:** `@EnableMethodSecurity` with `@PreAuthorize("hasAnyRole(...)")`.
* **Course-scoped roles:** a `CoursePermissionService` with `isOwner`, `isTa`, `isEnrolled`, used in `@PreAuthorize` SpEL or service checks. This ports `controllers/course.ts:70-73,97-100`.
* **Validation:** Bean Validation with `@Valid` on all request DTOs. Password ≥ 6 on register and change. Login only requires non-empty.
* **CORS:** origin from `CORS_ORIGIN` (default `http://localhost:3000`).
* **Registration roles:** see OD1.
* **Frontend:**
  * Token in localStorage, as in the reference (D-none).
  * On boot, if a token exists, dispatch `getCurrentUser` (`/api/auth/me`) (*D3*).
  * 401 → logout + redirect, as in the reference.
  * A reverse guard redirects logged-in users away from `/login` and `/register` (*D4*).

## 6. Frontend pages (reproduced 1:1 from `phase-2-frontend-report.md` §3–§7)
| Route | Page | Fidelity notes |
|---|---|---|
| `/login` | Login | Same layout, copy, Formik/Yup; password rule aligned to ≥ 6 (*D2*) |
| `/register` | Register | Same fields, dropdown and copy; role options per OD1 |
| `/` → `/dashboard` | Dashboard | Same 3 stat Papers + static "Recent Activity" |
| `/courses` | CourseList | Same grid/card; **card title links to detail** (*D6*); Enroll via Redux thunk |
| `/courses/:id` | CourseDetail | Same tabs and lists; module contents still summarised as in the reference |
| `/profile` | Profile | Same form; uses the shared API client |
| `*` | redirect `/` | Same |

* **Shell:** the same `Layout` (240 px drawer, AppBar avatar menu, 3 nav items).
* **Theme:** `theme.ts` tokens copied exactly (`phase-2-frontend-report.md` §7.1).
* **Single HTTP client:** one axios instance with base `VITE_API_URL` (*D7*). No raw `fetch`. One `types.ts`.

## 7. Folder structure
```
EduNexus-Recreation/
├── README.md                      run instructions, env var names, deliberate differences
├── .gitignore
├── backend/
│   ├── mvnw, mvnw.cmd, .mvn/wrapper/   (generated by Maven wrapper tooling, pinned)
│   ├── pom.xml
│   ├── .env.example               variable NAMES only
│   └── src/
│       ├── main/java/com/edunexus/
│       │   ├── EduNexusApplication.java
│       │   ├── config/            SecurityConfig, MongoConfig(indexes), CorsConfig
│       │   ├── security/          JwtService, JwtAuthFilter, AppUserPrincipal
│       │   ├── auth/              AuthController, AuthService, dto/
│       │   ├── user/              UserController, UserService, User, UserRepository, dto/
│       │   ├── course/            CourseController, CourseService, CoursePermissionService, Course(+embedded), CourseRepository, dto/
│       │   ├── content/           AnnouncementController, DiscussionController
│       │   ├── common/            ApiError, GlobalExceptionHandler, HealthController
│       │   └── demo/              DemoDataLoader (dev profile, opt-in flag)
│       ├── main/resources/application.yml   (no secrets; env placeholders)
│       └── test/java/com/edunexus/…        unit + slice + integration tests
└── frontend/
    ├── package.json, package-lock.json, vite.config.ts, tsconfig.json, index.html
    ├── .env.example               VITE_API_URL
    └── src/
        ├── main.tsx, App.tsx, theme.ts, types.ts
        ├── api/client.ts          single axios instance + interceptors
        ├── store/ (index.ts, slices/authSlice.ts, slices/courseSlice.ts)
        ├── components/ (Layout, PrivateRoute, PublicOnlyRoute, ErrorMessage, LoadingSpinner, SuccessMessage)
        ├── pages/ (Login, Register, Dashboard, CourseList, CourseDetail, Profile)
        └── __tests__/
```

## 8. Deliberate differences from the reference
| ID | Difference | Reason | User-visible? |
|---|---|---|---|
| D1 | Vite instead of CRA | CRA unmaintained | No |
| D2 | Login password min 6 (reference 8) | The reference rejects valid registered users | Validation message only |
| D3 | Session restore on reload | The reference shows a blank user | Yes (fix) |
| D4 | Logged-in users redirected away from `/login` and `/register` | Coherence | Minor |
| D5 | Course list/detail require auth (reference Express: public) | The reference UI already gates these pages; prevents PII exposure | No (UI already gated) |
| D6 | Course card links to detail | The reference has no navigation path to detail | Yes (small) |
| D7 | Single API base URL / port | The reference used two | No |
| D8 | JSON error envelope everywhere | The frontend expects `{message}` | Error texts become meaningful |
| D9 | Real role enforcement | The reference has none / always-deny | Yes (correct 403s) |
| D10 | Secret from env, never committed | Security | No |
| D11 | Content `type` serialised lowercase | Matches the frontend icon mapping | Icons render correctly |
| D12 | Course delete cleans `enrolledCourses` too | Integrity | No |

## 9. Open decisions (need user confirmation before Phase 6)
| ID | Question | Recommendation |
|---|---|---|
| OD1 | Allow self-registration as **admin**? The reference dropdown offers it. | Keep the Admin option visible for fidelity but **reject `admin` on the backend**; provide one seeded demo admin. Alternative: allow it (exact reference behaviour, insecure). |
| OD2 | May instructors delete their own courses? (Express: admin-only + owner) | Owner **and** admin |
| OD3 | Include optional extras (unenrol, TA management, module CRUD, admin user list)? | No for the first build; reassess after Phase 7 |
| OD4 | MongoDB runtime for dev/tests (none installed; no Docker) | (a) the user installs MongoDB Community locally, **or** (b) an embedded MongoDB test dependency inside the recreation, which downloads a `mongod` binary to a user cache on first test run |
| OD5 | Authorise dependency downloads (Maven Central via `mvnw`, npm registry) into the recreation folder only | Required for Phase 6 |
| OD6 | Backend stack confirmation (Spring recommended; Express alternative) | Spring |
