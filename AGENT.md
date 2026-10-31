# Rules for AI Coding Agents (Anti-Slop)

This document outlines the core guidelines for any AI agent or LLM (e.g., GitHub Copilot, Cursor, Whisk's internal agents) assisting with development on the Whisk codebase.

## 1. Zero Slop Policy
- **No Boilerplate Comments**: Do not generate comments that merely restate the code (e.g., `// Increment counter by 1` above `counter++`). Comments must explain *why*, not *what*.
- **No Hallucinations**: If you do not know the exact Jetpack Compose or Kotlin API, do not guess. Rely on standard Android SDK documentation.
- **Conciseness over Verbosity**: Write the minimum amount of code necessary to cleanly solve the problem.

## 2. Kotlin & Android Idioms
- **Modern Kotlin**: Use trailing lambdas, scope functions (`let`, `apply`, `run`, `with`), and expression bodies where appropriate, but optimize for readability.
- **Coroutines & Flow**: Always prefer Kotlin Coroutines and `StateFlow`/`SharedFlow` over callbacks or RxJava. Never block the main thread.
- **Compose Best Practices**:
  - Keep composable functions pure and side-effect free unless using explicit `LaunchedEffect`/`DisposableEffect`.
  - Prefer immutable state classes.
  - Remember to use `remember` for heavy computations.

## 3. Architecture Compliance
- Whisk follows **Clean Architecture** (UI -> Domain -> Data). Do not bypass layers (e.g., do not access Room DAOs directly from the UI).
- ViewModels should only expose `StateFlow` and handle intents/events. Do not pass `Context` into ViewModels.

## 4. UI & Design
- Honor the existing theme tokens in Whisk (Warm Light and Charcoal Dark). 
- Use standard Material 3 components and spacing tokens. Do not hardcode magic padding/margin numbers (e.g., `Modifier.padding(13.dp)`); use standard values.

## 5. Security Context
- If editing logic around File I/O, ensure absolute prevention of path traversal.
- Do not log sensitive data (API keys, note contents) to Logcat.

*Agents: Acknowledge these instructions implicitly by following them perfectly in your code generation.*