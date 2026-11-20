# Security Policy

## Supported Versions

Whisk is actively maintained. Security updates are provided for the latest stable release and the main development branch.

| Version | Supported          |
| ------- | ------------------ |
| v1.0.x  | :white_check_mark: |
| < v1.0  | :x:                |

## Reporting a Vulnerability

We take the security of Whisk and our users' data very seriously. If you discover a security vulnerability within Whisk, please do **not** open a public issue. 

Instead, please email the project maintainers (or use GitHub Security Advisories if enabled on this repository). 
We will acknowledge receipt of your vulnerability report within 48 hours and strive to send you regular updates about our progress.

### What to include in your report:
- Type of vulnerability (e.g., Path Traversal, Intent Redirection, Information Disclosure).
- Step-by-step instructions to reproduce the vulnerability.
- Proof-of-concept (PoC) code or screenshots, if applicable.
- The impact of the vulnerability.

## Security Architecture & Best Practices

Whisk is designed as an offline-first, zero-trust knowledge workspace. We implement several strict security measures:

### 1. Data Encryption & Credentials
All sensitive API keys (e.g., OpenAI, Anthropic, Gemini, OpenRouter) and access tokens are encrypted at rest using **AndroidX Security Crypto** (`EncryptedSharedPreferences` backed by the Android KeyStore). If KeyStore corruption occurs, `SecureStorageManager` recovers by re-creating the encrypted vault preferences and throws a `SecurityException` if encryption cannot be initialized, preventing any silent fallback to unencrypted storage.

### 2. File System Safety & Vault Integrity
Whisk allows importing and exporting vaults as ZIP archives. To prevent **Zip-Slip** (directory traversal) vulnerabilities, the import mechanism rigorously sanitizes entry paths (`!entryName.contains("..")`) ensuring archives cannot write outside the sandbox directory. Vault manifests contain SHA-256 digests of `data.json` for corruption detection during import/export.

### 3. Model Context Protocol (MCP) Safety Guards
Whisk integrates an MCP JSON-RPC 2.0 Client with local tool execution via `McpToolServerHub` and remote MCP server support. Since MCP tools can interact with local or remote systems, Whisk enforces a **Default-Deny Write-Tool Safety Guard**. Over 100 mutating verbs (such as `create`, `add`, `insert`, `post`, `put`, `update`, `edit`, `modify`, `rename`, `move`, `copy`, `duplicate`, `delete`, `remove`, `destroy`, `drop`, `clear`, `purge`, `truncate`, `push`, `write`, `save`, `store`, `set`, `upload`, `import`, `export`, `transfer`, `merge`, `rebase`, `revert`, `restore`, `replace`, `apply`, `patch`, `exec`, `execute`, `run`, `invoke`, `call`, `send`, `submit`, `commit`, `approve`, `close`, `reopen`, `assign`, `archive`, `unarchive`, `toggle`, `enable`, `disable`, `grant`, `revoke`, `reset`, `regenerate`, `cancel`, `alter`, `change`, `swap`, `shift`, `convert`, `transform`, `wipe`, `flush`, `kill`, `terminate`, `stop`, `start`, `restart`, `boot`, `reboot`, `shutdown`, `upgrade`, `downgrade`, `install`, `uninstall`, `overwrite`, `prune`, `detach`, `unbind`, `unlink`, `lock`, `unlock`, `sync`, `format`, `clean`, `dispatch`, `publish`, `deploy`, `erase`) require explicit user confirmation before tool execution unless the server identity is explicitly trusted and the tool is read-only. Tool execution logging recursively redacts secret credentials across all nested JSON argument structures.

### 4. P2P Collaboration & Cloud Backup Controls
- **P2P Sync**: Enforces TLS 1.3 / 1.2 peer authentication with SSLSocket/SSLServerSocket, peer authorization, bounded payload length reads (10MB limit), and full note metadata preservation (folder, type, aliases, pins, accent colors).
- **Cloud Backup**: Requires valid destination URL schemes, secure credential retrieval from `SecureStorageManager` (never persisted in plain text in WorkManager inputs), optional HTTP Bearer authentication headers, and mandates End-to-End Encryption (E2EE) passphrase protection before uploading vault backups.

### 5. Privacy by Design (On-Device AI)
To guarantee data privacy, Whisk ships with the ability to run LLMs locally using **MediaPipe Tasks GenAI** and **LiteRT-LM** (e.g., Gemma 4 E2B). Your notes, code, and thoughts do not need to leave your device for inference unless you explicitly configure a third-party cloud provider. Biometrically locked notes are automatically excluded from local RAG indexing (`DocumentIndexer`), AI context assembler (`ContextAssembler`), search indexing, and unencrypted exports.

### 6. Secure Networking
All external network calls (e.g., to GitHub APIs or Cloud LLMs) use **OkHttp 5.5.1** with strict HTTPS enforcement and mandatory TLS Certificate Pinning capabilities for primary cloud AI providers (OpenAI, Anthropic, Gemini, OpenRouter). Cleartext traffic is disabled by default for remote endpoints.