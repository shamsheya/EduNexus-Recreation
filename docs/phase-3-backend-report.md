# EduNexus — Phase 3: Backend Reverse-Engineering Report

| | |
|---|---|
| **Target** | `EduNexus\backend` (commit `8dec71f`) |
| **Date** | 2026-11-05 |
| **Inputs** | Phase 1 inspection (chat report), `analysis/phase-2-frontend-report.md`, full read of every backend source file |
| **Constraints honoured** | No file modified, nothing installed or executed, no secret values reproduced (config keys are named; values are omitted) |
| **Companion deliverables** | `api-contract.md`, `database-schema.md`, `role-permission-matrix.md`, `backend-feature-gap.md`, `recreation-architecture.md` |

**Status vocabulary** used in all Phase 3 documents:
* **Implemented** — code exists and the static reading indicates it performs its stated purpose.
* **Partial** — code exists but is incomplete or demonstrably defective.
* **Broken** — code exists but cannot work as written.
* **Missing** — referenced by the frontend or README, absent from both backends.
* **Inferred** — a conclusion from framework/library semantics rather than an explicit line of code; never asserted as fact.

All backend paths are relative to `backend/` unless prefixed.

---

## 0. Executive summary

* `backend/` contains **two independent backends sharing one folder**:
  1. **Spring Boot 3.2.3 / Java 17** (`src/main/java/com/edunexus`, 20 files, Maven). It implements only **register, login and health**, and has **zero role enforcement**.
  2. **Express + Mongoose (TypeScript)** (`src/*.ts`, 13 files). It implements **auth, course CRUD, enrolment and profile**, but has **no `package.json`/`tsconfig.json`**, **two conflicting entry points**, and **role checks that reject every request**.
* **Neither backend serves the frontend as wired** (Phase 2 §8).
* The **frontend's primary HTTP client targets Spring's port (8083) and Spring's JSON conventions** (`id`, lowercase roles). The **frontend's endpoint list mirrors the Express route set**. The original design was therefore most likely "Spring Boot backend with the API surface prototyped in Express" (inferred).
* **Spring defects:**
  * A **duplicate `jwtService` bean** prevents application context startup (inferred, high confidence).
  * Endpoints sit under a **doubled `/api/api/...` prefix**.
  * There are **two JWT implementations with incompatible key derivation**.
* **Express defects:**
  * `checkRole` reads `req.user.roles`, but the JWT carries only `userId`, so every role-gated route returns **403**.
  * Role names disagree three ways: `instructor` (route), `STUDENT/ADMIN` (model), and lowercase `student/instructor/admin` (frontend).
* **Data model:** one `User` collection and one `Course` collection with embedded modules → contents, announcements, discussions → replies. Both backends agree structurally (§4 and `database-schema.md`).
* **Recommendation** (detail in `recreation-architecture.md`): recreate on **Spring Boot 3 + Spring Security + Spring Data MongoDB**, with an endpoint surface reconstructed from the Express controllers and the frontend's expectations.

---

## 1. REA tool usage

| REA call | Result | Use in this report |
|---|---|---|
| `analyze_javascript_application` on `backend/` | Evidence `ev_f27360b6…dee6`. **13 TS files, 13 parsed, 0 parse failures**, 3 371 AST nodes, 143 findings, 23 configuration relations, 1 timer relation (`server.ts:44` reconnect `setTimeout`) | Confirms the Express file set is complete and syntactically valid TypeScript |
| `inspect_analysis_view` → `javascript-routes/occurrences` | Evidence `ev_61cbe2ff…93dd`. **0 route occurrences** | REA's route extractor targets client-side navigation and does **not** model Express `router.get/post` server routes. The endpoint inventory (§3) was therefore built by manual reading. |
| `analyze_javascript_application` on `frontend/` (Phase 2) | Evidence `ev_8028b1e9…8773` | Source of frontend endpoint expectations reused in `api-contract.md` |
| Java sources | — | REA provides analysers for compiled JVM/.NET artifacts (`inspect_managed_artifact`). No compiled `.class`/`.jar` exists and building one would execute Maven, which is out of scope. **Java was analysed by source reading only.** |

A grep of the Java tree found **no** `@PreAuthorize`, `@Secured`, `@RolesAllowed`, `@EnableMethodSecurity`, `hasRole` or `hasAuthority`. The only `*Mapping` annotations are in `AuthController` and `HealthController`.

---

## 2. TASK 1 — Spring Boot backend

