# EduNexus — Phase 2: Frontend & User-Experience Analysis

| | |
|---|---|
| **Target** | `EduNexus\frontend` (commit `8dec71f`, branch `main`) |
| **Date** | 2026-11-05 |
| **Method** | Full manual read of all 27 frontend files + REA static analysis (no code executed) |
| **Constraints honoured** | No source modified, no dependencies installed, no scripts run, no secret values reproduced |
| **Goal context** | Recreate the LMS as closely as possible — *not* repair the original |

All line numbers refer to files under `frontend/` unless stated otherwise.

---

## 0. Executive summary

* The frontend is a **Create-React-App 5 + TypeScript 4.9 + MUI 5 + Redux Toolkit 1.9 + React Router 6** single-page app with **6 routed pages**, **1 layout shell**, **1 route guard**, and **3 unused helper components**.
* The UI is a stock MUI "dashboard with side drawer" layout with a lightly customised theme (blue/pink, rounded 8–12 px corners, no button uppercase).
* **Only Login, Registration, the shell navigation and Logout are wired end-to-end in the frontend.** Course listing, course detail, enrolment and profile editing have UI and request code, but mismatched ports, URL shapes, role casing and data shapes stop them working against either backend in the repo (see §8).
* There are **no modals, no tables, no snackbars, no create/edit forms for courses, no admin screens**, and no assignment, grade or progress UI, even though the README advertises all of these.
* There are **two competing HTTP layers** (`utils/axios.ts` → port 8083, raw `fetch()` → port 8080). A third, `services/api.ts`, is never imported.
* There are **two competing type files** (`src/types.ts` is used; `src/types/index.ts` is dead).

---

## 1. REA tool usage and evidence

| REA call | Result |
|---|---|
| `analyze_javascript_application` on `frontend/` | Evidence `ev_8028b1e9…8773`. 27 relevant files, **22 JS/TS sources parsed, 0 parse failures**, 8 820 AST nodes, 416 findings, 18 request relations, 3 configuration (env) relations. |
| `inspect_analysis_view` → `javascript-routes / occurrences` | Evidence `ev_46e848f2…5d75`. 5 navigation-target occurrences: `Login.tsx:54` → `/`, `Login.tsx:106` → `/register`, `Register.tsx:64` → `/`, `Layout.tsx:56` → `/login`, `Layout.tsx:127` → `/profile`. |
| `inspect_analysis_view` → `javascript-facts` (pages + HTTP files) | Evidence `ev_9ed39720…718c`. Confirmed `localStorage['token']` reads (e.g. `CourseDetail.tsx:68`, `:138`, `:144`, `utils/axios.ts:15`) and endpoint literals such as `` `http://localhost:8080/api/courses/${id}/enroll` `` (`CourseDetail.tsx:135`) and `/courses` (`services/api.ts:63`). |

**REA limitations observed** (handled by manual reading):
* REA's route extractor recovered only imperative `navigate()` targets. It did **not** recover the declarative `<Route path=…>` tree in `App.tsx`, the `menuItems` paths in `Layout.tsx:59-63`, or the `<Navigate>` redirects.
* Extension-less TypeScript relative imports (`'../store'`, `'../store/slices/authSlice'`) were reported as `not-found` because no compiler options/`node_modules` were present. These imports are valid under CRA's resolver.
* REA reports static candidates only; it says nothing about runtime reachability.

---

## 2. File inventory and role

| File | Lines | Role | Used? |
|---|---|---|---|
| `public/index.html` | 24 | HTML shell, Roboto font link, `<div id="root">` | Yes |
| `public/manifest.json` | 25 | PWA manifest | Yes (references missing icons) |
| `src/index.tsx` | 15 | `createRoot` + `<React.StrictMode><App/>` | Yes |
| `src/App.tsx` | 47 | Providers + router | Yes |
| `src/theme.ts` | 72 | MUI theme | Yes |
| `src/components/Layout.tsx` | 192 | AppBar + Drawer shell | Yes |
| `src/components/PrivateRoute.tsx` | 29 | Token guard | Yes |
| `src/components/ErrorMessage.tsx` | 36 | Alert w/ retry | **Unused** |
| `src/components/LoadingSpinner.tsx` | 25 | Spinner + caption | **Unused** |
| `src/components/SuccessMessage.tsx` | 20 | Success alert | **Unused** |
| `src/pages/Login.tsx` | 120 | Login form (Formik component API) | Yes |
| `src/pages/Register.tsx` | 193 | Registration form (`useFormik` hook) | Yes |
| `src/pages/Dashboard.tsx` | 66 | Stat cards | Yes |
| `src/pages/CourseList.tsx` | 77 | Course grid | Yes |
| `src/pages/CourseDetail.tsx` | 253 | Tabbed course view | Yes |
| `src/pages/CourseDetails.tsx` | 71 | Alternate course view (price/level) | **Unrouted / dead** |
| `src/pages/Profile.tsx` | 159 | Profile view/edit | Yes |
| `src/store/index.ts` | 13 | `configureStore` | Yes |
| `src/store/slices/authSlice.ts` | 150 | Auth state + thunks | Yes |
| `src/store/slices/courseSlice.ts` | 201 | Course state + thunks | Partially |
| `src/utils/axios.ts` | 63 | Axios instance (8083) | Yes |
| `src/services/api.ts` | 101 | Alternate axios API layer | **Never imported** |
| `src/types.ts` | 83 | Types (lowercase roles, `id`) | Yes — wins resolution of `'../types'` |
| `src/types/index.ts` | 96 | Types (uppercase enums, `_id`) | **Dead** (shadowed by `types.ts`) |

