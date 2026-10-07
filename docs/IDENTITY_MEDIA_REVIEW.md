# Identity and media implementation review

This branch covers EMM-20 through EMM-26. Review in the order below. The Linear issues had no descriptions or acceptance criteria; these are the implementation targets used for this PR. Linear statuses and dates have not been changed.

## EMM-20 — Authorization tests for every mutation

**Behavior:** user updates and profile creation resolve the signed-in account, then reject a mismatching URL ID. Profile edits and connection requests check ownership. Media creation and completion also require authentication and ownership. Session-cookie mutations require CSRF tokens, including login and registration. Login rotates an existing session ID. Invalid credentials return 401.

**Files:** `config/AccountAccess.java`, existing controllers, `SecurityConfig.java`, `GlobalExceptionHandler.java`, `AuthorizationIntegrationTest.java`, `MediaIntegrationTest.java`.

**Verify:** run `./mvnw test`. The real PostgreSQL integration tests exercise an owner, a different account, anonymous requests, missing CSRF, persisted data after rejected mutations, and login/session/logout behavior. Existing MVC tests exercise registration and auth-flow endpoints.

**Review concept:** authentication identifies the caller; authorization decides whether that caller may change this particular resource. Knowing an ID grants no permission.

**Related fixes and limits:** usernames remain the session principal, so renaming is rejected with 409 until a stable-ID principal is introduced. Changing email clears email verification. Raw reset/verification token responses are allowed only by `eden.auth.expose-flow-tokens=true`, set in the local profile. Production defaults return 503 until an email delivery service exists. Do not enable that flag in a public environment. Password reset does not yet revoke other active sessions; email delivery and revocation remain follow-up identity work.

## EMM-21 — Authenticated routes and session restoration

**Behavior:** the frontend requests `/auth/me` on mount and keeps session state in React rather than local storage. Anonymous access to `/account` redirects to login. Successful login opens the account page. Signed-in visitors to login/register redirect to account. Logout clears state only after the backend accepts it. Network/server failures show a retry action rather than pretending the user is signed out.

**Files:** `frontend/src/auth/`, `App.tsx`, `pages/LoginPage.tsx`, `api/auth.ts`.

**Verify:** `npm test`, `npm run lint`, `npm run build` in `frontend`. Manually register, sign in, refresh `/account`, sign out, then visit `/account` again. Stop the backend and refresh to exercise the retry UI. Restart it and retry. The application uses same-origin requests through Vite locally; deploy behind a same-origin reverse proxy or configure secure CORS/cookie handling separately.

**Review concept:** route guards improve navigation; backend authorization remains the enforcement boundary. `/auth/csrf` returns a session-bound token; every frontend mutation fetches it and submits its named header.

## EMM-22 — Catalog and media migrations

**Behavior:** Flyway V4 creates releases, ordered tracks, credits with registered-profile or free-text attribution, posts, post/media attachments, and private media assets. Foreign keys, valid lifecycle states, size bounds, checksum format, preview bounds, and track ordering are enforced in PostgreSQL. Existing migrations are unchanged.

**Files:** `V4__catalog_and_media.sql`, `media/MediaAsset.java`, `media/MediaRepository.java`.

**Verify:** backend integration tests migrate fresh PostgreSQL 16 and 17 databases. Inspect the tables and foreign keys. Releases/posts default to DRAFT; media defaults to UPLOADING. READY requires a stream key and positive duration. Catalog CRUD and publishing are EMM-28/29 and are intentionally outside these seven issues.

**Review concept:** migrations define durable data invariants. A database table does not by itself implement a publishing workflow.

**Rollback:** stop writes and restore a pre-V4 database backup if rollback is necessary. Do not edit or delete an already-applied Flyway migration. Add a forward migration for schema corrections.

## EMM-24 — Direct object-storage uploads

**Behavior:** `POST /media/uploads` takes `contentType`, `sizeBytes`, lowercase hex `sha256`, and optional `previewSeconds`. It returns a random media ID, a 10-minute signed PUT URL, and required headers. The server assigns ownership and storage keys; callers cannot choose them. Upload the original bytes directly to storage, then call `POST /media/{id}/complete`. Repeated completion is harmless. Source objects stay private.

**Files:** `media/ObjectStorage.java`, `S3ObjectStorage.java`, `MediaController.java`, `compose.media.yaml`, `infra/`.

**Verify:** start storage and backend as described below. Create an upload, PUT the exact bytes with the returned Content-Type and signed headers, then complete it. Requests as another account must return 404 and must not queue the asset. The client supplies actual body length; browsers control Content-Length and Host, so do not manually set those two headers.

**Review concept:** a signed URL delegates one temporary storage operation without revealing storage credentials. Expiration does not make an object public or transfer ownership.

## EMM-23 — Server-side media validation

**Behavior:** completion checks stored size and declared Content-Type. The worker reads at most the expected bytes into a local file, recalculates SHA-256, checks WAV/MP3/FLAC signatures, and asks FFprobe to confirm an audio stream and a finite duration (maximum two hours). Unsupported inputs and mismatches fail permanently. Decoder inputs may use only file/pipe protocols.

**Files:** `MediaController.java`, `S3ObjectStorage.java`, `FfmpegAudioProcessor.java`, `MediaWorkerTest.java`.