### 2.1 Build and dependencies — `pom.xml`
| Artifact | Version | Line | Used in code? |
|---|---|---|---|
| `spring-boot-starter-parent` | 3.2.3 | 7-12 | — |
| Java | 17 | 21 | — |
| `spring-boot-starter-web` | (parent) | 27-30 | Yes |
| `spring-boot-starter-data-mongodb` | (parent) | 31-34 | Yes |
| `spring-boot-starter-security` | (parent) | 35-38 | Yes |
| `spring-boot-starter-validation` | (parent) | 39-42 | Annotations present on DTOs, **never triggered** (no `@Valid`) |
| `jjwt-api` / `-impl` / `-jackson` | 0.11.5 | 45-61 | Yes |
| `me.paulschwarz:spring-dotenv` | 3.0.0 | 64-68 | Implicit (loads a `.env` if present; no `.env` is committed) |
| `springdoc-openapi-starter-webmvc-ui` | 2.3.0 | 71-75 | Configured in properties only |
| `lombok` | (parent) | 78-82 | Yes |
| `spring-boot-starter-test`, `spring-security-test` | test | 85-94 | No tests exist (`src/test` absent) |

`application.properties:32-34` configures Actuator endpoints, but **`spring-boot-starter-actuator` is not a dependency**, so these keys are inert.

### 2.2 Package map
```
com.edunexus
├── EduNexusApplication.java          @SpringBootApplication + @EnableMongoAuditing   (:7-8)
├── config/
│   ├── MongoConfig.java              AbstractMongoClientConfiguration, repos scan
│   └── SecurityConfig.java           filter chain, CORS, BCrypt, DaoAuthProvider
├── controller/
│   ├── AuthController.java           POST /register, POST /login
│   └── HealthController.java         GET  (health)
├── dto/
│   ├── AuthenticationRequest.java    {email,password}           — used
│   ├── AuthenticationResponse.java   {token, user: User}        — used
│   ├── LoginRequest.java             {email,password}+validation — UNUSED
│   └── RegisterRequest.java          {email,password,firstName,lastName,role}+validation
├── exception/GlobalExceptionHandler.java
├── model/
│   ├── User.java                     implements UserDetails, @Document("users")
│   └── Course.java                   @Document("courses") + 5 nested static classes
├── repository/
│   ├── UserRepository.java           findByEmail, existsByEmail
│   └── CourseRepository.java         findByAdminId, findByEnrolledStudentsContaining, findByTeachingAssistantsContaining — UNUSED
├── security/
│   ├── JwtAuthenticationFilter.java  OncePerRequestFilter
│   └── JwtService.java               (variant A, Base64-decoded key)
└── service/
    ├── AuthService.java              register/login logic — UNUSED (never injected)
    ├── CustomUserDetailsService.java
    └── JwtService.java               (variant B, raw-bytes key)  ← same simple name as security.JwtService
```

### 2.3 Configuration (values withheld)
Two config files are loaded together. When the same key appears in both, `.properties` takes precedence over `.yml` (Spring Boot property-source ordering; inferred).

| Key | `application.properties` | `application.yml` | Effective |
|---|---|---|---|
| `server.port` | 8083 (`:2`) | 8080 (`:2`) | **8083** |
| `server.servlet.context-path` | `/api` (`:3`) | `/api` (`:4`) | `/api` |
| Mongo connection | host/port/database keys (`:6-8`) | `spring.data.mongodb.uri` (`:9`) | `MongoConfig` reads **only** `spring.data.mongodb.uri` (`MongoConfig.java:17`) → value comes from the yml |
| `spring.data.mongodb.auto-index-creation` | — | `true` (`:10`) | Likely **ignored**: a custom `AbstractMongoClientConfiguration` defaults `autoIndexCreation()` to `false` (inferred) |
| `jwt.secret` | present, 64-char hex string (`:15`) | — | Used by both `JwtService` variants |
| `spring.security.jwt.secret` / `.expiration` | — | present (`:12-14`) | **Never read** by any code |
| `jwt.expiration` | 24 h in ms (`:16`) | — | Token lifetime |
| `springdoc.*` | `/api-docs`, `/swagger-ui.html` (`:19-20`) | — | Behind auth (see §2.6) |
| `spring.web.cors.*` | (`:27-30`) | — | Not standard Boot properties; effective CORS comes from `SecurityConfig` |
| `logging.level.org.springframework.security` | DEBUG (`:11`) | DEBUG (`:18`) | Verbose security logs |

**Secret handling:** a signing secret is committed in source (`application.properties:15`). Its value is deliberately not reproduced in any Phase 3 document.