**Missing static assets:** `public/favicon.ico` (referenced at `index.html:5`, `manifest.json:6`), `logo192.png` (`index.html:12`, `manifest.json:11`), `logo512.png` (`manifest.json:16`). `public/` contains only `index.html` and `manifest.json`.

**Dead exports inside live files:** `authSlice` `getCurrentUser`, `setCredentials`, `setLoading`, `setError` (`authSlice.ts:63,79,92,95,149`). `courseSlice` `fetchCourseById` (used only by the dead `CourseDetails.tsx`), `createCourse`, `updateCourse`, `deleteCourse`, `enrollInCourse`, `clearCurrentCourse`, `clearError` — no UI dispatches them. In `index.tsx:4-5`, `store` and `theme` are imported but unused.

---

## 3. Application shell, providers and routing

### 3.1 Provider stack — `App.tsx:20-43`
```
<Provider store>                (react-redux)          App.tsx:20
  <ThemeProvider theme>         (MUI)                  App.tsx:21
    <CssBaseline/>                                     App.tsx:22
    <BrowserRouter>                                    App.tsx:23
      <Routes> … </Routes>
```
`<CssBaseline/>` is rendered a second time inside `Layout.tsx:85`.

### 3.2 Route table — `App.tsx:24-40`

| Path | Element | Guard | Line |
|---|---|---|---|
| `/login` | `Login` | public | 26 |
| `/register` | `Register` | public | 27 |
| `/` | `PrivateRoute > Layout` (parent, renders `<Outlet/>`) | token | 30 |
| `/` (index) | `<Navigate to="/dashboard" replace/>` | token | 31 |
| `/dashboard` | `Dashboard` | token | 32 |
| `/courses` | `CourseList` | token | 33 |
| `/courses/:id` | `CourseDetail` | token | 34 |
| `/profile` | `Profile` | token | 35 |
| `*` | `<Navigate to="/" replace/>` | — | 39 |

There is no role-based routing. Every authenticated user sees the same routes, regardless of role.

### 3.3 Route guard — `components/PrivateRoute.tsx`
* Reads `state.auth.token` and `state.auth.loading` (`:12`).
* While `loading` is true, it shows a full-viewport centred `CircularProgress` (`:14-20`).
* With no token it does `<Navigate to="/login" replace/>` (`:22-24`).
* It only checks that a token *exists*. It does not validate or decode it, check expiry, or check that `user` is loaded.

### 3.4 Layout shell — `components/Layout.tsx`
* **Drawer width:** 240 px (`:31`).
* **AppBar** (`:86-141`): `position="fixed"`. At `sm`+ it is offset by the drawer width (`:88-91`).
  * Hamburger `IconButton` (`:94-102`), visible only below `sm` (`display: { sm: 'none' }`).
  * Title "EduNexus", `h6`, `flexGrow: 1` (`:103-105`).
  * Avatar button (`:106-117`): 32×32 `Avatar` showing the upper-cased first letter of `user.firstName` (`:114-116`).
  * Account `Menu` (`:118-139`), anchored bottom-right. Items: **Profile** (`:127`, `navigate('/profile')`) and **Logout** (`:133`, `handleLogout`).
* **Navigation drawer** (`:65-81`): `Toolbar` spacer + `List` of `menuItems` (`:59-63`):
  | Text | Icon | Path |
  |---|---|---|
  | Dashboard | `Dashboard` | `/dashboard` |
  | Courses | `School` | `/courses` |
  | Profile | `Person` | `/profile` |
  Each item is `ListItem button` (`:70-71`; the `button` prop is deprecated in MUI 5). There is no selected/active-route highlight and no role-specific items.
* **Responsive drawers:** `variant="temporary"` for `xs` (`:146-162`, `keepMounted`) and `variant="permanent"` for `sm`+ (`:163-175`).
* **Main area** (`:177-187`): `flexGrow 1`, `p: 3`, `Toolbar` spacer, `<Outlet/>`.
* **Logout** (`:54-57`): `dispatch(logout())` then `navigate('/login')`.

---

## 4. Page-by-page UI structure

### 4.1 Login — `pages/Login.tsx`
* **Container:** `Container maxWidth="xs"` (`:35`) → `Box` column, `marginTop: 8` (64 px) (`:36-43`) → `Paper elevation={3}`, `p: 4` (`:44`).
* **Heading:** "Sign in", `h5`, centred (`:45-47`).
* **Form:** Formik component API (`:48-113`), validated with Yup (`:18-27`):
  | Field | Component | Rules |
  |---|---|---|
  | `email` | `TextField` "Email Address" (`:62-74`) | valid email, required |
  | `password` | `TextField type=password` (`:75-88`) | **min 8**, required |
* **Error line:** `Typography color="error"` with `auth.error` (`:89-93`).
* **Buttons:**
  * **Sign In** — `type=submit`, `variant=contained`, `fullWidth`, `mt 3 mb 2`. Shows a 24 px spinner while loading (`:94-102`).
  * **"Don't have an account? Sign Up"** — `variant=text`, navigates to `/register` (`:103-110`).
