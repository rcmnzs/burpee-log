# Security Policy - Burpee Log

This document describes the security guidelines, threat model, and vulnerability reporting process for the **Burpee Log** Android application.

---

## 1. Threat Model

**Burpee Log** is an Android application developed with Kotlin and Jetpack Compose, designed to log *Six-Count* and *Navy Seal* burpee sessions under the *Busy Dad Training* protocol.

### Key Architectural Characteristics:
- **100% Offline:** The application neither requests nor requires network access (`android.permission.INTERNET`). There are no backend servers, remote APIs, third-party analytics/telemetry, Firebase services, or cloud synchronization. Google Services and Firebase plugins and dependencies have been completely removed from the project.
- **Strictly Local Storage:** All workout data is persisted inside a local Room SQLite database (`burpee_log.db`), stored within the app's secure internal storage directory (`MODE_PRIVATE`), isolated by Android's application sandbox (Linux UID separation), and protected by native hardware-backed File-Based Encryption (FBE).
- **No External Authentication:** The app does not manage or store credentials, passwords, API keys, or session tokens.
- **Secure Local Backup & Restore:** Backups and restores use Android Storage Access Framework (SAF) exclusively via scoped document and directory pickers (`ACTION_CREATE_DOCUMENT`, `ACTION_OPEN_DOCUMENT`, and `ACTION_OPEN_DOCUMENT_TREE`). The app requires no broad storage permissions or external FileProviders.
- **Robust JSON Verification:** All backup files undergo multi-layer integrity verification, validating schema versions, enforcing size limits (max 10 MB), and validating structured session records prior to database insertion.

---

## 2. Security Posture and Implemented Controls (OWASP MASVS / Mobile Top 10)

| OWASP Category | Implemented Control in Burpee Log |
| :--- | :--- |
| **M1: Credentials** | Zero secrets, private keys, or access tokens in repository or source code. |
| **M2: Supply Chain** | Minimal set of trusted dependencies pinned in Gradle. Unused networking and tracking libraries removed. |
| **M3: Authentication & Components** | Minimized exported attack surface (`MainActivity` is the sole exported activity for Launcher). Services and Content Providers are non-exported (`android:exported="false"`). |
| **M4: Input & Output Validation** | Strict JSON restore validation (max 10 MB file size limit, schemaVersion check, session count bounds, date bounds checking from year 2000 to current date, rep count boundaries 0 to 5,000, duration boundaries 1s to 3,600s, and exercise type enum validation). 100% parameterized Room queries. |
| **M5: Network Communication** | Zero internet permissions. `android:usesCleartextTraffic="false"` enforced in AndroidManifest.xml. |
| **M6: Privacy** | Principle of least privilege. No sensitive telemetry or logs. Ongoing workout notification auto-dismisses upon workout completion. |
| **M7: Binary Protection** | R8/ProGuard code minification (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`) enabled for release builds. |
| **M8: Security Configuration** | Explicit backup and data extraction configurations (`data_extraction_rules.xml` and `backup_rules.xml`) preventing extraction of cache files and restricting backups to the primary database. |
| **M9: Secure Storage** | Internal Room database stored within private app sandbox. Backups managed via Storage Access Framework with persistable scoped permissions to user-selected folder only. |
| **M10: Cryptography** | Data at rest protected by Android OS hardware-backed File-Based Encryption (FBE). No custom or weak software crypto implementations. |

---

## 3. Reporting a Vulnerability

We welcome contributions from the security community to keep Burpee Log safe.

If you have discovered a potential security vulnerability:
1. **GitHub Security Advisory:** Please submit a report through GitHub's **private Security Advisory feature**.
2. **GitHub Issues:** If private advisories are unavailable, open an issue tagged `[Security]` providing reproduction steps, app version, and identified impact.
3. **Response Time:** We aim to review and acknowledge vulnerability reports within 48 business hours.

Please refrain from publicly disclosing vulnerability details until an official patch or release has been published.
