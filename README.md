# Short URL & QR Code Generator

A backend service that converts long URLs into trackable short links and customizable QR codes.

> 🚧 **Note:** This project is currently under construction.

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

### Domain modelling

**`Instant`, not `LocalDateTime`.** *Using absolute points on the timeline prevents critical bugs when evaluating expirations across different time zones*
- `LocalDateTime` carries no time zone
- An expiry computed in local time and compared on a server in another zone expires early or late

**Behaviour over setters.** *The entity encapsulates its own logic and state transitions to guarantee internal consistency at all times*
- The entity exposes `changeTargetUrl`, `attachLogo`, `removeLogo` instead of a setter per field
- `attachLogo` takes the bytes and the content type together, so the pair can never be left inconsistent

**Exceptions carry no HTTP semantics.** *Domain errors remain agnostic to the web transport layer, ensuring reusability across the different contexts*
- Plain `RuntimeException` subclasses, no `@ResponseStatus`
- Mapping to 404/409/410 belongs to the web layer
- Keeps the service usable from a batch job or a message consumer, where HTTP means nothing

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

### Testing

Three levels, each with a different cost and purpose:
- **`@DataJpaTest`** — real (in-memory) database, for the repository.
- **Mockito** — no database, for the service's own decisions.
- **MockMvc** — simulated HTTP requests for the web layer.

**Testing principles:**
- **Strict HTTP assertions:** The redirect test asserts `302 Found`, not a generic `3xx`. A `301` would ruin the retargeting promise, so the test must be as strict as the business rule.
- **Documented regressions:** Tests for specific bugs (e.g., URLs with spaces) include their origin story in the code. Without this context, future developers might delete them thinking they are redundant.
- **Isolated configuration:** `@WebMvcTest` explicitly pins the `shortlink.base-url` property. Tests must control their own inputs and not fail simply because a production configuration file was modified.
- **Unified assertion style:** AssertJ is used exclusively across the suite to maintain semantic readability and avoid mixing assertion libraries.
- **Database integrity:** Failure-path tests also explicitly verify that no visit is recorded.

## ⚠️ Known Limitations

- **JSON on public endpoints:** Scanning a dead, invalid, or expired QR code currently returns a JSON document in the browser. While correct for an API, it provides a poor experience for a human user. Pending: an HTML fallback page for the public redirect endpoint.
- **Problem Types are URNs:** The `type` fields in the error responses are URNs, not dereferenceable URLs. This is a conscious decision not to invent a documentation URL that does not actually exist.
- **No mutations yet:** There are no `PATCH` or `DELETE` endpoints. The short code is public by design (it is printed on signs), so it cannot serve as a credential to modify the link. An authorization mechanism must be built first, and is planned.

## 🛠️ Tech Stack
- **Java 21**
- **Spring Boot 4.1**
- **Spring Data JPA**
- **H2 Database** (file-based, local development)
- **Testing Ecosystem:**
  - **JUnit 5**
  - **Mockito** (Mocking domain logic without database overhead)
  - **AssertJ** (Fluent and semantic assertions)
  - **MockMvc** (HTTP layer simulation)
  - **Postman** (API contract validation)
  - 
## 🚀 How to Run

You can easily start the application from your terminal using the Maven wrapper.

1. Clone the repository and navigate to the project folder:
   ```bash
   git clone https://github.com/m4r10kyou/shortlinkQr
   cd shortlinkQr
   ```
2. Run the Spring Boot application:
    ```bash
    ./mvnw spring-boot:run
    ```
   *(If you are using Windows, run `mvnw.cmd spring-boot:run` instead).*


3. Once the application starts, it will be available at:
   ```text
   http://localhost:9080
   ```
4. Database Access:
   
   *You can inspect the local database using the H2 Console at:*
   ```text
   http://localhost:9080/h2-console 
   ```  

## 🧪 Testing the API

The collection at `docs/shortlinkQr.postman_collection.json` works as an
executable specification. Import it into Postman and run it with the Collection
Runner: every request asserts its expected status and body, so the result is a
pass/fail report rather than something to inspect by eye.

Twenty-three requests across four folders:

- **Links API** — creation and retrieval, including the alias conflict. The
  generated code is captured into a variable, so later requests need no manual
  copying.
- **Validation** — every constraint on the request DTO, each one checked against
  the field it should name in the `errors` map.
- **Protocol errors** — malformed JSON, unsupported method, unsupported media
  type. Before the advice extended `ResponseEntityExceptionHandler`, all three
  returned 500.
- **Redirect** — the public endpoint, asserting 302 specifically and that the
  response is not cacheable.

Last full run: 46 assertions, 46 passed.

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
- [x] **REST API**
   - [x] Request/response DTOs with validation
   - [x] Management endpoints
   - [x] Public redirect endpoint
   - [x] HTTP error mapping
- [ ] **QR code generation**
   - [ ] QR rendering
   - [ ] Logo embedding
- [ ] **Deployment**
   - [ ] Dockerfile
   - [ ] PostgreSQL profile
   - [ ] CI pipeline