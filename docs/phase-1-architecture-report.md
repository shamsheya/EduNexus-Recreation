# EduNexus — Phase 1: Repository Architecture Report

| | |
|---|---|
| **Target** | `EduNexus`, branch `main`, commit `8dec71febe056604d0629acb9226c0756e65cf77` (verified unchanged in Phase 0) |
| **Date** | 2026-11-05 (initial inspection delivered in chat; this file is the persisted, citation-complete version) |
| **Checks performed** | `git clone`, `git log --stat`, a full file listing, a full read of every tracked source and config file, lockfile version extraction, REA `analyze_javascript_application` on `frontend/` and `backend/` |
| **Not performed** | No install, build or run; no file modified |

---

## 1. Repository layout (68 tracked files)
```
EduNexus/
├── .gitignore                       IDE, build, node_modules, .env*, logs, OS files
├── LICENSE                          MIT
├── README.md                        product docs (claims verified in §7)
├── backend/
│   ├── .mvn/wrapper/maven-wrapper.properties   wrapper 3.3.2 → Maven 3.9.9
│   ├── mvnw, mvnw.cmd
│   ├── pom.xml                                 Spring Boot 3.2.3, Java 17
│   └── src/
│       ├── main/java/com/edunexus/…            20 Java files (Spring backend)
│       ├── main/resources/application.properties, application.yml
│       ├── controllers/, routes/, models/, middleware/   Express TS files
│       ├── index.ts, server.ts                  two Express entry points
└── frontend/
    ├── package.json, package-lock.json (1 430 package entries), tsconfig.json
    ├── public/index.html, manifest.json
    └── src/  (App, index, theme, types, components/, pages/, services/, store/, utils/)
```

## 2. Technologies actually present

| Layer | Technology | Evidence |
|---|---|---|
| Frontend | React 18 (locked 18.3.1), TypeScript ^4.9.5, CRA `react-scripts` 5.0.1 | `frontend/package.json:20-25`; lockfile |
| UI kit | MUI 5 (locked 5.17.1), Emotion, MUI icons | `package.json:6-9` |
| State | Redux Toolkit ^1.9.3 (locked 1.9.7), react-redux ^8.0.5 | `package.json:10,22` |
| Routing | react-router-dom ^6.8.2 (locked 6.30.0) | `package.json:23` |
| Forms | Formik ^2.2.9, Yup ^1.1.1 | `package.json:19,27` |
| HTTP | axios ^1.3.4 (locked 1.8.4) + `fetch` | `package.json:18` |
| Backend A | Spring Boot 3.2.3 (web, data-mongodb, security, validation), jjwt 0.11.5, spring-dotenv 3.0.0, springdoc 2.3.0, Lombok; Java 17 | `backend/pom.xml:7-12,20-23,25-95` |
| Backend B | Express + Mongoose + jsonwebtoken + bcryptjs + express-validator + helmet + compression + morgan + cors + dotenv (versions unknown; **no manifest**) | imports in `backend/src/*.ts` |
| Database | MongoDB, database `edunexus` | `application.yml:9`, `MongoConfig.java:21-23`, `backend/src/index.ts:31` |

## 3. Entry points
| Component | Entry | Evidence |
|---|---|---|
| Frontend | `src/index.tsx` → `createRoot(#root)` → `<App/>` | `frontend/src/index.tsx:7-15`; `public/index.html:22` |
| Spring | `EduNexusApplication.main` (`@SpringBootApplication`, `@EnableMongoAuditing`) | `EduNexusApplication.java:7-12` |
| Express (simple) | `backend/src/index.ts` (connect, then listen) | `index.ts:33-44` |
| Express (full) | `backend/src/server.ts` (helmet, retrying connect, error and 404 handlers) | `server.ts:16-96` |

## 4. Configuration (key names only; values withheld)
| File | Keys | Evidence |
|---|---|---|
| `application.properties` | `server.port` (8083), `server.servlet.context-path` (`/api`), `spring.data.mongodb.host/port/database`, logging levels, `jwt.secret` (**committed secret**), `jwt.expiration`, `springdoc.*`, multipart limits, `spring.web.cors.*`, `management.*` | `:1-34` |
| `application.yml` | `server.port` (8080), context-path, `spring.data.mongodb.uri`, `auto-index-creation`, `spring.security.jwt.secret` (placeholder), `.expiration`, logging | `:1-19` |
| Express env vars | `JWT_SECRET`, `MONGODB_URI`, `PORT`, `CORS_ORIGIN`, `NODE_ENV` | `controllers/auth.ts:35,82`; `middleware/auth.ts:29`; `index.ts:31,37`; `server.ts:19,39,84,93` |
| Frontend env vars | `REACT_APP_API_URL` (used only in the dead `services/api.ts:4`) | — |
| Frontend hard-coded URLs | `http://localhost:8083/api` (`utils/axios.ts:6`); `http://localhost:8080/api` (`CourseDetail.tsx:66,135,142`, `CourseList.tsx:17`, `Profile.tsx:37`) | — |
| `.env` / `.env.example` | **None committed** (`.env` ignored at `.gitignore:13-15`) | — |

