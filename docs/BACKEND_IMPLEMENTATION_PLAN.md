# LearnMate AI — Backend Implementation Plan

## Audit Findings (Stage 0 — 2026-10-24)

### Repository Structure
- Root: `learnmate-backend/`
  - `backend/learnmate-backend/` — Spring Boot project
  - `frontend/` — React + TypeScript (Vite, React 19, react-router-dom 7)
  - `docs/` — empty at audit time

### Backend: What Exists
| Item | Status |
|---|---|
| Spring Boot scaffold | ✅ Generated |
| `LearnmateBackendApplication.java` | ✅ Entry point only |
| `application.properties` | ⚠️ Only `spring.application.name` |
| `LearnmateBackendApplicationTests.java` | ⚠️ Empty context-load test |
| Controllers | ❌ None |
| Services | ❌ None |
| Repositories | ❌ None |
| Entities | ❌ None |
| DTOs | ❌ None |
| Security config | ❌ None |
| Exception handling | ❌ None |
| Database config | ❌ None |
| Migration strategy | ❌ None |
| Tests | ❌ Only scaffold test |

### Technology Stack (Verified)
- **Java**: 21.0.10 (OpenJDK) ✅ matches `pom.xml` requirement
- **Spring Boot**: 4.1.1 (cutting-edge; note: Spring Boot 4.x requires Java 21+) ✅
- **Build**: Maven Wrapper 3.3.4 → downloads Maven 3.10.0 (**requires internet — unavailable in this environment**)
- **Dependencies declared**: spring-boot-starter-data-jpa, security, validation, webmvc, devtools, mysql-connector-j, test starters
- **Missing from pom.xml**: JWT library, Lombok (optional), OpenAPI/Swagger, any migration tool

### Frontend (API Contract Reference)
- **API base URL**: `http://localhost:8080/api/v1`
- **HTTP client**: axios 1.x
- **Auth**: Not yet implemented in frontend — no token headers, interceptors, or auth state
- **Pages**: LandingPage only; no feature pages yet
- **Router**: React Router v7

### Critical Issue: pom.xml Dependencies
The test-scoped entries `spring-boot-starter-data-jpa-test`, `spring-boot-starter-security-test`,
`spring-boot-starter-validation-test`, and `spring-boot-starter-webmvc-test` are **not real
Spring Boot starter names**. They will cause Maven to fail to resolve them (no such artifacts exist).
These must be replaced with the correct test dependencies: `spring-boot-starter-test` and
`spring-security-test`.

### Build Environment Constraint
Network egress is disabled in this environment. Maven cannot download dependencies from Maven
Central. **We will write all source code files, fix the pom.xml, and document the exact build
commands. The user must run the Maven build locally or in CI where network is available.**

---

## Architecture Decision

Use layered architecture:
```
Controller (HTTP) → Service (business logic) → Repository (JPA) → MySQL
```

Package structure: `com.learnmate.backend.<domain>.<layer>`

Domains:
- `auth` — registration, login, JWT
- `user` — profiles, account management
- `exam` — code studio, quiz, mock exam
- `peer` — sessions, confirmation, feedback
- `reward` — ledger, transactions
- `progress` — streaks, XP, achievements
- `course` — catalogue, redemption
- `ai` — tutor conversations
- `notes` — library
- `community` — Q&A
- `admin` — administration
- `common` — security, exceptions, response wrappers, config

---

## Implementation Stages

| Stage | Description | Status |
|---|---|---|
| 0 | Audit and plan | ✅ Complete |
| 1 | Foundation: pom.xml fix, security config, exception handling, test profile, application.properties | 🔄 In progress |
| 2 | Auth + User: entities, JWT, registration, login, profile | ⬜ |
| 3 | Exam Centre: code studio, quiz, mock exam | ⬜ |
| 4 | Peer Learning + Reward ledger | ⬜ |
| 5 | Progress Centre | ⬜ |
| 6 | Course Rewards Store | ⬜ |
| 7 | AI Tutor, Notes, Community | ⬜ |
| 8 | Admin | ⬜ |
| 9 | Integration, QA, docs | ⬜ |

---

## External Services Required (Not Configurable Without Credentials)

| Service | Purpose | Notes |
|---|---|---|
| MySQL | Primary database | Must be provided by operator |
| Code execution judge | Code Studio execution | Judge0, Sphere Engine, or self-hosted isolate. Interface will be implemented; credentials required |
| AI provider | Tutor conversations | Anthropic Claude or OpenAI. Interface will be implemented; key required |
| File storage | Notes library uploads | Local filesystem (dev) or S3-compatible (prod). Config required |

---

## Commands to Run Backend (Once Built)

```bash
cd backend/learnmate-backend

# Provide environment variables (copy .env.example and fill in values)
export DB_URL=jdbc:mysql://localhost:3306/learnmate
export DB_USERNAME=learnmate_user
export DB_PASSWORD=your_password
export JWT_SECRET=your-256-bit-secret-here
export JWT_EXPIRY_MS=86400000

# Build
./mvnw clean package -DskipTests

# Run
java -jar target/learnmate-backend-0.0.1-SNAPSHOT.jar

# Or dev mode
./mvnw spring-boot:run
```
