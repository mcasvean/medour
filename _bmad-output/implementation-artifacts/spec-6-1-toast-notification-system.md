---
title: "Toast Notification System"
type: "feature"
created: "2026-08-14"
status: "ready-for-dev"
review_loop_iteration: 0
baseline_commit: ""
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The app surfaces errors and success messages only as inline VAlert banners inside each form. There is no global feedback mechanism — users miss confirmations for key actions, and server error messages are often swallowed or displayed inconsistently across views.

**Approach:** Create a global `ToastNotification.vue` component mounted once in `App.vue` and a Pinia `toastStore` that holds the active toast queue. Any view or store calls `toastStore.show(message, type)` to push a notification. Toasts stack bottom-right of the screen, auto-dismiss after 5 seconds, and each has a manual close button. Error toasts display the server's error message when one is provided.

## Boundaries & Constraints

**Always:**

- Toast types: `'error'`, `'success'`, `'warning'` — each with a distinct colour and icon.
- Toast position: fixed, bottom-right, `z-index` above all other elements.
- Auto-dismiss after 5 000 ms; a close button dismisses immediately.
- Up to 5 toasts stacked simultaneously; oldest is removed first when the limit is exceeded.
- Server error messages (from `error.response?.data?.message || error.response?.data?.error`) take priority over generic fallback text when showing error toasts.
- `ToastNotification.vue` is mounted once inside `App.vue` — no per-view imports needed.
- All toast calls go through `toastStore.show(message, type)` — no direct component manipulation.

**Ask First:**

- If a different auto-dismiss duration or max stack size is preferred.

**Never:**

- Do not replace existing inline VAlert error banners in this story — they are removed in each respective story as toasts are integrated (Stories 6.2+).
- Do not add BE changes — this is a pure FE feature.
- Do not use Vuetify's built-in snackbar component — implement a custom component for full design control.

## I/O & Edge-Case Matrix

| Scenario                    | Input / State                               | Expected Output / Behavior                                    | Error Handling                                 |
| --------------------------- | ------------------------------------------- | ------------------------------------------------------------- | ---------------------------------------------- |
| Success toast triggered     | `toastStore.show('Saved!', 'success')`      | Green toast appears bottom-right with check icon, auto-closes | —                                              |
| Error toast with server msg | `toastStore.showError(axiosError)`          | Red toast with `error.response.data.message` or fallback text | Falls back to `'An unexpected error occurred'` |
| Warning toast               | `toastStore.show('Check input', 'warning')` | Amber/yellow toast with warning icon                          | —                                              |
| Manual close                | User clicks ✕ on a toast                    | Toast removed immediately from queue                          | —                                              |
| 6 toasts triggered rapidly  | 6 `toastStore.show(...)` calls              | First (oldest) toast removed; 5 most recent remain            | —                                              |
| Auto-dismiss                | Toast shown, no user interaction for 5 s    | Toast fades out and is removed from DOM                       | —                                              |

</frozen-after-approval>

## Code Map

- `client/src/stores/toastStore.ts` — **NEW** Pinia store; state: `toasts: Toast[]` (max 5); actions: `show(message, type)` pushes a toast with a unique id and starts a 5 000 ms timeout to auto-remove; `showError(error)` extracts server message then calls `show(..., 'error')`; `dismiss(id)` removes by id
- `client/src/components/ToastNotification.vue` — **NEW** fixed bottom-right component; `v-for` over `toastStore.toasts`; each toast card has colour/icon by type, message text, close button calling `toastStore.dismiss(id)`; CSS transition for enter/leave animation
- `client/src/App.vue` — **EXTEND** import and mount `<ToastNotification />` inside `<VApp>` (after `<VMain>`)

## Tasks & Acceptance

**Execution:**

- [ ] `client/src/stores/toastStore.ts` — define `Toast` interface `{ id: number; message: string; type: 'error' | 'success' | 'warning' }`; `useToastStore` with `toasts: Toast[]`; `show(message, type)`: generate `id = Date.now()`, push to array (pop oldest if length > 5), call `setTimeout(() => dismiss(id), 5000)`; `showError(error: unknown)`: extract `(error as AxiosError)?.response?.data?.message ?? (error as AxiosError)?.response?.data?.error ?? 'An unexpected error occurred'`, call `show(msg, 'error')`; `dismiss(id)`: filter toasts by id
- [ ] `client/src/components/ToastNotification.vue` — fixed position `bottom: 24px; right: 24px`; flex column gap; `v-for="toast in toastStore.toasts" :key="toast.id"`; card per toast with: left border colour by type (error=`#EF5350`, success=`#4CAF50`, warning=`#FB8C00`); leading icon (`mdi-alert-circle` error, `mdi-check-circle` success, `mdi-alert` warning); message text; `VBtn icon="mdi-close" size="x-small"` calling `toastStore.dismiss(toast.id)`; `<TransitionGroup name="toast">` wrapping the list for slide-up animation
- [ ] `client/src/App.vue` — import `ToastNotification`; add `<ToastNotification />` as last child inside `<VApp>`, after `</VMain>`

## Acceptance Criteria

- Given `toastStore.show('Profile saved.', 'success')` is called, a green toast appears bottom-right with a check icon and the message text.
- Given `toastStore.showError(axiosError)` where `axiosError.response.data.message = 'Email already in use'`, the red toast displays `'Email already in use'`.
- Given `toastStore.showError(networkError)` where no response is available, the red toast displays `'An unexpected error occurred'`.
- Given a toast is shown and 5 seconds pass with no interaction, it disappears automatically.
- Given a user clicks the ✕ button on a toast, it disappears immediately.
- Given 6 toasts are pushed, only the 5 most recent are visible.
- Given a warning toast is shown, it displays in amber/orange with the warning icon.

## Verification

**Commands:**

- `cd client && npm run build` — expected: zero TypeScript errors
- `cd client && npm run test` — expected: all existing tests pass
