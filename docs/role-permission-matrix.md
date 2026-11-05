# EduNexus — Role & Permission Matrix (Phase 3)

Checks performed:
* Source reading of `backend/src/middleware/auth.ts`, `routes/*.ts`, `controllers/*.ts`, `models/User.ts`, all Spring `config`/`controller`/`model` files, and the frontend `Register.tsx`, `CourseDetail.tsx`, `PrivateRoute.tsx`.
* A grep of the Java tree for `PreAuthorize|Secured|RolesAllowed|EnableMethodSecurity|hasRole|hasAuthority` → **no matches**.

## 1. Role vocabularies found in the code

| Source | Roles | Representation | Evidence |
|---|---|---|---|
| README | Admin, Instructor, Student | prose | `README.md:9`, `:124` |
| Frontend (live types) | `student`, `instructor`, `admin` | `roles: UserRole[]` | `frontend/src/types.ts:1,8`; Register dropdown `Register.tsx:167-169` |
| Frontend (dead types) | `STUDENT`, `ADMIN` | enum | `frontend/src/types/index.ts:13-16` |
| Spring | `student`, `instructor`, `admin` | single `role: String`; authority `ROLE_<UPPER>` | `model/User.java:39,53,86-90` |
| Express model | `STUDENT`, `ADMIN` (**no instructor**) | `roles: String[]` | `models/User.ts:26-30` |
| Express routes | `student`, `instructor`, `admin` | `checkRole([...])` | `routes/course.ts:19-22` |
| Express register validator | `STUDENT`, `ADMIN` | — | `routes/auth.ts:15` |

There are also **course-scoped roles**, derived from course data rather than the user's global role:
* **Course owner** — `Course.adminId`.
* **Teaching assistant** — `Course.teachingAssistants[]`.
* **Enrolled student** — `Course.enrolledStudents[]`.

Evidence: `backend/src/controllers/course.ts:71,98,127`; `frontend/src/pages/CourseDetail.tsx:116-117`.

## 2. Enforcement mechanisms that exist

| Mechanism | Where | Effective? |
|---|---|---|
| Spring: authenticated vs anonymous | `SecurityConfig.java:38-41` | Yes, if the app starts — but no role rules |
| Spring: role-based rules | — | **None** (grep-verified) |
| Express: `auth` (valid JWT) | `middleware/auth.ts:21-35` | Yes |
| Express: `checkRole` (global role) | `middleware/auth.ts:37-52` | **Always denies.** The JWT payload is `{userId}` only (`controllers/auth.ts:33-37,80-84`), and casing/`instructor` don't match the model. |
| Express: course owner/TA checks | `controllers/course.ts:70-73, 97-100` | Logic is sound, but unreachable: `checkRole` runs first and denies |
| Frontend: `PrivateRoute` (token present) | `PrivateRoute.tsx:22-24` | Yes (presence only) |
| Frontend: role-conditional UI | `isEnrolled`/`isInstructor` hide the enrol button | `CourseDetail.tsx:116-128` only |

## 3. Permission matrix

**Columns:**
* **Intended** — derived from route declarations, README and UI. Legend: ✓ allowed · ✗ denied · ○ unspecified by any source.
* **Express actual** — static reading of the `server.ts` entry.
* **Spring actual** — static reading, assuming startup succeeded.
* **Frontend gating** — what the UI shows.

