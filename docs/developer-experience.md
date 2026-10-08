# Nango Developer Experience Notes (Java & Spring Boot Perspective)

## Executive Summary
This document records empirical developer experience (DX) observations compiled while building **IntegrateAI** : a Spring Boot 3.3 (Java 21) AI Agent application connecting to GitHub via Nango's Integration Infrastructure.

---

## Key DX Findings

### 1. What Was Easy to Understand
- **Conceptual Clarity**: The distinction between `Integration` (the 3rd party API metadata/OAuth config) and `Connection` (an authenticated end-user session) is intuitive and well-structured.
- **REST Proxy Concept**: The Nango Proxy model (`https://api.nango.dev/proxy/{path}`) with `Provider-Config-Key` and `Connection-Id` headers is standard, clean, and language-agnostic.
- **Connect Session Flow**: Initializing auth via `POST /connect/session` simplifies token generation for frontend popups.

### 2. What Required Investigation
- **Absence of Native Java Backend SDK**:
  - The documentation page at `/docs/reference/backend/backend-sdk/java` states: `Coming soon. Use the REST API in the meantime.`
  - Java developers must construct raw HTTP headers (`Provider-Config-Key`, `Connection-Id`, `Authorization: Bearer <key>`) manually or build custom Spring `WebClient` wrappers.
- **Header Naming Conventions**:
  - Headers in HTTP proxy calls use kebab-case (`Provider-Config-Key` and `Connection-Id`), whereas JSON request bodies in `/connect/session` use snake_case (`end_user`, `allowed_integrations`). Documenting header syntax clearly across HTTP client languages (Java, Go, C#) avoids header formatting errors.

### 3. Java/Spring Boot Specific Missing Artifacts
To make Nango instantly accessible to Java/Spring Enterprise teams, Nango would benefit from providing:
1. **Official Java SDK / Spring Boot Starter**:
   - Maven/Gradle artifact (e.g. `nango-spring-boot-starter`).
   - Auto-configuration of `NangoClient` via `@EnableNango`.
2. **Spring WebClient / RestClient Code Snippets**:
   - Adding tabbed code examples on doc pages showing Java `HttpClient`, Spring `RestClient`, and OkHttp alongside cURL and Node.js.
3. **Structured Webhook Event Models**:
   - Standard Java DTO definitions or JSON schemas for Nango webhook payloads (`connection.created`, `sync.completed`, `issues.opened`).

---

## Concrete Recommendations for Nango Documentation & SDK Roadmap

1. **Provide Multi-Language Tabs on Doc Pages**:
   Even before an official Java SDK is released, add a "Java / Spring Boot" tab under REST API examples showing how to pass headers using Java 21 `HttpClient` or Spring `RestClient`.
2. **Clarify Proxy Request Pathing**:
   Explicitly highlight whether the `{path}` in `/proxy/{path}` requires or forbids a leading slash (`/user/repos` vs `user/repos`).
3. **Create a "Nango for Java / Enterprise" Integration Guide**:
   Publish a tutorial demonstrating Spring Boot microservices consuming Nango REST endpoints for multi-tenant SaaS.
