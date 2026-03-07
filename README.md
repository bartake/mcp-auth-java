# MCP Auth — Java port

Java 17 + Maven port of [`mcp-auth-example`](../mcp-auth-example) (Node): mock login → JWT → MCP JSON-RPC (`/mcp`) with request-scoped user context → downstream API with role checks.

## Modules

| Module | Port | Description |
|--------|------|-------------|
| `auth-service` | 4000 | `POST /login`, OpenID discovery stub |
| `mcp-server` | 4001 | `POST /mcp` (JWT required), tools `get_user_info`, `fetch_downstream_data` |
| `downstream-api` | 4002 | `GET /public`, `GET|POST /data` (JWT + roles) |
| `ui-server` | 4003 | Static UI + `/api/login`, `/api/mcp`, `/api/data` proxies |
| `agent` | — | Fat JAR CLI (shade): login + MCP demo |
| `common` | — | Shared JWT (jjwt), demo users, ports |

## Prerequisites

- **JDK 17+**
- **Maven:** Not required on `PATH`. This repo includes the **Maven Wrapper** (`mvnw` / `mvnw.cmd`); the first run downloads Maven **3.9.9** into your user `.m2/wrapper` cache.

## Tests and coverage

**Windows (PowerShell / cmd):**

```bat
cd mcp-auth-java
.\mvnw.cmd verify
```

**macOS / Linux:**

```bash
cd mcp-auth-java
chmod +x mvnw
./mvnw verify
```

If `mvn` is installed globally, you can use `mvn verify` instead.

- **Unit tests:** `common` (`JwtServiceTest`), `auth-service` (`LoginControllerTest` with `@WebMvcTest`).
- **Coverage:** JaCoCo runs on the `verify` phase. Open per-module HTML reports at `*/target/site/jacoco/index.html` (for example `common/target/site/jacoco/index.html`, `auth-service/target/site/jacoco/index.html`).

## Build

**Windows:** `.\mvnw.cmd clean package -DskipTests`  
**Unix:** `./mvnw clean package -DskipTests`

Artifacts under each module’s `target/`:

- `auth-service/target/auth-service-1.0.0.jar`
- `mcp-server/target/mcp-server-1.0.0.jar`
- `downstream-api/target/downstream-api-1.0.0.jar`
- `ui-server/target/ui-server-1.0.0.jar`
- `agent/target/agent-1.0.0.jar` (runnable fat JAR)

## Run (four terminals)

Bind addresses are **127.0.0.1** (see each `application.properties`).

```bash
java -jar auth-service/target/auth-service-1.0.0.jar
java -jar downstream-api/target/downstream-api-1.0.0.jar
java -jar mcp-server/target/mcp-server-1.0.0.jar
java -jar ui-server/target/ui-server-1.0.0.jar
```

Then open **http://127.0.0.1:4003/** or run the agent (with the three backend JARs up):

```bash
java -jar agent/target/agent-1.0.0.jar alice pass123
```

## Configuration

- **JWT secret:** `mcp-auth.jwt.secret` or env `JWT_SECRET` (must match across all services).
- **Demo users:** same as Node — `alice` / `bob` / `charlie`, password `pass123`.

## Differences from Node

- User context uses **`ThreadLocal`** (`UserContextHolder`) per request instead of Node `AsyncLocalStorage`.
- Same ports and JSON-RPC behavior for the demo agent.
