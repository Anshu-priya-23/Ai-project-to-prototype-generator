# AI Project-to-Prototype Generator

AI Project-to-Prototype Generator is a microservice-based web application that turns a project name and detailed description into a saved, interactive product prototype. The generated specification includes a project overview, features, user roles, navigation, screens, sample UI content, and project-specific technology recommendations.

The React frontend presents each generated screen as a navigable desktop preview. Local Docker development runs prototype generation asynchronously through RabbitMQ. The free Render deployment uses a synchronous service-to-service HTTP fallback because free private services, background workers, and persistent disks are unavailable. Both modes store project data, generation status, and generated JSON in PostgreSQL.

## Features

- Register and sign in with email and password.
- Create and list projects associated with an owner email.
- Generate prototypes asynchronously in local Docker or synchronously on free Render.
- Track generation through `Queued`, `Generating`, `Completed`, and `Failed` states.
- Generate description-specific product specifications containing:
  - project overview;
  - key features;
  - user roles;
  - application screens;
  - navigation flows;
  - UI components and sample content;
  - a categorized recommended technology stack with reasons.
- Navigate between generated screens in an interactive desktop preview.
- Browse technology recommendations using Frontend, Backend, Database, AI / Integrations, and Tools & Deployment tabs.
- Hide AI / Integrations recommendations when they are not relevant to the generated project.
- Regenerate and persist a project's latest prototype specification.

## Architecture

```text
React + Vite (localhost:5173)
          |
          | /api via Vite proxy
          v
API Gateway (localhost:8080)
    |              |                 |
    v              v                 v
Auth Service   Project Service    AI Service
   :8081           :8082             :8083
    |              |  ^               |
    v              |  |               |
PostgreSQL         |  +--- results ---+
                   |                  ^
                   +--- requests -----+
                         RabbitMQ
```

| Component | Responsibility |
| --- | --- |
| **Frontend** | React user interface for authentication, project creation, status polling, interactive prototype rendering, and technology-stack tabs. Browser API requests use the Vite `/api` proxy. |
| **API Gateway** | Single HTTP entry point that routes authentication, project, and AI service requests to their Docker network destinations. |
| **Auth Service** | Registers users, authenticates credentials, stores users in PostgreSQL, and issues JWTs. |
| **Project Service** | Persists projects and prototype state. It uses RabbitMQ in local async mode and calls AI Service over HTTP in Render sync mode. |
| **AI Service** | Generates and validates structured Gemini responses through either its HTTP endpoint or its local RabbitMQ consumer. |
| **RabbitMQ** | Carries durable local-development request and result messages; it is not deployed to Render. |
| **PostgreSQL** | Stores application users, projects, prototype statuses, errors, and generated prototype JSON. |

## Tech Stack

| Layer | Technologies verified in the repository |
| --- | --- |
| Frontend | React 19, Vite 8, Axios, CSS, Tailwind CSS/PostCSS configuration |
| Gateway | Java 21, Spring Boot, Spring Cloud Gateway |
| Authentication | Java 21, Spring Boot, Spring Security, Spring Data JPA, JJWT, PostgreSQL |
| Project management | Java 21, Spring Boot Web, Spring Data JPA, Spring AMQP, PostgreSQL |
| AI generation | Java 21, Spring Boot Web, Spring AMQP, LangChain4j OpenAI client, Gemini OpenAI-compatible API |
| Messaging | RabbitMQ 3 with the management plugin |
| Persistence | PostgreSQL 15 |
| Runtime | Docker Compose; Node.js and npm for the frontend development server |

## Prototype Generation Modes

Local Docker Compose explicitly sets `PROTOTYPE_GENERATION_MODE=async`:

1. The frontend saves a project through the API Gateway.
2. A new project is stored with `NOT_STARTED` status.
3. The user selects **Generate prototype**.
4. Project Service changes the project to `QUEUED` and publishes JSON to the durable `prototype-generation-requests` queue.
5. AI Service consumes the request and publishes a `GENERATING` result.
6. AI Service builds a prompt from that project's name and description, requests a structured specification, and validates the returned screens and recommended stack.
7. AI Service publishes either:
   - `COMPLETED` with the generated specification; or
   - `FAILED` with an error message.
