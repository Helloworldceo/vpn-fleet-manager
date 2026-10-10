# VPN Fleet Manager

A Java/Spring Boot backend for managing a fleet of VPN servers ("nodes") and the client configs issued to users — the back-office a VPN service would run, not a VPN itself.

## Why this exists

Built as a focused demonstration of an enterprise Java backend stack (Java 17, Spring Boot, MyBatis, PostgreSQL, Redis, Docker, Nginx) applied to a real domain: VPN infrastructure, continuing the Xray-core/VLESS+REALITY work documented in this author's other projects. It is **not** connected to any real VPN server — see [Design decision: mocked provisioning](#design-decision-mocked-provisioning) below.

## Stack

- **Java 17 / Spring Boot 3**
- **MyBatis** (XML mappers) over **PostgreSQL**, schema managed by **Flyway**
- **Redis** (via Redisson) for a distributed lock around node-capacity allocation, and short-TTL caching of node availability
- **Spring Security + JWT**, stateless, two roles (`USER`, `ADMIN`)
- **Docker** multi-stage build; **docker-compose** wiring Postgres + Redis + the app + **Nginx** (reverse proxy with a real rate-limit zone on auth endpoints)
- **springdoc-openapi** (Swagger UI)

## Architecture

```
Client → Nginx (:80, rate-limited on /api/auth) → Spring Boot app (:8080)
                                                        │
                                   ┌────────────────────┼────────────────────┐
                                   │                     │                    │
                              PostgreSQL            Redis (lock +         VpnProvisioner
                           (users, nodes,            cache: nodes)        (Mock impl today)
                              clients)
```

`VpnClientService` takes a per-node Redisson lock before checking capacity, so concurrent requests against a nearly-full node can't over-allocate it — proven in `VpnClientConcurrencyTest` by firing 10 concurrent issuance requests at a capacity-1 node and asserting exactly one succeeds. The actual DB write lives in a separate bean (`VpnClientProvisioningTx`) so its `@Transactional` goes through a real Spring proxy — calling a `@Transactional` method on `this` from within the same class would silently skip the proxy entirely, a classic Spring AOP pitfall this avoids deliberately.

## Design decision: mocked provisioning

`VpnProvisioner` is an interface; the only implementation wired up is `MockVpnProvisioner`, which generates realistic VLESS+REALITY-style config strings in-process and makes no network calls. This is deliberate: it keeps this repository safe to make public without ever being able to touch a real personal VPN server or needing its credentials. A real implementation (e.g. one that calls a 3x-ui-style panel API over HTTPS) could implement the same interface and be swapped in via Spring configuration — the pluggable boundary is the point.

## Running it

```bash
cp .env.example .env   # set a real JWT_SECRET
docker compose up --build
```

Then:
- API: `http://localhost/api/...`
- Swagger UI: `http://localhost/swagger-ui.html`

Two nodes are seeded on first boot (see `V1__init.sql`) so `/api/nodes` and client issuance work immediately.

### API walkthrough

```bash
curl -X POST localhost/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"demo@example.com","password":"password123"}'

TOKEN=$(curl -s -X POST localhost/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"demo@example.com","password":"password123"}' | jq -r .token)

curl localhost/api/nodes -H "Authorization: Bearer $TOKEN"
curl -X POST localhost/api/clients -H "Authorization: Bearer $TOKEN"
curl localhost/api/clients/me -H "Authorization: Bearer $TOKEN"
```

Admin endpoints (`/api/admin/**`) require a user with `role = ADMIN`; promote one directly in Postgres for local testing: `UPDATE users SET role = 'ADMIN' WHERE email = '...';`.

### Running the test suite

The Docker image build skips tests (`-DskipTests`) to keep image builds fast and independent of Docker-in-Docker concerns. The integration and concurrency tests use **Testcontainers**, which needs access to a Docker daemon — run them via Docker-outside-of-Docker, mounting the host socket into a throwaway Maven container:

```bash
docker run --rm \
  -v "$(pwd)":/app -w /app \
  -v /var/run/docker.sock:/var/run/docker.sock \
  maven:3.9-eclipse-temurin-17 mvn test
```

This runs the Mockito-based unit tests (`VpnClientServiceTest`, `MockVpnProvisionerTest`) and the Testcontainers-backed integration tests (`AuthFlowIntegrationTest`, `VpnClientConcurrencyTest`) against real, ephemeral Postgres and Redis containers.

## Deploying to Google Cloud (not done in this build)

This project runs anywhere Docker Compose does. To deploy to GCP specifically:
1. **Cloud Run** (simplest): push the app image to Artifact Registry, run it as a Cloud Run service, point it at a **Cloud SQL for PostgreSQL** instance and a **Memorystore for Redis** instance via the Cloud SQL Auth Proxy / VPC connector respectively.
2. **Compute Engine**: provision a VM, install Docker, copy this repo, run `docker compose up -d` — closest to local behavior.

Not executed as part of this build since it requires the account owner's own `gcloud` authentication and billing project.

## Project layout

```
src/main/java/com/vpnfleet/
  config/        Security, Redisson, and cache configuration
  controller/    REST endpoints
  dto/           Request/response records
  entity/        Domain objects (plain, MyBatis-mapped — no JPA/Hibernate)
  exception/     ApiException + a @RestControllerAdvice
  mapper/        MyBatis mapper interfaces (XML in resources/mapper)
  provisioning/  VpnProvisioner interface + MockVpnProvisioner
  security/      JWT issuing/validation, the auth filter, current-user helper
  service/       Business logic, including the distributed-lock allocation flow
```
