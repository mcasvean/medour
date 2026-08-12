---
title: "Project Scaffold & Development Environment"
type: "chore"
created: "2026-08-12"
status: "done"
review_loop_iteration: 0
baseline_commit: "add0f15867e0200ef69e5464c6c43017879e8871"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The repo has no runnable application code — no client, no server, no shared tooling. Developers cannot build features until the local dev environment is bootstrapped end-to-end.

**Approach:** Scaffold a Vite + Vue 3 + TypeScript SPA in `client/` and a Spring Boot application in `server/`. Wire them with a Vite proxy, create the Axios instance with auth interceptors, Pinia store skeletons, Vue Router base config, Spring Security skeleton config, and a `/api/v1/health` endpoint returning 200 OK.

## Boundaries & Constraints

**Always:**

- `client/` and `server/` are the only top-level source roots; no cross-tree imports at build time
- All HTTP calls route through the Axios instance in `client/src/api/index.ts` only — no `fetch` or `XMLHttpRequest`
- All shared cross-component state lives in Pinia stores under `client/src/stores/`
- Spring Boot package root: `com.medour`; layered strictly: controller → service → repository; controllers call services only
- All REST endpoints prefixed `/api/v1/`
- Spring Security config: CSRF disabled, stateless sessions, all requests permitted for now (auth enforced in Story 1.3), CORS allows `http://localhost:5173`
- PostgreSQL datasource targets `localhost:5432/medour`; credentials via env vars with dev fallback `${DB_USERNAME:medour}` / `${DB_PASSWORD:medour}`; `ddl-auto: update`
- Maven wrapper (`./mvnw`) must be present in `server/`

**Ask First:**

- If PostgreSQL is unavailable locally and an alternative datasource (e.g. H2) is preferred for the initial run

**Never:**

- No auth enforcement in this story — no JWT validation, no role guards (Stories 1.3, 1.4)
- No feature routes beyond a single placeholder home route
- No hardcoded credentials in source files

## I/O & Edge-Case Matrix

| Scenario         | Input / State                    | Expected Output / Behavior                           | Error Handling                                                                     |
| ---------------- | -------------------------------- | ---------------------------------------------------- | ---------------------------------------------------------------------------------- |
| Health check     | `GET /api/v1/health` (no auth)   | 200 OK                                               | N/A                                                                                |
| Vue SPA loads    | Browser → `localhost:5173`       | App renders `<RouterView>` without console errors    | N/A                                                                                |
| 401 interception | Any Axios response with HTTP 401 | localStorage cleared; `router.push('/login')` called | Skip redirect when the originating URL is `/auth/login` to prevent a redirect loop |

</frozen-after-approval>

## Code Map

- `client/` -- does not exist; create from scratch
- `server/` -- does not exist; create from scratch

## Tasks & Acceptance

**Execution:**

- [x] `client/package.json` -- create; deps: `vue@^3`, `pinia`, `vue-router@4`, `axios`; devDeps: `vite`, `@vitejs/plugin-vue`, `typescript`, `vue-tsc`
- [x] `client/vite.config.ts` -- create; register `@vitejs/plugin-vue`; proxy `/api` → `http://localhost:8080` (changeOrigin: true, rewrite strips nothing — path passthrough)
- [x] `client/tsconfig.json` -- create standard Vue 3 + TS config; `target: ESNext`, `moduleResolution: bundler`, `@/` path alias resolving to `src/`
- [x] `client/index.html` -- create Vite entry HTML; `<div id="app">`; script module pointing to `src/main.ts`
- [x] `client/src/main.ts` -- create; `createApp(App).use(createPinia()).use(router).mount('#app')`
- [x] `client/src/App.vue` -- create; `<template><RouterView /></template>` only, no script or styles
- [x] `client/src/api/index.ts` -- create Axios instance with `baseURL: '/api/v1'`; request interceptor injects `Authorization: Bearer ${token}` when `localStorage.getItem('token')` is set; response interceptor on 401 clears localStorage and calls `router.push('/login')` unless the request config URL ends with `/auth/login`
- [x] `client/src/router/index.ts` -- create Vue Router 4 with `createWebHistory()`; one placeholder route `{ path: '/', component: () => import('../views/HomeView.vue') }`; export named `router`
- [x] `client/src/views/HomeView.vue` -- create minimal placeholder (`<template><div>Home</div></template>`)
- [x] `client/src/stores/authStore.ts` -- create Pinia store `defineStore('auth', { state: () => ({ token: localStorage.getItem('token') as string | null, user: null as null | Record<string, unknown>, isAuthenticated: !!localStorage.getItem('token') }) })`
- [x] `client/src/stores/appointmentStore.ts` -- create empty Pinia store skeleton (`defineStore('appointment', { state: () => ({}) })`)
- [x] `client/src/stores/userStore.ts` -- create empty Pinia store skeleton (`defineStore('user', { state: () => ({}) })`)
- [x] `client/src/stores/doctorStore.ts` -- create empty Pinia store skeleton (`defineStore('doctor', { state: () => ({}) })`)
- [x] `server/pom.xml` -- create Maven POM; parent: `spring-boot-starter-parent` 3.x (latest stable); deps: `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `postgresql`, `jjwt-api` + `jjwt-impl` + `jjwt-jackson` (io.jsonwebtoken, 0.12.x), `lombok`; plugin: `spring-boot-maven-plugin`
- [x] `server/src/main/java/com/medour/MedourApplication.java` -- create `@SpringBootApplication` entry point class
- [x] `server/src/main/java/com/medour/controller/HealthController.java` -- create `@RestController`; `@GetMapping("/api/v1/health")` returns `ResponseEntity.ok().build()`
- [x] `server/src/main/java/com/medour/config/SecurityConfig.java` -- create `@Configuration @EnableWebSecurity`; `SecurityFilterChain` bean: disable CSRF, `SessionCreationPolicy.STATELESS`, `authorizeHttpRequests` permits all, `CorsConfigurationSource` bean allows origin `http://localhost:5173`, all methods and headers
- [x] `server/src/main/resources/application.yml` -- create; `server.port: 8080`; datasource url `jdbc:postgresql://localhost:5432/medour`, username `${DB_USERNAME:medour}`, password `${DB_PASSWORD:medour}`; `spring.jpa.hibernate.ddl-auto: update`; `spring.jpa.show-sql: false`; `spring.jpa.properties.hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect`
- [x] `server/` -- after `pom.xml` is in place, generate Maven wrapper by running `mvn wrapper:wrapper` inside `server/`; confirm `mvnw` (Linux/Mac) and `mvnw.cmd` (Windows) are created
- [x] `client/package.json` -- add `vitest`, `jsdom`, `axios-mock-adapter` to devDeps; add `"test": "vitest run"` script (added during matrix test audit)
- [x] `client/vitest.config.ts` -- create; `environment: 'jsdom'`, `globals: true`
- [x] `client/src/api/__tests__/index.test.ts` -- Vitest tests: 401 on guarded endpoint clears localStorage and pushes `/login`; 401 on `/auth/login` does not redirect
- [x] `server/src/test/java/com/medour/controller/HealthControllerTest.java` -- `@WebMvcTest(addFilters=false)`; `GET /api/v1/health` returns 200
- [x] `client/package.json` -- add `vitest`, `jsdom`, `axios-mock-adapter` to devDeps; add `"test": "vitest run"` script
- [x] `client/vitest.config.ts` -- create; `environment: 'jsdom'`, `globals: true`
- [x] `client/src/api/__tests__/index.test.ts` -- create Vitest tests covering matrix rows 1 and 3: 401 clears localStorage and redirects; 401 on `/auth/login` does not redirect
- [x] `server/src/test/java/com/medour/controller/HealthControllerTest.java` -- create `@WebMvcTest` with `@AutoConfigureMockMvc(addFilters=false)`; verify `GET /api/v1/health` returns 200

