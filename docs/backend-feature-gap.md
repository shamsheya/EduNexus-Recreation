# EduNexus — Backend Feature-Gap Analysis (Phase 3)

**Inputs:**
* `phase-2-frontend-report.md` §8–§10 (frontend needs);
* `api-contract.md` (endpoint status);
* `database-schema.md` §6 (missing entities);
* `role-permission-matrix.md`.

**Classification:**
* **Required** — needed by a live frontend screen or call.
* **Latent** — needed by frontend code that exists but is dead or unrouted.
* **README-only** — claimed in docs, no code anywhere.
* **Inferred** — needed to make a required journey coherent; not referenced by any code.

**Priority:**
* **P1** — blocks a core journey of the reference UI.
* **P2** — completes a visible but partial feature.
* **P3** — README-only or optional.

## 1. Gaps blocking frontend journeys that exist today

| # | Gap | Needed by (frontend) | Spring | Express | Class | Priority |
|---|---|---|---|---|---|---|
| G1 | Auth endpoints reachable at `/api/auth/*` on the port the UI uses, with JSON `{message}` errors | `authSlice.ts:33,53,38,58` | Path doubled, plain-text errors, startup blocked | Correct path, wrong port, role-casing rejection | Required | P1 |
| G2 | Registration accepting `student`/`instructor`/`admin` | `Register.tsx:41-44,167-169` | Accepts any string | Rejects lowercase, no instructor | Required | P1 |
| G3 | Auth response user with `id` and `roles[]` (+ `enrolledCourses`, `managedCourses`) | `authSlice.ts:5-13`; `Dashboard.tsx:23,34,45` | `role` singular | `roles` uppercase, no course arrays | Required | P1 |
| G4 | Current-user endpoint for session restore | `authSlice.ts:63-73` (exists, never dispatched) | Missing | `GET /api/users/profile` (returns `_id`) | Required | P1 |
| G5 | List courses with `id` | `CourseList.tsx:51-64`; `courseSlice.ts:23` | Missing | Implemented (returns `_id`) | Required | P1 |
| G6 | Course detail with id-string refs and subdocument `id`s | `CourseDetail.tsx:116-246` | Missing | Implemented (populated refs, `_id`) | Required | P1 |
| G7 | Enrolment that works for students | `CourseList.tsx:17`; `CourseDetail.tsx:135` | Missing | Always 403 | Required | P1 |
| G8 | Working role enforcement (global role + course ownership) | implicit in every protected action | None | Always-deny | Required | P1 |
| G9 | Profile update with validation and a full user response | `Profile.tsx:37-49` | Missing | Partial (500 on duplicate email, drops course arrays) | Required | P2 |
| G10 | Consistent error envelope `{message}` | `authSlice.ts:38`; `CourseList.tsx:29` | Plain text | Mostly consistent | Required | P2 |
| G11 | CORS for the frontend dev origin | all calls | localhost:3000 | localhost:3000 (`server.ts`) / open (`index.ts`) | Required | P1 |
| G12 | Content `type` values matching the UI icon mapping | `CourseDetail.tsx:87-98` | UPPER enum | UPPER enum | Required | P2 |

## 2. Latent gaps (frontend code exists but has no UI path)

| # | Gap | Frontend evidence | Backend status | Priority |
|---|---|---|---|---|
| L1 | Create course | `courseSlice.ts:43-53` | Express only, 403 | P2 |
| L2 | Update course (safe field whitelist) | `courseSlice.ts:55-65` | Express only, 403, mass assignment | P2 |
| L3 | Delete course with referential cleanup | `courseSlice.ts:67-77` | Express only, 403, leaves `enrolledCourses` dangling | P2 |
| L4 | Enrol returning an updated `Course` | `courseSlice.ts:79-89,183-192` | Express returns `{message}` | P2 |
| L5 | Post announcement | `services/api.ts:82-85` | Missing | P2 |
| L6 | Start discussion | `services/api.ts:87-90` | Missing | P2 |
| L7 | Reply to discussion | `services/api.ts:92-98` | Missing | P2 |
| L8 | `GET /api/auth/me` | `services/api.ts:45-48` | Missing (G4 overlaps) | P2 |
| L9 | Course marketing fields (price, level, duration, instructor name) | `CourseDetails.tsx:46-56`; `types.ts:16-19` | Missing in both models | P3 |

## 3. Backend capabilities with no frontend consumer
| Capability | Location | Note |
|---|---|---|
| Change password | Express `routes/user.ts:12` | Keep; add UI in the recreation as a deliberate addition (decision) |
| Health | Express `server.ts:32`; Spring `HealthController` | Keep (ops) |
| Spring finders by admin/student/TA | `CourseRepository.java:11-13` | Suggest "my courses" queries (Inferred) |
| Swagger/OpenAPI config | `application.properties:19-20` | Useful for the recreation |

## 4. Inferred gaps (needed for coherent journeys; no code references them)

| # | Gap | Why | Priority |
|---|---|---|---|
| I1 | Unenrol | The enrolment journey has no reverse action | P3 |
| I2 | Manage TAs (add/remove) | `teachingAssistants` is read by auth logic, but nothing can write it safely | P2 |
| I3 | Module and content CRUD as sub-resources | Modules are only writable through a whole-course update | P2 |
| I4 | "My courses" (enrolled/managed) listing | Dashboard shows counts only | P3 |
| I5 | Server-side logout / token revocation | Logout is client-only | P3 |
| I6 | Seed or bootstrap admin account | If self-registration as admin is disallowed, an admin must come from somewhere | P1 (if admin self-registration is removed) |

## 5. README-only features (no code anywhere)

| Feature | README line | Priority |
|---|---|---|
| Assignments + submissions + grading | `:32-33,38,168-188` | P3 |
| Progress tracking | `:25,30,40` | P3 |
| Course categorisation | `:26,139` | P3 |
| Analytics and reporting | `:46` | P3 |
| System configuration | `:45` | P3 |
| Admin user CRUD + role management | `:43,47,234-237` | P3 (P2 if the admin role is kept) |
| CSRF protection | `:251` | n/a (stateless JWT; not required) |

## 6. Gap count summary
| Class | Count |
|---|---|
| Required (P1/P2) | 12 |
| Latent | 9 |
| Inferred | 6 |
| README-only | 7 |

**Key point for Phase 5:** reproducing the *reference UI* only requires closing G1–G12 plus, optionally, L1–L8. README-only features would make the recreation **exceed** the reference. Phase 5 should treat them as explicit deliberate additions or exclude them.
