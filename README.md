# AI Project-to-Prototype Generator

AI Project-to-Prototype Generator is a microservice-based web application that turns a project name and detailed description into a saved, interactive product prototype. The generated specification includes a project overview, features, user roles, navigation, screens, sample UI content, and project-specific technology recommendations.

The React frontend presents each generated screen as a navigable desktop preview. Prototype generation runs asynchronously through RabbitMQ, while project data, generation status, and the generated JSON specification are stored in PostgreSQL.

## Features

- Register and sign in with email and password.
- Create and list projects associated with an owner email.
- Submit prototype-generation jobs asynchronously.
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
| **Project Service** | Persists projects and prototype state, publishes generation requests, consumes AI results, and saves completed specifications or failure details. |
| **AI Service** | Consumes generation requests, prompts the configured Gemini model through its OpenAI-compatible endpoint, validates the structured response, and publishes generation results. |
| **RabbitMQ** | Carries durable prototype request and result messages between the project and AI services. |
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

## Asynchronous Prototype Generation

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
| `AI_MODEL` | No | Overrides the model; Compose defaults to `gemini-3.6-flash`. |
| `JWT_SECRET` | Yes | Signs authentication tokens. Use a long random value. |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Yes | Configure local Compose PostgreSQL. |
| `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS`, `RABBITMQ_ERLANG_COOKIE` | Yes | Configure local RabbitMQ. |
| `CORS_ALLOWED_ORIGIN` | No | Allowed browser origin; defaults locally to `http://localhost:5173`. |
| `VITE_API_BASE_URL` | Production | Public Gateway URL ending in `/api`; local development uses the Vite proxy when unset. |

Never commit real values. Render generates internal credentials where possible and prompts for deployment-specific values.

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

## Deploy to Render

The root [`render.yaml`](render.yaml) is a Render Blueprint. It attaches Auth Service and Project Service to the existing Render PostgreSQL database named `prototype-db`; it does not create another database.

### Before deployment

1. Push the repository to a Git provider connected to Render.
2. Confirm `prototype-db` exists in the Render workspace where the Blueprint will be created.
3. Ensure every private backend service and RabbitMQ is created in the **same region as `prototype-db`**. Render private networking and the database internal connection string are regional. If the database is not in Render's default Blueprint region, add the same supported `region` value to the five non-static services in `render.yaml` before applying it.
4. In the Render Dashboard, choose **New > Blueprint**, select this repository, and apply `render.yaml`.

### Values requested during Blueprint creation

| Service | Variable | Value to supply |
| --- | --- | --- |
| `prototype-ai-service` | `AI_API_KEY` | Your AI provider key. |
| `prototype-api-gateway` | `CORS_ALLOWED_ORIGIN` | The final frontend origin, for example `https://prototype-frontend.onrender.com` (no trailing slash). |
| `prototype-frontend` | `VITE_API_BASE_URL` | The final public Gateway URL plus `/api`, for example `https://prototype-api-gateway.onrender.com/api`. |

Render generates `JWT_SECRET`, the RabbitMQ password, and the Erlang cookie. Database details come from `prototype-db`; RabbitMQ connection values are shared internally through Blueprint references. Do not enter or commit those generated values manually.

Because Vite embeds `VITE_API_BASE_URL` at build time, redeploy the static frontend after its final Gateway URL is set. If Render assigns URLs only after the first Blueprint creation, set both public URL variables in the Dashboard and trigger one frontend redeploy.

### Deployment order and verification

The Blueprint manages dependencies, but the useful readiness order is:

1. Existing `prototype-db`.
2. `prototype-rabbitmq`.
3. `prototype-auth-service`, `prototype-project-service`, and `prototype-ai-service`.
4. `prototype-api-gateway`.
5. `prototype-frontend` after its build-time Gateway URL is known.

Verify the Gateway first:

```text
GET https://<gateway-host>/actuator/health
GET https://<gateway-host>/api/v1/ai/status
```

Then open the frontend and test registration, authentication, project creation, listing, prototype generation, and the saved prototype preview. The Gateway is the only public backend; Auth, Project, AI, and RabbitMQ are Render private services.

Render uses the Gateway's `/actuator/health` as its HTTP health check. Private services expose the same actuator endpoint for diagnostics and receive Render's private-service TCP readiness check.

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