* **On success:** `dispatch(login(values)).unwrap()` then `navigate('/')` (`:51-58`).
* **UX notes:**
  * All fields and buttons are disabled while `auth.loading` is true.
  * There is no "remember me", no "forgot password", and no show/hide password control.
  * `auth.error` is never cleared on navigation, so a failed login error also appears on the Register page.

### 4.2 Registration — `pages/Register.tsx`
* **Container:** same shell as Login (`:72-90`). The `Paper` adds `alignItems: center` (`:81-90`).
* **Header:** `Avatar` with `PersonAddOutlined` icon, `bgcolor: secondary.main` (pink) (`:91-93`). Title "Create an Account", `h5` (`:94-96`).
* **Error:** full-width MUI `Alert severity="error"` (`:97-101`). This differs from Login, which uses a plain Typography line.
* **Form:** `useFormik` hook (`:52-69`) + Yup (`:26-45`):
  | Field | Component | Rules | Line |
  |---|---|---|---|
  | `firstName` | `TextField`, `autoComplete=given-name`, **autoFocus** | required | 107-119 |
  | `lastName` | `TextField`, `family-name` | required | 120-131 |
  | `email` | `TextField`, `email` | valid, required | 132-143 |
  | `password` | `TextField type=password`, `new-password` | **min 6**, required | 144-156 |
  | `role` | **`Select` dropdown** in `FormControl` (`InputLabel` "Role") | one of `student/instructor/admin` | 157-171 |
* **Role options:** Student (`:167`), Instructor (`:168`), Admin (`:169`). The default is `'student'` (`:58`).
* **Buttons:** **Create Account** (submit; label becomes "Creating Account..." while loading) (`:172-180`), plus a **"Already have an account? Sign In"** `Link` to `/login` (`:181-185`).
* **On success:** `navigate('/')` (`:64`).
* **UX notes:**
  * There is no `onBlur` handler, so field errors only appear after the first submit.
  * Inputs are **not** disabled while loading; only the button is (contrast with Login).
  * There is no password-confirmation field.
  * **Anyone can self-select Admin.**

### 4.3 Dashboard — `pages/Dashboard.tsx`
* **Wrapper:** `Box flexGrow 1, p: 3` (`:10`). With Layout's `p: 3`, total padding is 48 px.
* **Heading:** "Welcome, {user.firstName}!", `h4` (`:11-13`).
* **Grid:** `Grid container spacing={3}` (`:15`):
  | Card | Grid | Content | Line |
  |---|---|---|---|
  | Enrolled Courses | `xs 12 md 4` | `user.enrolledCourses.length \|\| 0`, `h3` | 17-26 |
  | Managed Courses | `xs 12 md 4` | `user.managedCourses.length \|\| 0`, `h3` | 28-37 |
  | Role | `xs 12 md 4` | `user.roles.join(', ')`, `h3` | 39-48 |
  | Recent Activity | `xs 12` | static "No recent activity to display." | 51-60 |
* Cards are `Paper p: 2`, not `Card`.
* **No interactivity:** no links, buttons, charts or course lists. The numbers depend entirely on what the login/register response put into `auth.user`.

### 4.4 Course listing — `pages/CourseList.tsx`
* **Data:** `dispatch(fetchCourses())` on mount (`:11-13`), read from `state.courses` (`:9`).
* **Loading:** centred spinner, `minHeight 200px` (`:37-43`).
* **Heading:** "Available Courses", `h4` (`:47-49`).
* **Grid:** `spacing 3`. Each item is `xs 12 / sm 6 / md 4`, so 1, 2 or 3 columns (`:50-52`).
* **Card** (`:53-69`): title (`h6`), description (`text.secondary`, `paragraph`), and an **Enroll** button (`contained`, primary).
* **Enroll** (`:15-35`) does a raw `fetch POST http://localhost:8080/api/courses/{id}/enroll` (`:17`).
  * On success it refetches the list.
  * On failure it shows a **browser `alert()`** (`:29`, `:33`).
* **Missing:**
  * **No link from a card to `/courses/:id`.** The detail page is only reachable by typing the URL.
  * No search, filter, sort, pagination, empty state or error display (`state.courses.error` is ignored).
  * No create-course button.
  * The Enroll button appears for every role and every course, including ones the user is already enrolled in.

### 4.5 Course details — `pages/CourseDetail.tsx` (routed)
* **Data:**
  * Local `useState`, not Redux (`:59-61`).
  * Raw `fetch GET http://localhost:8080/api/courses/{id}` with a Bearer token from `localStorage` (`:63-81`).
  * There is no `response.ok` check (`:71-72`). An error JSON body is stored as the course and then crashes at `course.enrolledStudents.includes` (`:116`).
* **Loading:** spinner in a `100vh` box (`:100-106`); inside the Layout this overflows the viewport.
* **Not found:** "Course not found" (`:108-114`), shown only when `course` is null.
* **Header:** title `h4` (`:121-123`), description (`:124-126`).
* **Role derivation:**
  * `isEnrolled = course.enrolledStudents.includes(user.id)` (`:116`).
  * `isInstructor = course.adminId === user.id || teachingAssistants.includes(user.id)` (`:117`).
