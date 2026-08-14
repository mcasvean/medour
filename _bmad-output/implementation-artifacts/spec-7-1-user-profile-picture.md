---
title: "User Profile Picture"
type: "feature"
created: "2026-08-14"
status: "done"
review_loop_iteration: 0
baseline_commit: "307fd47ca7fdb78be9a2a4c12e5d2fdd3f045474"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Users have no visual identity in the app. The header shows a role chip and a name but no image. Profile customisation is limited to text fields — there is no way for a user to upload a personal avatar.

**Approach:**

- Add a `profile_picture` column (`TEXT NULL`) to the `users` table, storing the image as a Base64 data-URI string (e.g. `data:image/jpeg;base64,...`).
- Add `PATCH /api/v1/users/me/profile-picture` (authenticated, any role) accepting `multipart/form-data` with a single `file` part (≤ 512 KB, JPEG/PNG only); the server converts the bytes to a Base64 data-URI and persists it on the current user's record.
- Add `DELETE /api/v1/users/me/profile-picture` to remove the picture (sets column to `NULL`).
- Include `profilePicture: string | null` in the user profile response of `GET /api/v1/users/me` and in the JWT login response (so `authStore.user.profilePicture` is always current).
- In the header `#append`, show a small `VAvatar` (size 32, circular) with the image when set — order: role chip → avatar → username. When no picture is set, show the existing layout without an avatar placeholder.
- In `AccountView.vue`, add an upload section: a circular avatar (size 96) showing the current picture (or `mdi-account` icon placeholder), an "Upload photo" button that opens a file input, and a "Remove photo" button (visible only when a picture is set). Successful upload/removal shows a success toast (requires Story 6.1).
- Each user can only change their own picture. Admin cannot change another user's picture via this endpoint.

## Boundaries & Constraints

**Always:**

- Maximum file size: 512 KB — reject with 400 `{"error": "File too large. Maximum size is 512 KB."}` if exceeded.
- Accepted MIME types: `image/jpeg` and `image/png` only — reject with 400 `{"error": "Only JPEG and PNG images are accepted."}` for other types.
- Stored as Base64 data-URI in the `profile_picture TEXT` column.
- The endpoint is `/api/v1/users/me/profile-picture` — only modifies the authenticated user's record.
- `profilePicture` is included in the `UserProfileResponse` DTO and in the `AuthResponse` DTO.
- `authStore.user` must be updated in place after a successful upload/removal so the header avatar refreshes without re-login.
- Avatar in header: `VAvatar size="32"` with `rounded="circle"` and `<VImg :src="authStore.user.profilePicture" cover />`; shown only when `authStore.user.profilePicture` is non-null.

**Ask First:**

- Whether to keep the Base64 column approach or use a filesystem/object-storage path. The Base64 approach is self-contained (no external dependency) but does not scale to large images — acceptable given the 512 KB cap.

**Never:**

- Admin cannot use this endpoint to update another user's profile picture — the endpoint always operates on the authenticated user only.
- Do not resize or compress the image server-side in this story — accept and store as-is within the size limit.
- Do not display the avatar on appointment cards or in admin user list — header and account page only.

## I/O & Edge-Case Matrix

| Scenario                      | Input / State                                     | Expected Output / Behavior                                                 | Error Handling |
| ----------------------------- | ------------------------------------------------- | -------------------------------------------------------------------------- | -------------- |
| Upload valid JPEG ≤ 512 KB    | `PATCH /me/profile-picture` with valid file       | 200; `profilePicture` updated in DB; response includes new data-URI        | —              |
| Upload file > 512 KB          | File size 600 KB                                  | 400 `"File too large. Maximum size is 512 KB."`                            | —              |
| Upload non-image file         | MIME type `application/pdf`                       | 400 `"Only JPEG and PNG images are accepted."`                             | —              |
| Remove picture                | `DELETE /me/profile-picture`                      | 200; `profile_picture` set to `NULL`; avatar no longer shown in header     | —              |
| Get profile with picture      | `GET /me` after upload                            | `profilePicture` field contains the data-URI                               | —              |
| Header with picture set       | Authenticated user with `profilePicture` non-null | Small circular avatar appears between role chip and username in header     | —              |
| Header with no picture        | `profilePicture` is null                          | No avatar element rendered; layout unchanged from baseline                 | —              |
| authStore update after upload | Upload succeeds in AccountView                    | `authStore.user.profilePicture` updated; header avatar refreshes instantly | —              |

