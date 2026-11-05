# EduNexus — UI Comparison: Reference vs Recreation (Phase 7)

**Basis:**
* **Reference:** the source-derived specification in `phase-2-frontend-report.md` §3–§7. The reference app was never run: as committed it cannot complete login (Phases 1–4), and running it was out of scope.
* **Recreation:** observed in headless Chromium via REA (`test-report.md` §4).

**Comparison method:**
* component-by-component source comparison (same MUI components, `sx` values, copy text);
* runtime observation of the recreation (accessibility trees and two inspected screenshots).

There are **no reference screenshots**, so pixel-level comparison was not possible. Differences are classified as **D#** (deliberate, from `recreation-architecture.md` §8), **F** (small fix recorded in `implementation-log.md`) or **Defect**.

## Screen-by-screen

| Screen | Reference spec (phase-2 §) | Recreation (observed) | Differences |
|---|---|---|---|
| **Shell / Layout** | 240 px drawer, AppBar "EduNexus" `h6`, avatar initial, account menu Profile/Logout, 3 nav items, hamburger < 600 px (§3.4) | Same structure. The accessibility tree shows banner "EduNexus", "account menu" button with the initial, navigation Dashboard/Courses/Profile. At 375 px the permanent drawer is hidden and the hamburger opens the temporary drawer | `ListItem button` → `ListItemButton` (same look; avoids the deprecated prop). Mobile drawer closes after navigation (F). |
| **Login** | `Container xs`, Paper p4, "Sign in" h5, Email/Password fields, error Typography, "Sign In" with spinner, "Don't have an account? Sign Up" (§4.1) | Same. Verified by test (`pages.test.tsx`) and U1/U2 | **D2:** min password 6 (was 8). **F:** error cleared on mount. **D4:** signed-in users redirected away. |
| **Register** | Avatar `PersonAddOutlined` secondary, "Create an Account", Alert, 4 fields + Role Select (Student/Instructor/Admin), "Create Account"/"Creating Account...", Sign In link (§4.2) | Same fields, options and copy (accessibility tree in U3). Admin selection shows the backend error in the Alert | **OD1:** admin rejected server-side (option kept for fidelity). **F:** error cleared on mount. |
| **Dashboard** | "Welcome, {first}!" h4, 3 Papers (Enrolled, Managed, Role) xs12/md4 with `h3` values, "Recent Activity" static text (§4.3) | Identical headings, values and static text (U2 accessibility tree) | **D3:** values survive a reload. **F:** enrolled count updates after enrolling. |
| **Course list** | "Available Courses" h4, Grid xs12/sm6/md4, Card with h6 title, description, "Enroll"; `alert()` on failure; no link to detail (§4.4) | Same grid and card. One column at 375 px (screenshot), multi-column at 1280 | **D6:** title is a link to the detail page. Enrol now goes through the Redux thunk (same visible behaviour, `alert()` kept). |
| **Course detail** | h4 title, description, "Enroll in Course" (hidden for enrolled/owner/TA), Paper with Tabs Modules/Announcements/Discussions; lists with icons and " - date" (§4.5) | Same (U1 screenshot and tree: tabs, discussion items with " - 11/4/2026"). The Enroll button disappears after enrolling | Uses the shared client instead of raw `fetch` (D7). **F:** a non-404 load error shows `ErrorMessage` instead of crashing. Icons now resolve because types are lowercase (D11). Module contents and replies are still not rendered, **matching the reference**. |
| **Profile** | 100 px avatar, name h4, roles, Divider, First/Last/Email fields disabled until **Edit Profile**, Save/Cancel (§4.6) | Same (U4 tree). Save updates the header name | **D3/F:** form re-syncs after a reload (the reference was blank). |
| **Theme** | `#1976d2` / `#dc004e`, background `#f5f5f5`, Roboto, h1–h6 sizes, Button radius 8 and no uppercase, Card radius 12, TextField radius 8 (§7.1) | `theme.ts` copied token-for-token. The screenshots show the blue AppBar, grey background, rounded cards and non-uppercase buttons | **None.** Note: MUI Tabs labels render uppercase in both apps; the theme only overrides `MuiButton`. |
| **Document** | Title "EduNexus LMS", Roboto link, description meta, favicon/logo links (files missing) (§2, §7.1) | Same title, font and meta | Favicon/manifest links omitted because the reference files never existed (no visible difference). |

## Defects found in the recreation during comparison
None that block a journey. Observations kept for fidelity, which could be revisited:
1. The course-list **Enroll** button is visible to instructors and admins and produces a browser alert "Access denied". This is reference behaviour (§4.4).
2. Module contents (URLs, quiz text) and discussion replies are stored and returned by the API but **not displayed**. This is reference behaviour (§4.5). The reply API exists with no UI.
3. The CourseDetail loading box uses `height: 100vh` inside the padded main area. This is the reference quirk (§7.4), kept.
4. The production bundle is 582 kB minified (Vite advisory). Code-splitting is not applied.

## Conclusion
All six reference screens and the shell are reproduced with the same components, copy, theme tokens and responsive breakpoints. Every difference is either one of the documented deliberate changes D1–D12 or a small, logged fix (F) that makes a reference flow work. No unexplained visual difference was found in the screens observed.
