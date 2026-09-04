# Eden Development Roadmap

## Product target

Eden is a human-made music network where listeners discover artists, follow their work, buy music directly, and join paid artist communities. It serves listeners, artists, bands, DJs, producers, and engineers without forcing each person into a single account type.

The first product loop is:

1. Discover a release or post.
2. Listen and visit the creator's profile.
3. Follow the creator.
4. Buy a digital release or join a membership.
5. Receive future public and supporter-only work.

The first beta succeeds when a local artist can join Eden, publish music, reach a listener who did not already follow them, and receive a direct payment.

## Scope decisions

### First public beta

- Secure accounts and profile onboarding
- Multiple profile roles: listener, artist, band, DJ, producer, engineer
- Genre and local-scene membership
- Releases, tracks, credits, posts, and audio playback
- Following, saving, blocking, and notifications
- A following feed and a recommendation feed
- One-time sales of digital music and downloadable music-related products
- Artist membership tiers and supporter-only content
- Reporting, moderation cases, sanctions, and appeals
- A strict human-made-content declaration at publishing time

### After the core loop works

- Paid opportunity listings and applications
- Booking, commissions, and service sales
- Collaboration agreements and payment protection
- Tickets, physical merchandise, shipping, and inventory
- Venue and promoter accounts
- Direct messaging between arbitrary users
- Visual artists as an independent marketplace category
- Live streaming

Physical goods, ticketing, and paid work introduce fulfillment, taxes, disputes, ownership terms, and fraud risks. They should not delay proving music discovery and direct digital sales.

## Technical direction

Build Eden as a modular monolith before considering microservices:

- **Backend:** Spring Boot and Java 21
- **Frontend:** React, TypeScript, and Vite
- **Primary database:** PostgreSQL
- **Media:** S3-compatible object storage; store metadata and ownership in PostgreSQL, not media bytes
- **Media delivery:** private objects with short-lived signed URLs for protected downloads
- **Payments:** a marketplace-capable payment provider behind an Eden payment interface
- **Background work:** database-backed jobs initially; introduce a dedicated queue only when processing volume requires it
- **Deployment:** one backend service, one static frontend deployment, one database, and one object-storage bucket for the beta

Organize backend code by product module rather than only by technical layer. Suggested modules are `identity`, `profiles`, `catalog`, `social`, `feed`, `commerce`, `memberships`, `moderation`, `notifications`, and `shared`. Modules should expose services and DTOs instead of sharing JPA entities through controllers.

## Current project assessment

The existing project provides a useful start: user registration, password hashing, artist profiles, connection requests, and React login/register screens.

Before extending it, address these foundation gaps:

- Login verifies a password but does not create an authenticated session.
- Spring Security currently permits every request.
- Profile mutation trusts a user ID supplied in the URL instead of the authenticated user.
- `UserController` returns persistence entities, which can expose password hashes and internal account data.
- Controllers accept persistence entities directly instead of validated request DTOs.
- The React authentication forms do not yet call the API.
- There is no database migration history or meaningful automated test coverage.
- The local Maven wrapper and npm launcher currently fail to start and must be repaired before a reliable baseline can be recorded.

Preserve `User`, `Profile`, and the visual authentication work. Treat the current `ConnectionRequest` as the beginning of professional networking, not as a follower relationship.

## Domain model

The names below describe responsibilities, not a required final schema.

### Identity and profiles

- `User`: credentials, account state, email verification, timestamps
- `Profile`: public name, handle, biography, location, avatar, banner
- `ProfileRole`: one profile can have several roles
- `Genre` and `ProfileGenre`
- `Scene` and `ProfileScene`: city/region plus a human-readable scene identity
- `Follow`: one-way listener-to-profile relationship
- `Block`: prevents visibility and interaction in both directions
- `ProfessionalConnection`: approved creator-to-creator relationship; evolve the current connection request into this workflow

Do not create separate account tables for listeners and creators. A listener can publish later, and a producer can also be an artist or DJ.