### 2.4 Application startup analysis
| # | Finding | Evidence | Confidence |
|---|---|---|---|
| S1 | **Two `@Service` classes both named `JwtService`** (`security/JwtService.java:18-19`, `service/JwtService.java:17-18`). Spring's default bean name is the decapitalised simple class name (`jwtService`), so component scanning should fail with `ConflictingBeanDefinitionException` and the context would not start. | Source + Spring naming convention | Inferred, high |
| S2 | `MongoConfig` requires `spring.data.mongodb.uri`. It is satisfied only through `application.yml:9`; delete the yml and startup fails. | `MongoConfig.java:17-18` | Implemented (fragile) |
| S3 | `MongoConfig.getDatabaseName()` is hard-coded to `"edunexus"` (`:21-23`) and overrides any database in the URI. | Source | Implemented |
| S4 | `@Indexed(unique = true)` on `User.email` (`User.java:32`) is probably not created, because auto-index creation is off under a custom `AbstractMongoClientConfiguration`. Uniqueness then rests only on the application-level check in `AuthController.java:31`, which races. | Source + framework default | Inferred, medium |

### 2.5 URL resolution
`server.servlet.context-path=/api` (`application.properties:3`) is prepended to every controller mapping:

| Controller mapping | Effective public URL |
|---|---|
| `@RequestMapping("/api/auth")` + `@PostMapping("/register")` (`AuthController.java:18,28`) | **`POST http://localhost:8083/api/api/auth/register`** |
| `… @PostMapping("/login")` (`:55`) | **`POST /api/api/auth/login`** |
| `@RequestMapping("/api/health")` + `@GetMapping` (`HealthController.java:9,12`) | **`GET /api/api/health`** |
| springdoc | `/api/api-docs`, `/api/swagger-ui.html` |

The frontend calls `http://localhost:8083/api/auth/login` (`frontend/src/utils/axios.ts:6` + `authSlice.ts:33`). No mapping exists at that path, so the request is rejected by the security chain (`anyRequest().authenticated()`, `SecurityConfig.java:40`) → **403** (inferred).

### 2.6 Security filter chain — `config/SecurityConfig.java`
| Concern | Code | Line |
|---|---|---|
| CSRF | disabled | 36 |
| CORS | allowed origin `http://localhost:3000`; methods GET/POST/PUT/DELETE/OPTIONS; headers `Authorization`, `Content-Type`; credentials true | 37, 51-62 |
| Public paths | `requestMatchers("/api/auth/**").permitAll()` | 39 |
| Everything else | `.anyRequest().authenticated()` | 40 |
| Sessions | `STATELESS` | 42-44 |
| Auth provider | `DaoAuthenticationProvider` + `CustomUserDetailsService` + `BCryptPasswordEncoder` (strength default 10) | 64-80 |
| JWT filter | `addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)` | 46 |
| Controller-level CORS | `@CrossOrigin(origins = "http://localhost:3000")` duplicated on `AuthController` | `AuthController.java:20` |

**Inferred matcher semantics (Spring Security 6.2, single DispatcherServlet):** `requestMatchers(String)` builds an MVC matcher evaluated *after* the context path is stripped. `/api/auth/**` therefore matches the doubled URL `/api/api/auth/**`, which is exactly where the controller lives. Auth endpoints would be reachable at the doubled URL once S1 is resolved.

**Not public:** `/api/api/health`, swagger UI and api-docs, and the `/error` path. A framework error forward on a public endpoint would surface as 401/403 rather than the real error (inferred).

**No role-based authorization exists.** There are no `hasRole` rules and no method security. Any authenticated user can reach any future endpoint.

### 2.7 Authentication flow (traced)

#### Registration — `AuthController.register` (`:28-53`)
1. `@RequestBody RegisterRequest request` — **no `@Valid`**, so every `@NotBlank`/`@Email`/`@Size` on `RegisterRequest.java:11-26` is ignored.
2. `userRepository.findByEmail(email).isPresent()` → `400` with plain-text body `Email already exists` (`:31-33`).
3. Builds `User` with `passwordEncoder.encode(password)` and **`role(request.getRole())` copied verbatim** — no enum check, null allowed (`:35-41`). `RegisterRequest.isValidRole()` (`:28-35`) exists but is not called here.
4. `userRepository.save(user)` (`:43`).
5. `jwtService.generateToken(user)` uses **`service.JwtService`** (import `AuthController.java:8`).
6. Returns `200 OK`, `AuthenticationResponse{ token, user }` with the full `User` entity (`:46-49`).
7. Any exception → `400` plain text `Registration failed: <exception message>` (`:50-52`), leaking internals.

