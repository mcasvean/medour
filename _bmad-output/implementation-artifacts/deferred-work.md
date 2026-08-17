# Deferred Work

Items collected during implementation reviews that are real but out of scope for the originating story.

- source_spec: `spec-4-4-admin-appointment-management.md`
  summary: Admin appointments list has no pagination — fetches all rows unbounded; will degrade for large datasets.
  evidence: Blind hunter finding; pre-existing pattern across the codebase; no pagination spec'd for this story.

- source_spec: `spec-4-4-admin-appointment-management.md`
  summary: No 403 test for non-admin JWT on GET/DELETE /api/v1/admin/appointments; protected by SecurityConfig blanket rule but not asserted.

- source_spec: `spec-filter-past-slots.md`
  summary: SlotService 30-minute slot duration is repeated as a magic literal; extract to a named constant.
  evidence: Blind hunter finding; pre-existing in the slot generator loop — not introduced by this change.
  evidence: Verification gap finding; same gap exists for all prior /admin/\*\* endpoints.
  evidence: Edge case hunter finding; router has only the placeholder '/' route in this scaffold.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: authStore stays stale (isAuthenticated=true, token populated) after a forced 401 logout because the interceptor only clears localStorage, not the Pinia store.
  evidence: Verification gap finding; the SPA store instance survives navigation so components and guards will see stale auth state until reload or until Story 1.3 redesigns the auth flow.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: HealthControllerTest uses @AutoConfigureMockMvc(addFilters=false) so it never validates the assembled SecurityFilterChain.
  evidence: Verification gap finding; a change from anyRequest().permitAll() to anyRequest().authenticated() would not be caught by this test.

- source_spec: `spec-6-1-toast-notification-system.md`
  summary: ToastNotification.vue has no component-level render tests — DOM output, icon selection per type, close-button wiring, and CSS transition classes are untested.
  evidence: Verification gap finding; store tests verify state mutations but not rendering; @vue/test-utils component mount tests would be needed.

- source_spec: `spec-6-1-toast-notification-system.md`
  summary: Toast container and cards are missing accessibility attributes — role="alert", aria-live="polite", aria-atomic="true" — so screen readers do not announce new notifications.
  evidence: Blind hunter finding; accessibility gap not covered by spec; should be addressed in a dedicated accessibility pass.

- source_spec: `spec-6-2-required-field-indicators.md`
  summary: DoctorRegisterForm age and gender fields carry a `required` HTML attribute but are spec'd as optional — asterisks correctly omitted, but the pre-existing `required` attribute creates a false browser-native constraint.
  evidence: Edge case hunter finding; pre-existing before this story; removal should be a targeted cleanup.

- source_spec: `spec-6-2-required-field-indicators.md`
  summary: Required form fields lack aria-required="true" — screen readers cannot identify mandatory fields from the HTML alone.
  evidence: Blind hunter finding; accessibility gap not in spec; should be addressed in a dedicated accessibility pass.

- source_spec: `spec-6-3-header-navigation-improvements.md`
  summary: Burger menu Sign Out item and clickable app title lack aria-label / keyboard-only navigation hints.
  evidence: Blind hunter finding; accessibility gap not in spec; should be addressed in a dedicated accessibility pass.

- source_spec: `spec-6-4-light-dark-mode.md`
  summary: Theme toggle button lacks an aria-label; screen readers announce only the icon name rather than the action.
  evidence: Blind hunter finding; accessibility gap not in spec.

- source_spec: `spec-6-4-light-dark-mode.md`
  summary: App ignores OS-level prefers-color-scheme — dark-mode users must manually toggle instead of getting the right theme on first load.
  evidence: Edge case hunter finding; feature request not in spec; localStorage preference takes priority once set.

- source_spec: `spec-5-1-rating-submission-on-completed-appointments.md`
  summary: PatientAppointmentService.getHistory executes N+1 queries — one ratingRepository.findByAppointmentId call per appointment row instead of a single LEFT JOIN.
  evidence: Blind hunter finding; causes a linear increase in DB round-trips for patients with many appointments.

- source_spec: `spec-5-1-rating-submission-on-completed-appointments.md`
  summary: Rating entity stores patient and doctor as separate ManyToOne columns, redundantly with the Appointment relation; creates potential for data divergence.
  evidence: Blind hunter finding; both patient and doctor are already reachable via appointment.patient / appointment.doctor.

- source_spec: `spec-5-1-rating-submission-on-completed-appointments.md`
  summary: No Vue component test for PatientAppointmentsView rating widget; render conditions, prefill, and disabled-after-submit have no automated coverage.
  evidence: Verification gap finding; component-level test layer is absent for this feature.

- source_spec: `spec-5-2-rating-editing.md`
  summary: PATCH /api/v1/ratings/{id} role guard (DOCTOR/ADMIN → 403) has no controller-layer test; @WebMvcTest role enforcement is unreliable in this project.
  evidence: Verification gap finding; same limitation documented for POST in story 5-1; security config and @PreAuthorize are both in place for production.