### Music and social content

- `Release`: single, EP, album, mix, remix, or other release type
- `Track`: audio metadata, duration, preview policy, and media asset
- `Credit`: profile, role, and free-text fallback for collaborators not yet on Eden
- `MediaAsset`: storage key, owner, media type, size, checksum, and processing state
- `Post`: text and attachments with public, follower, supporter, or tier visibility
- `Save` and optional lightweight reactions
- `ListeningEvent`: append-only events used for recommendations and artist analytics

Tracks and releases need explicit draft, processing, published, hidden, and removed states. Never make an upload public until media processing and required declarations complete.

### Commerce

- `Product`: artist-owned sellable item with type, price, currency, and availability
- `ProductAsset`: files granted after purchase
- `Order` and `OrderItem`: immutable purchase snapshot
- `Payment`: provider identifiers and payment lifecycle
- `LedgerEntry`: gross amount, processing charge, Eden fee, tax/adjustment, and artist amount
- `Entitlement`: durable proof that a user may access purchased content
- `Refund`: full or partial refund state

Eden's database must derive payment state from verified provider webhooks, not from a browser redirect. Use idempotency keys for checkout creation and webhook processing. Never compute historical artist balances from mutable product prices.

### Memberships

- `MembershipTier`: creator, name, price, benefits, and active state
- `Subscription`: supporter, tier, provider reference, and lifecycle
- `ContentGrant`: optional explicit access in addition to audience rules

Build membership access on the same entitlement model used by purchases. This avoids creating unrelated authorization systems for bought and subscribed content.

### Moderation

- `ContentDeclaration`: records the human-made-content agreement and policy version accepted for each publication
- `Report`: reporter, subject, category, evidence, and state
- `ModerationCase`: groups reports and reviewer work
- `ModerationAction`: warning, content removal, feature restriction, suspension, or ban
- `Appeal`: user response and final decision
- `AuditEvent`: append-only record of sensitive moderator and payment actions

Automated AI detectors should not be treated as proof. Enforcement should rely on the publication declaration, reports, evidence, account history, and human review.

## Feed implementation

Avoid machine-learning infrastructure in the first version. Collect useful events first and use an explainable ranking function.

### Feed v1

- A chronological following feed
- Cursor pagination based on publication time and ID
- A discovery feed assembled from followed genres, scenes, saves, listening history, and followed-profile similarity
- Candidate filters for blocks, removals, visibility, repeated items, and the user's own content
- A bounded popularity signal so already-large creators do not dominate every recommendation
- No paid ranking boosts

A first scoring model can combine recency, profile affinity, genre affinity, scene affinity, completion/saves, and a diversity penalty. Store the factors that caused a recommendation so the UI can say things such as “Because you follow Mexico City shoegaze.”

### Feed v2

After the beta produces enough behavior data:

- Create daily listener and release preference vectors
- Add collaborative signals from users with similar listening and purchase behavior
- Reserve feed positions for local, new, and low-exposure creators
- Run offline evaluation before changing production ranking
- Measure artist distribution as well as clicks and listening time

The optimization target should include meaningful follows, saves, purchases, and creator diversity—not only time spent scrolling.

## Delivery roadmap

Estimates assume one developer working consistently and include implementation and basic automated tests. They are planning ranges, not deadlines.

### Milestone 0 — Reproducible baseline (1 week)

**Implementation**

- Repair the Maven wrapper and npm installation/path
- Record the supported Java and Node versions
- Add local development instructions and sample environment configuration
- Add PostgreSQL through a reproducible local setup
- Add Flyway and create migrations for the existing schema
- Add backend test configuration and frontend component/API test tooling
- Add continuous integration for backend tests, frontend tests, lint, and production build
- Remove secrets from committed configuration and document required environment variables

**Exit criteria**

- A clean checkout can be started from written instructions
- Backend tests and frontend checks pass locally and in CI
- Database changes are applied only through migrations

