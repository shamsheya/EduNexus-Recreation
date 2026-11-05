# EduNexus Recreation — Test Report (Phase 7)

**Date:** 2026-11-05. Every result below comes from a command or tool call actually run in this session. Nothing is extrapolated.

## 1. Summary
| Suite | Tool | Result |
|---|---|---|
| Backend integration tests (M1–M5 acceptance) | `./mvnw -B test` (JUnit 5, MockMvc, embedded MongoDB 7.0.12) | **19 / 19 passed** |
| Frontend unit / component tests | `npx vitest run` (jsdom, Testing Library) | **12 / 12 passed** |
| Frontend typecheck + production build | `npx tsc --noEmit`, `npx vite build` | **pass** (bundle-size advisory only) |
| Live end-to-end API journeys J0–J9 + error handling | `bash e2e.sh` (curl against the running backend on :8083) | **37 / 37 passed** (second run; see §3) |
| Live browser UI journeys | REA `capture_browser_scenario` (headless Chromium 156 via Playwright) | **5 / 5 scenarios passed** (see §4) |

## 2. Automated suites (details in `implementation-log.md`)
* **Backend tests (19)** cover:
  * health; JSON 404;
  * register shape, roles, admin rejection, duplicate email, validation map;
  * login success and failure;
  * `/me` with none, garbage, tampered and valid tokens; deleted-user token;
  * profile get/update/duplicate/invalid; password change;
  * courses auth-required/404; create (ids, owner, lowercase types, `managedCourses`);
  * create, update and delete permission matrix; mass-assignment protection;
  * enrol rules; delete cleanup of both reference arrays;
  * announcements, discussions and replies permissions; anonymous flag; validation.
* **Frontend tests (12)** cover:
  * auth reducer (login success/failure, session restore success/failure, logout, profile update, enrolment count);
  * unauthenticated redirect to Login;
  * Login Yup messages;
  * Register fields and the three role options;
  * shared 6-character password rule;
  * module icon mapping.

## 3. Live API journeys (`e2e.sh`, servers running with demo data)
The **first run reported 14 pass / 19 fail**. Root cause: a defect **in the test script**. Git-Bash `curl` wrote to `/tmp/b.json`, which Windows Python resolved to a different path, so no token or id was ever extracted. The script was fixed to use an absolute path and rerun with no application change → **37/37 pass**.

**Side effect of the faulty run:** it created a course titled "E2E course" (the create call succeeded) but never deleted it (the id was not extracted). This course remains in the throwaway embedded database and is visible in the mobile screenshot.

| Group | Checks |
|---|---|
| J0 CORS | preflight 200; `Access-Control-Allow-Origin: http://localhost:3000`; foreign origin gets no allow header |
| J1 Register | student 201; admin 400 with "Admin accounts cannot be self-registered" |
| J2 Login | 6-character password user logs in (fixes reference D2); wrong password 400 |
| J3 Session | `/auth/me` 200 with a valid token, 401 with a bad token |
| J4 Browse | list 200, demo courses present; unauthenticated 401 |
| J5 Detail | 200; content type `video` (lowercase); `id` strings, no `_id` |
| J6 Enrol | student 200; repeat 400; `user.enrolledCourses` updated; instructor 403 |
| J7 Content | enrolled student starts discussion 201; TA reply 201; student announcement 403; instructor announcement 201 |
| J8 Profile | update 200, response keeps `enrolledCourses`; duplicate email 400; password change 200 |
| J9 Roles | student create 403; instructor create 201; non-owner update 403; owner update 200; admin delete 200; then 404 |
| Errors | unknown route 404 JSON `{message}`; malformed JSON 400 |

Script location (scratchpad, not part of the deliverable): `e2e.sh (temporary local scratch file)`.

## 4. Live browser UI journeys (REA `capture_browser_scenario`)
The Claude-in-Chrome extension was **not connected**, so the REA Playwright provider was used (headless Chromium 156, provider-owned process, terminated after each run). Locators are CSS or ARIA role. Assertions are `wait_for` steps; a step that never satisfies its locator never completes.

| # | Scenario | Viewport | Steps | Result | Evidence |
|---|---|---|---|---|---|
| U1 | `/dashboard` unauthenticated → redirect to Login → sign in (student2) → Dashboard "Welcome, Sky!" → drawer **Courses** → "Available Courses" → **click course title link (D6)** → detail → **Enroll in Course** → button disappears (now enrolled) → **Discussions** tab | 1280×720 | 13 | all completed | final accessibility tree shows the tabs and both discussions; screenshot inspected |
| U2 | Sign in → Dashboard → **full page reload** → "Welcome, Sky!" still shown (**session restore, D3**) → visit `/login` → **redirected back to Dashboard (D4)** | 1280×720 | 8 | all completed | `ev_2ce81052…f51` (accessibility: Enrolled 1, Managed 0, Role student) |
| U3 | Register with role **Admin** → Alert "Admin accounts cannot be self-registered" (OD1), form values kept | 1280×720 | 8 | all completed | `ev_7f58a91c…7c1a` |
| U4 | Register as **Instructor** → Dashboard "Welcome, Ada!" → drawer **Profile** → Edit → change First Name → Save → header "Adaline Attempt" → **reload `/profile`** → form pre-filled "Adaline" | 1280×720 | 15 | all completed | `ev_e74920f1…7e5` (accessibility shows the disabled filled form, role "instructor") |
| U5 | Mobile: sign in (instructor) → permanent drawer hidden → hamburger "open drawer" → temporary drawer → **Courses** → single-column card grid | 375×812 | 9 | all completed | `ev_9b3d3606…f915`; screenshot inspected |

**Locator note:** a first attempt at U3/U4 hung on `role=combobox name="Role"`. MUI exposes the select's accessible name as "Role Student", so the wait never matched. I stopped the task (TaskStop) and reran it with the `#role` CSS locator. This was a test-authoring issue, not an application defect.

## 5. Not tested / gaps
* **Not tested:** cross-browser (only Chromium); 768 px tablet width (only 1280 and 375 were run); keyboard-only navigation and screen-reader audit beyond the accessibility tree.
* **Token expiry** after 24 h was not exercised live. Invalid/tampered tokens are covered by tests.
* **Real (non-embedded) MongoDB** was not exercised. The app uses the standard `MONGODB_URI` path.
* **Load and performance:** none.
* **ESLint:** not installed (TypeScript strict only).
* **Reference-fidelity behaviours left as-is and observed:** the course-list **Enroll** button is shown to every role, so an instructor clicking it gets the reference's browser `alert()` with "Access denied". Not clicked in the browser runs, to avoid modal dialogs; verified at API level (J6, 403).