| Action | Student | Instructor | Admin | Intended source | Express actual | Spring actual | Frontend gating |
|---|---|---|---|---|---|---|---|
| Register self | ✓ | ✓ | ✓ (self-select) | `Register.tsx:167-169`; `routes/auth.ts:15` | Anyone; STUDENT/ADMIN only; **instructor impossible; self-ADMIN allowed** | Anyone; **any role string accepted** (no `@Valid`) | Dropdown offers all three |
| Log in | ✓ | ✓ | ✓ | — | Any registered user | Any registered user | Public page |
| View course list | ✓ | ✓ | ✓ | `routes/course.ts:15` (public) | **Public incl. anonymous** | No endpoint | Behind `PrivateRoute` |
| View course detail | ✓ | ✓ | ✓ | `routes/course.ts:16` (public) | **Public incl. anonymous**; populates enrolled students' name and email (`controllers/course.ts:22`) | No endpoint | Behind `PrivateRoute` |
| Create course | ✗ | ✓ | ✓ | `routes/course.ts:19` | **Nobody** (403) | No endpoint | No UI |
| Update course | ✗ | ✓ own/TA | ✓ own/TA | `routes/course.ts:20`; `controllers/course.ts:70-73` | **Nobody** (403) | No endpoint | No UI |
| Delete course | ✗ | ✗ | ✓ own only | `routes/course.ts:21`; `controllers/course.ts:97-100` | **Nobody** (403) | No endpoint | No UI |
| Enrol in course | ✓ | ✗ | ✗ | `routes/course.ts:22` | **Nobody** (403) | No endpoint | Button for **all** users on the list; on detail, hidden only for owner/TA |
| Unenrol | ○ | ○ | ○ | — | No endpoint | No endpoint | No UI |
| Assign TA | ○ | ○ | ○ | — | Only via mass-assignment PUT (unreachable) | No endpoint | No UI |
| Post announcement | ○ | ○ | ○ | FE-dead `services/api.ts:83` | No endpoint | No endpoint | No UI |
| Start discussion / reply | ○ | ○ | ○ | FE-dead `services/api.ts:88,93` | No endpoint | No endpoint | No UI |
| View own profile | ✓ | ✓ | ✓ | `routes/user.ts:10` | Any authenticated user (own only) | No endpoint | Profile page |
| Update own profile | ✓ | ✓ | ✓ | `routes/user.ts:11` | Any authenticated user; email changeable without uniqueness feedback | No endpoint | Profile page |
| Change own password | ✓ | ✓ | ✓ | `routes/user.ts:12` | Any authenticated user | No endpoint | No UI |
| List/manage all users, change roles | ✗ | ✗ | ✓ | `README.md:47,234-237` | No endpoint | No endpoint | No UI |
| Health check | ✓ | ✓ | ✓ | — | Public | **Authenticated only** | — |

## 4. Enforcement gaps (confirmed by source)
1. **No working role enforcement in either backend.**
   * Express denies every role-gated action.
   * Spring has no role rules at all.
2. **Privilege escalation at registration** in both backends.
3. **Express read endpoints are public**, including enrolled-student PII on course detail.
4. **The only correct authorization logic** (owner/TA checks) is unreachable.
5. **The `instructor` role** is absent from the Express model but required by its routes and by the frontend.
6. **The frontend gates nothing by role**, apart from the enrol button on the detail page.

## 5. Inferred intended model (INFERENCE — for Phase 5 design input)

| Capability | Student | Instructor | Admin |
|---|---|---|---|
| Browse and view courses | ✓ | ✓ | ✓ |
| Enrol in / unenrol from a course | ✓ | — | — |
| Read the content of an enrolled course | ✓ | ✓ (own/TA) | ✓ |
| Post in the discussions of an enrolled course | ✓ | ✓ | ✓ |
| Create a course (becomes `adminId`) | — | ✓ | ✓ |
| Edit course and modules; post announcements | — | ✓ if owner/TA | ✓ |
| Delete course | — | ? (Express: admin-only) | ✓ |
| Manage TAs | — | ✓ if owner | ✓ |
| Manage users and roles | — | — | ✓ |

This model reconciles `routes/course.ts:19-22`, `controllers/course.ts:70-73,97-100`, `CourseDetail.tsx:117` and `README.md:9`. **Open decisions** with conflicting or no evidence:
* instructor delete rights;
* whether admin may be self-registered;
* who may post announcements and discussions.