#### Login — `AuthController.login` (`:55-73`)
1. `authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password))` (`:58-60`) → `DaoAuthenticationProvider` → `CustomUserDetailsService.loadUserByUsername(email)` (`CustomUserDetailsService.java:17-20`) → BCrypt match.
2. Re-reads the user by email (`:62-63`), generates a token (`:65`), returns `200 {token, user}` (`:66-69`).
3. Bad credentials or unknown user → caught by `catch (Exception)` → `400` plain text `Authentication failed: <message>` (`:70-72`). **`GlobalExceptionHandler.handleBadCredentialsException` (401) is never reached**, because the controller swallows the exception.
4. **Null-role users:** `User.getAuthorities()` evaluates `role.toUpperCase()` (`User.java:53`). A user registered without a role would throw a `NullPointerException` when Spring Security builds the authenticated token → caught → 400 (inferred).

#### Authenticated request — `JwtAuthenticationFilter.doFilterInternal` (`:27-58`)
1. No `Authorization: Bearer …` header → pass through as anonymous (`:36-39`).
2. `userEmail = jwtService.extractUsername(jwt)` (`:42`), using **`service.JwtService`** (import `:3`).
3. If no authentication is in context: load `UserDetails` by email (`:45`). If `validateToken` succeeds, set a `UsernamePasswordAuthenticationToken` with authorities `[ROLE_<ROLE>]` (`:47-55`).
4. **There is no try/catch.** An expired or tampered token throws a JJWT exception (`ExpiredJwtException`, `SignatureException`, `MalformedJwtException`) out of the filter. This happens *before* `DispatcherServlet`, so `GlobalExceptionHandler` cannot catch it, and the client gets a container error (500, or 401/403 after the error forward) instead of a clean 401 (inferred). It also affects `/api/auth/**` if a stale token header is sent.

#### Unused path — `service/AuthService.java`
* Implements a stricter version of the same flow: an `existsByEmail` check, a role check through `isValidRole` (`:41-44`), and JSON error bodies `{ "message": … }` (`:38`, `:43`, `:73`, `:99`).
* Its response user is a **flattened map**: `{ id, email, firstName, lastName, role }` (`:62-68`, `:89-95`).
* It uses **`security.JwtService`** (import `:20`), not the service variant.
* **No class injects `AuthService`.** This is effectively a second, abandoned design. Its JSON shape is closer to what the frontend parses (`response.data.message`).

### 2.8 JWT implementation comparison
| Aspect | `security/JwtService.java` (A) | `service/JwtService.java` (B) |
|---|---|---|
| Used by | `AuthService` (unused) | `AuthController`, `JwtAuthenticationFilter` |
| Signing key | `Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))` (`:73-76`) | `Keys.hmacShaKeyFor(secret.getBytes())` (`:47-50`) |
| Algorithm | HS256 (`:47`) | HS256 (`:67`) |
| Claims | `sub` = email, `iat`, `exp`, plus optional extra claims (`:40-49`) | `sub` = email, `iat`, `exp` (`:56-69`) |
| Validation | `username.equals(userDetails.getUsername()) && !expired` (`:51-54`) | same (`:71-74`) |
| Role / id in token | **No** | **No** |

* Because A and B derive different key bytes from the same secret, **tokens from one cannot be verified by the other**. This is moot only because A's only consumer is unused.
* **Tokens carry no roles and no user id**, so every authorization decision must hit the database through `loadUserByUsername`.
* There is no refresh token, no revocation and no logout endpoint.

### 2.9 Error handling — `exception/GlobalExceptionHandler.java`
| Handler | Status | Body | Reached? |
|---|---|---|---|
| `MethodArgumentNotValidException` (`:18-27`) | 400 | `{ field: message }` map | **Never** (no `@Valid` anywhere) |
| `UsernameNotFoundException` (`:29-32`) | 404 | plain text | Not by controllers (swallowed); not from the filter (outside MVC) |
| `BadCredentialsException` (`:34-37`) | 401 | `Invalid email or password` | Never (swallowed in `AuthController`) |
| `Exception` (`:39-43`) | 500 | `An unexpected error occurred: <msg>` (leaks message) | Only for exceptions escaping controllers |

**The response envelope is inconsistent:** plain strings (controller), JSON `{message}` (unused `AuthService`), field maps (validation handler). The frontend only reads `response.data.message` (Phase 2 §8.4).