</frozen-after-approval>

## Code Map

**Backend:**

- `server/src/main/java/com/medour/model/User.java` — **EXTEND** add `@Column(name = "profile_picture", columnDefinition = "TEXT") private String profilePicture;`
- `server/src/main/java/com/medour/dto/UserProfileResponse.java` — **EXTEND** add `String profilePicture` field
- `server/src/main/java/com/medour/dto/AuthResponse.java` — **EXTEND** add `String profilePicture` field (populated from `user.getProfilePicture()` in `AuthService`)
- `server/src/main/java/com/medour/service/UserService.java` — **EXTEND** add `@Transactional updateProfilePicture(Long userId, MultipartFile file)`: validate size (≤ 512 KB → 400), validate MIME (jpeg/png → 400), encode to Base64 data-URI, persist; add `removeProfilePicture(Long userId)`: set `profilePicture = null`, save
- `server/src/main/java/com/medour/controller/UserController.java` — **EXTEND** add `@PatchMapping("/me/profile-picture")` accepting `@RequestParam("file") MultipartFile file` (authenticated) → `service.updateProfilePicture(userId, file)` → 200 `{ "profilePicture": dataUri }`; add `@DeleteMapping("/me/profile-picture")` → `service.removeProfilePicture(userId)` → 200
- `server/src/main/java/com/medour/config/SecurityConfig.java` — **EXTEND** permit `PATCH /api/v1/users/me/profile-picture` and `DELETE /api/v1/users/me/profile-picture` for all authenticated roles
- `server/src/test/java/com/medour/service/UserServiceTest.java` — **EXTEND** add tests: valid upload → profilePicture stored; oversize → 400; wrong MIME → 400; remove → null

**Frontend:**

- `client/src/stores/authStore.ts` — **EXTEND** add `profilePicture: string | null` to `User` interface; update `updateUser` picks to include `profilePicture`
- `client/src/stores/userStore.ts` — **EXTEND** add `uploadProfilePicture(file: File)` action: POST `FormData` to `PATCH /users/me/profile-picture`; call `authStore.updateUser({ profilePicture: response.profilePicture })`; add `removeProfilePicture()` action: DELETE `/users/me/profile-picture`; call `authStore.updateUser({ profilePicture: null })`
- `client/src/App.vue` — **EXTEND** in `#append` template, between role chip `div` and username `span`, add `<VAvatar v-if="authStore.user?.profilePicture" size="32" rounded="circle"><VImg :src="authStore.user.profilePicture" cover /></VAvatar>`
- `client/src/views/AccountView.vue` — **EXTEND** add profile picture section above the form: `VAvatar size="96"` showing picture or `mdi-account-circle` icon; "Upload photo" `VBtn` that triggers a hidden `<input type="file" accept="image/jpeg,image/png">` via ref; on file selected call `userStore.uploadProfilePicture(file)` and show success toast; "Remove photo" `VBtn variant="outlined" color="error"` `v-if="authStore.user?.profilePicture"` calling `userStore.removeProfilePicture()` and showing success toast on completion

## Tasks & Acceptance

**Execution:**