- source_spec: `spec-5-3-colour-coded-doctor-rating-display.md`
  summary: Rating badge not shown on a doctor profile page — no doctor profile view exists yet in the codebase.
  evidence: Blind hunter finding; epics.md FR53 requires badge on "doctor profile and search results"; BookingSearchView covers search results only.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: spring.jpa.hibernate.ddl-auto is set to 'update', which mutates schema on every boot; Flyway or Liquibase should own migrations.
  evidence: Blind hunter finding; ddl-auto:update will silently drop renamed columns and cause schema drift across environments.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: No PasswordEncoder @Bean in SecurityConfig; will throw NoSuchBeanDefinitionException once UserDetailsService is introduced.
  evidence: Blind hunter finding; JJWT and Spring Security auth beans are wired in Story 1.3.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: CORS allowed origin is hardcoded to http://localhost:5173 — all cross-origin requests rejected in non-local deployments.
  evidence: Edge case hunter finding; should be externalized to an environment property before any staging or production deployment.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: vitest.config.ts is missing the '@/' path alias; any future unit test that uses @/ imports will fail to resolve.
  evidence: Blind hunter finding; no current test uses @/ so it is not broken today.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: Concurrent 401 dedup unit test is unreliable due to mock-adapter's async response scheduling; needs an integration test.
  evidence: The localStorage-based gate is correct in production (synchronous clear is visible immediately) but two mock responses arrive in separate event-loop ticks, so each interceptor sees the token as non-null. Requires an e2e or integration test to verify.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: isAuthenticated is plain state, not a derived getter — can diverge from token.
  evidence: Blind hunter finding; should become a Pinia getter in Story 1.3 when auth state ownership is fully established.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: authStore does not persist user to localStorage — page refresh sets user back to null.
  evidence: Verification gap; any component reading authStore.user after refresh gets null even though isAuthenticated is true. Story 1.3 will add a /me endpoint or token-parsing to rehydrate user.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: No clearAuth/logout action in authStore — logout will be ad-hoc until Story 1.3.
  evidence: Blind hunter finding; Story 1.3 will introduce proper session termination.

- source_spec: none
  summary: Admin user detail panel (expanded row in AdminUsersView) is not readable in dark mode — text and/or background colours use hardcoded or surface-token values that lack contrast when the dark theme is active.
  evidence: Deferred during multi-goal split from user intent 2026-08-17; dark-mode fix is independently shippable.

- source_spec: none
  summary: Navigation sidebar (VNavigationDrawer) should support a pinned/persistent mode so the user can keep it permanently expanded rather than always collapsed.
  evidence: Deferred during multi-goal split from user intent 2026-08-17; sidebar pinning is independently shippable as a UX preference feature.

- source_spec: none
  summary: Header avatar slot should always display an mdi-account placeholder icon for the authenticated user even when no profile picture is set; currently Story 7.1 hides the avatar entirely when profilePicture is null.
  evidence: Deferred during multi-goal split from user intent 2026-08-17; default avatar is independently shippable as a one-line template change.

- source_spec: `spec-admin-reset-user-password-ui.md`
  summary: No component test for AdminUsersView / AdminUserForm interaction covering the "password field empty → no reset call" and "form shows reset section on open" matrix rows.
  evidence: Matrix test audit finding; project has no @vue/test-utils component test infrastructure (consistent with prior deferred entries).

- source_spec: `spec-admin-reset-user-password-ui.md`
  summary: handleSave swallows the actual server error and shows a hardcoded 'Failed to save user.' message — server 400 validation messages are invisible to the admin.
  evidence: Blind hunter finding; pre-existing pattern in the original handleSave code before this story; not introduced by this change.

- source_spec: `spec-admin-dark-mode-panel-fix.md`
  summary: VExpansionPanel block is repeated verbatim in all three user-role card sections (Admins, Doctors, Patients) — should be extracted into a shared child component to eliminate the duplication.
  evidence: Blind hunter finding; pre-existing template duplication; not introduced by this change.

- source_spec: `spec-admin-dark-mode-panel-fix.md`
  summary: VExpansionPanel child has redundant elevation="0" when the parent VExpansionPanels already uses flat, which zeroes elevation.
  evidence: Blind hunter finding; pre-existing; cosmetic cleanup.

- source_spec: `spec-admin-dark-mode-panel-fix.md`
  summary: VExpansionPanelTitle "View details" is a hardcoded English string with no i18n wrapper.
  evidence: Blind hunter finding; pre-existing; relevant if localisation is added later.

- source_spec: `spec-sidebar-pinning.md`
  summary: logout() does not clear medour_sidebar_pinned from localStorage — on a shared device the next user inherits the previous user's pinned preference.
  evidence: Blind hunter finding; design decision about UI preference cleanup on logout.

