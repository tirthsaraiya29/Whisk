# Security Policy

## Supported Versions

Whisk is actively maintained. Security updates are provided for the latest stable release and the main development branch.

| Version | Supported          |
| ------- | ------------------ |
| v1.0.x  | :white_check_mark: |
| < v1.0  | :x:                |

## Reporting a Vulnerability

We take the security of Whisk and our users' data very seriously. If you discover a security vulnerability within Whisk, please do **not** open a public issue. 

Instead, please send an email to the project maintainers (or use GitHub Security Advisories if enabled on this repository). 
We will acknowledge receipt of your vulnerability report within 48 hours and strive to send you regular updates about our progress.

### What to include in your report:
- Type of vulnerability (e.g., Path Traversal, Intent Redirection, Information Disclosure).
- Step-by-step instructions to reproduce the vulnerability.
- Proof-of-concept (PoC) code or screenshots, if applicable.
- The impact of the vulnerability.

## Security Architecture & Best Practices

Whisk is designed as an offline-first, zero-trust knowledge workspace. We implement several strict security measures:

### 1. Data Encryption & Credentials
All sensitive API keys (e.g., OpenAI, Anthropic, Gemini, OpenRouter) and access tokens are never stored in plain text. Whisk uses **AndroidX Security Crypto** (`EncryptedSharedPreferences` backed by the Android KeyStore) to encrypt and securely manage credentials on the device.

### 2. File System Safety (Zip-Slip Prevention)
Whisk allows importing and exporting vaults as ZIP files. To prevent **Zip-Slip** (directory traversal) vulnerabilities, the import mechanism rigorously sanitizes entry paths (`!entryName.contains("..")`) ensuring malicious archives cannot write outside the designated sandbox directory.

### 3. Model Context Protocol (MCP) Safety Guards
Whisk integrates an MCP JSON-RPC 2.0 Client. Since MCP tools can interact with local or remote systems, Whisk enforces a **Write-Tool Safety Guard**. Any destructive or write-oriented operation mandates explicit user confirmation before tool execution, protecting against autonomous or accidental destructive actions.

### 4. Privacy by Design (On-Device AI)
To guarantee data privacy, Whisk ships with the ability to run LLMs locally using **MediaPipe Tasks GenAI** and **LiteRT-LM** (e.g., Gemma 4 E2B). Your notes, code, and thoughts do not need to leave your device for inference unless you explicitly configure a third-party cloud provider.

### 5. Secure Networking
All external network calls (e.g., to GitHub APIs or Cloud LLMs) use **OkHttp 4** with strict HTTPS enforcement and optional TLS Certificate Pinning capabilities. Cleartext traffic is disabled by default.