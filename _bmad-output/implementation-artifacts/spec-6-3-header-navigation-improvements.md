---
title: "Header & Navigation Improvements"
type: "feature"
created: "2026-08-14"
status: "ready-for-dev"
review_loop_iteration: 0
baseline_commit: ""
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Three related usability gaps exist in the current `App.vue` header and burger menu:

1. Clicking the "Medour" app name in the header bar does nothing — users expect it to navigate to the home page.
2. The sign-out button sits in the header's `#append` slot with no visual separation from the username/role — the element feels crowded and sign-out is too prominent.
3. The burger menu has no sign-out option; the action lives only in the header where there is limited space.

**Approach:**

- Wrap the `VAppBarTitle` text in a `<span>` or `<a>` that calls `router.push('/')` on click (only when authenticated; when unauthenticated the logo is non-interactive).
- Remove the `VBtn icon="mdi-logout"` from the header `#append` slot entirely.
- Add a new Sign Out list item at the bottom of the `VNavigationDrawer` menu with `mdi-logout` icon, red-tinted colour (Vuetify `color="error"`), and separated from the items above by a `VDivider`.
- Increase the gap between the role chip and the username text from `ga-2` to `ga-4` in the header's append section.

## Boundaries & Constraints

**Always:**

- The "Medour" title click must call `router.push('/')` — do not use an `<a href="/">` to avoid full page reload.
- The sign-out list item must be the last item in the burger menu, visually separated by a `VDivider`.
- Clicking sign-out from the burger menu calls the same `logout()` function (clears auth, pushes to `/login`) and also closes the drawer (`drawer.value = false`).
- The header `#append` slot no longer contains a logout button after this change — it contains only the role chip and username text.
- The app name title is only clickable when the user is authenticated; for unauthenticated users (login page) it is plain text.

**Ask First:**

- (none)

**Never:**

- Do not remove or restructure any existing menu items.
- Do not apply the red colour to the icon in the admin section — only the sign-out item gets the error colour.

## I/O & Edge-Case Matrix

| Scenario                        | Input / State                   | Expected Output / Behavior                                          | Error Handling |
| ------------------------------- | ------------------------------- | ------------------------------------------------------------------- | -------------- |
| Authenticated user clicks title | Any authenticated page          | Navigates to `/`                                                    | —              |
| Unauthenticated user sees title | Login page                      | Title is plain text, not clickable                                  | —              |
| User clicks Sign Out in burger  | Burger menu open, authenticated | `logout()` called; drawer closes; user redirected to `/login`       | —              |
| Header append slot              | Any authenticated page          | Shows role chip + username text with `ga-4` gap; no sign-out button | —              |

</frozen-after-approval>

## Code Map

- `client/src/App.vue` — **EXTEND**:
  - `VAppBarTitle`: wrap inner `<span class="text-white ...">Medour</span>` with a click handler; when `authStore.isAuthenticated` add `style="cursor: pointer"` and `@click="router.push('/')"`, otherwise render as plain (no handler)
  - `#append` template: change `ga-2` to `ga-4` on the wrapping `div`; remove the `<VBtn icon="mdi-logout" ...>` entirely
  - `VNavigationDrawer`: add `<VDivider class="my-2" />` followed by `<VListItem prepend-icon="mdi-logout" title="Sign Out" rounded="lg" color="error" class="font-weight-medium" @click="logoutFromMenu" />` at the very bottom of the `VList`
  - add `logoutFromMenu()` function: calls `logout()` then sets `drawer.value = false` (or reuse `logout()` since `watch(route, ...)` already closes the drawer on navigation, but explicit close is cleaner here)

## Tasks & Acceptance

**Execution:**

- [ ] `client/src/App.vue` — in `VAppBarTitle`, replace `<span class="text-white font-weight-bold text-h6">Medour</span>` with `<span class="text-white font-weight-bold text-h6" :style="authStore.isAuthenticated ? 'cursor:pointer' : ''" @click="authStore.isAuthenticated && router.push('/')">Medour</span>`
- [ ] `client/src/App.vue` — in `#append`, change the `div` class from `ga-2` to `ga-4`; remove the `<VBtn icon="mdi-logout" ...>` element
- [ ] `client/src/App.vue` — in `VList` inside `VNavigationDrawer`, after the last existing `VListItem` and before the closing `</VList>`, add: `<VDivider class="my-2" />` then `<VListItem prepend-icon="mdi-logout" title="Sign Out" rounded="lg" color="error" class="font-weight-medium" @click="logoutFromMenu" />`
- [ ] `client/src/App.vue` — in `<script setup>`, add `function logoutFromMenu() { logout(); drawer.value = false }`

## Acceptance Criteria

- Given an authenticated user clicks the "Medour" text in the header, they are navigated to the home page (`/`).
- Given an unauthenticated user on the login page, the "Medour" title is not clickable (no cursor change, no navigation on click).
- Given the header is rendered, the role chip and username text have more horizontal spacing between them than before.
- Given the header is rendered, there is no sign-out button in the header `#append` area.
- Given the burger menu is opened, "Sign Out" appears as the last item in red, separated by a divider from items above it.
- Given the user clicks "Sign Out" in the burger menu, they are logged out and redirected to `/login`, and the drawer closes.

## Verification

**Commands:**

- `cd client && npm run build` — expected: zero TypeScript errors
- `cd client && npm run test` — expected: all existing tests pass