* **Enroll in Course** button (`:128-156`): shown when neither enrolled nor instructor. It POSTs to `/enroll` (`:135`), then refetches (`:142`). There is no success or error feedback.
* **Tabs** (`Paper` > `Tabs`) (`:158-163`): **Modules | Announcements | Discussions**.
  * **Modules** (`:166-190`): `List` of `ListItem`. The icon comes from `getModuleIcon(module.contents[0].type)` (`:87-98`), which matches lowercase `'assignment' | 'quiz' | 'video'` and otherwise shows `Description`. Each item shows the title (`h6`) and description. **Content items, links and due dates are not rendered.**
  * **Announcements** (`:192-218`): `Announcement` icon, title, content, ` - ` + `toLocaleDateString(createdAt)`.
  * **Discussions** (`:220-246`): `Forum` icon, title, content and date. **Replies are not shown.** There is no reply or post UI.
* **Unused code:**
  * `TabPanel` (`:39-53`) is defined but never used.
  * Imported but unused: `useNavigate`/`navigate` (`:2`, `:57`), `Grid`, `Card`, `CardContent`, `Divider`, `Chip` (`:4-21`).
* There is no back button or breadcrumb.

### 4.5b Course details (dead variant) — `pages/CourseDetails.tsx`
This page is not routed. It uses the Redux `fetchCourseById` (`:13-17`) and renders a single `Card` with:
* description,
* **Instructor** (`:46`), **Duration** (`:49`), **Level** (`:52`),
* **Price** `$…` in `h5` primary (`:54-56`),
* an **Enroll Now** button with **no handler** (`:57-64`).

It reflects a "course marketplace" design (price/level/duration) that does not exist in any backend model. It is a useful hint of the author's intended visuals, but it is non-functional.

### 4.6 User profile — `pages/Profile.tsx`
* **Container:** `Box p 3` > `Paper p 3` (`:59-60`).
* **Header:** 100×100 `Avatar` with initial at `2.5rem` (`:62-71`), full name `h4` (`:73-75`), and roles joined in `text.secondary` (`:76-78`). Then a `Divider` with `my 3` (`:82`).
* **Form:** plain React state, not Formik, with **no validation** (`:19-32`):
  | Field | Grid | Line |
  |---|---|---|
  | First Name | `xs 12 sm 6` | 86-95 |
  | Last Name | `xs 12 sm 6` | 96-105 |
  | Email | `xs 12` | 106-115 |
  The fields are disabled until **Edit Profile** is clicked.
* **Buttons** (`:117-150`):
  * View mode: **Edit Profile**.
  * Edit mode: **Save Changes** (submit) and **Cancel** (outlined; resets to the store values).
* **Save** (`:34-56`): raw `fetch PUT http://localhost:8080/api/users/profile` (`:37`). On `ok`, `dispatch(updateProfile(json))` replaces the entire `auth.user` (`:48`). Failure is logged to the console only.
* **Issues:**
  * `formData` is initialised once from `user` (`:20-24`). After a reload `user` is null, so the form stays blank.
  * There is no success or error message.
  * There is no change-password UI, even though a backend route exists (`backend/src/routes/user.ts:12`).
  * There is no avatar upload, enrolled-course list or managed-course list.

---

## 5. Inventory of interactive elements

| Type | Where | Notes |
|---|---|---|
| **Forms** | Login (Formik), Register (useFormik), Profile (useState) | Three different form techniques |
| **Text inputs** | Login ×2, Register ×4, Profile ×3 | All MUI `TextField` outlined, radius 8 |
| **Dropdowns** | Register role `Select` (`Register.tsx:157-171`) | The only dropdown in the app |
| **Menus / popovers** | Account menu (`Layout.tsx:118-139`) | The only popover |
| **Buttons** | Sign In, Sign Up link-button, Create Account, Enroll (list), Enroll in Course (detail), Edit/Save/Cancel profile, Hamburger, Avatar | `Enroll Now` in the dead page has no handler |
| **Links** | Register → Login `Link` (`Register.tsx:182`) | Login → Register uses a `Button` instead |
| **Cards** | CourseList `Card` (`:53`), dead CourseDetails `Card` | Dashboard uses `Paper`, not `Card` |
| **Tabs** | CourseDetail (`:159-163`) | 3 tabs |
| **Lists** | Drawer nav, CourseDetail tab lists | `ListItem` / `ListItemIcon` / `ListItemText` |
| **Avatars** | AppBar 32 px, Profile 100 px, Register icon avatar | Letter avatars only |
| **Spinners** | PrivateRoute, Login button, CourseList, CourseDetail, CourseDetails | `LoadingSpinner` component unused |
| **Alerts** | Register error `Alert` | `ErrorMessage` and `SuccessMessage` components unused |
| **Browser dialogs** | `alert()` in CourseList (`:29`, `:33`) | |
| **Modals / Dialogs** | **None** | No MUI `Dialog` anywhere |
| **Tables** | **None** | No `Table` / `DataGrid` anywhere |
| **Snackbars / toasts** | **None** | |
| **Drawers** | Responsive nav drawer | |

---

## 6. State management and authentication flow