### 2.10 Document models (summary; full field tables in `database-schema.md`)
* **`User`** (`model/User.java`, `@Document("users")`):
  * `id`, `email` (`@Indexed(unique)`), `password` (`@JsonIgnore`), `firstName`, `lastName`;
  * **`role: String` (single)**, `enrolledCourses: List<String>`, `managedCourses: List<String>`, `enabled`;
  * `createdAt` / `updatedAt` as `Long` with `@CreatedDate` / `@LastModifiedDate`.
  * Implements `UserDetails`: authorities `ROLE_` + upper-cased role, username = email, all account flags `true`.
  * Nested `enum Role { student, instructor, admin }` (`:86-90`) is referenced only by `RegisterRequest.isValidRole()`.
  * The Lombok `@Builder` with field initialiser `enabled = true` (`:42`) and no `@Builder.Default` means builder-created users persist `enabled=false` (Lombok semantics, inferred). This is harmless only because `isEnabled()` is hard-coded to `true` (`:82-84`).
* **`Course`** (`model/Course.java`, `@Document("courses")`):
  * `id`, `title`, `description`, `adminId: String`, `enrolledStudents: Set<String>`, `teachingAssistants: Set<String>`;
  * `modules: List<CourseModule>`, `announcements: List<Announcement>`, `discussions: List<Discussion>`;
  * `createdAt` / `updatedAt` as `LocalDateTime` (no auditing annotations, so never populated automatically).
* **Serialisation of `AuthenticationResponse.user`** (inferred from Jackson defaults plus the `@JsonIgnore` placements in `User.java:35,51-84`):
  * expected keys: `id, email, firstName, lastName, role, enrolledCourses, managedCourses, createdAt, updatedAt`;
  * `password`, `authorities`, `username` and the account-flag properties are suppressed;
  * `enrolledCourses` and `managedCourses` will be `null` for new users (no initialiser).

### 2.11 Spring limitations (consolidated)
1. Context startup blocked by the duplicate bean (S1).
2. Endpoints under `/api/api/...` (§2.5).
3. Only 3 endpoints exist: register, login, health. **No course, user, profile, enrolment, announcement or discussion API.** `CourseRepository` is unused.
4. No input validation (no `@Valid`).
5. Any role string is accepted at registration, including `admin`, null or arbitrary text.
6. No role-based authorization anywhere.
7. JWT exceptions are unhandled in the filter.
8. Error bodies are plain text; the frontend expects `{message}`.
9. The response user has a singular `role`; the frontend expects `roles[]`.
10. Secrets are committed; security logging is at DEBUG.
11. Dead code: `AuthService`, `security.JwtService`, `LoginRequest`, `CourseRepository` finders, Actuator config, `spring.security.jwt.*` yml keys.
12. No tests.

---

## 3. TASK 2 — Express backend

### 3.1 Build status
* **There is no `package.json`, `tsconfig.json`, lockfile or `.env.example`** under `backend/`. The TypeScript cannot be compiled or run as committed.
* Dependencies implied by imports (versions unknown): `express`, `cors`, `morgan`, `dotenv`, `mongoose`, `helmet`, `compression`, `express-validator`, `jsonwebtoken`, `bcryptjs`.
* **Environment variables read** (names only):
  * `JWT_SECRET` (`controllers/auth.ts:35,82`, `middleware/auth.ts:29`; falls back to a hard-coded placeholder string);
  * `MONGODB_URI` (`index.ts:31` has a localhost fallback; `server.ts:39` has no fallback, using a non-null assertion);
  * `PORT` (fallback 8080: `index.ts:37`, `server.ts:93`);
  * `CORS_ORIGIN` (fallback `http://localhost:3000`, `server.ts:19`);
  * `NODE_ENV` (`server.ts:84`).

### 3.2 Two competing entry points
| Concern | `src/index.ts` | `src/server.ts` |
|---|---|---|
| Middleware | `cors()` **open to all origins** (`:16`), `express.json()`, `morgan` | `helmet()`, `cors({origin, credentials})`, `express.json({limit:'10mb'})`, `compression`, `morgan` (`:17-24`) |
| Auth routes | `./routes/auth` (`:6,21`) | `./routes/auth` (`:8,27`) |
| Course routes | **`./routes/courses`** (stub) (`:7,22`) | **`./routes/course`** (real) (`:9,28`) |
| User routes | **`./routes/users`** (stub) (`:8,23`) | **`./routes/user`** (real) (`:10,29`) |
| Health | `GET /health` (`:26-28`) | `GET /health` (`:32-34`) |
| DB connect | connect, *then* listen; on failure, log and never listen (`:33-44`) | listen immediately; connect with 5 s retry loop (`:37-48`, `:93-96`) |
| Error middleware | none | Mongoose `ValidationError` → 400, duplicate key → 400, JWT errors → 401, other → 500 (`:51-86`) |
| 404 | none | `{message:'Route not found'}` (`:89-91`) |

