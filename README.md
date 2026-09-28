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
- A sequential database id in the URL would let anyone walk the whole
  table by counting up
- It would also tie the public API contract to the persistence layer

**`SecureRandom`, not `Random`.** *Cryptographically secure generation ensures future codes cannot be guessed from observed outputs*
- `Random` is a linear congruential generator with a 48-bit seed
- A few observed outputs are enough to reconstruct its state and
  predict the next codes
- Same concern as above: nobody should be able to guess codes that
  have not been issued yet

**Full 62-character alphabet.** *Maximizing the address space takes priority over visual clarity, as QR scanning is the primary use case*
- Considered excluding visually ambiguous characters (0/O, 1/l/I) to
  reduce transcription errors when someone types a code from a printed
  sign
- Kept the full alphabet; the QR is the primary path and manual typing
  is the exception

**The generator does not know about the database.** *Collision checking is delegated to the caller to maintain a pure, easily testable domain service*
- `generate` takes a `Predicate<String>` that answers whether a code is
  taken; the caller decides how to check
- This keeps the component free of persistence and makes the collision
  path testable with a plain lambda

### Visit counting

**Atomic UPDATE, not read-modify-write.** *Delegating the increment operation to the database guarantees accuracy under concurrent traffic without unnecessary locking*
- Loading the entity, incrementing in Java and saving loses visits
  under concurrency: two requests read 40, both write 41
- `set visit_count = visit_count + 1 where code = ?` lets the database
  serialise it
- Optimistic and pessimistic locking would also be correct, but for a
  commutative increment they add retries or contention for nothing

### Domain modelling

**`Instant`, not `LocalDateTime`.** *Using absolute points on the timeline prevents critical bugs when evaluating expirations across different time zones*
- `LocalDateTime` carries no time zone
- An expiry computed in local time and compared on a server in another
  zone expires early or late

**Behaviour over setters.** *The entity encapsulates its own logic and state transitions to guarantee internal consistency at all times*
- The entity exposes `changeTargetUrl`, `attachLogo`, `removeLogo`
  instead of a setter per field
- `attachLogo` takes the bytes and the content type together, so the
  pair can never be left inconsistent

**Exceptions carry no HTTP semantics.** *Domain errors remain agnostic to the web transport layer, ensuring reusability across the different contexts*
- Plain `RuntimeException` subclasses, no `@ResponseStatus`
- Mapping to 404/409/410 belongs to the web layer
- Keeps the service usable from a batch job or a message consumer,
  where HTTP means nothing

### Testing

Three levels, each with a different cost and purpose:
- **`@DataJpaTest`** — real (in-memory) database, for the repository
- **Mockito** — no database, for the service's own decisions
- **MockMvc** — simulated HTTP requests, once the controller exists

Failure-path tests also verify that no visit is recorded.


## 🛠️ Tech Stack
* **Java 21**
* **Spring Boot 4.1**
* **Spring Data JPA**
* **H2 Database** (file-based, local development)

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
- [ ] **REST API**
   - [ ] Request/response DTOs with validation
   - [ ] Management endpoints
   - [ ] Public redirect endpoint
   - [ ] HTTP error mapping
- [ ] **QR code generation**
   - [ ] QR rendering
   - [ ] Logo embedding
- [ ] **Deployment**
   - [ ] Dockerfile
   - [ ] PostgreSQL profile
   - [ ] CI pipeline