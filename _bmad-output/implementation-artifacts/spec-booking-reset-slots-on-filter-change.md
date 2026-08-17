---
title: "Reset Slot Display When Speciality Filter Eliminates Selected Doctor"
type: "bugfix"
created: "2026-08-17"
status: "done"
route: "one-shot"
---

## Intent

**Problem:** When a patient selects a doctor and views their slots, then changes the speciality filter and re-runs the search, the slot grid remains visible showing stale slots for a doctor who no longer appears in the filtered results.

**Approach:** In `handleSearch()`, after the search completes, check whether the previously-selected doctor is still in the new result set. If not, cancel any active slot reservation, disconnect SSE, and clear all slot-related state before returning.

## Suggested Review Order

- Core guard: `stillInResults` check that triggers the reset path
  [`BookingSearchView.vue:238`](../../client/src/views/BookingSearchView.vue#L238)

- Active-reservation cancel before state wipe to release the server-side lock
  [`BookingSearchView.vue:240`](../../client/src/views/BookingSearchView.vue#L240)

- Full slot-state reset: SSE disconnect, selectedDoctorId, slots, reservationId, errorMessage
  [`BookingSearchView.vue:243`](../../client/src/views/BookingSearchView.vue#L243)