**Acceptance Criteria:**

- Given the repo is cloned and PostgreSQL is running with database `medour`, when `cd client && npm install && npm run dev` is run, then the Vue app starts on `localhost:5173` without errors.
- Given the repo is cloned, when `cd server && ./mvnw spring-boot:run` is run, then the Spring Boot server starts on port 8080 and connects to PostgreSQL without errors.
- Given the server is running, when `GET /api/v1/health` is called, then 200 OK is returned.
- Given the client is running, when any Axios response returns 401, then localStorage is cleared and the router navigates to `/login` — except when the originating request was `/auth/login`.
- Given `npm run build` in `client/`, then it completes with zero TypeScript compilation errors.

## Verification

**Commands:**

- `cd client && npm install` -- expected: exits 0, no peer-dep errors
- `cd client && npm run build` -- expected: `dist/` produced, zero TS errors
- `cd client && npm test` -- expected: 4 passed (request interceptor 2 + 401 interceptor 2)
- `cd server && ./mvnw compile` -- expected: `BUILD SUCCESS`, zero Java errors
- `cd server && ./mvnw test` -- expected: `Tests run: 1, Failures: 0`

## Suggested Review Order

**Auth interceptor (highest-leverage entry point)**

- Axios instance base URL, token injection, and 401 dedup-redirect guard
  [`index.ts:1`](../../client/src/api/index.ts#L1)

**Server security baseline**

- CSRF off, stateless session, permitAll, CORS for localhost:5173
  [`SecurityConfig.java:18`](../../server/src/main/java/com/medour/config/SecurityConfig.java#L18)

- Health endpoint — only concrete API route in this story
  [`HealthController.java:8`](../../server/src/main/java/com/medour/controller/HealthController.java#L8)

**Client bootstrap**

- App entry point wiring Pinia + Router onto the Vue app
  [`main.ts:1`](../../client/src/main.ts#L1)

- Router: single placeholder route; no guards yet (Story 1.4)
  [`router/index.ts:1`](../../client/src/router/index.ts#L1)

- authStore state shape — token/user/isAuthenticated from localStorage at init
  [`authStore.ts:1`](../../client/src/stores/authStore.ts#L1)

**Configuration**

- Datasource, JPA dialect, open-in-view:false, error stack trace suppressed
  [`application.yml:1`](../../server/src/main/resources/application.yml#L1)

- Maven POM — Spring Boot 3.3.5, JJWT 0.12.5, java 17
  [`pom.xml:1`](../../server/pom.xml#L1)

- Vite config: @vitejs/plugin-vue, /api proxy to :8080
  [`vite.config.ts:1`](../../client/vite.config.ts#L1)

**Tests**

- Request interceptor + 401 interceptor unit tests (4 cases)
  [`index.test.ts:1`](../../client/src/api/__tests__/index.test.ts#L1)

- Health endpoint WebMvcTest (filters disabled — see deferred-work)
  [`HealthControllerTest.java:1`](../../server/src/test/java/com/medour/controller/HealthControllerTest.java#L1)
