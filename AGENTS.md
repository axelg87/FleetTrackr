

🚨 Agent Compliance Guide

All agents working on this repository are expected to follow the guidelines and standards defined below. The goal is to maintain production-grade quality, enterprise-level stability, and a clean, maintainable codebase.


---

📌 MANDATORY EXPECTATIONS

✅ 1. Follow Clean Architecture Principles

Presentation layer: UI only (no logic, no data sources).

Application layer: Handles logic, navigation coordination, and state.

Domain layer (if applicable): Business rules, use cases.

Infrastructure layer: API, database, Firebase, services.


✅ 2. Apply SOLID Principles

S: Single Responsibility Principle — one reason to change per class/file.

O: Open/Closed Principle — extendable without modifying core logic.

L: Liskov Substitution Principle — interchangeable interfaces/components.

I: Interface Segregation Principle — no bloated interfaces.

D: Dependency Inversion — depend on abstractions, not concrete types.


✅ 3. Use DRY and KISS Principles

DRY: No duplicate logic — refactor shared logic into helpers or managers.

KISS: Avoid unnecessary complexity — always favor clarity.


✅ 4. Centralize State Management

Global app state, navigation state, and user/session context must be centralized using a shared state holder (ViewModel, NavigationManager, etc.)

UI must be reactive to state changes, not manually forced.


✅ 5. Ensure Robust Navigation

Bottom navigation and swipe pager must stay synchronized.

Use a central navigation manager — no local NavHostController manipulation in screens.

All gestures (taps, swipes) must be reflected in the navigation state.



---

⚠️ FUNCTIONALITY & UI.

Load user/session data in background, not blocking UI thread.








---

🔍 DEVELOPMENT GUIDELINES

✅ Code Structure

Use Kotlin best practices: val > var, sealed classes, data classes.

Keep files short and readable.

Use remember, LaunchedEffect, and Compose tools properly.

Avoid tightly coupled logic — keep UI and business logic decoupled.


✅ Error Handling

Never leave failing states unhandled.

Handle edge cases (e.g., empty data, nulls, out-of-bounds).


✅ Experimental APIs

If using @OptIn(...), clearly comment why it's justified.

Prefer stable alternatives unless experimental use is necessary for UX.



---

📦 CONTRIBUTION STANDARDS



All PRs must:

Include a clear title

Reference the feature or bug being addressed






---

🧠 Agent Mental Model

You are acting as a professional developer in a collaborative team. You should:

Think through side effects and navigation flows.

Ask questions if behavior is ambiguous.

Leave no half-implemented feature.

Refactor when needed, not just patch.

Consider performance and scalability.


---








Unit tests for utility or business logic



Regression prevention when working on critical flows





---

🧪 Test Authoring Rules

- Tests must follow the Arrange-Act-Assert structure with clear `// Arrange`, `// Act`, and `// Assert` sections in every test method.
- Use descriptive test names that state the expected outcome and context using backticked function names where appropriate.
- Favor deterministic inputs and cover both success and failure paths for critical validations and business logic.
- Keep assertions focused: prefer a small number of precise expectations that verify behavior without duplicating production logic.