8. Project Service consumes the result from `prototype-generation-results` and persists it.
9. The frontend polls the project list while work is queued or generating, then renders the saved specification after completion.

The prototype specification is stored in the project's `prototypeSpec` field as JSON, so screens and recommendations remain available after a browser refresh.

Render sets `PROTOTYPE_GENERATION_MODE=sync`. Project Service marks the project `GENERATING`, calls `POST /api/v1/ai/prototype` using the public HTTPS `AI_SERVICE_BASE_URL`, and saves the returned `COMPLETED` or `FAILED` result before responding. RabbitMQ listeners, queues, and health checks are inactive in this mode.

## Prerequisites

- Docker Desktop or another Docker installation with Docker Compose support.
- Node.js and npm compatible with the repository's Vite 8 frontend.
- A valid API key for the configured Gemini OpenAI-compatible API.

Java and Maven do not need to be installed locally when the backend is built with Docker; each service image builds its own application JAR using the included Maven Wrapper.

## Environment Configuration

Copy the committed variable template and supply local-only values. `.env` and all `.env.*` files except `.env.example` are ignored by Git.

```powershell
Copy-Item .env.example .env
```

```bash
cp .env.example .env
```

| Variable | Required | Purpose |
| --- | --- | --- |
| `AI_API_KEY` | Yes | Authenticates AI Service requests to the configured Gemini endpoint. |
| `AI_MODEL` | No | Overrides the model; the default is `gemini-3.6-flash`. |
| `PROTOTYPE_GENERATION_MODE` | No | `async` for local Compose; Render sets `sync`. |
| `JWT_SECRET` | No locally | Signs authentication tokens. Compose has a local-only default; Render generates a production secret. |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | No locally | Optional overrides; Compose defaults match the original `enterprise_db` local volume. |
| `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS`, `RABBITMQ_ERLANG_COOKIE` | No locally | Optional overrides; Compose supplies nonblank local defaults. |
| `CORS_ALLOWED_ORIGIN` | No | Allowed browser origin; defaults locally to `http://localhost:5173`. |
| `VITE_API_BASE_URL` | Production | Public Gateway URL ending in `/api`; local development uses the Vite proxy when unset. |
| `AUTH_SERVICE_BASE_URL` | Render Gateway | Public HTTPS origin of Auth Service; Docker uses `http://auth-service:8081`. |
| `PROJECT_SERVICE_BASE_URL` | Render Gateway | Public HTTPS origin of Project Service; Docker uses `http://project-service:8082`. |
| `AI_SERVICE_BASE_URL` | Render Gateway and Project Service | Public HTTPS origin of AI Service; Docker uses `http://ai-service:8083`. |

For local Docker, `.env` only needs `AI_API_KEY`. Never commit real values. Render generates internal credentials where possible and prompts for deployment-specific values.

## Local Setup

### 1. Start the backend infrastructure and services

From the repository root:

```powershell
docker compose up --build -d
```

This builds and starts PostgreSQL, RabbitMQ, Auth Service, Project Service, AI Service, and the API Gateway on the shared `enterprise-network` Docker network, using values from `.env`.

Check container status:

```powershell
docker compose ps
```

Follow service logs when troubleshooting:

```powershell
docker compose logs -f api-gateway auth-service project-service ai-service
```

### 2. Start the React development server

In a second terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Vite proxies browser requests beginning with `/api` to the API Gateway at `http://localhost:8080`, avoiding a separate browser CORS configuration.

### 3. Stop the backend

From the repository root:

```powershell
docker compose down
```

The named PostgreSQL volume is retained by this command, so saved application data remains available the next time the stack starts.

## Service URLs and Ports

