# Whisk 🚀

> **A Premium, Minimalist Developer Knowledge Workspace for Android**

Whisk is a local-first, developer-centric knowledge and productivity system built natively for Android in **Kotlin** and **Jetpack Compose**. It combines the power of personal knowledge management (bidirectional links, interactive 2D knowledge graphs, spatial canvas) with developer utilities (syntax-aware code blocks, checklist aggregators, GitHub integration, Model Context Protocol client, and on-device AI).

---

## 🌟 Key Features

### 1. Markdown Notes & Code Editor
- **Dual-Mode Editor**: Seamlessly toggle between **Edit** and **Formatted Preview** modes.
- **Rich Syntax Support**: Headings, bold, italic, bullet points, interactive checklists (`- [ ]`, `- [x]`), blockquotes, and internal `[[Note Title]]` links.
- **Syntax-Aware Code Blocks**: Dedicated code blocks with language labels, copy action, line numbers, and preserved indentation for Kotlin, Java, Python, JS, TS, C, Rust, Go, SQL, Bash, and JSON.
- **Automatic Autosave**: Debounced background persistence ensures no edits are ever lost.
- **Backlinks & Link Resolution**: Automatic incoming/outgoing relationship detection and broken link discovery.

### 2. Interactive Knowledge Graph
- **Global & Local Views**: Explore your entire vault or center around a specific note.
- **2D Canvas Rendering**: Interactive pan, pinch-to-zoom, and tap node selection.
- **Node Classification**: Clear visual distinction for Notes, Tags, and Folders.
- **Node Inspector**: Bottom sheet displaying node details and quick jump links.
- **Orphan Filtering**: Quickly locate unlinked notes.

### 3. Spatial Visual Canvas
- **Deliberate Layouts**: Separate from the auto-derived graph—arrange ideas, note cards, and text blocks on an infinite 2D canvas.
- **Card Connections**: Draw connection lines between related cards.
- **Persistent Layout**: Card coordinates are persisted independently without altering note content.

### 4. Vault Checklist Aggregator
- **Automated Extraction**: Scans all vault notes for Markdown checklists (`- [ ]` / `- [x]`).
- **Interactive Toggling**: Complete tasks directly from the master checklist view.
- **Source Links**: Tap any task to jump straight to its source note.

### 5. AI Workspace (Local Gemma & Remote LLMs)
- **Local Gemma 4 E2B**: On-device generative AI via LiteRT-LM runtime with storage validation, progress reporting, and zero cloud dependency.
- **Remote Providers**: Native support for OpenAI, Google Gemini API, Anthropic Claude, OpenRouter, and custom HTTPS endpoints.
- **Context Assembly**: Inject active notes, linked notes, or code blocks into AI prompts with token estimates and privacy disclosure.
- **Edit Previews**: Preview AI-generated code edits before applying them to your notes.

### 6. GitHub, MCP Client & GitDiagram
- **Repository Exploration**: Search public/private repos, browse issues, pull requests, and commits.
- **GitHub MCP Client**: Full JSON-RPC 2.0 Model Context Protocol (MCP) implementation for tool discovery and execution.
- **GitDiagram Integration**: One-tap architecture visualization route for any GitHub repository.

### 7. Storage, Security & Backups
- **Storage Access Framework (SAF)**: Native Android SAF integration for single-note export and vault backup.
- **ZIP Vault Import/Export**: Full vault backup to ZIP with Zip-Slip path sanitization protection against path traversal attacks.
- **Data Protection**: Local-first storage using Room database and DataStore preferences with Keystore backing.

---

## 🛠️ Architecture & Tech Stack

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose with Material 3 foundation
- **Database**: Room Database (SQLite)
- **Preferences**: DataStore Preferences
- **Async Runtime**: Kotlin Coroutines & StateFlow
- **Networking**: OkHttp 4
- **Serialization**: Kotlinx Serialization JSON
- **Build System**: Gradle Kotlin DSL with Version Catalog (`libs.versions.toml`) and AGP 9 built-in Kotlin

---

## 🚀 Building & Running

### Build Commands
```bash
# Clean and assemble debug APK
./gradlew :app:assembleDebug

# Run unit test suite
./gradlew :app:testDebugUnitTest

# Install on connected device/emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.whisk/com.whisk.MainActivity
```

---

## 🎨 Theme & Design Philosophy

Whisk follows an unbranded, quiet, professional design philosophy:
- **Minimal Light Theme**: Warm beige background (`#FBF9F6`), subdued surface cards, dark slate text.
- **Charcoal Dark Theme**: Deep charcoal background (`#131417`), comfortable contrast, muted slate accent.
- **No Decorative Clutter**: Clean typography, compact bottom navigation bar, searchable Command Palette (`Ctrl/Cmd+K` trigger).

---

## 📄 License

Licensed under the [GPL-3.0 License](LICENSE).