- source_spec: `spec-sidebar-pinning.md`
  summary: No responsive breakpoint guard — a pinned non-temporary drawer on narrow viewports will push main content on mobile, which may be undesirable.
  evidence: Blind hunter finding; mobile layout behaviour should be reviewed and guarded (e.g. only allow pinning above sm breakpoint).

- source_spec: `spec-sidebar-pinning.md`
  summary: Pin toggle VBtn has no aria-pressed attribute — assistive technology cannot communicate whether the sidebar is currently pinned.
  evidence: Blind hunter finding; consistent with existing accessibility deferral pattern in this project.

- source_spec: `spec-configurations-user-preferences.md`
  summary: Drawer shows as closed on first authenticated render then snaps open if pinnedSidebar is true — async BE fetch introduces a visible layout flash that the old localStorage approach avoided.
  evidence: Architectural trade-off; would require localStorage cache of the preference as a fast-read layer to fix.

- source_spec: `spec-configurations-user-preferences.md`
  summary: UserPreference.userId stored as a plain Long column with no @ManyToOne JPA relationship — no FK referential integrity or cascade behaviour.
  evidence: Design decision; extensibility spec allows lazy FK; deferred for cleanliness pass.

- source_spec: `spec-configurations-user-preferences.md`
  summary: getOrCreate and update lack a concurrent-insert guard — two simultaneous first-login requests can both find no row and both INSERT, causing a DataIntegrityViolationException on the unique constraint.
  evidence: Edge case hunter finding; acceptable risk at current scale; can be addressed with upsert query or serializable isolation later.

- source_spec: `spec-configurations-user-preferences.md`
  summary: App.vue isAuthenticated watcher → fetchPreferences → drawer-open wiring has no automated test; removing the drawer-open line silently regresses the feature.
  evidence: Verification gap finding; component tests not available in this project; consistent with existing deferred component-test gap pattern.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: Token expiry is not checked at store init — an expired token in localStorage makes isAuthenticated true until the first 401.
  evidence: Blind hunter finding; Story 1.3 JWT validation filter and auth guard will handle this.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: password has no minimum-length constraint on either client or server.
  evidence: Blind hunter / edge case hunter; flagged as Ask First in spec — deliberately deferred until password policy is defined.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: Admin seeded with mustChangePassword=false using the well-known default password — privileged account has no forced rotation in dev.
  evidence: Blind hunter; production deployments must set ADMIN_PASSWORD env var; noted in application.yml comments.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: No submit-button loading/disabled state — double-click can submit duplicate registration requests.
  evidence: Blind hunter; UX improvement not in spec scope.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: setAuth-before-push ordering is untested — a future refactor could invert them, breaking navigation guards.
  evidence: Verification gap; ordering is correct today but Story 1.4 navigation guards will make this observable and testable.

- source_spec: `spec-1-3-user-login-jwt-session.md`
  summary: JwtAuthFilter is added to the security chain but every existing test bypasses it with addFilters=false — the filter's token-to-SecurityContext path has no integration test.
  evidence: Verification gap; a regression that breaks role claim extraction or JwtException handling would not be caught by the controller slice tests.

- source_spec: `spec-1-3-user-login-jwt-session.md`
  summary: A soft-deleted user who already holds a valid JWT can access protected routes until their token expires (up to 1 hour) — JwtAuthFilter does not check deleted_at on every request.
  evidence: Edge case hunter; a DB check on every request is expensive; the 1-hour window is the accepted trade-off, but it should be documented as a known gap.

- source_spec: `spec-1-3-user-login-jwt-session.md`
  summary: clearAuth() resets only authStore — sibling stores (appointmentStore, doctorStore, userStore) retain stale user-scoped data after logout.
  evidence: Blind hunter; full logout cleanup is within scope of Story 1.5 or a dedicated logout story.

- source_spec: `spec-1-3-user-login-jwt-session.md`
  summary: No accessDeniedHandler configured in SecurityConfig — a 403 (authenticated but unauthorized role) returns Spring's default HTML error page, inconsistent with the custom JSON 401 format.
  evidence: Blind hunter; role-based access control becomes active in Story 1.4, at which point an accessDeniedHandler returning JSON should be added.

- source_spec: `spec-1-4-role-display-route-guards.md`
  summary: App.vue has no component-level tests — logout redirect and isAdmin visibility are unverified by any automated test.
  evidence: Verification gap; @vue/test-utils is not installed; adding it is deferred to when component testing becomes a broader pattern.

- source_spec: `spec-1-4-role-display-route-guards.md`
  summary: Server ADMIN route restriction has no integration test — the hasRole("ADMIN") rule is never exercised with filters enabled.
  evidence: Verification gap; all controller tests use addFilters=false; a @SpringBootTest slice test is deferred until actual admin endpoints are added in Epic 4.
