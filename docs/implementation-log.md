# EduNexus Recreation — Implementation Log (Phase 6)

**Recreation root:** `EduNexus-Recreation`. The original reference is untouched (checked at each milestone with `git status`).

## Decisions confirmed by the user (2026-11-05, "yes do that")
| ID | Decision |
|---|---|
| OD1 | Admin option stays visible on Register; the backend rejects `admin` self-registration; a demo admin is seeded (dev profile only) |
| OD2 | Course delete: owner **or** admin |
| OD3 | No optional extras (unenrol, TA management UI, module CRUD, admin user CRUD) in the first build |
| OD4 | Embedded MongoDB (flapdoodle) inside the recreation for tests and local dev — no system install |
| OD5 | Dependency downloads authorised, limited to the recreation (Maven wrapper cache `~/.m2`, npm `node_modules`) |
| OD6 | Spring Boot backend |

---

## M0 — Toolchain (done)
| Check | Result |
|---|---|
| Created `EduNexus-Recreation/` (`backend/`, `frontend/`), `git init -b main` (separate repository; no commits made) | ✓ |
| Maven wrapper scripts 3.3.4 downloaded from Maven Central; **SHA-1 verified** (`640f7c39…ae56`) | ✓ |
| Maven pinned to 3.9.16 (`backend/.mvn/wrapper/maven-wrapper.properties`); `./mvnw -v` → Maven 3.9.16 on JDK 25.0.2 | ✓ |
| Versions pinned from Maven Central and npm: Spring Boot **3.5.16** (latest 3.x), jjwt 0.12.7, springdoc 2.8.17, flapdoodle spring3x 4.33.0, embedded MongoDB 7.0.12 | ✓ |
| Compile with `--release 21` on JDK 25 | ✓ (`mvnw compile` succeeded) |

## M1–M5 — Backend (done)
**Files** (`backend/src/main/java/com/edunexus/`):
* `EduNexusApplication` — excludes the default in-memory user;
* `common/` — `ApiError`, `ApiException`, `GlobalExceptionHandler`, `HealthController`;
* `config/SecurityConfig`;
* `security/` — `JwtService`, `JwtAuthFilter`, `AuthenticatedUser`, `CurrentUser`;
* `auth/` — `AuthController`, `AuthService`, `AuthDtos`;
* `user/` — `Role`, `User`, `UserRepository`, `UserDto`, `UserService`, `UserRequests`, `UserController`;
* `course/` — `Course` (+ embedded types, `ContentType`), `CourseRepository`, `CourseRequests`, `CoursePermissions`, `CourseService`, `CourseController`;
* `demo/DemoDataLoader`;
* `resources/application.yml` (env placeholders only; no secrets).

**Tests** (`backend/src/test/java/com/edunexus/`): `ApiTestSupport`, `AuthAndUserApiTest` (12), `CourseApiTest` (7), plus `TestEduNexusApplication` (dev runner with embedded MongoDB).

**Command run:** `./mvnw -B test` → **Tests run: 19, Failures: 0, Errors: 0 — BUILD SUCCESS.** The first run downloaded the embedded `mongod` 7.0.12 binary into the user cache.

**Acceptance criteria verified by those tests:**
| Criterion (implementation-plan.md) | Test |
|---|---|
| `/api/health` public 200; unknown route → JSON 404 | `healthIsPublic`, `unknownRouteReturnsJson404` |
| Register 201 with FE user shape, lowercase roles, no password in body | `registerReturns201WithFrontendUserShape` |
| Instructor self-registration allowed; admin rejected (OD1) | `registerAsInstructorIsAllowed`, `registerAsAdminIsRejected` |
| Duplicate email 400 (case-insensitive); validation 400 with field map | `registerDuplicateEmailIs400`, `registerValidationErrorsAre400WithFieldMap` |
| Login OK / wrong password / unknown user | `loginSucceedsAndFailsWith400` |
| `/me`: none, garbage or tampered token → 401 JSON; valid → 200 | `meRequiresValidToken` |
| Token of a deleted user rejected | `tokenOfDeletedUserIsRejected` |
| Profile get/update, duplicate email 400, invalid email 400, full UserDto returned | `profileGetAndUpdate` |
| Password change: wrong current, too short, success, then login with the new password | `changePassword` |
| Courses require auth; 404 JSON | `listAndDetailRequireAuthentication` |
| Create sets `adminId`, subdocument ids, lowercase content type, `managedCourses` | `createSetsOwnerIdsAndLowercaseContentTypes` |
| Student create 403; admin 201; validation 400 | `createPermissions` |
| Update: student/other instructor 403; TA and admin 200; mass-assignment fields ignored | `updatePermissionsAndWhitelist` |
| Enrol: instructor 403; student 200 returns Course; repeat 400; user array updated; 404 | `enrolmentRules` |
| Delete: student/other 403; owner 200; admin 200; both user arrays cleaned (D12) | `deleteRulesAndReferenceCleanup` |
| Announcements staff-only; discussions participants-only; replies; anonymous flag; 404 discussion; validation | `announcementsDiscussionsAndReplies` |

