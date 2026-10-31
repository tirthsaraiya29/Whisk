# Security Policy

## Supported Versions

Whisk is actively maintained. Security updates are applied to the latest stable release and the main development branch.

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

---

## Reporting a Vulnerability

We take the security of Whisk and user data very seriously. If you discover a security vulnerability, please report it responsibly rather than opening a public issue.

### Private Disclosure Method
- **GitHub Security Advisories**: Navigate to the [Whisk GitHub Security Tab](https://github.com/whisk/whisk/security/advisories/new) and submit a private vulnerability report.
- **Direct Contact**: If you are unable to use GitHub Security Advisories, please reach out directly to the repository maintainers via the maintainer profile contact details.

### What to Include in Your Report
To help us triage and remediate the issue as quickly as possible, please provide:
1. **Description**: A clear summary of the vulnerability and its potential impact.
2. **Components**: Affected modules, classes, or endpoints (e.g., ZIP import path validation, MCP client JSON-RPC handling, Local AI storage decryption).
3. **Reproduction Steps**: Step-by-step instructions, proof-of-concept code, or minimal reproduction snippets.
4. **Environment**: Android version, device architecture, and Whisk version.

---

## Disclosure Policy & Timeline

We follow principles of **Coordinated Disclosure**:
1. **Acknowledgment**: You will receive an acknowledgment of your report within 48 hours.
2. **Verification & Triage**: Our engineering team will investigate and confirm the vulnerability within 5 business days.
3. **Remediation**: Patches will be developed, tested, and released. We aim to deploy critical security fixes within 14 to 30 days depending on complexity.
4. **Public Disclosure**: Public advisories or release notes will be published once the patch is publicly available and users have had a reasonable window to update.

---

## Safe Harbor

We support authorized security research. We will not take legal action against researchers who:
- Make a good faith effort to avoid privacy violations, data destruction, and service interruption.
- Keep vulnerabilities confidential until they have been resolved and publicly disclosed according to our timeline.