### Milestone 1 — Secure identity and onboarding (1–2 weeks)

**Implementation**

- Replace login text responses with secure cookie-based authentication
- Add logout, current-user, email verification, and password-reset flows
- Restrict endpoints through Spring Security
- Replace entity request/response bodies with validated DTOs
- Remove or secure account-listing endpoints
- Connect the React registration and login forms to the backend
- Add authenticated route handling and session restoration
- Expand profiles with handle, roles, genres, scene, avatar, and onboarding state
- Add authorization tests for every mutation

**Exit criteria**

- A user can register, verify, sign in, refresh the page, sign out, and reset a password
- A user cannot read private account data or change another profile
- A user can complete listener or creator onboarding without selecting a permanent account type

### Milestone 2 — Publishing and media pipeline (2–3 weeks)

**Implementation**

- Add releases, tracks, credits, posts, and media-asset migrations
- Upload directly to object storage with signed upload requests
- Validate MIME type, size, checksum, and ownership server-side
- Add a background media-processing state machine
- Generate streamable audio and optional preview files
- Add draft, preview, publish, hide, and delete workflows
- Require acceptance of the current human-made-content policy when publishing
- Add creator profile, release, track, and post pages

**Exit criteria**

- A creator can publish a playable release with credits and a post
- Failed or incomplete uploads never appear publicly
- Every published asset is attributable to a user and policy declaration

### Milestone 3 — Social graph and feed (2–3 weeks)

**Implementation**

- Add follow, unfollow, block, save, and basic notification flows
- Separate professional connections from follows
- Build chronological following and profile feeds
- Add genre, scene, creator, and release search
- Add listening-event collection with privacy-aware retention
- Build discovery candidates and the first explainable ranking function
- Add cursor pagination and feed diversity rules
- Add rate limits for follows, reactions, publishing, and searches

**Exit criteria**

- A new listener can select interests and receive a useful discovery feed
- Following an artist changes the listener's feed
- Blocking removes both users from each other's discovery and interaction surfaces
- Recommendations can state at least one reason they were shown

### Milestone 4 — One-time digital commerce (3–4 weeks)

**Implementation**

- Select a marketplace payment provider through a short compliance and country-availability spike
- Add creator payment onboarding and payout status
- Add products, orders, payments, ledger entries, refunds, and entitlements
- Implement provider checkout and verified, idempotent webhooks
- Add artist-controlled pricing and supported currencies
- Add buyer library and signed protected downloads
- Add receipts, purchase history, artist sales view, and reconciliation jobs
- Publish transparent platform-fee and refund rules

**Exit criteria**

- A test buyer can purchase a release and retain access in their library
- The creator, Eden fee, processor fee, refund, and payout amounts reconcile
- Replayed or out-of-order webhooks do not duplicate orders or access
- A buyer cannot download another user's protected purchase

### Milestone 5 — Memberships and supporter content (2–3 weeks)

**Implementation**

- Add membership tiers and recurring subscriptions
- Add supporter- and tier-only post/release visibility
- Reuse commerce entitlements for access checks
- Handle renewal, cancellation, failed payment, grace period, and tier retirement
- Add member lists and audience-limited artist announcements
- Enforce frequency limits and opt-outs for announcements

**Exit criteria**

- A listener can subscribe, receive access, cancel, and retain access only through the paid period
- Artists can publish to all followers, all supporters, or a particular tier
- Payment failures and webhook retries cannot create incorrect access

### Milestone 6 — Moderation and beta operations (2–3 weeks)

Basic reporting and blocking begin in earlier milestones; this milestone completes the operational system.

**Implementation**

- Add report forms for profiles, releases, tracks, posts, and messages/announcements
- Build a moderator queue, case history, evidence view, and action controls
- Add content takedown, account restriction, suspension, ban, and appeal flows
- Add human-made-content, harassment, hate, impersonation, spam, and rights categories
- Add immutable audit events for moderator actions
- Add abuse rate limits, duplicate-report controls, and moderator permissions
- Create operational dashboards for failed jobs, payment discrepancies, reports, and storage processing
- Write incident, takedown, refund, and appeal procedures before inviting beta users