## 5. Build setup
| Component | Build | Status |
|---|---|---|
| Frontend | `npm start` / `build` / `test` via react-scripts (`package.json:29-34`); lockfile present | Buildable in principle (not executed) |
| Spring | `mvnw` / `mvn` with `spring-boot-maven-plugin` (`pom.xml:97-112`) | Buildable in principle; **runtime startup expected to fail** — duplicate `jwtService` bean (Phase 3 §2.4 S1, inferred) |
| Express | — | **Not buildable**: no `package.json`, `tsconfig.json` or lockfile |
| Tests | — | **None.** No `backend/src/test`, no `*.test.tsx` |
| CI / Docker | — | None |

## 6. Git history (7 commits, all 2025-04-19, single author)
| Commit | Message | Content |
|---|---|---|
| `fecedbc` | Initial commit | `README.md` (1 line) |
| `9d291e0` | EduNexus | Added the whole backend, **all 27 frontend files**, `.gitignore`, README, **and an unrelated `Custom-Object-Detection-from-Drone-main/` project incl. a 13.4 MB `frozen_inference_graph.pb`** |
| `a86661e` | Resolve merge conflict in README.md | — |
| `92758a6` | EduNexus | Removed the drone project (still in history); README rewrite |
| `a2068f9` | EduNexus | LICENSE |
| `ddddf3e`, `8dec71f` | EduNexus | README edits |

`git log -- frontend` returns only `9d291e0`. All application code arrived in one bulk import, and later commits touch only README/LICENSE. The history carries **no incremental development signal**: the code's evolution (e.g. Express → Spring) cannot be recovered from Git and is inferred from the code alone. The author name appears in two spellings across commits.

## 7. Missing files
| Missing | Referenced by |
|---|---|
| `public/favicon.ico`, `logo192.png`, `logo512.png` | `public/index.html:5,12`; `manifest.json:6,11,16` |
| `frontend/README.md`, `backend/README.md` | `README.md:99,111` |
| `backend/src/test/` | `README.md:97` |
| Express `package.json`, `tsconfig.json` | required to build `backend/src/*.ts` |
| `.env.example` | env vars documented in `README.md:255-267` |

## 8. README claims vs. source
| README claim (line) | Source reality | Verdict |
|---|---|---|
| Stack: React, Spring Boot, MongoDB (`:3`, `:49-68`) | True, **plus** an undocumented Express backend | Partially true |
| Java 11+ (`:73`) | `pom.xml:21` requires 17 | False |
| Frontend `App.js`, `index.js` (`:108-109`) | `App.tsx`, `index.tsx` | Outdated |
| RBAC Admin/Instructor/Student (`:9`) | No working role enforcement in either backend (Phase 3) | False |
| BCrypt (`:10`) | `SecurityConfig.java:77-80`; `models/User.ts:44-54` | True |
| Course creation/management (`:22`) | Express only; role check always denies | Not working |
| Enrolment (`:23`) | Express only; role check always denies | Not working |
| Progress tracking, categorisation, assignments, grades, analytics, system config (`:25-47`) | Absent from all code | False |
| User schema `role: String` (`:124`) | Spring: `role`; Express: `roles[]` | Partially true |
| Course schema `instructorId`, `category`, `assignments` (`:138-161`) | Code uses `adminId`, `teachingAssistants`, `announcements`, `discussions` | False |
| Assignment collection (`:168-188`) | Not in code | False |
| `GET /api/auth/me` (`:231`) | Not implemented | False |
| `/api/users` CRUD (`:234-237`) | Only `/api/users/profile` + `/password` in Express | False |
| `/api/courses` CRUD (`:240-244`) | Express only | Partially true |
| CSRF protection (`:251`) | `SecurityConfig.java:36` disables CSRF | False |
| `REACT_APP_API_URL=http://localhost:8083/api` (`:266`) | Read only by the dead `services/api.ts:4` | Misleading |
| `mvn spring-boot:run` to start backend (`:206-208`) | Would hit the startup failure (inferred) | Unverified / likely false |

## 9. Summary
The repository combines one buildable frontend, one buildable-but-likely-non-starting Spring backend that implements only auth, and one unbuildable Express backend that holds most of the API logic. Full detail is in the Phase 2 and Phase 3 reports.

**Next:** Phase 2 (already complete → `phase-2-frontend-report.md`), then Phase 3 completion.
