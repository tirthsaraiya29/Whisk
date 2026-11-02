<div style="text-align: center;">

# WHISK
### *The Ultimate Local-First Developer Knowledge Workspace & AI-Augmented IDE for Android*

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Compose-BOM_2026.09-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Android SDK](https://img.shields.io/badge/Android-SDK_37-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![ONNX Runtime](https://img.shields.io/badge/ONNX_Runtime-384d_MiniLM-005CED?style=flat-square)](https://onnxruntime.ai)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL_3.0-blue.svg?style=flat-square)](https://github.com/tirthsaraiya29/Whisk/blob/main/LICENSE)

<p style="text-align: center;">
  <b>Whisk</b> is a high-performance, offline-first personal knowledge management (PKM) system and developer suite built natively for Android in Kotlin and Jetpack Compose. It bridges the gap between mobile computing and developer tooling by integrating bidirectional wikilinks, interactive 2D knowledge graphs, infinite spatial canvases, an MCP client, and zero-latency on-device semantic vector search via pretrained ONNX embedding models and LiteRT-LM.
</p>

</div>

---

## Table of Contents

- [Core Capabilities](#core-capabilities)
- [On-Device Semantic Vector Search & Hybrid RAG](#on-device-semantic-vector-search--hybrid-rag)
- [On-Device AI & Hybrid LLM Workspace](#on-device-ai--hybrid-llm-workspace)
- [GitHub Explorer & Model Context Protocol (MCP) Client](#github-explorer--model-context-protocol-mcp-client)
- [Data Security & Vault Portability](#data-security--vault-portability)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Quick Start & Build](#quick-start--build)
- [License](#license)

---

## Core Capabilities

### Advanced Markdown & Code Studio
- **Dual-Mode Editor**: Seamlessly toggle between raw Markdown editing and live formatted preview.
- **Syntax-Aware Code Blocks**: Dedicated code blocks with language labels, copy actions, line numbers, and preserved formatting.
- **Wikilinks & Backlinks**: Automatic bidirectional link discovery (`[[Note Title]]`), relationship tracking, and backlink resolution.
- **Autosave**: Background persistence ensuring zero data loss.

### Interactive 2D Knowledge Graph
- **Global & Local Views**: Explore your entire vault graph or center focus around a specific note node.
- **Fluid Gestures**: Interactive pan, zoom, and smooth node selection.
- **Smart Classification**: Clear visual distinction between Notes, Tags, and Relationships.
- **Orphan Finder**: Instantly isolate unlinked notes in your vault.

### Infinite Spatial Canvas
- **Visual Thinking**: Freely arrange idea cards, notes, and containers on a coordinate plane.
- **Vector Connections**: Draw direct connection lines between related concept cards.
- **Independent Persistence**: Card coordinates are saved locally in Room without altering underlying note contents.

---

## On-Device Semantic Vector Search & Hybrid RAG

Whisk includes a local Retrieval-Augmented Generation (RAG) engine powered by on-device ONNX inference:

- **Pretrained ONNX Embedding Inference**: Genuine on-device semantic vector generation using `sentence-transformers/all-MiniLM-L6-v2` (384-dimensional L2-normalized embeddings) running locally via ONNX Runtime.
- **WordPiece Tokenizer**: Full BERT WordPiece tokenizer (`vocab.txt`, 30,522 tokens) with attention mask handling and mean pooling.
- **Hybrid Retrieval & RRF Fusion**: Combines FTS4 full-text search with cosine vector similarity via Reciprocal Rank Fusion (RRF) for contextual relevance.
- **Transactional Atomic Indexing**: Thread-safe background chunking and vector storage with Room database transaction safety.

---

## On-Device AI & Hybrid LLM Workspace

Whisk empowers mobile engineering with a robust hybrid AI architecture:

- **Local Gemma / LiteRT-LM**: Run open-weight models directly on-device using LiteRT-LM runtime (`com.google.ai.edge.litertlm`) with zero cloud latency and complete offline privacy.
- **Multi-Provider Cloud Integration**: Native connectivity for OpenAI, Google Gemini API, Anthropic Claude, OpenRouter, and custom HTTPS endpoints.
- **Context Assembly**: Dynamically inject active notes, linked documentation, or retrieved chunks into prompt contexts.
- **Durable Streaming Persistence**: Incremental persistence with process-death recovery and cancellation handling.

---

## GitHub Explorer & Model Context Protocol (MCP) Client

- **Repository Explorer**: Search and browse repositories, inspect issues, review pull requests, and trace commit histories.
- **MCP JSON-RPC Client**: Model Context Protocol implementation (`tools/list`, `tools/call`) for discovering and executing MCP tools.
- **Write-Tool Safety Guards**: Automatic detection of write/destructive operations requiring user approval before tool execution.

---

## Data Security & Vault Portability

- **Zero-Trust Local Storage**: Built on Room Database with Full-Text Search (FTS4) and DataStore Preferences.
- **Safe ZIP Vault Import/Export**: Full vault backup and restoration with strict Zip-Slip path traversal sanitization and SHA-256 manifest validation.

---

## Quick Start & Build

Prerequisites: **Android Studio Ladybug+**, **Android SDK 37**, and JDK 17.

```bash
# Clone the repository
git clone https://github.com/tirthsaraiya29/Whisk.git
cd Whisk

# Build debug APK
./gradlew :app:assembleDebug

# Run complete unit test suite
./gradlew :app:testDebugUnitTest

# Install directly on a connected device or emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## License

Whisk is open-source software licensed under the [GPL-3.0 License](LICENSE).
