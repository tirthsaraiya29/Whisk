# Contributing to Whisk

First off, thank you for considering contributing to Whisk! It's people like you that make the open-source community such an amazing place to learn, inspire, and create.

## Code of Conduct

By participating in this project, you are expected to uphold our Code of Conduct. Please be respectful, inclusive, and constructive in your interactions with other community members.

## How Can I Contribute?

### Reporting Bugs
Bugs are tracked as GitHub issues. When creating a bug report, please use the **Bug Report** issue template and provide as much detail as possible:
- A quick summary and/or background.
- Steps to reproduce.
- Expected vs. Actual behavior.
- Device model and Android OS version.

### Suggesting Enhancements
Enhancement suggestions are also tracked as GitHub issues. Use the **Feature Request** issue template to detail your idea, why it would be useful, and how it aligns with Whisk's offline-first, developer-centric philosophy.

### Pull Requests
1. **Fork the repo** and create your branch from `main`.
2. **Ensure your code adheres to our architecture**: Clean Architecture, MVVM, Coroutines/StateFlow.
3. **Write tests**: If you've added code that should be tested, add unit tests in the `test/` directory.
4. **Pass the build**: Run `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest` to ensure everything compiles and tests pass.
5. **Keep UI consistent**: Follow the Design System (Warm Light / Charcoal Dark) and use Material 3 Compose components.

## Development Setup

See the [SETUP.md](SETUP.md) file for detailed instructions on how to set up your local development environment.

## Code Style & Architecture Guidelines
- **Kotlin 2.2+**: Use idiomatic Kotlin. Avoid unnecessary boilerplate.
- **Jetpack Compose**: Keep composables stateless where possible. Hoist state to ViewModels. Use `StateFlow` for state management.
- **Room Database**: Keep database operations off the main thread using Coroutines.
- **No AI Slop**: If you are using AI tools to help write code, review the output carefully. Remove generic AI boilerplate comments and ensure the code is concise and maintainable (refer to [AGENT.md](AGENT.md)).