### 6.1 Store shape — `store/index.ts:5-10`
```ts
{
  auth:    { user: User|null, token: string|null, loading: boolean, error: string|null },   // authSlice.ts:15-27
  courses: { courses: Course[], currentCourse: Course|null, loading: boolean, error: string|null } // courseSlice.ts:5-17
}
```
* `auth.user` type (`authSlice.ts:5-13`): `{ id, email, firstName, lastName, roles: UserRole[], enrolledCourses: string[], managedCourses: string[] }`.
* `UserRole` comes from `src/types.ts:1`: `'student' | 'instructor' | 'admin'`.

### 6.2 Thunks and reducers
| Thunk / action | HTTP | Used by UI? | File:line |
|---|---|---|---|
| `login` | POST `/auth/login` → stores `token` in localStorage | Login | `authSlice.ts:29-41` |
| `register` | POST `/auth/register` → stores `token` | Register | `authSlice.ts:43-61` |
| `getCurrentUser` | GET `/users/profile` | **No** | `authSlice.ts:63-73` |
| `logout` (reducer) | — clears user, token and localStorage | Layout, axios 401 | `authSlice.ts:87-91` |
| `updateProfile` (reducer) | — replaces user | Profile | `authSlice.ts:84-86` |
| `setCredentials`, `setLoading`, `setError` | — | **No** | `authSlice.ts:79-98` |
| `fetchCourses` | GET `/courses` | CourseList | `courseSlice.ts:19-29` |
| `fetchCourseById` | GET `/courses/:id` | dead page only | `courseSlice.ts:31-41` |
| `createCourse`, `updateCourse`, `deleteCourse`, `enrollInCourse` | POST/PUT/DELETE | **No** | `courseSlice.ts:43-89` |

* Error messages come from `error.response?.data?.message` with a fixed fallback string (e.g. `authSlice.ts:38`).
* `getCurrentUser.rejected` clears the token only if the payload equals `'Invalid token'` (`authSlice.ts:141-144`).
* `enrollInCourse.fulfilled` expects a `Course`, but the only backend implementation returns `{ message }` (`backend/src/controllers/course.ts:140`). The reducer would find index −1 and do nothing (`courseSlice.ts:183-192`).
* `courses.loading` is shared by every course thunk.

### 6.3 Authentication flow (as coded)
```
             ┌─────────────── app boot ───────────────┐
             │ auth.token  = localStorage.token        │ authSlice.ts:24
             │ auth.user   = null   (never rehydrated) │ authSlice.ts:23
             └────────────────────────────────────────┘
 /login ──submit──► login thunk ──POST /auth/login (8083)──► {token,user}
                    │ localStorage.token = token             authSlice.ts:35
                    │ auth.user/token = payload              authSlice.ts:106-110
                    └► navigate('/') ► index ► /dashboard    Login.tsx:54, App.tsx:31
 /register ─submit─► register thunk (same pattern)          authSlice.ts:43-61, Register.tsx:63-64

 every axiosInstance request: Authorization: Bearer <localStorage.token>   utils/axios.ts:13-24
 every raw fetch():          Authorization: Bearer <localStorage.token>   CourseDetail.tsx:68, CourseList.tsx:20, Profile.tsx:41

 axiosInstance 401 ► store.dispatch(logout()) ► window.location.href='/login' (full reload)  utils/axios.ts:32-35
 fetch() 401      ► NOT handled
 Layout Logout   ► dispatch(logout()) ► navigate('/login')   Layout.tsx:54-57
```
**Flow defects relevant to a faithful recreation:**
1. **No user rehydration on reload.** The token survives but `user` is null. The result:
   * the avatar is blank (`Layout.tsx:115`),
   * the Dashboard says "Welcome, !" with zeros (`Dashboard.tsx:12,23,34,45`),
   * the Profile form is empty (`Profile.tsx:20-24`).
2. **An authenticated user can still open `/login` and `/register`.** There is no reverse guard.
3. **Token storage:** `localStorage` only. There is no refresh token and no expiry check.
4. **Circular import:** `utils/axios.ts:2-3` imports `store` and `logout`; `store/index.ts` imports `authSlice`; `authSlice.ts:2` imports `utils/axios`. This works only because `store` is dereferenced lazily inside the interceptor.
5. **Role shape:** the UI expects `user.roles: string[]`. The Spring backend returns a singular `role` string. Express returns `roles` in UPPERCASE while the frontend uses lowercase. As a result, the role text in Dashboard and Profile renders blank with Spring and as `STUDENT` with Express.
6. **The login/register password rules disagree** (8 vs 6). A user who registers with a 6–7 character password cannot log in through the UI.

---

## 7. Visual design system

### 7.1 Theme — `src/theme.ts`
| Token | Value | Line |
|---|---|---|
| `palette.primary.main` | `#1976d2` (MUI default blue) | 5-7 |
| `palette.secondary.main` | `#dc004e` (MUI v4 pink) | 8-10 |
| `palette.background.default` | `#f5f5f5` | 12 |
| `palette.background.paper` | `#ffffff` | 13 |
| `typography.fontFamily` | `"Roboto","Helvetica","Arial",sans-serif` | 17 |
| `h1` | 2.5rem / 500 | 18-21 |
| `h2` | 2rem / 500 | 22-25 |
| `h3` | 1.75rem / 500 | 26-29 |
| `h4` | 1.5rem / 500 | 30-33 |
| `h5` | 1.25rem / 500 | 34-37 |
| `h6` | 1rem / 500 | 38-41 |
| `MuiButton.root` | `textTransform: none`, `borderRadius: 8` | 44-51 |
| `MuiCard.root` | `borderRadius: 12`, `boxShadow: 0 2px 4px rgba(0,0,0,0.1)` | 52-59 |
| `MuiTextField` outlined input | `borderRadius: 8` | 60-68 |

