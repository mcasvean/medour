---
title: "Sidebar Pinning"
type: "feature"
created: "2026-08-17"
status: "done"
route: "one-shot"
baseline_commit: "71c7f517eeb39ffa3f8c7eeff2bc2dc395eaf09d"
---

# Sidebar Pinning

## Intent

**Problem:** The `VNavigationDrawer` was always `temporary` — it overlaid content and closed on every route change, forcing the user to reopen it on every navigation.

**Approach:** Add a `pinned` state to `App.vue` backed by `localStorage`. When pinned, `:temporary="!pinned"` switches the drawer to persistent mode (pushes `VMain`), navigation no longer closes it, and the preference survives page reloads. A pin/unpin icon button appears in the drawer header. Closing the drawer while pinned automatically unpins (state invariant enforced by a `watch`).

## Suggested Review Order

- `:temporary="!pinned"` — the single prop that switches overlay ↔ persistent layout mode
  [`App.vue:48`](../../client/src/App.vue#L48)

- Pin toggle button in drawer header
  [`App.vue:55`](../../client/src/App.vue#L55)

- IIFE reads localStorage before first render, seeding both `drawer` and `pinned` without flash
  [`App.vue:165`](../../client/src/App.vue#L165)

- Route watcher skipped when pinned — navigation no longer closes the drawer
  [`App.vue:180`](../../client/src/App.vue#L180)

- `watch(drawer)` auto-unpin invariant — closing the drawer while pinned clears the preference
  [`App.vue:183`](../../client/src/App.vue#L183)

- `togglePin` — opens drawer on pin, persists to localStorage
  [`App.vue:207`](../../client/src/App.vue#L207)
