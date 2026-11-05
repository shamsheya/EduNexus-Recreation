# EduNexus — Authentication Flow (Phase 3)

All steps are traced from source code. Steps marked *(inferred)* depend on framework semantics rather than an explicit line. No secret values are reproduced.

## 1. Frontend side (common to both backends)

```mermaid
sequenceDiagram
    autonumber
    participant U as User
    participant L as Login.tsx / Register.tsx
    participant S as authSlice (Redux)
    participant AX as utils/axios.ts (base :8083/api)
    participant LS as localStorage
    participant R as Router / PrivateRoute

    U->>L: submit form (Yup-validated)
    L->>S: dispatch(login|register)
    S->>AX: POST /auth/login | /auth/register
    AX-->>S: { token, user }   (expected)
    S->>LS: setItem('token') [authSlice.ts:35,55]
    S->>S: state.user, state.token set [authSlice.ts:106-110,120-124]
    L->>R: navigate('/') then /dashboard [Login.tsx:54, Register.tsx:64, App.tsx:31]
    R->>R: PrivateRoute renders if token else /login [PrivateRoute.tsx:22-24]
    Note over AX: every request adds Authorization: Bearer <token>  (axios.ts:13-24)
    Note over AX: 401 → dispatch(logout) + window.location='/login'  (axios.ts:32-35)
    Note over S,LS: Reload: token restored (authSlice.ts:24), user NOT restored (getCurrentUser never dispatched)
```

| Step | Evidence |
|---|---|
| Login validation: email + password ≥ 8 | `Login.tsx:18-27` |
| Register validation: names, email, password ≥ 6, role ∈ student/instructor/admin | `Register.tsx:26-45` |
| Token persisted | `authSlice.ts:35,55` |
| Logout clears state and storage | `authSlice.ts:87-91`; triggered by `Layout.tsx:54-57` and `axios.ts:33` |
| Raw `fetch` calls send the token manually and have **no 401 handling** | `CourseDetail.tsx:68,138,144`; `CourseList.tsx:20`; `Profile.tsx:41` |

## 2. Spring Boot backend

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant F as JwtAuthenticationFilter
    participant SC as SecurityFilterChain
    participant AC as AuthController
    participant AM as AuthenticationManager / DaoAuthProvider
    participant UDS as CustomUserDetailsService
    participant DB as Mongo users
    participant J as service.JwtService

    C->>F: POST /api/api/auth/login {email,password}
    F->>F: no Bearer header → pass through   (JwtAuthenticationFilter.java:36-39)
    F->>SC: permitAll /api/auth/** (context-relative, inferred)  (SecurityConfig.java:39)
    SC->>AC: login()   (AuthController.java:55)
    AC->>AM: authenticate(email,password)   (:58-60)
    AM->>UDS: loadUserByUsername(email)   (CustomUserDetailsService.java:17-20)
    UDS->>DB: findByEmail
    AM->>AM: BCrypt match   (SecurityConfig.java:64-80)
    AC->>DB: findByEmail again   (:62-63)
    AC->>J: generateToken(user) → HS256 {sub=email,iat,exp}   (service/JwtService.java:56-69)
    AC-->>C: 200 {token, user}  | 400 "Authentication failed: …" (plain text)

    C->>F: GET /api/api/<anything> + Bearer
    F->>J: extractUsername (throws on bad/expired token — uncaught, inferred)
    F->>UDS: loadUserByUsername
    F->>J: validateToken (sub==email && !expired)
    F->>SC: SecurityContext = [ROLE_<ROLE>]   (:47-55)
    SC->>SC: anyRequest().authenticated()  — no role rules   (SecurityConfig.java:40)
```

**Weak points (confirmed unless marked):**
* The duplicate `jwtService` bean blocks startup *(inferred, high)*.
* Endpoints are at the doubled `/api/api` path.
* No `@Valid` on request bodies.
* A null role causes an NPE in `getAuthorities` (`User.java:53`).
* JWT exceptions in the filter are unhandled *(inferred)*.
* Two JWT key derivations exist (`security/JwtService.java:73-76` vs `service/JwtService.java:47-50`).
* The token carries no id or roles.
* No refresh or revocation.

## 3. Express backend

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant V as express-validator
    participant AC as controllers/auth.ts
    participant DB as Mongo users (Mongoose)
    participant MW as middleware/auth.ts
    participant CR as checkRole
    participant H as handler

    C->>V: POST /api/auth/register {email,password,firstName,lastName,role?}
    V-->>C: 400 {errors[]} if role ∉ {STUDENT,ADMIN} etc.   (routes/auth.ts:8-16)
    V->>AC: register   (controllers/auth.ts:6)
    AC->>DB: findOne(email); new User({..., roles:[role||'STUDENT']}); save (pre-save bcrypt)
    AC-->>C: 201 {token = sign({userId}, 24h), user{id,email,firstName,lastName,roles}}

    C->>MW: request + Bearer
    MW->>MW: jwt.verify → req.user = {userId, iat, exp}   (:23-31)
    MW-->>C: 401 {message} on missing/invalid
    MW->>CR: checkRole(['instructor','admin'])
    CR->>CR: req.user.roles is undefined → []   (:43)
    CR-->>C: 403 {message:'Access denied'}   — ALWAYS
    Note over CR,H: handler (with correct owner/TA checks) never reached
```

**Weak points:**
* Roles are missing from the token.
* Role casing and the missing `instructor` role.
* Self-ADMIN registration.
* A hard-coded fallback secret when `JWT_SECRET` is unset (`controllers/auth.ts:35,82`, `middleware/auth.ts:29`).
* `auth` does not confirm that the user still exists.
* No refresh or revocation.

## 4. Session lifecycle summary

| Event | Frontend | Spring | Express |
|---|---|---|---|
| Token lifetime | not checked | `jwt.expiration` (24 h) | `24h` |
| Token contents used by the server | — | `sub` = email → DB lookup per request | `userId` |
| Expiry handling | axios 401 → logout + redirect; fetch → none | filter throws *(inferred)* | 401 `Token is not valid` |
| Logout | client-side only | no endpoint | no endpoint |
| Session restore | **not implemented** | — | `GET /api/users/profile` available but unused |