* Everything else is MUI 5 default: 8 px spacing unit, breakpoints `xs 0 / sm 600 / md 900 / lg 1200 / xl 1536`, default AppBar elevation, and error/success/warning palettes.
* There is no dark mode. `<meta name="theme-color">` is `#000000` (`index.html:7`) and the manifest uses `theme_color #000000` / `background_color #ffffff` (`manifest.json:23-24`).
* **Font:** Roboto 300/400/500/700 from Google Fonts (`index.html:14-17`). The document title is **"EduNexus LMS"** (`index.html:18`).
* **CSS:** there are **no `.css`/`.scss` files**. All styling is MUI `sx` props plus the theme, with inline `style` only in `ErrorMessage.tsx:19-25` and `PrivateRoute.tsx:16`.

### 7.2 Typography actually used
`h4` for page titles (Dashboard, CourseList, CourseDetail, Profile name), `h5` for auth titles, `h6` for card and module titles, `h3` for Dashboard stat values, `body2` for list secondary text.

### 7.3 Spacing patterns
| Context | Value |
|---|---|
| Layout main padding | `p: 3` = 24 px (`Layout.tsx:181`) |
| Page wrappers | additional `p: 3` → **48 px effective inset** (Dashboard, CourseList, CourseDetail, Profile) |
| Grid gutters | `spacing={3}` = 24 px |
| Auth cards | `marginTop: 8` = 64 px, `Paper p: 4` = 32 px |
| Stat cards | `Paper p: 2` = 16 px |
| Submit buttons | `mt 3 mb 2` |

### 7.4 Responsive behaviour
| Breakpoint | Behaviour |
|---|---|
| `< 600px (xs)` | Drawer hidden, hamburger visible, temporary drawer overlays; AppBar full width; all grids 1 column; Profile name fields stacked |
| `≥ 600px (sm)` | Permanent 240 px drawer; AppBar and main offset; CourseList 2 columns; Profile names side by side |
| `≥ 900px (md)` | CourseList 3 columns; Dashboard stats 3 across |
| Auth pages | Fixed `maxWidth="xs"` (444 px) centred column at all sizes |

Known layout quirk: CourseDetail's loading state uses `height: 100vh` inside the padded main area (`CourseDetail.tsx:102`), which causes a vertical scroll.

---

## 8. Frontend → backend API map

**Base URLs in play:**
* **A** — `utils/axios.ts:6` → `http://localhost:8083/api`. Used by every Redux thunk.
* **B** — hard-coded `http://localhost:8080/api` in raw `fetch()` calls.
* **C** — `services/api.ts:4` → `REACT_APP_API_URL || http://localhost:8080/api`. Dead.

**Backend facts from Phase 1:**
* Spring listens on 8083, but because of the doubled prefix its endpoints sit at `/api/api/auth/*` and `/api/api/health` only.
* The Express code would listen on 8080 at `/api/auth/*`, `/api/courses/*` and `/api/users/*`, but it has no build setup.

### 8.1 Live call sites

| # | Caller (file:line) | Method & URL as sent | Expected backend handler | Spring (actual) | Express (actual) | Status |
|---|---|---|---|---|---|---|
| 1 | `authSlice.ts:33` (Login) | POST **A**`/auth/login` | Auth login | Exists at `/api/api/auth/login` → **path mismatch** | `routes/auth.ts:24` on 8080 → **port mismatch** | Broken as wired |
| 2 | `authSlice.ts:53` (Register) | POST **A**`/auth/register` body `{email,password,firstName,lastName,role:'student'…}` | Auth register | path mismatch (as above) | port mismatch; validator only allows `STUDENT/ADMIN` (`routes/auth.ts:15`) → 400 | Broken as wired |
| 3 | `courseSlice.ts:23` (CourseList mount) | GET **A**`/courses` | List courses | **none** | `routes/course.ts:15` (public) on 8080 → port mismatch | Broken |
| 4 | `CourseList.tsx:17` | POST **B**`/courses/{id}/enroll` | Enrol current user | none | `routes/course.ts:22`, `checkRole(['student'])` → **always 403** (token has no roles) | Broken |
| 5 | `CourseDetail.tsx:66` and `:142` | GET **B**`/courses/{id}` | Course by id (populated) | none | `routes/course.ts:16` (public) | Would work against Express |
| 6 | `CourseDetail.tsx:135` | POST **B**`/courses/{id}/enroll` | Enrol | none | same as #4 → 403 | Broken |
| 7 | `Profile.tsx:37` | PUT **B**`/users/profile` body `{firstName,lastName,email}` | Update profile | none | `routes/user.ts:11` | Would work against Express |

### 8.2 Defined but never invoked from UI

