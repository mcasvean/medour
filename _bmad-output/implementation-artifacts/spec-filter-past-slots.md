---
title: 'Filter past time slots from slot grid'
type: 'feature'
created: '2026-08-17'
status: 'done'
review_loop_iteration: 0
route: 'one-shot'
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The slot grid returned by `GET /doctors/{id}/slots?date=` always generates all 24 daily slots (08:00–19:30), including slots whose start time is already in the past. Patients see and can attempt to book slots that have already passed.

**Approach:** In `SlotService.getSlotsForDoctor`, return an empty list for past dates and skip any slot whose `startTime` is strictly before `LocalTime.now()` when the requested date is today. The client already renders an informational "No slots available" message for an empty array, so no front-end changes are needed.

## Suggested Review Order

**Core filtering logic**

- Public method delegates to package-private overload so tests can inject a fixed clock
  [`SlotService.java:31`](../../server/src/main/java/com/medour/service/SlotService.java#L31)

- Past-date early return; today time-filter loop advancing `candidate` past `current`
  [`SlotService.java:36`](../../server/src/main/java/com/medour/service/SlotService.java#L36)

**Tests**

- Past date → empty list; today at 16:03 → first slot is 16:30
  [`SlotServiceTest.java:108`](../../server/src/test/java/com/medour/service/SlotServiceTest.java#L108)

- Today at 19:35 (past all slots) → empty list
  [`SlotServiceTest.java:135`](../../server/src/test/java/com/medour/service/SlotServiceTest.java#L135)

</frozen-after-approval>