**Exit criteria**

- Every public content type can be reported and hidden by an authorized moderator
- Actions are auditable and reversible where policy allows
- Suspended and blocked accounts cannot bypass restrictions through normal endpoints
- Payment and safety incidents have a documented response path

### Milestone 7 — Closed local-scene beta (2–4 weeks)

**Implementation and operations**

- Recruit a focused cohort from one local scene
- Seed releases and profiles before listeners arrive
- Add invite controls and beta feedback capture
- Instrument onboarding completion, discovery, follows, saves, purchases, subscriptions, reports, and creator earnings
- Fix onboarding, feed quality, payment, and moderation failures before expanding

**Exit criteria**

- Several creators publish without developer assistance
- At least one listener discovers and purchases from an artist they did not know before Eden
- Moderation reports meet a defined response target
- Payment reconciliation has no unexplained differences
- The team can identify where users leave the core loop

## Post-beta roadmap

### Paid opportunities

Start with structured listings rather than unrestricted direct messages:

- Role needed, location/remote status, budget, deadline, deliverables, and rights expectations
- Applications and creator portfolios
- Shortlisting, acceptance, completion, review, and dispute states
- Clear prohibition on unpaid “exposure” listings unless explicitly categorized as volunteer collaboration

Do not hold funds or promise escrow until legal, payment-provider, dispute, and refund responsibilities are understood.

### Broader commerce

Add service sales, tickets, and physical goods separately. Each requires its own fulfillment and dispute model. Do not represent every sellable thing with identical UI merely because products share an order table.

## Cross-cutting engineering requirements

Apply these requirements in every milestone rather than postponing them:

- Database migrations and rollback planning
- DTO validation and service-level authorization
- Idempotency for payments, publishing, and background jobs
- Cursor pagination for feeds and large collections
- Rate limiting and abuse controls
- Structured logs without passwords, tokens, payment details, or private media URLs
- Accessibility for forms, playback, keyboard navigation, and moderation tools
- Account deletion, data export, content removal, and retention rules
- Unit tests for domain rules, integration tests for database/security, and end-to-end tests for critical user journeys
- Feature flags for payments, memberships, recommendations, and beta access

## Suggested implementation sequence inside each milestone

For each vertical feature:

1. Write the user journey and authorization rules.
2. Add the database migration.
3. Add domain/service behavior and tests.
4. Expose DTO-based API endpoints.
5. Build the React experience.
6. Add integration and end-to-end coverage.
7. Add logging, metrics, moderation hooks, and failure handling.
8. Release behind a feature flag and verify production behavior.

## Initial metrics

Avoid optimizing only for engagement time. Track:

- Listener onboarding completion
- Time to first meaningful play, save, and follow
- Discovery-to-profile and profile-to-purchase conversion
- Percentage of purchases from previously unknown artists
- Creator time to first follower and first sale
- Distribution of impressions and earnings across creators
- Repeat buyers and retained supporters
- Report rate, moderator response time, appeal outcomes, and repeat violations
- Payment failures, refunds, chargebacks, and reconciliation discrepancies

## Immediate next backlog

The next development cycle should remain entirely inside Milestones 0 and 1:

1. Repair and document the Java/Node toolchain.
2. Add Flyway and migrate the existing `users`, `profiles`, and `connection_requests` tables.
3. Define the authentication strategy and implement a real server-side session.
4. Remove persistence entities from public controller contracts.
5. Lock down Spring Security and eliminate public user enumeration.
6. Connect the React registration/login screens to the API.
7. Add current-user, logout, and protected-route behavior.
8. Add multi-role profile onboarding with genres and a scene.
9. Add backend authorization integration tests and one browser-level authentication journey.

Do not begin uploads, feeds, or payments until this slice is secure and reproducible. It becomes the foundation every later feature depends on.