**Deviations from the plan, recorded:**
* **Login failure returns 400, not 401** (the plan said 401). This matches the reference Express (`controllers/auth.ts:69-77`). It is also required because the reference frontend's axios interceptor turns any 401 into a hard redirect to `/login`, which would erase the error message on the login form.
* Content endpoints return the updated `Course` (decided at M5 as the plan allowed), consistent with enrol.

## M6–M7 — Frontend (done)
**Setup:**
* `frontend/` is a Vite 7.3.7 project.
* Exact pinned versions: React 18.3.1, MUI 5.18.0, RTK 1.9.7, react-redux 8.1.3, react-router-dom 6.30.6, Formik 2.4.9, Yup 1.7.1, axios 1.21.0, TypeScript 5.9.3, Vitest 3.2.7.
* Installed with `npm install --ignore-scripts` (no package install scripts run); 269 packages.
* `@testing-library/jest-dom` was pinned to 6.9.1 because npm flagged 6.10.0 as a faulty release.

**Files** (`frontend/src/`):
* `main.tsx`, `App.tsx` (route table identical to the reference, plus `SessionBootstrap`), `theme.ts` (token-for-token copy), `types.ts`;
* `api/client.ts` (single axios client; the store registers the 401 handler, removing the reference's circular import);
* `store/` (`index.ts`, `hooks.ts`, `slices/authSlice.ts`, `slices/courseSlice.ts`);
* `components/` (`Layout`, `PrivateRoute`, `PublicOnlyRoute`, `ErrorMessage`);
* `pages/` (`Login`, `Register`, `Dashboard`, `CourseList`, `CourseDetail`, `Profile`);
* `__tests__/` (`authSlice.test.ts`, `pages.test.tsx`).

**Commands run:**
| Command | Result |
|---|---|
| `npx tsc --noEmit` | exit 0 (strict mode, `noUnusedLocals`) |
| `npx vitest run` | **12/12 passed** (7 auth-slice reducer tests, 5 routing/page/schema tests) |
| `npx vite build` | built (582 kB JS, 189 kB gzip; Vite's > 500 kB chunk advisory noted, not an error) |

**Deviation from the plan:** ESLint is not installed. Static checking relies on TypeScript strict mode; recorded as a gap.

**Frontend changes beyond the D1–D12 list** (small, behaviour-preserving):
* `clearError` on Login/Register mount (the reference let a login error leak onto the Register page).
* Profile Edit button calls `preventDefault` (avoids an accidental submit when the button swaps to Save).
* After enrolment, `auth.user.enrolledCourses` is updated so Dashboard counts stay current.
* A failed session restore signs the user out instead of spinning forever.

## M8 — Demo data (done)
* `DemoDataLoader` runs only with profile `dev` **and** `app.demo-data=true`, and only on an empty `users` collection.
* It creates 5 fictional users (`*@demo.edunexus.test`, shared demo-only password documented in the recreation README) and 3 courses with modules, contents, an announcement and a discussion.
* Verified in the log: `Demo data created: 5 users, 3 courses`.

## Runtime used for Phase 7
* Backend: `./mvnw spring-boot:test-run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--app.demo-data=true` on :8083, with embedded MongoDB 7.0.12 and a random per-process JWT key.
* Frontend: `npx vite --port 3000`.

## Other files
`EduNexus-Recreation/README.md`, `backend/.env.example`, `frontend/.env.example`, `.gitignore`.

**Commits:** no Git commits were made in the recreation repository (none were requested).