- [x] `server/src/main/java/com/medour/model/User.java` — add `@Column(name = "profile_picture", columnDefinition = "TEXT") private String profilePicture;`
- [x] `server/src/main/java/com/medour/dto/UserProfileResponse.java` — add `String profilePicture`
- [x] `server/src/main/java/com/medour/dto/AuthResponse.java` — add `String profilePicture`; populate in `AuthService` mapping
- [x] `server/src/main/java/com/medour/service/UserService.java` — add `updateProfilePicture(Long userId, MultipartFile file)`: `if (file.getSize() > 512 * 1024) throw 400`; `if (!List.of("image/jpeg","image/png").contains(file.getContentType())) throw 400`; encode `Base64.getEncoder().encodeToString(file.getBytes())`; store as `"data:" + file.getContentType() + ";base64," + encoded`; save user; add `removeProfilePicture(Long userId)`: find user, set `profilePicture = null`, save
- [x] `server/src/main/java/com/medour/controller/UserController.java` — `@PatchMapping("/me/profile-picture")` + `@DeleteMapping("/me/profile-picture")` wired to service methods; both return 200
- [x] `server/src/main/java/com/medour/config/SecurityConfig.java` — ensure authenticated access for `PATCH` and `DELETE` on `/api/v1/users/me/profile-picture`
- [x] `server/src/test/java/com/medour/service/UserServiceTest.java` — 3 new tests: valid JPEG upload stores data-URI; 600 KB file → 400; PDF MIME → 400; removeProfilePicture → null
- [x] `client/src/stores/authStore.ts` — add `profilePicture: string | null` to `User` interface; include `profilePicture` in `updateUser` partial type
- [x] `client/src/stores/userStore.ts` — add `uploadProfilePicture(file: File)` and `removeProfilePicture()` actions
- [x] `client/src/App.vue` — add `VAvatar` between role chip and username in `#append`, `v-if="authStore.user?.profilePicture"`
- [x] `client/src/views/AccountView.vue` — add picture upload/remove section with circular avatar, hidden file input, upload and remove buttons, and toast feedback

## Acceptance Criteria

- Given a user uploads a valid JPEG ≤ 512 KB, their profile picture is stored and immediately visible as a small circular avatar in the header.
- Given a user uploads a file larger than 512 KB, the server returns an error and a toast shows "File too large. Maximum size is 512 KB."
- Given a user uploads a non-image file, the server returns an error and an appropriate toast is shown.
- Given a user clicks "Remove photo", the picture is cleared from DB and the header avatar disappears.
- Given a user has a picture set, the header shows: role chip → circular avatar → username (in that order).
- Given a user has no picture, the header shows the existing layout without any avatar element.
- Given an admin logs in, they cannot change another user's picture through this endpoint (endpoint always applies to self).
- Given a picture upload succeeds, a success toast is shown in AccountView.

## Verification

**Commands:**

- `cd server && ./mvnw test` — expected: all existing tests + 4 new UserService tests pass
- `cd client && npm run build` — expected: zero TypeScript errors
- `cd client && npm run test` — expected: all existing tests pass

## Suggested Review Order

**Backend — validation & storage**

- Empty-file guard, size limit, case-insensitive MIME check, Base64 encoding, IOException wrap.
  [`UserService.java:updateProfilePicture`](../../server/src/main/java/com/medour/service/UserService.java)

- PATCH and DELETE endpoints; multipart form-data handling; `parseUserId` ownership via JWT.
  [`UserController.java:44`](../../server/src/main/java/com/medour/controller/UserController.java#L44)

- `profile_picture TEXT` column; `AuthResponse` and `UserProfileResponse` updated to carry it.
  [`User.java`](../../server/src/main/java/com/medour/model/User.java)

**Frontend — state & UI**

- `profilePicture` in `User` interface; `updateUser` picks extended.
  [`authStore.ts:10`](../../client/src/stores/authStore.ts#L10)

- `uploadProfilePicture` and `removeProfilePicture` actions; in-place `authStore.updateUser` call.
  [`userStore.ts:43`](../../client/src/stores/userStore.ts#L43)

- Header avatar (32px, `v-if="profilePicture"`) between role chip and username.
  [`App.vue:38`](../../client/src/App.vue#L38)

- AccountView upload/remove section: circular 96px avatar, hidden file input, loading states, toast feedback.
  [`AccountView.vue:14`](../../client/src/views/AccountView.vue#L14)

**Tests**

- 6 UserService tests: valid upload, oversize, empty file, uppercase MIME, PDF MIME, remove→null.
  [`UserServiceTest.java`](../../server/src/test/java/com/medour/service/UserServiceTest.java)