`server.ts` is the more complete and intended entry point (inferred from its richer middleware and real routes). Every controller catches its own errors and returns 500 `{message:'Server error'}`, so **the `server.ts` error middleware is effectively unreachable** for controller errors.

### 3.3 Route inventory (both entry points)
| Method | Path | Middleware | Handler | File:line | Mounted by |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | `registerValidation` | `register` | `routes/auth.ts:23` | both |
| POST | `/api/auth/login` | `loginValidation` | `login` | `routes/auth.ts:24` | both |
| GET | `/api/courses` | — (public) | `getAllCourses` | `routes/course.ts:15` | server.ts |
| GET | `/api/courses/:id` | — (public) | `getCourseById` | `routes/course.ts:16` | server.ts |
| POST | `/api/courses` | `auth`, `checkRole(['instructor','admin'])` | `createCourse` | `routes/course.ts:19` | server.ts |
| PUT | `/api/courses/:id` | `auth`, `checkRole(['instructor','admin'])` | `updateCourse` | `routes/course.ts:20` | server.ts |
| DELETE | `/api/courses/:id` | `auth`, `checkRole(['admin'])` | `deleteCourse` | `routes/course.ts:21` | server.ts |
| POST | `/api/courses/:id/enroll` | `auth`, `checkRole(['student'])` | `enrollInCourse` | `routes/course.ts:22` | server.ts |
| GET | `/api/users/profile` | `auth` (router-level) | `getProfile` | `routes/user.ts:8,10` | server.ts |
| PUT | `/api/users/profile` | `auth` | `updateProfile` | `routes/user.ts:11` | server.ts |
| PUT | `/api/users/password` | `auth` | `changePassword` | `routes/user.ts:12` | server.ts |
| GET | `/api/courses` | `auth` | stub → `{message:'Courses route'}` | `routes/courses.ts:7-9` | index.ts |
| GET | `/api/users/profile` | `auth` | stub → `{message:'User profile route'}` | `routes/users.ts:7-9` | index.ts |
| GET | `/health` | — | `{status:'ok'}` | `index.ts:26`, `server.ts:32` | both |

### 3.4 Middleware — `middleware/auth.ts`
* **`auth`** (`:21-35`):
  * Strips `Bearer ` from the `Authorization` header. Missing → `401 {message:'No token, authorization denied'}`.
  * `jwt.verify(token, secret)` → `req.user = decoded`. Any failure → `401 {message:'Token is not valid'}`.
  * It does **not** check that the user still exists.
* **`checkRole(roles)`** (`:37-52`): reads `req.user.roles || []` (`:43`) and returns 403 unless one of `roles` is included.
  * **Defect E1 — roles are never in the token.** Tokens are signed with payload `{ userId }` only (`controllers/auth.ts:33-37`, `:80-84`). `req.user.roles` is always `undefined`, so **every `checkRole` route returns `403 {message:'Access denied'}`**: create, update, delete and enrol.
  * **Defect E2 — role vocabulary mismatch.** Routes check lowercase `'instructor'`, `'admin'`, `'student'` (`routes/course.ts:19-22`). The model enum is uppercase `'STUDENT' | 'ADMIN'` (`models/User.ts:28`). **`instructor` does not exist in the Express model at all.** Even if roles were embedded in the token, the casing would still fail.
* The global `Express.Request.user` augmentation (`:13-19`) types `user` as `{userId}`, contradicting the `roles` read. TypeScript accepts it only because handlers use `AuthRequest` with `user?: any` (`:5-7`).

### 3.5 Controllers — behaviour as written

#### `controllers/auth.ts`
| Function | Behaviour | Response |
|---|---|---|
| `register` (`:6-56`) | express-validator result → 400 `{errors:[…]}`. Rejects an existing email with 400. Creates User with `roles: role ? [role] : ['STUDENT']` (`:27`), so **self-registration as `ADMIN` is allowed**. The password is hashed by the pre-save hook. | `201 { token, user: { id, email, firstName, lastName, roles } }` (`:39-48`). Duplicate-key race → 400 `{message:'Email already exists'}` (`:51-53`). Other errors → 500 `{message:'Server error'}`. |
| `login` (`:58-100`) | Validator. `findOne({email})`, `comparePassword`. Unknown user or wrong password → 400 `{message:'Invalid credentials'}`. | `200 { token, user: { id, email, firstName, lastName, roles } }` (`:86-95`) |

