# Short URL & QR Code Generator

![CI](https://github.com/m4r10kyou/shortlinkQr/actions/workflows/ci.yml/badge.svg)
<a href="https://stats.uptimerobot.com/Au8WWQRPE6?utm_source=status_badge&utm_medium=referral" target="_blank" rel="noopener"><picture><source media="(prefers-color-scheme: dark)" srcset="https://badge.uptimerobot.com/psp/dedaf8b05e48e1034cf3a37c74c77eb9.svg?style=logo&theme=dark"><img src="https://badge.uptimerobot.com/psp/dedaf8b05e48e1034cf3a37c74c77eb9.svg?style=logo&theme=light" alt="shortlinkqr.onrender.com"></picture></a>
![Uptime 30d](https://img.shields.io/uptimerobot/ratio/30/m804357575-040a3f2b579c23775389f929)

A backend service that converts long URLs into trackable short links and customizable QR codes.

**Live:** [shortlinkqr.onrender.com](https://shortlinkqr.onrender.com)

## 💡 The Idea (The "Why")
While many URL shorteners exist, the core value of this service lies in **dynamic QR redirection**.

The generated QR code encodes the *shortlink*, not the final destination. This means you can change where a printed QR code points without ever needing to reprint it.

**Example:** You print table tents for a restaurant with a QR code pointing to the summer menu. Months later, you want to redirect customers to the winter menu. You simply update the target URL in the system, and all previously printed QR codes will seamlessly redirect to the new page.

## ⚙️ How It Works
1. A target URL (and optionally a custom image) is submitted to the service.
2. The system generates a unique shortlink and a QR code pointing specifically to that shortlink.
3. When a user clicks the link or scans the QR, their device makes an HTTP request to the shortlink endpoint.
4. The server intercepts this request, increments the access counter to track metrics, and redirects the user to the current target URL.

## 📐 Design Decisions

### Short codes

**Random, not sequential.** *URLs use unpredictable strings to prevent data scraping and decouple the API from the Database*
- 7 Base62 characters ≈ 3.5 trillion combinations
- A sequential database id in the URL would let anyone walk the whole table by counting up
- It would also tie the public API contract to the persistence layer

**`SecureRandom`, not `Random`.** *Cryptographically secure generation ensures future codes cannot be guessed from observed outputs*
- `Random` is a linear congruential generator with a 48-bit seed
- A few observed outputs are enough to reconstruct its state and predict the next codes
- Same concern as above: nobody should be able to guess codes that have not been issued yet

**Full 62-character alphabet.** *Maximizing the address space takes priority over visual clarity, as QR scanning is the primary use case*
- Considered excluding visually ambiguous characters (0/O, 1/l/I) to reduce transcription errors when someone types a code from a printed sign
- Kept the full alphabet; the QR is the primary path and manual typing is the exception

**The generator does not know about the database.** *Collision checking is delegated to the caller to maintain a pure, easily testable domain service*
- `generate` takes a `Predicate<String>` that answers whether a code is taken; the caller decides how to check
- This keeps the component free of persistence and makes the collision path testable with a plain lambda

### Visit counting

**Atomic UPDATE, not read-modify-write.** *Delegating the increment operation to the database guarantees accuracy under concurrent traffic without unnecessary locking*
- Loading the entity, incrementing in Java and saving loses visits under concurrency: two requests read 40, both write 41
- `set visit_count = visit_count + 1 where code = ?` lets the database serialise it
- Optimistic and pessimistic locking would also be correct, but for a commutative increment they add retries or contention for nothing

### Redirect path

**The redirect reads two columns.** *A projection, not the entity.*
- `resolveCode` needs the target URL and the expiry date. Loading the `ShortLink` entity fetched every column, including the one holding the logo bytes, on the endpoint that every scan of every printed QR hits.
- A `RedirectTarget` projection narrows the query to `select target_url, expires_at from short_links where code = ?`. The logo never leaves the database on that path.
- The expiry rule lives in `ExpirationPolicy`, shared by the entity and the service, so "expired" has one definition rather than one per caller.

### Domain modelling

**`Instant`, not `LocalDateTime`.** *Using absolute points on the timeline prevents critical bugs when evaluating expirations across different time zones*
- `LocalDateTime` carries no time zone
- An expiry computed in local time and compared on a server in another zone expires early or late

**Behaviour over setters.** *The entity encapsulates its own logic and state transitions to guarantee internal consistency at all times*
- The entity exposes `changeTargetUrl`, `changeExpiry`, `removeExpiry`, `attachLogo` and `removeLogo` instead of a setter per field
- The mutators reject null. `removeExpiry` and `removeLogo` are the only ways to clear those fields, so "set this to nothing" is always an explicit call and never a null arriving from a layer above. Those guards are what make the partial-update rule enforceable rather than merely followed
- `attachLogo` and `getLogoData` copy the array on the way in and on the way out. A `byte[]` is mutable, unlike the `String` fields around it, so handing out the internal reference would let any caller rewrite the entity's state without going through a method — which is what this section exists to prevent. Two tests pin it.

**Exceptions carry no HTTP semantics.** *Domain errors remain agnostic to the web transport layer, ensuring reusability across the different contexts*
- Plain `RuntimeException` subclasses, no `@ResponseStatus`
- Mapping to 404/409/410 belongs to the web layer
- Keeps the service usable from a batch job or a message consumer, where HTTP means nothing

### Schema and migrations

**Flyway owns the schema, in every environment.** *One migration, one source of truth.*
- `ddl-auto` is `validate` everywhere, so Hibernate compares the entity against the schema at startup and refuses to start when the two disagree. A mismatch becomes a boot failure naming the column, instead of a query failing in production months later.
- H2 runs with `;MODE=PostgreSQL` in development and in tests, so a single `V1__create_short_links.sql` serves every environment. A migration set per database would mean the thing being tested is not the thing being deployed.
- Applied migrations are immutable. Flyway stores a checksum of each one, and editing a file that has already run stops the application; changes go in a new version.

**`bytea`, not a large object.** *`@Lob` on a `byte[]` means different things on different databases.*
- Hibernate maps `@Lob byte[]` to PostgreSQL's `oid`: the bytes live outside the row in `pg_largeobject`, reached through a handle, and deleting the row does not remove them unless something calls `lo_unlink`. On H2 the same annotation was harmless, which is exactly why the problem would only have surfaced on the way to production.
- The column is declared `bytea` in the migration and the annotation is gone. A logo of at most a few megabytes belongs in the row.

### Deployment

**Four profiles, and no default.** *The application refuses to start without being told where it is running.*
- `dev` is H2 in a file, `test` is H2 in memory, `integration` deliberately supplies no datasource so Testcontainers can provide one, and `prod` is PostgreSQL. The shared `application.properties` holds only what every environment agrees on.
- Nothing is active by default, and a `BeanFactoryPostProcessor` rejects both an empty profile list and a name that is not one of the four. It has to run that early: as an ordinary `@Component` the guard was constructed in the middle of context refresh, by which time Flyway had already migrated a database Boot had invented — with no `spring.datasource.url` and an embedded driver on the classpath, Boot creates an anonymous in-memory H2 rather than failing. "No default profile" did not mean "will not start" until the check moved ahead of every singleton.
- A misspelling is rejected too, not only an absent profile. `prod,prd` passed the first version of the guard, which complained only when *every* name was unknown.
- The `prod` profile reads its connection details from environment variables with no fallback values, so a missing variable is a boot failure rather than a silent connection to something else. Not a loud one, though: `spring.datasource.*` binds through the `Binder`, which ignores an unresolvable placeholder and leaves the literal `${DATABASE_URL}` in the property, so the message is `'url' must start with "jdbc"` and never names the variable. A `@Value` would have thrown a `PlaceholderResolutionException` that did, because it resolves placeholders through a different mechanism.

**The application does not know who hosts it.** *Everything environment-specific arrives as a variable.*
- The database, the base URL and the admin token all come from the environment. Moving from one provider to another is a matter of changing values, not code — which is exactly what happened when the first choice of host turned out to cost $38/month for a database that will hold a few dozen rows.
- The port comes from the environment too, because platforms assign one and expect the service to listen on it. The default stays at 9080 for local runs, where no such variable exists.

**The H2 console is disabled explicitly in production.** *Defence in depth for a profile that does not use H2 at all.*
- It costs one line, and it removes any path by which a database console with a blank `sa` password could become reachable on a public address.

**Production logs to stdout.** *The container is ephemeral.*
- A log file written inside the container is discarded along with it, so the platform collects stdout instead. `dev` still writes a file, because that machine persists.

**The image is built in two stages.**
- Dependencies are resolved before the sources are copied, so editing a class does not invalidate the dependency layer.
- The runtime stage carries a JRE and the jar alone: no sources, no Maven, no local repository, and a non-root user.
- Tests do not run during the image build. Finding out that something is broken belongs in CI, not in packaging.
- The JVM heap is capped at 75% of container memory. The default is 25%, chosen for machines larger than the 512MB this runs on.

### Health and monitoring

**Two endpoints, because one of them costs money.** *The database runs on a metered plan.*
- The managed PostgreSQL this deploys against bills by compute hour and suspends itself after five minutes without queries. The monthly allowance is roughly an eighth of a month of continuous uptime.
- The application host, meanwhile, sleeps after fifteen minutes without HTTP traffic and takes about a minute to wake. Keeping it awake means pinging it every few minutes.
- Those two facts collide: a monitor polling an endpoint that queries the database would keep the database awake as well, and exhaust a month's allowance in about four days. So the endpoint the monitor calls must be incapable of reaching the database.

**`GET /api/health/ping`** answers `200` with `{"status":"OK"}` and has no dependencies at all — nothing is injected into it. It is what the uptime monitor and the platform's own health check call. Caching the result would not have helped: one query per minute still prevents the database from ever suspending.

**`GET /api/health`** reports the project name, version and description to anyone, and adds the real health status only to a caller presenting a valid admin token in an `X-Admin-Token` header.
- An absent header is not a failed attempt — it is a client that did not ask for that part — so it gets `200` with a smaller body and no database query.
- A header that is present and wrong gets `403`. Someone who tried to authenticate deserves to be told they failed; otherwise a typo in the deployed token would look like success forever.

**The version comes from the build.** `app.version` and `app.description` are filled from the POM through resource filtering, so the endpoint cannot report a version the build does not have. A hand-written string would be a claim nobody maintains.

**Actuator is on the classpath with its web endpoints closed.** `management.endpoints.web.exposure.include` is empty. The `HealthEndpoint` bean is used directly by the controller, and `/actuator/health` — which queries the database and would otherwise be public — does not exist as a route.

### API & Web Layer

**Error responses.** *RFC 7807 (`application/problem+json`) is used for all errors.*
- The body includes a `type` property so clients can branch their logic by code rather than parsing text.
- Validation errors add a field-specific `errors` map.
- Domain exceptions publish the failed identifier (`code` or `alias`) as a custom property at the root level, so no one has to extract it from the message using a regular expression.

**Client errors stay 4xx.** *The global advice extends `ResponseEntityExceptionHandler`.*
- Without this, a malformed JSON or an unsupported HTTP method would result in a 500 Internal Server Error.
- A 5xx status means "server failure" and is the signal upon which monitoring alerts are built. If a badly programmed client can generate them at will, the signal becomes useless.

**302, never 301.** *The core decision for dynamic redirection.*
- A 301 tells the browser the destination will never change, causing it to be cached permanently. This breaks the core promise of the project: a printed QR code must be retargetable after publication.
- This is also the reason for the explicit `Cache-Control: no-cache` header on the redirect response, which tells the browser "you can store it, but you must revalidate it before using it."

**Root redirect with restricted routing.** *Shielding the database from automated bot traffic.*
- The public redirect endpoint lives at the root (`/`), but the route pattern is strictly limited to match valid code structures. The regular expression is centralized in `ShortLinkConstraints` and shared with the DTO validation.
- Without this, every bot scanning for `/wp-admin` or `/.env` would trigger a database lookup, generating unnecessary load and polluting the logs.

**Absolute base URL from configuration.** *Consistency for physical media.*
- The base URL is injected via configuration properties rather than inferred from the incoming HTTP request (e.g., via `ServletUriComponentsBuilder`).
- Inferring it from the request would cause the generated link to change depending on whether it was created via a proxy, a local IP, or a custom domain. For a link destined to be printed on a physical sign, the canonical domain must remain fixed.

**URL validation.** *A regular expression cannot accurately express URI syntax.*
- A URL with a space would pass a simple regex validation, get saved to the database, and crash with a 500 error during the public redirect.
- Validation is performed by constructing a `java.net.URI` within a custom constraint validator, which also verifies the allowed schemes (HTTP/HTTPS).

### Authorization

**A capability, not an identity.** *For links, the system has resources and no principals.*
- There are no accounts, so there is nothing to authenticate. Creating a link returns a 32-byte `SecureRandom` token, shown once; possession of it is the entire authorisation. The link stores only its SHA-256 digest.
- A JWT was the obvious alternative and buys nothing here: there is no subject to put in the claims, statelessness saves no work when the link has to be loaded from the database anyway, revocation would need a blocklist — which is the state the statelessness was supposed to avoid — and expiry is actively harmful for a QR printed on a wall for two years.
- The short code cannot be the credential. It travels in the URL and gets printed on signs. A public identifier is not a secret.

**SHA-256, not BCrypt.** *Slow hashing exists for low-entropy secrets.*
- BCrypt and Argon2 are deliberately slow because human passwords are guessable and the defence is making each guess expensive. 32 bytes from `SecureRandom` are not guessable at any speed, so the slowness would buy nothing and cost around 100 ms of CPU on every authorised request.
- No salt, for the same reason: a salt defends against precomputed tables over a small, predictable input space, and no table covers 2^256.
- It is the same reasoning GitHub applies to personal access tokens.

**403 for everything, and always the same 403.** *A missing token, a wrong token and a token for another link are one answer.*
- Not 401: a `WWW-Authenticate` challenge would have nothing to ask the client for.
- The comparison is `MessageDigest.isEqual`, constant time, so response timing does not leak how much of a guess was right.
- A link with no stored hash denies. The guard fails closed, and deleting it turns a test red, which is how we know the deny is load-bearing rather than decorative.
- The failing code goes to the log and not to the response body. The owner of the service can correlate it; the caller who has just proved they are not the owner of the link learns nothing. It is the one error in this API that does not publish its identifier, and the 404 and the 403 therefore look different on purpose.

**One operator credential, and only one.** *The health endpoint introduced the single principal in the system.*
- `SHORTLINK_ADMIN_TOKEN` authorises an operator, not a user. It grants no power over any link: it cannot create, edit, delete or read one. All it unlocks is the `status` field of `/api/health`.
- It is read from the environment with no default. A deployment that does not supply it rejects every attempt rather than answering freely, and a blank header cannot match a blank configured token — a test fails if that branch is removed.
- Rejections are indistinguishable from outside, so the response never reveals whether the deployment configured a token at all. The distinction is in the log, where the operator can see it and the caller cannot.
- The header is `X-Admin-Token`, deliberately not the one links use. Two credentials of different natures sharing a header name is a confusion waiting to happen.

**What the token does not cover.** *The redirect and the QR stay public.*
- Scanning is the product. If the redirect ever required a header, every printed code would stop working.
- The management read is behind the token, because it exposes the destination and the visit count.

**The upload authorises twice.** *Deliberate, not a leftover.*
- The controller authorises before reading the file bytes, so a rejected caller costs no image decoding — the cheap defence against someone firing large files at an endpoint they cannot use.
- The service authorises again when it attaches the logo. The endpoint stays correct if the controller's early check is ever removed, and the service method is safe to call from anywhere else.

**The token cannot be recovered or rotated.** *Only the digest is stored.*
- Lose it and the link is read-only for good. Rotation would be a new endpoint authorised by the very token being rotated: a reasonable next feature, not this one.

### Partial updates

**`PATCH`, not `PUT`.** *Updates are partial by nature.*
- `PUT` would mean sending the whole representation on every edit, including the fields the client does not care about. It is left unmapped, and the collection asserts the 405.

**An absent field means "leave it alone".** *Null is not an instruction.*
- The request record carries `null` for anything the client did not send, and the service only touches what it was given. A request that changes the destination must not silently clear the expiry.

**`clearExpiry`, not a sentinel date.** *The model can express it; the protocol cannot.*
- A null expiry already means "never expires", so sending `null` cannot also mean "remove it": absent and null are the same thing on the wire.
- The alternatives were worse. `9999-12-31` is not perpetual, it expires in 7973 years and leaves a lie in the column. `-1` is not a date at all. A `DELETE /api/links/{code}/expiry` sub-resource would work, but two paths to one state is two things to keep in agreement.
- Sending both a date and the flag is contradictory and rejected at the boundary. The service still resolves it deterministically — the flag wins — because a public method has to behave predictably even when something calls it without validation in front.

**`Boolean`, not `boolean`, in the request record.** *Spring Boot 4 ships Jackson 3, which changed a default.*
- Jackson 3 enables `FAIL_ON_NULL_FOR_PRIMITIVES`. An absent primitive in a record component is treated as null, the body fails to bind, and the client gets a 400 that says "Failed to read request" and names nothing.
- Disabling the feature globally would have fixed this one field and silenced a genuine error everywhere else. The record takes a `Boolean` and normalises absent to `false` in its compact constructor instead, so the rest of the application still receives a primitive and the check stays on for every other DTO.

### QR codes

**Error correction level H.** *Reed-Solomon error correction recovers around 30% of damaged codewords.*
- Normally this level is paid for in capacity, but since what is being encoded here is about 30 characters, there is plenty of capacity: the maximum level comes for free.
- This is what allows a logo to cover part of the symbol without breaking readability.

**The symbol is not stored.** *It is a deterministic function of the short URL.*
- The same input produces the same bytes, always. Storing it would be caching, not persistence. A test enforces this.

**Cached for five minutes, unlike the redirect.** *The symbol depends on the logo, and the logo can change.*
- It used to carry `max-age=31536000, immutable`, which was honest while the QR was a function of the code alone: the same code always produced the same bytes. Once the symbol depends on a logo that can be uploaded and removed, `immutable` is a promise the endpoint cannot keep — a client that fetched the QR before an upload would serve the old one for a year without ever asking again.
- Five minutes bounds that staleness without turning the endpoint into traffic. An `ETag` would be more precise, but the consumer of `/qr` is the owner of the link downloading an image to print, once: conditional requests would save bandwidth nobody is spending, and a tag computed wrong serves a stale image forever.
- The redirect still carries `no-cache`. Two endpoints in the same project with opposite policies, each for its own reason: the image changes rarely, the destination it points to must be re-read every time.

**Generating a QR is not scanning it.** *The endpoint reads with `getByCode`, never with `resolveCode`.*
- If it counted as a visit, the counter would go up every time someone opens the image to print it. A test prevents this.

**PNG, not JPEG.** *A lossless format is required.*
- Lossy compression blurs the edges between modules, and scanning begins to fail intermittently.

**The quiet zone is not configurable.** *Four modules, the minimum of the specification, as a constant.*
- The general rule: make configurable what someone might reasonably want to change. The image size does depend on the use case; the margin has a single correct value, and leaving it open only allows lowering it and breaking scanning without understanding why.

**Logo coverage is measured, not guessed.** *A centred white plate of 25% of the image width.*
- An experiment swept plate sizes from 0% to 70% of the image width over ten different codes, decoding every result. 32% was the last size where all ten decoded, 33% lost three of them, and nothing decoded from 38% on. The percentage is taken over the full image, quiet zone included, so slightly more of the symbol itself is covered.
- Production uses 25%, seven points below the measured ceiling. The margin is not timidity: the measurement decodes a perfect in-memory PNG with the most tolerant reader configuration available, while a real scan involves a phone camera, an angle, print quality and ambient light.
- Between 33% and 37% success depends on the *content* of the link, because the module pattern and the mask differ per code. A feature that works for four links out of ten, with no way for the user to know which, is worse than a smaller logo.
- Width, not area: 25% of the width covers about 6% of the surface. The ~30% figure for level H is a ceiling on recoverable codewords, not a budget for how much of the picture a logo may occupy.
- The logo fills 80% of the plate, not all of it. That remaining ring is white on purpose: a logo reaching the plate edge would put its dark pixels against the dark modules and blur the boundary the scanner depends on. That 80% is a visual criterion, not a measurement.
- The logo keeps its own aspect ratio. One scale factor, the smaller of the two candidates, is applied to both dimensions, so a 4:1 logo stays 4:1 and simply leaves more white on its short side.
- The experiment is kept, disabled, in `QrLogoCoverageExperiment`. If the base URL ever gets longer, the symbol gains a version, its capacity changes and the ceiling has to be measured again.

**The logo needs a colour canvas.** *`MatrixToImageWriter` returns a one-bit image.*
- `toBufferedImage` produces a `TYPE_BYTE_BINARY` canvas: one bit per pixel, two colours. Drawing a colour logo onto it quantises every pixel to pure black or pure white — a dark red seal came out solid black, a blue icon came out as a white silhouette — and it flattens the antialiased corners of the plate at the same time.
- `generateWithLogo` copies the symbol onto a `TYPE_INT_RGB` canvas first. `generate` does not: the plain QR stays one bit and its PNG is a fraction of the size, so only the path that needs colour pays for it.
- `RGB`, not `ARGB`: the symbol covers the canvas entirely, so the result is opaque and an unused alpha channel would only add weight. A logo with transparency resolves against the white plate, which is why the plate is painted before the logo.

### Logo uploads

**Uploaded logos are sanitized, not inspected.** *The only verifiable thing about an upload is its bytes.*
- The declared content type and the filename are written by the client. An executable renamed to `logo.png` declares `image/png` quite happily, and a valid PNG from a careless client may declare `application/octet-stream`. Both are hints, never guarantees.
- What decides is the reader lookup: `ImageIO.getImageReaders` picks a reader by sniffing the magic number in the header, so the verdict comes from the content and not from a label. Hand-rolling a table of byte prefixes would duplicate that and do it worse, missing the format variants the SPI already knows.
- The image is then decoded and re-encoded to PNG, and whatever was not pixel data does not survive the round trip. A test appends a PHP payload after the PNG `IEND` chunk — a file that is a valid image *and* a valid script, which is how a polyglot gets through a careless upload — and asserts it comes back out gone.
- Reading index 0 comes free with the same property: an animated GIF arrives animated and leaves as a static first frame, a multi-page TIFF leaves as its first page.

**The content type is checked even though it proves nothing.** *A filter for the honest mistake, not for the attacker.*
- Checking it costs nothing and turns an "unsupported image format" three layers down into "we do not accept application/zip" at the door. The attacker renames his file and sails past it; the user who picked the wrong file in a dialog gets told what is wrong. That second person is who the check is for.
- The allowed list is restricted to what this JVM can actually read, and a test asserts it against `ImageIO.getReaderMIMETypes()`. Accepting a type the decoder cannot handle would produce two different errors for one cause, and the user would understand neither.
- `image/svg+xml` is why the check is a list and not an `image/` prefix. An SVG is an image by content type and XML by content, so a prefix check would walk it straight to a decoder that cannot read it, and XML is a class of problem of its own.
- The comparison ignores case and tolerates parameters: `IMAGE/PNG` and `image/jpeg; charset=utf-8` are both legal HTTP, and rejecting them would be rejecting correct clients.

**Validation reads the header before it decodes.** *A 40 KB PNG can declare 30000x30000 pixels.*
- Decoding that first would ask the heap for several gigabytes and take the process down with one small request. So dimensions come from `getWidth(0)`/`getHeight(0)` on the reader, which parses the header only, and the pixels are decoded last.
- The same reader is reused for that decode rather than going back through `ImageIO.read`. Reopening the stream would re-parse the file, and it would also mean the bytes that were measured and the bytes that get decoded are not provably the same bytes.
- A malformed or truncated file raises an `IOException` during the decode — the header can promise 100x100 with the pixel data cut off halfway. It is wrapped as `InvalidLogoException`, because the bytes are the client's: a 400, not a 500. The same `IOException` maps to a different status depending on whose fault the bytes are, which is why `QrCodeGenerator` wraps its own into `QrGenerationException` instead.

**Upload size is limited at two layers, with two different numbers.** *The container's limit is not the application's limit.*
- `spring.servlet.multipart.max-file-size` and `max-request-size` are enforced by Tomcat before the controller method exists. Both are set, because the first caps each part and the second the whole body: with only the first, twenty parts of 1 MB each still arrive.
- `shortlink.logo.max-upload-bytes` repeats that limit inside the application, because a validator cannot depend on a framework property it does not own.
- `shortlink.logo.max-stored-bytes` is a larger number, checked after the re-encode. A 900 KB lossy JPEG becomes a lossless PNG of several megabytes, so a file that passed every input limit can still produce a stored image nobody wants. A single shared limit would reject it only after doing all the work.
- All four logo limits are properties rather than constants. Not only for configurability: injecting them is what lets a test exercise the dimension and size checks without fabricating an oversized fixture.

**The endpoint measures before it materialises.** *The same rule as the decoder, one layer up.*
- `getSize()` and `getContentType()` read the part's metadata and cost nothing; `getBytes()` copies the whole file into heap. The controller validates first and materialises second, so a rejected upload is never held in memory in order to announce that it is too big.
- The 413 for an oversized upload is the container's, not ours: Tomcat enforces `max-file-size` while reading the request, so the controller method is never entered. Verified with a real 5 MB upload rather than assumed — Spring already maps the exception, so no handler of ours was needed. It is also the only error in this API without a `type`, since that response is the framework's.
- The response is 200, not 201. Nothing is created at a new URL: an existing link gains a logo, and the logo has no address of its own. The body is the updated link, so the client learns `hasLogo` without a second request.

**Sanitising happens outside the transaction.** *Decoding an image is CPU work and has no business holding a database connection.*
- The controller validates and sanitises; the service method's `@Transactional` covers only loading the link and attaching the bytes.
- The other way round would hold a pooled connection for the whole decode and re-encode. Under concurrent uploads the pool empties while the database itself sits idle — the kind of incident where an hour goes into looking at the wrong machine.
- The ordering is validate, authorise, then sanitise: an unknown code or a bad token is rejected before the file is ever copied into the heap, let alone decoded. An earlier version decoded first and answered the 404 afterwards, which is what this bullet used to describe.
- What the split does cost is a second read. The controller's authorisation loads the link, and `attachLogo` loads and authorises it again inside the transaction. That is the price of the paragraph above: duplicated work in exchange for a service method that stays correct wherever it is called from.

### Testing

Four levels, each with a different cost and purpose:
- **Testcontainers** — a real PostgreSQL in Docker, for the migration, the column types and the constraints.
- **`@DataJpaTest`** — in-memory database, for the repository's derived queries.
- **Mockito** — no database, for the service's own decisions.
- **MockMvc** — simulated HTTP requests for the web layer.

**`method_condition_result` naming.** *A failing build should name the method that broke.*
- Most test classes cover several operations — `existsByCode`, `findByCode` and `registerVisitCount` share one class — so the method under test comes first.
- `with...` for a property of the input (`withUnknownCode`), `when...` for a state of the system (`whenRetriesAreExhausted`).
- The result is the observable outcome, including the status code in web-layer tests (`returns410WithFailingCode`).
- One exception: `ShortlinkQrApplicationTests.contextLoads()` has no method under test — it asserts that the Spring context starts — and it is the name Spring Initializr generates.

**Testing principles:**
- **Strict HTTP assertions:** The redirect test asserts `302 Found`, not a generic `3xx`. A `301` would ruin the retargeting promise, so the test must be as strict as the business rule.
- **Documented regressions:** Tests for specific bugs (e.g., URLs with spaces) include their origin story in the code. Without this context, future developers might delete them thinking they are redundant.
- **Isolated configuration:** `@WebMvcTest` explicitly pins the `shortlink.base-url` property. Tests must control their own inputs and not fail simply because a production configuration file was modified.
- **One assertion style, with one exception:** AssertJ across the suite, for semantic readability and to avoid mixing libraries. Two assertions in `ShortLinkControllerTest` use Hamcrest's `containsString` inside `header().string(...)`, checking the QR endpoint's `max-age=300` without pinning the rest of the header. That is the matcher API MockMvc offers for a header, and going around it would mean pulling the response apart by hand to say something less clear. Stating the rule with its exception beats stating it absolutely and being wrong.
- **Database integrity:** Failure-path tests also explicitly verify that no visit is recorded.
- **Round-trip testing:** *Generate, decode, and compare with the original URL.* This is the only assertion that proves the image can actually be scanned. It is also the instrument used to measure how far the logo can grow.
- **Decoding without the detector:** Round-trip tests decode with `PURE_BARCODE`. Without that hint ZXing locates the finder patterns and resamples through a perspective transform, and for some module patterns a sample point lands on a module boundary: one code in ten failed to decode from a perfectly generated PNG. The pre-existing round-trip test was green only because its URL happened to fall on the right side of that.
- **Persistence is proved, not assumed:** The Testcontainers tests clear the persistence context before reading back, and assert that the instance they read is not the instance they saved. Without that, Hibernate's first-level cache would satisfy every assertion against an empty database.
- **Constraint names, not just exception types:** The uniqueness test asserts that `uk_short_links_code` appears in the error. Asserting only `DataIntegrityViolationException` would pass for any integrity failure, including one coming from a different column.
- **Values chosen to expose the mapping:** The logo test uses bytes containing a zero and a high byte, so a column that stopped at the first null would fail. The expiry test uses microsecond precision, matching the column, because a value carrying nanoseconds would be silently truncated on the way in.
- **Guards are tested where they live:** The admin token verifier has its own tests, built by hand without Spring. The controller tests mock it, so they prove the controller reacts correctly to each verdict but say nothing about how the verdict is reached — a distinction that matters, because an inverted comparison inside the verifier once passed the entire controller suite.

## ⚠️ Known Limitations

- **JSON on public endpoints:** Scanning a dead, invalid, or expired QR code currently returns a JSON document in the browser. While correct for an API, it provides a poor experience for a human user. Pending: an HTML fallback page for the public redirect endpoint.
- **Problem Types are URNs:** The `type` fields in the error responses are URNs, not dereferenceable URLs. This is a conscious decision not to invent a documentation URL that does not actually exist.
- **The public health view does not prove the service works.** Without a token, `/api/health` reports that the process is running and nothing more, and `/api/health/ping` reports even less. If the database is unreachable, both still answer `200` while every redirect fails. This is deliberate — a health check that queried the database on every anonymous call would exhaust the database's monthly compute allowance — but it means the uptime badge measures liveness, not correctness.
- **The management GET loads the logo to answer a boolean.** `getByCode` fetches the whole entity, logo bytes included, and `hasLogo` is derived from it. A `@Basic(fetch = LAZY)` on a basic attribute does nothing without bytecode enhancement, so the blob travels on every read of a link that has one. The redirect path already avoids this with a projection; the management path would need its own.
- **The free hosting tier sleeps.** The application host suspends the service after fifteen minutes without traffic and takes about a minute to wake, so the first request after a quiet period is slow. The database suspends after five minutes and wakes in well under a second.

## 🛠️ Tech Stack
- **Java 21**
- **Spring Boot 4.1**
- **Spring Data JPA**
- **Flyway** (schema migrations, one set for every environment)
- **PostgreSQL** (production, hosted on Neon)
- **H2 Database** (file-based in development, in-memory in tests, both in PostgreSQL compatibility mode)
- **Docker** (multi-stage image build)
- **Render** (application hosting)
- **ZXing** (`core` for encoding, `javase` for the bridge to `BufferedImage`).
- **Testing Ecosystem:**
    - **JUnit 6** (managed by `spring-boot-dependencies` 4.1.1, not chosen explicitly)
    - **Testcontainers** (a real PostgreSQL for the persistence tests)
    - **Mockito** (Mocking domain logic without database overhead)
    - **AssertJ** (Fluent and semantic assertions)
    - **MockMvc** (HTTP layer simulation)
    - **Postman** (API contract validation)
- **GitHub Actions** (the suite runs on every pull request and on every push to `main`)

## 🚀 How to Run

Requirements: Java 21, and Docker running if you intend to execute the test
suite.

1. Clone the repository and navigate to the project folder:
   ```bash
   git clone https://github.com/m4r10kyou/shortlinkQr
   cd shortlinkQr
   ```
2. Run the Spring Boot application with the development profile:
    ```bash
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
    ```
   *(If you are using Windows, run `mvnw.cmd` instead).*

   The profile is not optional. No profile is active by default, so the
   application will not start without being told which environment it is in.

3. Once the application starts, it will be available at:
   ```text
   http://localhost:9080
   ```
4. Database Access:

   *You can inspect the local database using the H2 Console at:*
   ```text
   http://localhost:9080/h2-console
   ```
   *The JDBC URL in the console form must match the one in
   `application-dev.properties` exactly, or it will open a different, empty
   database. The console is enabled in `dev` only.*

5. Run the test suite:
   ```bash
   ./mvnw clean test
   ```
   *Docker must be running: the persistence tests start a PostgreSQL container.*

### Environment variables

The `prod` profile declares no fallback values, so every one of these must be
supplied by the deployment or the application will not start:

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Which environment this is. No default anywhere. |
| `DATABASE_URL` | JDBC URL, e.g. `jdbc:postgresql://host/db?sslmode=require` |
| `DATABASE_USERNAME` | |
| `DATABASE_PASSWORD` | |
| `SHORTLINK_BASE_URL` | The canonical origin that goes inside generated QR codes |
| `SHORTLINK_ADMIN_TOKEN` | Unlocks the `status` field of `/api/health`. Without it, that field is never served. |
| `PORT` | Assigned by the platform. Defaults to 9080 when absent. |

## 🧪 Testing the API

The collection at [`docs/shortlinkQr-API.postman_collection.json`](docs/shortlinkQr-API.postman_collection.json) works as an
executable specification. Import it into Postman and run it with the Collection
Runner: every request asserts its expected status and body, so the result is a
pass/fail report rather than something to inspect by eye.

Four requests upload a file from `docs/`, referenced relatively, so set
Postman's working directory (Settings → General) to the repository root before
running the collection.

Forty-one requests across seven folders:

- **Links API** — creation and retrieval, including the alias conflict. The
  generated code and the management token are captured into variables, so later
  requests need no manual copying.
- **Validation** — every constraint on the request DTO, each one checked against
  the field it should name in the `errors` map.
- **Protocol errors** — malformed JSON, unsupported method, unsupported media
  type. Before the advice extended `ResponseEntityExceptionHandler`, all three
  returned 500.
- **Redirect** — the public endpoint, asserting 302 specifically and that the
  response is not cacheable.
- **QR & Logo** — the QR endpoint before and after an upload, asserting that the
  branded symbol comes back as a different, larger image; plus a rejected file
  and an unknown code.
- **Authorization & edition** — the `PATCH` in its three valid shapes, plus the
  empty body and the contradictory one, both rejected at the boundary; 403 for a
  missing token and for a wrong one on the `PATCH`, and for a wrong one on the
  management read and the logo upload. One request fetches the QR without a
  header and asserts the 200: the symbol has to stay public.
- **Health** — `/api/health/ping`, and `/api/health` both without a header and
  with a wrong one. The authorised view is not here: a valid
  `SHORTLINK_ADMIN_TOKEN` cannot live in a versioned collection, and an empty
  variable would fail for anyone who clones the repository.

Last full run against an empty database: 41 requests, 91 assertions, 91 passed.
Two of those assertions live inside a conditional in request 02 and only run
the first time, when the custom alias is still free; a second pass reports 89.

## 📋 Project Status

- [x] **Project setup** — Spring Boot, JPA, H2, configuration
- [x] **Domain model and persistence**
    - [x] `ShortLink` entity with JPA mapping
    - [x] Repository with derived queries
    - [x] Atomic visit counter
- [x] **Core business logic**
    - [x] Base62 code generator with collision retry
    - [x] Link creation, resolution and retargeting
    - [x] Domain exceptions
- [x] **Authorization**
    - [x] Per-link management token, issued on creation
    - [x] Guarded mutations and management read
    - [x] Partial updates with `PATCH`
- [x] **REST API**
    - [x] Request/response DTOs with validation
    - [x] Management endpoints
    - [x] Public redirect endpoint
    - [x] HTTP error mapping
- [x] **QR code generation**
    - [x] QR rendering
    - [x] Logo embedding
    - [x] Logo upload and sanitization
- [x] **Deployment**
    - [x] Flyway migrations with `validate` in every profile
    - [x] PostgreSQL profile
    - [x] Multi-stage Dockerfile
    - [x] Persistence tests against a real PostgreSQL
    - [x] CI pipeline
    - [x] Health endpoint with an operator-authenticated view
    - [x] Deployed and reachable

### Next

- `DELETE /api/links/{code}` and `DELETE /api/links/{code}/logo`
- Management token rotation
- A projection for the management read, so a boolean does not cost a blob
- An HTML fallback for the public redirect
