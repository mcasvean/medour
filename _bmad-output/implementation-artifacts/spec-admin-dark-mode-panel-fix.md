---
title: 'Admin User Detail Panel Dark-Mode Fix'
type: 'bugfix'
created: '2026-08-17'
status: 'done'
route: 'one-shot'
baseline_commit: '9763f634df83fe8e174b20afacfa492f47879a7f'
---

# Admin User Detail Panel Dark-Mode Fix

## Intent

**Problem:** The "View details" expansion panel in `AdminUsersView.vue` used `bg-color="grey-lighten-5"` on all three user-role card sections (Admins, Doctors, Patients). `grey-lighten-5` resolves to #FAFAFA — a near-white hardcoded colour that breaks in dark mode, rendering the panel as an opaque white rectangle over the dark card surface.

**Approach:** Replace the hardcoded colour with the Vuetify 3 semantic token `bg-color="surface-variant"`, which maps to an appropriate tonal surface for both light and dark themes while preserving the subtle visual differentiation between the panel and its parent card.

## Suggested Review Order

- Single-line attribute swap applied identically to all three role-section cards
  [`AdminUsersView.vue:47`](../../client/src/views/AdminUsersView.vue#L47)

- Second occurrence — Doctors section
  [`AdminUsersView.vue:116`](../../client/src/views/AdminUsersView.vue#L116)

- Third occurrence — Patients section
  [`AdminUsersView.vue:185`](../../client/src/views/AdminUsersView.vue#L185)