**Validation rules** (`routes/auth.ts:8-21`):
* register: `email` isEmail, `password` length ≥ 6, `firstName` and `lastName` notEmpty, `role` optional and in `['STUDENT','ADMIN']`;
* login: `email` isEmail, `password` notEmpty.

Note: the frontend sends lowercase `role` (`Register.tsx:58`), so Express registration from the UI fails validation with **400**.

#### `controllers/course.ts`
| Function | Behaviour | Response | Defects |
|---|---|---|---|
| `getAllCourses` (`:5-15`) | `Course.find()`, populating `adminId` and `teachingAssistants` with `firstName lastName email` | `200 [Course…]`, Mongoose docs with `_id`, `__v`; `enrolledStudents` left as ids | No pagination or filter |
| `getCourseById` (`:17-33`) | `findById`, populating `adminId`, `teachingAssistants`, `enrolledStudents` | `200 Course` or `404 {message:'Course not found'}` | Invalid ObjectId → CastError → 500 |
| `createCourse` (`:35-59`) | Body `{title, description, modules}`; `adminId = req.user.userId`; save; `$push` into `User.managedCourses` | `201 Course` | Unreachable (E1). Mongoose validation error → 500. Not transactional. |
| `updateCourse` (`:61-86`) | Owner check: `course.adminId.equals(userId)` OR `teachingAssistants.includes(userId)`, else 403 `{message:'Not authorized'}`; then `findByIdAndUpdate(id, {$set: req.body}, {new:true})` | `200 Course` | Unreachable (E1). **Mass assignment**: the body can overwrite `adminId`, `enrolledStudents`, `teachingAssistants`, etc. No validators run. `updatedAt` never refreshed. |
| `deleteCourse` (`:88-115`) | Owner-only check; `course.remove()`; `$pull` from every `User.managedCourses` | `200 {message:'Course deleted successfully'}` | Unreachable (E1). `Document.prototype.remove()` was removed in Mongoose 7 (version-dependent, inferred). **Does not pull the course from students' `enrolledCourses`** (dangling refs). |
| `enrollInCourse` (`:117-145`) | 404 if missing; 400 `{message:'Already enrolled in this course'}`; push the user to `enrolledStudents`, save; `$push` the course into `User.enrolledCourses` | `200 {message:'Successfully enrolled in course'}` | Unreachable (E1). Not idempotent across a race. No unenrol counterpart. |

Note on `teachingAssistants.includes(userId)` / `enrolledStudents.includes(userId)` with a string id: Phase 1 flagged this as a string-vs-ObjectId comparison. Mongoose arrays implement `includes` via a casting `indexOf`, so these checks **likely work** (library semantics, inferred). This is downgraded from "bug" to "unverified".

#### `controllers/user.ts`
| Function | Behaviour | Response | Defects |
|---|---|---|---|
| `getProfile` (`:5-22`) | `findById(userId).select('-password')`, populating `enrolledCourses` and `managedCourses` with `title description` | `200 User` (full doc incl. `_id`, `roles`, timestamps, `__v`) | — |
| `updateProfile` (`:24-52`) | Optional `firstName`, `lastName`, `email`; save | `200 { id, email, firstName, lastName, roles }` (`:41-47`) | No validation; duplicate email → 11000 → caught → **500**. The response omits `enrolledCourses`/`managedCourses`, which causes the Phase 2 dashboard-count regression. |
| `changePassword` (`:54-79`) | `comparePassword(currentPassword)` → 400 `{message:'Current password is incorrect'}`; set and save (hashed by hook) | `200 {message:'Password updated successfully'}` | No strength or length validation on `newPassword` |

### 3.6 Models — `models/User.ts`, `models/Course.ts`
Full field tables are in `database-schema.md`. Key points:
* **`User`**:
  * `email` unique, lower-cased, trimmed;
  * `password` hashed in a `pre('save')` hook with bcrypt and 10 salt rounds (`:44-54`);
  * `comparePassword` method (`:57-63`);
  * `roles: [String enum STUDENT|ADMIN]` (`:26-30`; the `default` sits inside the element definition, so it does not default the array — inferred);
  * `enrolledCourses` and `managedCourses` as ObjectId refs to `Course`;
  * `timestamps: true`.
