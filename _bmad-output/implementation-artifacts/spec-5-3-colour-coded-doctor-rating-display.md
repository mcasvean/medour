---
title: "Colour-Coded Doctor Rating Display"
type: "feature"
created: "2026-08-14"
status: "done"
review_loop_iteration: 0
baseline_commit: "c57d72eac9d94cf87b71c7250998fac360db4591"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Doctor search results show `averageRating` as plain text; there is no visual differentiation of rating quality, and the display is not badge-styled.

**Approach:** Replace the plain-text rating span in `BookingSearchView.vue` with a colour-coded badge component computed from `averageRating`; the badge colour follows three tiers; no badge is shown when a doctor has no ratings.

## Boundaries & Constraints

**Always:**

- Colour tiers: 1.00–5.00 → orange; 5.01–8.00 → light blue; 8.01–10.00 → light green.
- No badge rendered when `averageRating` is `null`.
- Badge displays the numeric value to one decimal place (e.g. "8.9").
- No server-side changes required — `averageRating` is already in `DoctorSearchResult`.

**Ask First:** None identified.

**Never:**

- Do not change the DTO, service, or API endpoint.
- Do not add a star-rating graphic or non-numeric display.

## I/O & Edge-Case Matrix

| Scenario      | Input / State                     | Expected Output / Behavior             | Error Handling |
| ------------- | --------------------------------- | -------------------------------------- | -------------- |
| No ratings    | `averageRating: null`             | No badge rendered                      | —              |
| Low rating    | `averageRating: 4.5` (1.00–5.00)  | Badge shown in orange, text "4.5"      | —              |
| Mid rating    | `averageRating: 6.0` (5.01–8.00)  | Badge shown in light blue, text "6.0"  | —              |
| High rating   | `averageRating: 9.2` (8.01–10.00) | Badge shown in light green, text "9.2" | —              |
| Boundary 5.00 | `averageRating: 5.0`              | Orange badge                           | —              |
| Boundary 5.01 | `averageRating: 5.01`             | Light blue badge                       | —              |
| Boundary 8.00 | `averageRating: 8.0`              | Light blue badge                       | —              |
| Boundary 8.01 | `averageRating: 8.01`             | Light green badge                      | —              |

</frozen-after-approval>

## Code Map

- `client/src/views/BookingSearchView.vue` -- **EXTEND** — replace `<span>Rating: {{ doctor.averageRating !== null ? doctor.averageRating : 'No rating' }}</span>` with a `<span>` badge that is `v-if="doctor.averageRating !== null"`, shows the value formatted to 1 decimal, and has a dynamic class bound to the tier computed from `averageRating`
- `client/src/stores/doctorStore.ts` -- read-only reference — `DoctorSearchResult.averageRating: number | null` already present
- No server files change

## Tasks & Acceptance

**Execution:**

- [x] `client/src/views/BookingSearchView.vue` -- ADD a `ratingBadgeClass(rating: number | null)` function (or computed-equivalent) returning `'badge-orange'` for 1–5, `'badge-blue'` for 5.01–8, `'badge-green'` for 8.01–10; replace the existing rating span with a `v-if="doctor.averageRating !== null"` badge `<span>` using `ratingBadgeClass` and displaying `doctor.averageRating.toFixed(1)` -- implements the colour-coded badge with correct tier thresholds
- [x] `client/src/views/BookingSearchView.vue` -- ADD CSS classes `.badge-orange`, `.badge-blue`, `.badge-green` in `<style scoped>` -- orange (#f97316 background or similar), light blue (#93c5fd), light green (#86efac)

**Acceptance Criteria:**

- Given a doctor with no ratings (`averageRating: null`), no rating badge is shown on their card.
- Given a doctor with `averageRating` between 1.00 and 5.00 inclusive, an orange badge showing the value to 1 decimal is shown.
- Given a doctor with `averageRating` between 5.01 and 8.00 inclusive, a light-blue badge is shown.
- Given a doctor with `averageRating` between 8.01 and 10.00 inclusive, a light-green badge is shown.
- The badge value is formatted to 1 decimal place.

## Verification

**Commands:**

- `cd client && npm run build` -- expected: TypeScript compile succeeds with no errors
- `cd client && npm run test` -- expected: all vitest tests pass
