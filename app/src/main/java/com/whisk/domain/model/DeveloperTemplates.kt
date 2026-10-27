package com.whisk.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DeveloperTemplates {

    fun createDailyNote(): Note {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return Note(
            title = "Daily Note: $dateStr",
            body = """
# Daily Note: $dateStr 📅

## Top Focus Areas
- [ ] Task 1: 
- [ ] Task 2: 
- [ ] Task 3: 

## Today's Notes & Learnings
- 

## Code Snippets & Debug Insights
```kotlin
// Quick code notes
```

## Related Vault Notes
- [[Architecture Overview]]
            """.trimIndent(),
            tags = listOf("daily-note", "journal")
        )
    }

    fun createADR(title: String = "Architecture Decision Record"): Note {
        return Note(
            title = title,
            body = """
# ADR-001: $title 🏛️

## Status
Proposed | **Accepted** | Deprecated | Superseded

## Context & Problem Statement
Describe the technical context and problem that requires an architectural decision.

## Decision Drivers
- Performance and responsiveness
- Data privacy and local-first persistence
- Maintainability and testability

## Considered Options
1. Option A: 
2. Option B: 

## Decision Outcome
Chosen Option: **Option A** because...

## Positive Consequences
- 

## Negative Consequences & Risks
- 
            """.trimIndent(),
            tags = listOf("adr", "architecture")
        )
    }

    fun createDebuggingJournal(issueTitle: String = "Bug Debugging Journal"): Note {
        return Note(
            title = issueTitle,
            body = """
# Debugging Journal: $issueTitle 🐞

## Symptoms & Error Message
```
// Stack trace or error log
```

## Reproduction Steps
1. Step 1
2. Step 2

## Hypotheses & Experiments
- [x] Hypothesis 1: 
- [ ] Hypothesis 2: 

## Root Cause Analysis
Explain why the issue occurred.

## Resolution & Fix
```kotlin
// Code fix snippet
```
            """.trimIndent(),
            tags = listOf("debugging", "journal", "bugfix")
        )
    }

    fun createCodeSnippetNote(snippetName: String = "Code Snippet"): Note {
        return Note(
            title = "Snippet: $snippetName",
            body = """
# Snippet: $snippetName 💻

## Description
Short explanation of what this utility or function accomplishes.

```kotlin
// Reusable snippet
fun example() {
    println("Hello Whisk Snippet")
}
```

## Dependencies & Requirements
- Kotlin 2.0+
- Android SDK 26+
            """.trimIndent(),
            tags = listOf("snippet", "code")
        )
    }
}
