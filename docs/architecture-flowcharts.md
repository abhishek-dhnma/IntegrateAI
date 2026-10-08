# IntegrateAI Architecture and Flowcharts

This document details the architectural diagrams and technical interaction flows for IntegrateAI.

---

## 1. System Architecture

```mermaid
flowchart TD
    subgraph ClientLayer["Client Layer (Browser)"]
        UI["Web Dashboard (index.html)"]
        NangoPopup["Nango Connect Modal"]
    end

    subgraph BackendLayer["Spring Boot 3.3 Backend"]
        AC["AgentController"]
        CC["ConnectionController"]
        AS["AgentService"]
        TR["ToolRegistry"]
        GT["GitHubTool"]
        NC["NangoClient / NangoService"]
    end

    subgraph AILayer["AI Intelligence Layer"]
        OR["OpenRouter API (LLM)"]
    end

    subgraph InfrastructureLayer["Integration Infrastructure"]
        NangoProxy["Nango REST Proxy"]
        NangoVault["Nango Token Vault (Encrypted)"]
    end

    subgraph ExternalServices["External APIs"]
        GitHubAPI["GitHub REST API (api.github.com)"]
    end

    UI -->|"User Prompts / Chat"| AC
    UI -->|"Request Connect Session"| CC
    CC -->|"POST /connect/session"| NC
    NC -->|"Session Token"| NangoProxy
    NangoPopup -->|"OAuth 2.0 Auth"| GitHubAPI
    GitHubAPI -->|"Store Encrypted Tokens"| NangoVault

    AC --> AS
    AS -->|"Prompt + Tool Schemas"| OR
    OR -->|"Tool Call Intent"| AS
    AS --> TR
    TR --> GT
    GT --> NC
    NC -->|"Proxy Request + Connection-Id"| NangoProxy
    NangoVault -.->|"Inject OAuth Bearer Token"| NangoProxy
    NangoProxy -->|"Authenticated Request"| GitHubAPI
    GitHubAPI -->|"Response Data"| NangoProxy
    NangoProxy -->|"JSON Output"| NC
    NC --> AS
    AS -->|"Formatted Output"| AC
    AC -->|"Response"| UI
```

---

## 2. Request and Tool Execution Flow

```mermaid
sequenceDiagram
    autonumber
    actor User as User (Browser)
    participant Backend as Spring Boot (IntegrateAI)
    participant LLM as OpenRouter (LLM)
    participant Nango as Nango Cloud Proxy
    participant GitHub as GitHub API

    User->>Backend: POST /api/agent/chat (prompt, connectionId)
    Backend->>Backend: Check Connection Status
    Backend->>LLM: Send Prompt + Registered Tool Definitions
    LLM-->>Backend: Return Tool Decision (e.g., listRepositories)
    alt Read-Only Tool (e.g., listRepositories, listOpenIssues)
        Backend->>Nango: GET /proxy/user/repos (Provider-Config-Key, Connection-Id)
        Nango->>GitHub: GET /user/repos (with injected Bearer token)
        GitHub-->>Nango: 200 OK (Repository JSON data)
        Nango-->>Backend: Return JSON data
        Backend->>Backend: Format Markdown response
        Backend-->>User: Display AI response in chat
    else Write Tool (e.g., createIssue)
        Backend-->>User: Return requiresConfirmation = true + pendingAction
        Note over User,Backend: Safety Intercept Triggered
        User->>Backend: POST /api/agent/confirm (pendingAction)
        Backend->>Nango: POST /proxy/repos/{owner}/{repo}/issues
        Nango->>GitHub: POST /repos/{owner}/{repo}/issues (with injected Bearer token)
        GitHub-->>Nango: 201 Created
        Nango-->>Backend: Return Created Issue JSON
        Backend-->>User: Display confirmation and link to created issue
    end
```

---

## 3. OAuth Handshake Flow

```mermaid
sequenceDiagram
    autonumber
    actor User as User
    participant Frontend as Browser UI
    participant Backend as Spring Boot
    participant Nango as Nango Cloud
    participant GitHub as GitHub OAuth

    User->>Frontend: Click "Connect GitHub"
    Frontend->>Backend: POST /api/connection/session
    Backend->>Nango: POST /connect/session (secret key, connectionId, providerKey)
    Nango-->>Backend: Return connect_token
    Backend-->>Frontend: Return session token
    Frontend->>Frontend: Open Nango Frontend Modal (using connect_token)
    Frontend->>GitHub: Redirect to GitHub Authorization
    User->>GitHub: Grant requested scopes (repo, read:user)
    GitHub-->>Nango: Authorization Code
    Nango->>GitHub: Exchange Code for Access and Refresh Tokens
    GitHub-->>Nango: Access and Refresh Tokens
    Nango->>Nango: Encrypt and store tokens in secure vault
    Frontend->>Backend: GET /api/connection/status?connectionId=...
    Backend->>Nango: GET /connection/{connectionId}
    Nango-->>Backend: Status: ACTIVE
    Backend-->>Frontend: connected: true
    Frontend->>Frontend: Update status pill to "Connected"
```

---

## 4. Safety Confirmation Intercept Flow

```mermaid
flowchart TD
    Start["User submits write prompt<br>(e.g., 'Create issue in owner/repo')"] --> ParseIntent["Agent parses intent via LLM"]
    ParseIntent --> CheckTool{"Is tool mutating?<br>(createIssue)"}

    CheckTool -- "No (Read-only)" --> DirectExec["Execute via Nango Proxy immediately"]
    DirectExec --> Output["Return data to User"]

    CheckTool -- "Yes (Mutating)" --> GeneratePreview["Generate PendingAction Preview"]
    GeneratePreview --> PromptModal["Render interactive modal on Dashboard"]
    PromptModal --> UserDecision{"User Decision"}

    UserDecision -- "Cancel" --> Abort["Action Aborted<br>No changes made to GitHub"]
    UserDecision -- "Confirm" --> ConfirmAPI["Call POST /api/agent/confirm"]
    ConfirmAPI --> ExecProxy["Send POST via Nango REST Proxy"]
    ExecProxy --> InjectedAuth["Nango injects Bearer token"]
    InjectedAuth --> CreateOnGitHub["GitHub creates issue"]
    CreateOnGitHub --> Complete["Display success with issue URL"]
```
