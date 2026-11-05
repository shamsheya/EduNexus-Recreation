# EduNexus — Phase 4: Database & End-to-End Flows

**Database deliverable:** `analysis/database-schema.md`, which holds the field tables, indexes, evidenced vs inferred relationships, Mermaid ER diagram and missing entities. This document traces each user journey **UI → frontend state → HTTP → backend → database**, using static reading only (nothing executed).

**Per-step verdicts:**
* ✓ works as coded;
* ✗ breaks here (reason given);
* ◐ works only under conditions that the reference never satisfies together;
* *(inferred)* depends on framework semantics.

**Index summary** (from `database-schema.md` §1, §5):
* Express `users.email` unique (`models/User.ts:8`).
* Spring `@Indexed(unique)` on `email` (`User.java:32`), likely not auto-created *(inferred)*.
* No other indexes.

---

## J1 — Registration
| Step | Layer | Code | Verdict |
|---|---|---|---|
| 1 | UI form: firstName, lastName, email, password ≥ 6, role (`student` default) | `Register.tsx:26-59,107-171` | ✓ |
| 2 | Thunk `register` → POST `http://localhost:8083/api/auth/register` | `authSlice.ts:43-61`; `utils/axios.ts:6` | — |
| 3a | **Spring** on 8083: no handler at `/api/auth/register`; real path `/api/api/auth/register` | `application.properties:2-3`; `AuthController.java:18,28` | ✗ path; also the context fails to start (S1, inferred) |
| 3b | **Express** handler (`routes/auth.ts:23`) listens on 8080 | `server.ts:93` | ✗ port |
| 4 (hypothetical, Express reached) | Validator rejects `role:'student'` (needs `STUDENT`/`ADMIN`) | `routes/auth.ts:15` | ✗ 400 |
| 5 (hypothetical, Spring reached) | Saves `users` doc `{email, password(bcrypt), firstName, lastName, role:'student'}`; returns `{token, user}` | `AuthController.java:35-49` | ◐ |
| 6 | FE stores the token, sets `user`, navigates `/` → `/dashboard` | `authSlice.ts:55,120-124`; `Register.tsx:64` | ✓ if 2xx |
| **DB effect** | `users` insert | — | — |
| **Outcome** | **Fails** in the reference as wired | | |

## J2 — Login
| Step | Layer | Code | Verdict |
|---|---|---|---|
| 1 | UI: email, password ≥ **8** | `Login.tsx:18-27` | ✓ (rejects 6–7 character passwords allowed at registration) |
| 2 | POST `:8083/api/auth/login` | `authSlice.ts:33` | — |
| 3 | Spring: path mismatch / startup; Express: port mismatch | as J1 | ✗ |
| 4 (hypothetical) | Express: `findOne` + bcrypt → `{token({userId}), user{id,…,roles}}`; Spring: `authenticate` → `{token(sub=email), user{…, role}}` | `controllers/auth.ts:68-95`; `AuthController.java:58-69` | ◐ |
| 5 | FE renders Dashboard: `user.roles.join` → blank for Spring (`role` singular); counts from `enrolledCourses`/`managedCourses` | `Dashboard.tsx:12-45` | ◐ |
| **DB effect** | read `users` | | |
| **Outcome** | **Fails** as wired | | |

## J3 — Session restore (page reload)
| Step | Code | Verdict |
|---|---|---|
| Token rehydrated from localStorage | `authSlice.ts:24` | ✓ |
| `user` stays `null`; `getCurrentUser` never dispatched | `authSlice.ts:23,63-73` | ✗ |
| Dashboard "Welcome, !"; avatar blank; Profile form empty | `Dashboard.tsx:12`; `Layout.tsx:115`; `Profile.tsx:20-24` | ✗ |
| Backend support: Express `GET /api/users/profile` | `routes/user.ts:10` | exists, unused |

## J4 — Course browsing
| Step | Layer | Code | Verdict |
|---|---|---|---|
| 1 | Drawer "Courses" → `/courses` | `Layout.tsx:61` | ✓ |
| 2 | `fetchCourses` → GET `:8083/api/courses` | `courseSlice.ts:19-29` | — |
| 3 | Spring: no course endpoint; Express on 8080 | — | ✗ |
| 4 (hypothetical, Express reached) | `Course.find()` + populate `adminId`, `teachingAssistants` → array with `_id` | `controllers/course.ts:7-10` | ✓ backend |
| 5 | FE uses `course.id` for key and enrol → `undefined` | `CourseList.tsx:52,64` | ✗ |
| 6 | No navigation from card to detail | `CourseList.tsx:53-69` | ✗ (UX dead end) |
| **DB effect** | read `courses`, join `users` | | |