| Service | Host URL or port | Notes |
| --- | --- | --- |
| Frontend development server | [http://localhost:5173](http://localhost:5173) | Started separately with `npm run dev`. |
| API Gateway | [http://localhost:8080](http://localhost:8080) | Public backend entry point used by the Vite proxy. |
| RabbitMQ management UI | [http://localhost:15672](http://localhost:15672) | Exposed by Docker Compose. |
| PostgreSQL | `localhost:5433` | Host port mapped to PostgreSQL's container port `5432`. |
| Auth Service | Internal port `8081` | Accessible to the Gateway inside the Docker network. |
| Project Service | Internal port `8082` | Accessible to the Gateway inside the Docker network. |
| AI Service | Internal port `8083` | Accessible to the Gateway inside the Docker network. |
| RabbitMQ broker | Internal port `5672` | Used by Project Service and AI Service inside Docker. |

## API Endpoints

All browser-facing API calls go through the API Gateway at `http://localhost:8080`.

| Method | Endpoint | Purpose | Main request data |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Register a user and receive a JWT. | `firstname`, `lastname`, `email`, `password`; `role` is optional. |
| `POST` | `/api/v1/auth/authenticate` | Authenticate a user and receive a JWT. | `email`, `password` |
| `GET` | `/api/v1/projects?ownerEmail={email}` | List projects for the supplied owner email, newest first. | Required `ownerEmail` query parameter |
| `POST` | `/api/v1/projects` | Save a new project with `NOT_STARTED` status. | `name`, `description`, `ownerEmail` |
| `POST` | `/api/v1/projects/{id}/prototype` | Queue or requeue prototype generation for a project. Returns the project with `QUEUED` status. | Project ID in the path |
| `GET` | `/api/v1/ai/status` | Return the AI service readiness response. | None |

Authentication responses contain `token` and `email`. Project responses contain the project metadata, `prototypeStatus`, saved `prototypeSpec`, and `prototypeError` when generation fails.

Backend health is available directly on every Spring service at `/actuator/health`. The public Gateway check is [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).

## Deploy to Render for Free

The root [`render.yaml`](render.yaml) creates one free static site and four free Docker web services. It references the existing free Render PostgreSQL database named `prototype-db`; it creates no database, private service, worker, RabbitMQ instance, or disk.

All four backend services must use the same region as `prototype-db`. If the existing database is not in Render's default region, add its supported `region` value to every Docker web service in `render.yaml` before committing.

### Dashboard steps

1. Push this repository to GitHub, GitLab, or Bitbucket.
2. Confirm in the target Render workspace that the existing free database is named exactly `prototype-db`, and note its region.
3. Select **New > Blueprint**, connect the repository, keep the Blueprint path as `render.yaml`, and select **Apply**.
4. At the environment-variable prompts, enter the values in the table below. Render generates `JWT_SECRET` and wires the database connection automatically. Backend-to-backend traffic uses the configured public HTTPS service origins.
5. Wait for all five resources to be created. Copy the actual `onrender.com` URLs shown for `prototype-frontend` and `prototype-api-gateway`.
6. Open **prototype-api-gateway > Environment**, set `CORS_ALLOWED_ORIGIN` to the exact frontend origin with no trailing slash, and save/redeploy.
7. Open **prototype-frontend > Environment**, set `VITE_API_BASE_URL` to the exact Gateway URL plus `/api`, save, then select **Manual Deploy > Clear build cache & deploy** because Vite embeds this value at build time.
8. Verify `https://<gateway-host>/actuator/health` and `https://<gateway-host>/api/v1/ai/status`. Then test registration, login, project creation, generation, preview navigation, tech-stack tabs, logout, login again, and saved-project loading.

| Service | Variable | Exact value at the Blueprint prompt |
| --- | --- | --- |
| `prototype-ai-service` | `AI_API_KEY` | A real Gemini API key; keep it secret. |
| `prototype-api-gateway` | `AUTH_SERVICE_BASE_URL` | `https://prototype-auth-service.onrender.com` |
| `prototype-api-gateway` | `PROJECT_SERVICE_BASE_URL` | `https://prototype-project-service.onrender.com` |
| `prototype-api-gateway` | `AI_SERVICE_BASE_URL` | `https://prototype-ai-service.onrender.com` |
| `prototype-api-gateway` | `CORS_ALLOWED_ORIGIN` | Initially `https://prototype-frontend.onrender.com`, then the actual frontend origin. |
| `prototype-project-service` | `AI_SERVICE_BASE_URL` | `https://prototype-ai-service.onrender.com` |
| `prototype-frontend` | `VITE_API_BASE_URL` | Initially `https://prototype-api-gateway.onrender.com/api`, then the actual Gateway URL plus `/api`. |

The Blueprint supplies the remaining production values:

| Service | Variable | Value/source |
| --- | --- | --- |
| Auth | `DATABASE_URL` | Existing `prototype-db` private connection string |
| Auth | `JWT_SECRET` | Render-generated secret |
| Project | `DATABASE_URL` | Existing `prototype-db` private connection string |
| Project | `PROTOTYPE_GENERATION_MODE` | `sync` |
| Project | `RABBIT_HEALTH_ENABLED` | `false` |
| AI | `PROTOTYPE_GENERATION_MODE` | `sync` |
| AI | `RABBIT_HEALTH_ENABLED` | `false` |
| AI | `AI_MODEL` | `gemini-3.6-flash` |

### Free-tier limitations

- Free web services spin down after 15 minutes without incoming traffic. A first workflow after inactivity can be slow because multiple services may wake independently.
- A workspace receives 750 free web-service instance hours per month. Four simultaneously active backend services consume those hours independently, so this is a lightly used portfolio demo rather than an always-busy production system.
- A free PostgreSQL database is limited to 1 GB, has no backups or managed connection pooling, and expires 30 days after creation. Export or recreate demo data before expiry.
- Free web services have ephemeral filesystems and cannot attach persistent disks. Durable application data belongs in PostgreSQL.
- All backend components are web services and service-to-service calls use their public HTTPS Render URLs. This is not security-hardened production infrastructure.
- Generation is one synchronous HTTP request in the free deployment. A cold start, AI-provider latency, provider quota, or upstream timeout can fail it; the project records `FAILED` and can be regenerated.
- Render's included bandwidth/build-minute quotas and the Gemini API's own quotas still apply.

## Project Structure

```text
enterprise-ai-platform/
├── api-gateway/
│   ├── src/main/java/com/enterprise/gateway/
│   ├── src/main/resources/application.yml
│   ├── Dockerfile
│   └── pom.xml
├── auth-service/
│   ├── src/main/java/com/enterprise/auth/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   ├── Dockerfile
│   └── pom.xml
├── project-service/
│   ├── src/main/java/com/enterprise/project/
│   │   ├── controller/
│   │   ├── entity/
│   │   ├── messaging/
│   │   └── repository/
│   ├── Dockerfile
│   └── pom.xml
├── ai-service/
│   ├── src/main/java/com/enterprise/ai/
│   │   ├── controller/
│   │   └── messaging/
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── App.jsx
│   │   ├── index.css
│   │   └── main.jsx
│   ├── package.json
│   └── vite.config.js
├── docker-compose.yml
└── README.md
```

## Screenshots

### Authentication

> Screenshot placeholder: registration and sign-in screen.

### Project Workspace

> Screenshot placeholder: project creation form, saved projects, and generation status.

### Interactive Prototype

> Screenshot placeholder: desktop prototype preview with screen navigation and Product Brief.

### Recommended Tech Stack

> Screenshot placeholder: project-specific technology recommendation tabs and cards.

## Future Enhancements

The following are potential improvements and are not part of the current implementation:

- Validate JWTs at the API Gateway and derive project ownership from authenticated identity instead of a request parameter.
- Add request validation and consistent API error responses.
- Replace automatic Hibernate schema updates with versioned database migrations.
- Add project editing, deletion, sharing, and prototype version history.
- Export generated specifications or previews to portable formats.
- Add automated integration tests for RabbitMQ processing and AI response validation.
- Add centralized logs, metrics, and distributed tracing.

## Security Notes

- Never commit `AI_API_KEY` or other credentials.
- Use environment-specific secrets and database credentials outside local development.
- The current project-list endpoint accepts `ownerEmail` as a query parameter; enforce authenticated ownership before using the application in a production environment.