| Caller | Method & URL | Spring | Express |
|---|---|---|---|
| `authSlice.ts:67` `getCurrentUser` | GET A`/users/profile` | none | `routes/user.ts:10` |
| `courseSlice.ts:35` `fetchCourseById` | GET A`/courses/:id` | none | `routes/course.ts:16` |
| `courseSlice.ts:47` `createCourse` | POST A`/courses` | none | `routes/course.ts:19` (`instructor/admin`) |
| `courseSlice.ts:59` `updateCourse` | PUT A`/courses/:id` | none | `routes/course.ts:20` |
| `courseSlice.ts:71` `deleteCourse` | DELETE A`/courses/:id` | none | `routes/course.ts:21` (`admin`) |
| `courseSlice.ts:83` `enrollInCourse` | POST A`/courses/:id/enroll` | none | `routes/course.ts:22` |
| `services/api.ts:36,41` | POST C`/auth/login`, `/auth/register` | path mismatch | ✓ |
| `services/api.ts:46` | GET C`/auth/me` | **none** | **none** |
| `services/api.ts:53-80` | courses CRUD + enroll | none | ✓ |
| `services/api.ts:83` | POST C`/courses/:id/announcements` | **none** | **none** |
| `services/api.ts:88` | POST C`/courses/:id/discussions` | **none** | **none** |
| `services/api.ts:93-96` | POST C`/courses/:id/discussions/:did/replies` | **none** | **none** |

### 8.3 Backend routes with no frontend caller
* Express `PUT /api/users/password` (`backend/src/routes/user.ts:12`) — change password.
* `GET /health` in Express, and Spring `/api/api/health`.

### 8.4 Data-shape expectations the UI imposes (contract for recreation)
* **Auth response:** `{ token: string, user: { id, email, firstName, lastName, roles: string[], enrolledCourses?: string[], managedCourses?: string[] } }` (`authSlice.ts:5-13,106-110`).
* **Course (list):** `{ id, title, description }` (`CourseList.tsx:52-60`).
* **Course (detail):** `{ id, title, description, adminId: string, enrolledStudents: string[], teachingAssistants: string[], modules: [{ id, title, description, contents: [{ type }] }], announcements: [{ id, title, content, createdAt }], discussions: [{ id, title, content, createdAt }] }` (`CourseDetail.tsx:116-246`).
  * **Conflict:** Express returns `_id`, and `adminId`/`teachingAssistants`/`enrolledStudents` are *populated objects* in `getCourseById` (`backend/src/controllers/course.ts:19-22`). The UI compares them to string ids, so `isEnrolled`/`isInstructor` would always be false.
* **Error body:** `{ message: string }` (`authSlice.ts:38`, `CourseList.tsx:29`).
* **Content type casing:** the UI expects lowercase (`CourseDetail.tsx:89-93`, `types.ts:38`). Both backends use UPPERCASE `VIDEO/DOCUMENT/ASSIGNMENT/QUIZ/REFERENCE_BOOK`.

---

## 9. Feature status matrix

| Feature | Status | Evidence |
|---|---|---|
| Login form + validation + error display | **Functional (frontend)**; blocked only by backend wiring | `Login.tsx`, `authSlice.ts:29-41` |
| Registration form + role dropdown | **Functional (frontend)**; role values conflict with Express | `Register.tsx`, `authSlice.ts:43-61` |
| Token persistence + Bearer header | **Functional** | `utils/axios.ts:13-24` |
| Route protection | **Functional (token presence only)** | `PrivateRoute.tsx` |
| Logout (menu) + 401 auto-logout | **Functional** | `Layout.tsx:54-57`, `utils/axios.ts:32-35` |
| Responsive shell / drawer / account menu | **Functional** | `Layout.tsx` |
| Session restore after reload | **Missing** (thunk exists, never dispatched) | `authSlice.ts:63-73` |
| Dashboard stats | **Partial** — reads store only; blank after reload; role field shape mismatch | `Dashboard.tsx` |
| Dashboard "Recent Activity" | **Visual only** (static text) | `Dashboard.tsx:56-58` |
| Course list | **Partial** — fetch wired via Redux; wrong port vs Express; no navigation to detail | `CourseList.tsx` |
| Enrol from list | **Partial** — request made; backend role check always 403; `alert()` UX | `CourseList.tsx:15-35` |
| Course detail (modules/announcements/discussions read-only) | **Partial** — renders if data matches; no error handling; id/populate mismatches | `CourseDetail.tsx` |
| Enrol from detail | **Partial** — no feedback | `CourseDetail.tsx:128-156` |
| Module content viewing (video/doc/quiz) | **Missing** — only module title and description | `CourseDetail.tsx:166-190` |
| Discussion replies / posting | **Missing** (API stub in dead `services/api.ts`) | `services/api.ts:88-98` |
| Announcement posting | **Missing** (API stub only) | `services/api.ts:82-85` |
| Profile view/edit | **Partial** — works when the user is in the store; no validation or feedback | `Profile.tsx` |
| Change password | **Missing** UI (backend route exists) | — |
| Course create/edit/delete (instructor/admin) | **Missing** UI (thunks exist) | `courseSlice.ts:43-77` |
| Price / level / duration course card | **Visual only, unrouted** | `CourseDetails.tsx` |
| Reusable Error/Success/Loading components | **Built, unused** | `components/*Message.tsx`, `LoadingSpinner.tsx` |
| Role-based UI (admin/instructor/student) | **Missing** — only the "enrol vs instructor" check in detail | `CourseDetail.tsx:116-117` |
| README features: assignments, grades, progress, categories, analytics, admin user management | **Not present in the frontend at all** | — |

