---
title: "Light/Dark Mode Toggle"
type: "feature"
created: "2026-08-14"
status: "done"
review_loop_iteration: 0
baseline_commit: "3242379690c7fbd1f506e9a16151130f768a6810"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The application has a single fixed colour scheme. Users who prefer dark environments have no way to switch to a dark theme, and the preference is not remembered between sessions.

**Approach:** Use Vuetify's built-in theme system (`useTheme`) to switch between a `'light'` and `'dark'` theme at runtime. A sun/moon icon toggle button is added to the right side of the header `#append` slot (before the role chip). The selected theme name is persisted in `localStorage` under `'theme'` so the preference is restored on next load. The `VMain` background adapts automatically via Vuetify theming.

## Boundaries & Constraints

**Always:**

- Theme names used: `'light'` (default) and `'dark'` — these are Vuetify built-in themes; no custom theme definition is required unless a colour tweak is needed.
- Icon: `mdi-weather-sunny` for light mode (indicating "switch to dark"), `mdi-weather-night` for dark mode (indicating "switch to light").
- `localStorage` key: `'medour_theme'`; read on `main.ts` Vuetify setup or in `App.vue` `onMounted`.
- The button is always visible in the header — for both authenticated and unauthenticated users — so users can set dark mode before logging in.
- The `VMain` background colour currently hard-coded as `style="background: #F5F7FA"` must be removed (or made theme-aware) so it does not override Vuetify's dark surface colour.

**Ask First:**

- (none)

**Never:**

- Do not create custom theme palettes in this story — rely on Vuetify defaults for light/dark.
- Do not store theme preference in the user profile or in the database — it is a local device preference only.

## I/O & Edge-Case Matrix

| Scenario                        | Input / State                           | Expected Output / Behavior                                                   | Error Handling |
| ------------------------------- | --------------------------------------- | ---------------------------------------------------------------------------- | -------------- |
| Default load, no stored pref    | `localStorage['medour_theme']` absent   | Light theme applied                                                          | —              |
| Stored dark preference          | `localStorage['medour_theme'] = 'dark'` | Dark theme applied on load before first paint                                | —              |
| User clicks toggle (light→dark) | Theme is `'light'`                      | Theme switches to `'dark'`; icon changes to `mdi-weather-night`; pref saved  | —              |
| User clicks toggle (dark→light) | Theme is `'dark'`                       | Theme switches to `'light'`; icon changes to `mdi-weather-sunny`; pref saved | —              |
| VMain background                | Dark mode active                        | Main content area uses Vuetify dark surface colour (no hard-coded `#F5F7FA`) | —              |

</frozen-after-approval>

## Code Map

- `client/src/App.vue` — **EXTEND**:
  - In `<script setup>`: import `useTheme` from `'vuetify'`; declare `const theme = useTheme()`; add `const isDark = computed(() => theme.global.name.value === 'dark')`; add `toggleTheme()` that flips the theme and writes to `localStorage`; in `onMounted`, read `localStorage.getItem('medour_theme')` and apply if present
  - In `#append` template: add `<VBtn :icon="isDark ? 'mdi-weather-night' : 'mdi-weather-sunny'" color="white" variant="text" density="comfortable" @click="toggleTheme" />` as the first child of `#append` (before role chip and username)
  - Remove `style="background: #F5F7FA"` from `<VMain>` so Vuetify handles the surface colour

## Tasks & Acceptance

**Execution:**

- [x] `client/src/App.vue` — in `<script setup>`: `import { useTheme } from 'vuetify'`; `const theme = useTheme()`; `const isDark = computed(() => theme.global.name.value === 'dark')`; add `onMounted` (import from vue): read `const stored = localStorage.getItem('medour_theme')`; if stored is `'dark'` or `'light'`, call `theme.global.name.value = stored`; add `function toggleTheme() { const next = isDark.value ? 'light' : 'dark'; theme.global.name.value = next; localStorage.setItem('medour_theme', next) }`
- [x] `client/src/App.vue` — in `#append` template, add `<VBtn :icon="isDark ? 'mdi-weather-night' : 'mdi-weather-sunny'" color="white" variant="text" density="comfortable" @click="toggleTheme" />` as first element inside the `#append` div (before the role chip div)
- [x] `client/src/App.vue` — remove `style="background: #F5F7FA; min-height: 100vh;"` from `<VMain>` (leave `min-height: 100vh` via CSS class if needed, but remove the hardcoded background colour)

## Acceptance Criteria

- Given the app loads with no stored theme preference, the light theme is applied.
- Given `localStorage['medour_theme']` is `'dark'`, the dark theme is applied immediately on load.
- Given the user clicks the sun icon (light mode active), the theme switches to dark and the icon changes to a moon.
- Given the user clicks the moon icon (dark mode active), the theme switches to light and the icon changes to a sun.
- Given dark mode is active, the main content area uses Vuetify's dark surface colour (not the hard-coded `#F5F7FA` from light mode).
- Given the user refreshes the page, their most recent theme choice is preserved.
- The toggle button is visible to both authenticated and unauthenticated users.

## Verification

**Commands:**

- `cd client && npm run build` — expected: zero TypeScript errors
- `cd client && npm run test` — expected: all existing tests pass

## Suggested Review Order

- Toggle VBtn in #append (always visible); role chip + username wrapped in auth guard.
  [`App.vue:20`](../../client/src/App.vue#L20)

- `isDark` computed and `toggleTheme()` — localStorage wrapped in try/catch for storage errors.
  [`App.vue:157`](../../client/src/App.vue#L157)

- `onMounted` theme restore — validates `'dark'|'light'` before applying; silent on storage error.
  [`App.vue:148`](../../client/src/App.vue#L148)

- `VMain` style removed; Vuetify dark surface colour now applies in dark mode.
  [`App.vue:126`](../../client/src/App.vue#L126)