* **`Course`**:
  * `title` and `description` required;
  * `adminId` is a required ObjectId ref to `User`;
  * `enrolledStudents` and `teachingAssistants` are arrays of ObjectId refs to `User`;
  * embedded `modules[] → contents[]`, `announcements[]`, `discussions[] → replies[]` (each subdocument gets an auto `_id` — Mongoose default);
  * `createdAt` / `updatedAt` are manual defaults, not `timestamps`.

### 3.7 Express defects and incomplete work (consolidated)
| ID | Defect | Location |
|---|---|---|
| E1 | JWT payload lacks roles → all `checkRole` routes 403 | `controllers/auth.ts:33-37,80-84`; `middleware/auth.ts:43` |
| E2 | Role vocabulary mismatch (`instructor` not in enum; case mismatch) | `routes/course.ts:19-22` vs `models/User.ts:28` |
| E3 | Two entry points mounting different routers | `index.ts` vs `server.ts` |
| E4 | Stub routers `courses.ts`, `users.ts` marked `TODO` | `routes/courses.ts:6`, `routes/users.ts:6` |
| E5 | No `package.json`/`tsconfig` → not buildable | `backend/` |
| E6 | Self-registration as `ADMIN` | `routes/auth.ts:15`, `controllers/auth.ts:27` |
| E7 | Mass-assignment update | `controllers/course.ts:75-79` |
| E8 | Version-dependent `course.remove()` | `controllers/course.ts:102` |
| E9 | Delete leaves dangling `enrolledCourses` refs | `controllers/course.ts:104-108` |
| E10 | Hard-coded JWT fallback secret | `controllers/auth.ts:35,82`, `middleware/auth.ts:29` |
| E11 | Open CORS in `index.ts` | `index.ts:16` |
| E12 | No endpoints for announcements, discussions, replies, TAs, modules, unenrol, user admin | — |
| E13 | Responses use `_id` while the frontend reads `id` (except auth/profile-update, which map `id` manually) | `controllers/course.ts:10,28`; `controllers/user.ts:17` |
| E14 | Duplicate-email profile update returns 500, not 400 | `controllers/user.ts:39,48-51` |

---

## 4. Cross-backend comparison

| Concern | Spring | Express | Frontend expectation |
|---|---|---|---|
| Port | 8083 | 8080 | 8083 (Redux client), 8080 (raw fetch) |
| Base path | `/api/api` (bug) | `/api` | `/api` |
| Id field in JSON | `id` | `_id` (except mapped auth/profile responses) | `id` |
| Role field | `role: String` | `roles: String[]` | `roles: string[]` |
| Role values | `student`, `instructor`, `admin` (enum, unenforced) | `STUDENT`, `ADMIN` | `student`, `instructor`, `admin` |
| JWT subject / claims | `sub=email` | `{userId}` | opaque (UI never decodes) |
| Error body | plain text | `{message}` or `{errors:[…]}` | `{message}` |
| Register status | 200 | 201 | any 2xx |
| Course API | none | full CRUD + enrol | full CRUD + enrol + announcements, discussions, replies |
| Profile API | none | get/update/password | get (unused), update |
| Role enforcement | none | present but always denies | UI-only check (`isInstructor`) |
| Buildable | yes (Maven wrapper) | no | yes |
| Starts | no (S1, inferred) | n/a | n/a |

**Interpretation (inferred, labelled):** the frontend's *conventions* match Spring (`id`, lowercase roles incl. `instructor`, port 8083 in `utils/axios.ts`), while its *endpoint surface* matches Express. The Spring `Course` model is a field-for-field translation of the Mongoose `Course` schema (same nested types, same `ContentType` enum, same `isAnonymous` flags). The most consistent reading is that Express was the prototype and Spring the intended production backend, and the migration stopped after auth.

---

## 5. Pointers to companion deliverables
| Task | Document |
|---|---|
| Task 3 — Database reconstruction, Mermaid ER diagram | `database-schema.md` |
| Task 4 — Unified API catalogue, Phase 2 comparison | `api-contract.md` |
| Task 5 — Role and permission matrix | `role-permission-matrix.md` |
| Task 6 — Feature gap analysis | `backend-feature-gap.md` |
| Task 7 — Recreation architecture recommendation | `recreation-architecture.md` |

## 6. Corrections to earlier phases
* **Phase 1** described Express `teachingAssistants.includes(userId)` as a definite string-vs-ObjectId bug. It is now classified as **likely working** (Mongoose casting semantics), unverified (§3.5).
* **Phase 1** described Spring's `/api/auth/**` permit rule without analysing the matcher. §2.6 refines this: the matcher is context-path-relative, so it does cover the doubled controller path (inferred).