## J5 — Course detail
| Step | Code | Verdict |
|---|---|---|
| Reached only by typing `/courses/:id` | `App.tsx:34` | ◐ |
| `fetch GET :8080/api/courses/:id` (Express port) | `CourseDetail.tsx:66` | ✓ if Express runs |
| Express returns course with **populated** `adminId`/`teachingAssistants`/`enrolledStudents` | `controllers/course.ts:19-22` | ✓ backend |
| `isEnrolled`/`isInstructor` compare objects to `user.id` → always false | `CourseDetail.tsx:116-117` | ✗ |
| Module icon expects lowercase types; DB stores UPPER | `CourseDetail.tsx:87-98`; `models/Course.ts:10` | ✗ (always a document icon) |
| Subdocument keys use `module.id`; DB has `_id` | `CourseDetail.tsx:169,195,223` | ✗ (React key warnings) |
| No `response.ok` check → an error body crashes render | `CourseDetail.tsx:71-72,116` | ✗ on 404/500 |
| **DB effect** | read `courses`, join `users` ×3 | |

## J6 — Enrolment
| Step | Code | Verdict |
|---|---|---|
| Button on the list (all users) or on detail (non-owner/TA) | `CourseList.tsx:61-67`; `CourseDetail.tsx:128-156` | ✓ UI |
| POST `:8080/api/courses/:id/enroll` with Bearer | `CourseList.tsx:17-22`; `CourseDetail.tsx:135-140` | — |
| Express `auth` ✓ → `checkRole(['student'])` → `req.user.roles` undefined → **403** | `routes/course.ts:22`; `middleware/auth.ts:43-48` | ✗ |
| (unreachable) push user → `courses.enrolledStudents`; push course → `users.enrolledCourses` | `controllers/course.ts:132-138` | — |
| FE list shows `alert('Access denied')`; detail silently refetches | `CourseList.tsx:28-29`; `CourseDetail.tsx:141-148` | ✗ |
| **Intended DB effect** | `courses.enrolledStudents` += userId; `users.enrolledCourses` += courseId (two non-atomic writes) | |

## J7 — Content access (modules, announcements, discussions)
| Step | Code | Verdict |
|---|---|---|
| Tabs render embedded arrays from the course document | `CourseDetail.tsx:158-247` | ◐ read-only |
| Module contents (video/doc/quiz URLs) are **never rendered** | `CourseDetail.tsx:166-190` | ✗ missing |
| Replies never rendered; no composer | `CourseDetail.tsx:220-246` | ✗ missing |
| No endpoint to add modules, announcements, discussions or replies (only the unreachable mass-assignment PUT) | `api-contract.md` C4, C7-C9 | ✗ missing |
| No enrolment check on reading content (course detail is public in Express) | `routes/course.ts:16` | security gap |

## J8 — Profile view / update
| Step | Code | Verdict |
|---|---|---|
| Form initialised from `auth.user` (empty after reload — J3) | `Profile.tsx:20-24` | ◐ |
| PUT `:8080/api/users/profile` `{firstName,lastName,email}` | `Profile.tsx:37-44` | — |
| Express `auth` ✓ → update fields, save → `{id,email,firstName,lastName,roles}` | `controllers/user.ts:24-47` | ✓ backend (but the token must come from Express, and login targets 8083 → no such token exists) |
| Duplicate email → 11000 → 500 | `controllers/user.ts:48-51` | ✗ |
| FE replaces `user` → loses `enrolledCourses`/`managedCourses` → Dashboard counts 0 | `Profile.tsx:48` | ✗ |
| **DB effect** | update `users` | |

## J9 — Role-specific journeys
| Journey | Path | Verdict |
|---|---|---|
| **Student:** register → browse → enrol → study → discuss | J1 ✗ → J4 ✗ → J6 ✗ → J7 missing | ✗ end to end |
| **Instructor:** register (Express has no `instructor` role) → create course → add modules → announce | No UI; `POST /api/courses` 403; no sub-resource APIs | ✗ (no UI at all) |
| **Admin:** self-register as admin → manage users/courses | No UI; no user-admin API; delete 403 | ✗ (no UI at all) |
| **Any:** logout | `Layout.tsx:54-57` → clears state → `/login` | ✓ (client-only) |

## Summary table
| Journey | UI present | API present (any backend) | Works end to end in reference |
|---|---|---|---|
| J1 Register | ✓ | ✓ | ✗ |
| J2 Login | ✓ | ✓ | ✗ |
| J3 Session restore | ✗ | ✓ (Express) | ✗ |
| J4 Browse | ✓ | ✓ (Express) | ✗ |
| J5 Detail | ✓ (URL only) | ✓ (Express) | ✗ |
| J6 Enrol | ✓ | ✓ (403) | ✗ |
| J7 Content | ◐ read-only | ✗ | ✗ |
| J8 Profile | ✓ | ✓ (Express) | ✗ |
| J9 Instructor/Admin | ✗ | ◐ | ✗ |
| Logout | ✓ | n/a | ✓ |

**Conclusion:** only logout is functional end to end. The recreation must therefore reproduce the reference's **intended** behaviour (UI as designed, APIs as declared), not its observed runtime behaviour. Phase 5 records this as the guiding principle.

**Next:** Phase 5 — recreation design.
