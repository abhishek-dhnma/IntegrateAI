# IntegrateAI - AI Assistant for GitHub

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://oracle.com/java)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Nango](https://img.shields.io/badge/Integration-Nango--Cloud-blue.svg)](https://nango.dev)
[![OpenRouter](https://img.shields.io/badge/AI-OpenRouter-purple.svg)](https://openrouter.ai)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> Autonomous AI Agent mini-application built with Spring Boot 3.3 and Java 21, connecting to GitHub securely using Nango Integration Infrastructure and OpenRouter LLMs.
>
> Eliminates raw OAuth 2.0 token storage, prevents secret leaks into AI model contexts, and enforces human-in-the-loop safety intercepts for write operations.

---

## Table of Contents
- [1. Overview](#1-overview)
- [2. Why IntegrateAI and Nango?](#2-why-integrateai-and-nango)
- [3. Key Features](#3-key-features)
- [4. Architecture and Flowcharts](#4-architecture-and-flowcharts)
- [5. Step-by-Step Setup Guide](#5-step-by-step-setup-guide)
  - [Prerequisites](#prerequisites)
  - [Step 1: Clone the Repository](#step-1-clone-the-repository)
  - [Step 2: Create a GitHub OAuth App](#step-2-create-a-github-oauth-app)
  - [Step 3: Setup Nango Cloud Integration](#step-3-setup-nango-cloud-integration)
  - [Step 4: Configure Environment Variables](#step-4-configure-environment-variables)
  - [Step 5: Run the Application](#step-5-run-the-application)
  - [Step 6: Connect GitHub and Chat](#step-6-connect-github-and-chat)
- [6. Interactive Web Dashboard](#6-interactive-web-dashboard)
- [7. Safety Confirmation Intercept](#7-safety-confirmation-intercept)
- [8. Example Prompts](#8-example-prompts)
- [9. REST API Reference](#9-rest-api-reference)
- [10. Project Directory Layout](#10-project-directory-layout)
- [11. Security Architecture](#11-security-architecture)
- [12. Developer Experience Notes](#12-developer-experience-notes)
- [13. License](#13-license)

---

## 1. Overview

IntegrateAI is a reference architecture and production-grade showcase project that bridges modern enterprise Java and Spring Boot development with autonomous AI tool-calling agents.

Instead of writing custom OAuth 2.0 flows, managing token refresh timers, or storing user access tokens in a local database, IntegrateAI delegates credential storage and proxying to Nango Integration Infrastructure.

---

## 2. Why IntegrateAI and Nango?

| Traditional Integration Approach | IntegrateAI + Nango Approach |
| :--- | :--- |
| Handcraft OAuth 2.0 flows, PKCE, token refresh jobs, and database tables for credentials. | **Zero Token Storage**: Nango handles the entire token lifecycle; backend only needs a connection_id. |
| Tokens often passed into application memory or exposed to LLM system prompts. | **Zero Leakage**: LLM only sees structured tool schemas and sanitized API responses. |
| Uncontrolled write mutations when LLMs call external APIs. | **Safety Intercept Modal**: Destructive or mutating actions (such as creating issues) require explicit user approval. |
| Re-inventing API proxy logic, retry headers, and rate limiting. | **Unified Proxy**: Spring Boot calls api.nango.dev/proxy/* with uniform headers (Provider-Config-Key, Connection-Id). |

---

## 3. Key Features

- **Autonomous AI Tool Calling**: Intelligent intent parsing and function execution using OpenRouter models.
- **Safety Confirmation Intercept**: Write actions (such as creating issues) trigger an interactive confirmation dialog with preview before any GitHub mutation occurs.
- **Tokenless Backend Architecture**: The Spring Boot server never touches or stores raw GitHub OAuth tokens.
- **Seamless Nango Connect UI**: Users connect their GitHub account in a single click using Nango's frontend modal.
- **Modern Responsive UI**: Dark-mode chat interface with Markdown rendering, quick action prompts, and real-time connection status.
- **Container and Native Support**: Run via native Maven, PowerShell script (run.ps1), or docker-compose.

---

## 4. Architecture and Flowcharts

IntegrateAI follows a decoupled architecture separating client UI interaction, backend agent orchestration, secure token proxying, and external API execution:

- **Client Layer**: Single-page dark mode dashboard communicating via REST and opening Nango Connect popups for user authorization.
- **Spring Boot Backend**: Hosts `AgentController`, `AgentService`, and `ToolRegistry` for tool selection and safety enforcement.
- **OpenRouter AI Layer**: Evaluates user prompts against registered tool schemas and outputs structured tool calls.
- **Nango Integration Infrastructure**: Manages encrypted OAuth tokens and proxies authenticated requests to GitHub with zero secret exposure.

Detailed flowcharts and sequence diagrams have been moved to dedicated documentation for clear viewing and zoom control:
- [docs/architecture-flowcharts.md](docs/architecture-flowcharts.md)
  - System Architecture Flowchart
  - Request and Tool Execution Sequence Flow
  - OAuth Handshake Sequence Flow
  - Safety Confirmation Intercept Flowchart

---

## 5. Step-by-Step Setup Guide

### Prerequisites
- **Java 21 JDK** installed ([Eclipse Temurin](https://adoptium.net/) or Oracle JDK)
- **Maven 3.9+** (or use IntelliJ / IDE bundled Maven)
- **GitHub Account** (to register an OAuth app)
- **Nango Account** ([Sign up at nango.dev](https://app.nango.dev))
- **OpenRouter Account & API Key** ([openrouter.ai](https://openrouter.ai))

---

### Step 1: Clone the Repository
```bash
git clone https://github.com/abhishek-dhnma/IntegrateAI.git
cd IntegrateAI
```

---

### Step 2: Create a GitHub OAuth App
1. Go to GitHub: **Settings** -> **Developer Settings** -> **OAuth Apps** -> **New OAuth App**
2. Fill in the fields:
   - **Application name**: `IntegrateAI`
   - **Homepage URL**: `http://localhost:8080`
   - **Authorization callback URL**: `https://api.nango.dev/oauth/callback`
3. Click **Register application**.
4. Note your **Client ID**.
5. Click **Generate a new client secret** and copy the **Client Secret**.

---

### Step 3: Setup Nango Cloud Integration
1. Log in to [app.nango.dev](https://app.nango.dev).
2. Create or select your Environment.
3. In the left navigation, go to **Integrations** -> Click **Add Integration** -> Select **GitHub**.
4. Configure the integration:
   - **Integration Key / Unique ID**: `github`
   - **Client ID**: Paste your GitHub OAuth Client ID from Step 2.
   - **Client Secret**: Paste your GitHub OAuth Client Secret from Step 2.
   - **Scopes**: Ensure `repo` and `read:user` are included.
5. In Nango, go to **Environment Settings** -> **Keys** and copy your **Secret Key**.

---

### Step 4: Configure Environment Variables
Copy the sample environment file:
```bash
# On Linux / macOS:
cp .env.example .env

# On Windows (PowerShell):
Copy-Item .env.example .env
```

Open `.env` and fill in your keys:
```properties
# Nango Infrastructure Configuration
NANGO_SECRET_KEY=your_actual_nango_secret_key_here
NANGO_HOST=https://api.nango.dev
GITHUB_INTEGRATION_ID=github
NANGO_CONNECTION_ID=demo-user-1

# AI LLM Provider Configuration (OpenRouter)
OPENROUTER_API_KEY=your_actual_openrouter_api_key_here
OPENROUTER_BASE_URL=https://openrouter.ai/api/v1
AI_MODEL=openrouter/free
AI_MOCK_MODE=false

# Server Configuration
PORT=8080
```

---

### Step 5: Run the Application

#### Option A: PowerShell Helper (Windows)
```powershell
.\run.ps1
```

#### Option B: Standard Maven
```bash
mvn clean spring-boot:run
```

#### Option C: Docker Compose
```bash
docker-compose up --build
```

The application will start at:
**`http://localhost:8080`**

---

### Step 6: Connect GitHub and Chat
1. Open your browser to **`http://localhost:8080`**.
2. Click the top-right button: **"Connect GitHub"**.
3. A Nango Connect popup will open asking you to authorize your GitHub OAuth app.
4. Authorize the application. The button status pill will turn **green ("Connected")**.
5. Type your first prompt into the chat box.

---

## 6. Interactive Web Dashboard

The web dashboard is a single-page dark mode client located at [src/main/resources/static/index.html](src/main/resources/static/index.html):
- **Real-time Status Pill**: Shows whether your GitHub connection is active or disconnected.
- **Dynamic Connection Handshake**: Triggers `POST /api/connection/session` to obtain a short-lived Nango connect token.
- **Quick Action Pills**: Clickable sample prompts to test repository queries, issue inspections, and issue creations instantly.
- **Markdown and Code Formatting**: Responses with lists, links, and code blocks render clearly via marked.js.

---

## 7. Safety Confirmation Intercept

To prevent LLMs from making unintentional changes, IntegrateAI enforces a safety intercept pattern on write operations:

1. When a user asks: *"Create an issue in my-org/demo titled 'Bug in login'"*
2. The agent identifies this as a **mutating write operation** (`createIssue`).
3. Instead of executing immediately, the backend returns:
   ```json
   {
     "requiresConfirmation": true,
     "pendingAction": {
       "tool": "createIssue",
       "owner": "my-org",
       "repo": "demo",
       "title": "Bug in login"
     }
   }
   ```
4. The dashboard displays an interactive confirmation modal with:
   - Target Repository
   - Proposed Issue Title and Body
   - **"Confirm & Create"** or **"Cancel"** buttons
5. Only upon clicking confirm does `POST /api/agent/confirm` execute the proxied request to GitHub via Nango.

---

## 8. Example Prompts

| Prompt | Tool Triggered | Behavior |
| :--- | :--- | :--- |
| `"List my GitHub repositories"` | `listRepositories` | Queries user repos through Nango proxy. |
| `"What open issues exist in facebook/react?"` | `listOpenIssues` | Fetches open issues, author, and labels. |
| `"Tell me about the repository torvalds/linux"` | `getRepository` | Returns stars, forks, and description. |
| `"Create an issue in my-user/test-repo titled 'Fix navbar' with body 'Navbar is overflowing on mobile'"` | `createIssue` | Triggers Safety Confirmation Intercept before creating. |

---

## 9. REST API Reference

- **`GET /api/connection/status?connectionId={id}`**: Checks whether the connection ID has an active authenticated session in Nango.
- **`POST /api/connection/session`**: Requests a temporary Nango Connect session token for the frontend OAuth popup.
- **`POST /api/agent/chat`**: Passes user prompt to the agent pipeline for tool selection and execution.
- **`POST /api/agent/confirm`**: Executes an action previously held in the safety confirmation state.

---

## 10. Project Directory Layout

- [docs/](docs/)
  - [docs/architecture-flowcharts.md](docs/architecture-flowcharts.md) : Detailed architecture flowcharts and sequence diagrams
  - [docs/article-notes.md](docs/article-notes.md) : Technical article outline and architecture notes
  - [docs/developer-experience.md](docs/developer-experience.md) : In-depth DX evaluation and Nango Java roadmap observations
- [src/](src/)
  - [src/main/](src/main/)
    - [src/main/java/com/integrateai/](src/main/java/com/integrateai/)
      - [IntegrateAiApplication.java](src/main/java/com/integrateai/IntegrateAiApplication.java) : Spring Boot main entrypoint
      - [agent/](src/main/java/com/integrateai/agent/)
        - [AgentService.java](src/main/java/com/integrateai/agent/AgentService.java) : Orchestrates LLM prompt parsing and tool execution
        - [Tool.java](src/main/java/com/integrateai/agent/Tool.java) : Base interface for executable agent tools
        - [ToolCallResult.java](src/main/java/com/integrateai/agent/ToolCallResult.java) : DTO encapsulating tool output
        - [ToolRegistry.java](src/main/java/com/integrateai/agent/ToolRegistry.java) : Registry and lookup for available tools
      - [config/](src/main/java/com/integrateai/config/)
        - [AppConfig.java](src/main/java/com/integrateai/config/AppConfig.java) : Spring WebClient and bean configuration
        - [NangoProperties.java](src/main/java/com/integrateai/config/NangoProperties.java) : Strongly-typed configuration properties
      - [controller/](src/main/java/com/integrateai/controller/)
        - [AgentController.java](src/main/java/com/integrateai/controller/AgentController.java) : Chat and confirmation REST endpoints
        - [ConnectionController.java](src/main/java/com/integrateai/controller/ConnectionController.java) : Status and Connect session endpoints
        - [WebhookController.java](src/main/java/com/integrateai/controller/WebhookController.java) : Nango incoming webhook receiver
      - [dto/](src/main/java/com/integrateai/dto/)
        - [AgentChatRequest.java](src/main/java/com/integrateai/dto/AgentChatRequest.java) : Chat request payload
        - [AgentChatResponse.java](src/main/java/com/integrateai/dto/AgentChatResponse.java) : Chat response payload
        - [ConfirmationRequest.java](src/main/java/com/integrateai/dto/ConfirmationRequest.java) : Safety confirmation payload
        - [ConnectionStatusResponse.java](src/main/java/com/integrateai/dto/ConnectionStatusResponse.java) : Connection health DTO
        - [WebhookEventDto.java](src/main/java/com/integrateai/dto/WebhookEventDto.java) : Webhook event payload
      - [exception/](src/main/java/com/integrateai/exception/)
        - [GitHubIntegrationException.java](src/main/java/com/integrateai/exception/GitHubIntegrationException.java) : Integration error definition
        - [GlobalExceptionHandler.java](src/main/java/com/integrateai/exception/GlobalExceptionHandler.java) : Centralized controller advice
        - [NangoException.java](src/main/java/com/integrateai/exception/NangoException.java) : Nango API error wrapper
      - [github/](src/main/java/com/integrateai/github/)
        - [GitHubService.java](src/main/java/com/integrateai/github/GitHubService.java) : GitHub API client via Nango proxy
        - [GitHubTool.java](src/main/java/com/integrateai/github/GitHubTool.java) : Implementations for listRepos, listIssues, createIssue
        - [dto/](src/main/java/com/integrateai/github/dto/)
          - [CreateIssueRequest.java](src/main/java/com/integrateai/github/dto/CreateIssueRequest.java) : Issue creation schema
          - [GitHubIssue.java](src/main/java/com/integrateai/github/dto/GitHubIssue.java) : Issue representation model
          - [GitHubRepo.java](src/main/java/com/integrateai/github/dto/GitHubRepo.java) : Repository representation model
      - [nango/](src/main/java/com/integrateai/nango/)
        - [NangoClient.java](src/main/java/com/integrateai/nango/NangoClient.java) : Low-level HTTP client for Nango REST API
        - [NangoService.java](src/main/java/com/integrateai/nango/NangoService.java) : High-level Nango session and proxy operations
        - [dto/](src/main/java/com/integrateai/nango/dto/)
          - [NangoConnectionDto.java](src/main/java/com/integrateai/nango/dto/NangoConnectionDto.java) : Connection details DTO
          - [NangoConnectSessionDto.java](src/main/java/com/integrateai/nango/dto/NangoConnectSessionDto.java) : Connect session token response
      - [service/](src/main/java/com/integrateai/service/)
        - [EventLogService.java](src/main/java/com/integrateai/service/EventLogService.java) : In-memory audit event logger
    - [src/main/resources/](src/main/resources/)
      - [application.yml](src/main/resources/application.yml) : Application properties and defaults
      - [static/](src/main/resources/static/)
        - [index.html](src/main/resources/static/index.html) : Single-page dark mode dashboard and chat interface
  - [src/test/](src/test/)
    - [src/test/java/com/integrateai/](src/test/java/com/integrateai/)
      - [IntegrationTest.java](src/test/java/com/integrateai/IntegrationTest.java) : End-to-end integration test suite
      - [agent/AgentServiceTest.java](src/test/java/com/integrateai/agent/AgentServiceTest.java) : Unit tests for agent service
      - [github/GitHubServiceTest.java](src/test/java/com/integrateai/github/GitHubServiceTest.java) : Unit tests for GitHub tool operations
      - [nango/NangoServiceTest.java](src/test/java/com/integrateai/nango/NangoServiceTest.java) : Unit tests for Nango client and proxy
- [.env.example](.env.example) : Template for environment variables
- [.gitignore](.gitignore) : Excludes secrets, target builds, IDE files, and OS dumps
- [docker-compose.yml](docker-compose.yml) : Container orchestration setup
- [Dockerfile](Dockerfile) : Multi-stage build definition
- [pom.xml](pom.xml) : Maven dependencies and plugins configuration
- [run.ps1](run.ps1) : Windows PowerShell helper launcher
- [LICENSE](LICENSE) : MIT License file
- [README.md](README.md) : Project documentation and setup guide

---

## 11. Security Architecture

1. **Token Insulation**: Raw access tokens are never saved to disk, printed in logs, or sent to the client browser.
2. **LLM Sandboxing**: The AI model receives only high-level schema definitions and structured tool responses. Credentials and secret keys are completely excluded from the model context window.
3. **Safe Defaults**: Write operations are marked as mutating tools, preventing autonomous writes without human verification.
4. **Environment Isolation**: Production deployments utilize environment variable injection without hardcoded secrets.

---

## 12. Developer Experience Notes

See [docs/developer-experience.md](docs/developer-experience.md) for recommendations and feedback regarding Nango's Java and Spring Boot ecosystem.

---

## 13. License

Distributed under the MIT License. See [LICENSE](LICENSE) for details.