**Verify:** upload a valid file, a file with an incorrect checksum, and a text file labeled as audio. Poll `/media/{id}`; only the valid audio should reach READY. Header/size mismatches fail completion; byte/checksum/decoder mismatches become FAILED in the worker. No playback URL is available before READY.

**Review concept:** client declarations are hints. Validation must examine the downloaded bytes. Processing that snapshot also prevents subsequent writes to the still-valid upload URL from changing generated playback.

## EMM-26 — Durable background media state machine

**Behavior:** UPLOADING → QUEUED → PROCESSING → READY or FAILED. The database atomically claims jobs with `FOR UPDATE SKIP LOCKED`, stores an attempt count and ten-minute lease, and recovers expired leases after restart. Each claim receives a distinct fencing token. READY/failure updates must match the live lease. Transient failures retry up to three attempts; invalid content does not retry. Output keys include the lease token so stale workers cannot overwrite winning output.

**Files:** `MediaRepository.java`, `MediaWorker.java`, `MediaIntegrationTest.java`.

**Verify:** run the lease/retry integration test. For manual recovery, queue an asset, stop the backend during processing, wait for its ten-minute lease to expire, and restart. Confirm processing resumes and attempts increase. Failed jobs have a safe failure code, with no private storage URL in responses or logs.

**Review concept:** a persisted queue survives restarts; a fencing token prevents an old worker from finishing a newer worker's job. The worker processes one item per poll per backend instance.

## EMM-25 — Streamable audio and previews

**Behavior:** FFmpeg produces stereo, 44.1 kHz, 192 kbps MP3 with metadata removed. Optional previews are 5–60 seconds from the start, or the available duration for shorter tracks. READY is written only after all requested output uploads succeed. `GET /media/{id}/playback` gives the owner five-minute signed URLs for the processed MP3 and optional preview. Source audio is never returned as playback.

**Files:** `AudioProcessor.java`, `FfmpegAudioProcessor.java`, `MediaWorker.java`, `AudioConversionTest.java`, `Dockerfile`.

**Verify:** listen to the signed stream and preview. Run the real conversion test with `EDEN_TEST_FFMPEG` and `EDEN_TEST_FFPROBE` set to executable paths. It synthesizes an eight-second WAV, converts it, and measures a five-second MP3 preview. This optional test is skipped when those variables are absent; CI sets them.

For the full local storage smoke test, start the media Compose services, set the local AWS credentials and both FFmpeg variables, set `EDEN_TEST_STORAGE=true`, and run `./mvnw -Dtest=MediaStorageSmokeTest test`. It uses a temporary PostgreSQL database, signs and uploads a real WAV to MinIO, completes it through the API, waits for the real worker, verifies the unsigned original returns 403, and verifies signed MP3 range playback returns 206. It leaves small test objects in the local bucket. This test is opt-in because it requires the separately running storage service.

**Review concept:** this is progressive MP3 playback, not HLS transcoding. Public playback and purchased-content entitlements belong to later publishing/commerce work.

## Local media setup

Media is disabled by default. Starting ordinary Eden does not require storage credentials or FFmpeg.

1. Start Docker Desktop.
2. Run `docker compose -f compose.yaml -f compose.media.yaml up --build -d`.
3. Install FFmpeg/FFprobe on PATH, or supply `--eden.media.ffmpeg=<path>` and `--eden.media.ffprobe=<path>` as Spring Boot arguments. The backend Dockerfile includes FFmpeg.
4. In the backend terminal, set `AWS_ACCESS_KEY_ID=eden_local`, `AWS_SECRET_ACCESS_KEY=eden_local_storage`, and `AWS_REGION=us-east-1`.
5. Run `./mvnw spring-boot:run -Dspring-boot.run.profiles=local,media-local` (quote the `-D...` argument in PowerShell).
6. Run the frontend normally. Its development proxy forwards `/auth` and `/media`.

The local storage endpoint is `http://localhost:9000`; its console is `http://localhost:9001`. Both bind only to loopback. The init service creates a private `eden-media` bucket and CORS for `http://localhost:5173`. The pinned official MinIO sources are built locally because the historical official images are unavailable. Building initially takes longer than pulling a prebuilt image. These historical binaries are local test infrastructure, not a production deployment recommendation.

For hosted S3-compatible storage, enable `eden.media.enabled`, set `eden.media.bucket`, `eden.media.region`, and optionally `eden.media.endpoint`. Use AWS SDK credential environment variables or an IAM role. Provision a private bucket, least-privilege credentials, and exact frontend CORS origins. Never use the local credentials in a hosted environment. HTTPS and secure session cookies are required there.

## Operational limits to review before public deployment

* No public uploads/feed/payment UI is included; media endpoints currently serve owners only.
* Staging originals and orphaned outputs from failed or stale attempts need storage lifecycle cleanup rules. READY outputs must be retained.
* Add upload quotas/rate limits and a sandboxed media worker with CPU/memory/disk limits before accepting untrusted public uploads.
* Keep FFmpeg patched; the Dockerfile supplies it but is not a complete hardened deployment.
* Object storage must support signed PUT/GET and private range GETs. Run the storage smoke test against the chosen hosted provider before deployment.
* The new catalog relationships need service-level ownership and publication/entitlement rules when their APIs are added.

## Suggested review order

Read the ownership checks, then the frontend session flow, then V4. Trace one upload through the controller, queue, validator, converter, and READY transition. Predict what happens for a wrong owner, bad checksum, worker restart, and stale lease; then run the corresponding tests.