---

## 10. Missing pages, components and incomplete journeys

### 10.1 Missing pages
1. **Course create / edit form** (instructor/admin), including module and content builders.
2. **My Courses** (enrolled, and managed for instructors). The Dashboard counts point at this list but nothing shows it.
3. **Module / content viewer** (video player, document viewer, quiz, assignment submission).
4. **Discussion thread page** with replies and a reply composer (anonymous toggle — the `isAnonymous` field exists in backend models).
5. **Announcement composer** for course admins/TAs.
6. **Admin console:** user list, role management, course management.
7. **Assignments & grades** (README only).
8. **Change password** and **forgot / reset password**.
9. **404 / Not Found** page (the `*` route silently redirects to `/`).
10. **Public landing page** (`/` always requires auth).

### 10.2 Missing components
* Confirmation **Dialog/Modal** (delete course, leave course, logout).
* **Snackbar** / toast feedback (replacing `alert()` and console-only errors).
* **Data table** (admin user list, enrolled students, grades).
* Active-route highlight in the nav drawer; **breadcrumbs** / back button.
* Search, filter and pagination controls for courses.
* **Empty states** (no courses, no modules, no announcements).
* **Error boundary** and a page-level error display (the unused `ErrorMessage` would fit).
* Reverse auth guard (redirect logged-in users away from `/login` and `/register`).
* Session bootstrap (dispatch `getCurrentUser` when a token exists).

### 10.3 Incomplete user journeys
| Journey | Where it breaks |
|---|---|
| **Student: browse → view course → enrol → learn** | No link from list card to detail (`CourseList.tsx:53-69`); enrol returns 403 against Express; no content viewer |
| **Instructor: create course → add modules → post announcement** | No UI at all |
| **Admin: manage users/roles** | No UI at all |
| **Any user: return next day** | Token restored, user not → blank dashboard and profile (`authSlice.ts:23-24`) |
| **Any user: update profile → see dashboard** | `updateProfile` replaces `user` with a response lacking `enrolledCourses`/`managedCourses` → counts drop to 0 (`Profile.tsx:48`, `backend/src/controllers/user.ts:41-47`) |
| **Register with 6–7 char password → later log in** | Login requires 8 (`Login.tsx:25` vs `Register.tsx:33`) |
| **Discuss in a course** | Read-only titles; no replies or composer |

---

## 11. Reconstruction notes (what to keep vs. normalise)

To recreate the original **as closely as possible**:
* **Keep exactly:**
  * the route table (§3.2);
  * the Layout shell (240 px drawer, AppBar avatar menu, three nav items);
  * page compositions and copy text ("Sign in", "Create an Account", "Welcome, {name}!", "Available Courses", "Enroll", "Enroll in Course", tab labels, "No recent activity to display.");
  * theme tokens (§7.1) and spacing (§7.3);
  * the responsive breakpoints (§7.4).
* **Decide explicitly** — these are inconsistencies in the original, not design intent:
  1. A single HTTP client and base URL.
  2. A single `types` module, with role casing and `roles[]` vs `role`.
  3. `id` vs `_id`.
  4. Content-type casing.
  5. Whether to adopt the dead `CourseDetails.tsx` marketplace fields (price/level/duration).
  6. Login/register password-rule alignment.
* **Out of scope for "faithful" recreation unless desired:** README-only features (assignments, grades, analytics, categories).

---

## 12. Recommended next phase

**Phase 3 — Backend API, data model and authorization contract reconstruction.**

Phase 2 shows the frontend's behaviour depends on a backend contract that exists only in fragments across three sources: the Spring code, the orphaned Express code, and the frontend's own expectations (§8.4). Before any implementation, Phase 3 should:

1. **Enumerate every endpoint** across Spring, Express, `courseSlice` and `services/api.ts` into one canonical endpoint catalogue (method, path, auth, role, request body, response body, error body), with file:line provenance.
2. **Reconcile the data model:**
   * Java `model/User.java`, `model/Course.java` vs Mongoose `models/User.ts`, `models/Course.ts` vs `src/types.ts`;
   * covering field names, id strategy, embedded vs referenced documents, enums and casing, and timestamps.
3. **Build a role/permission matrix** (student / instructor / TA / admin × each action), derived from `checkRole` usage, `updateCourse`/`deleteCourse` ownership checks, and the frontend `isEnrolled`/`isInstructor` logic.
4. **Specify the auth/session contract:**
   * JWT claims needed by the UI (id, roles);
   * expiry, the `/auth/me` or `/users/profile` bootstrap, and the 401 behaviour.
5. **Pick a canonical backend target** for the recreation: Spring (matches README and build tooling) or Express (closer to the frontend's expected API). Record the decision and rationale.
6. **Output:**
   * `analysis/phase-3-backend-contract.md`;
   * an OpenAPI-style draft (`analysis/edunexus-api.yaml`);
   * a reconciled ER/document diagram.
   * No implementation code.

An optional later **Phase 4 (visual baseline)** could capture reference wireframes and screenshots of each page in an isolated sandbox, once a safe, dependency-pinned run of the frontend is approved.
