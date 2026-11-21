package com.whisk.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeBlockTokenizerTest {

    @Test
    fun testCodeBlockLineCount() {
        val sampleCode = """
            fun main() {
                val message = "Hello Whisk"
                println(message)
            }
        """.trimIndent()

        val lines = sampleCode.lines()
        assertEquals(4, lines.size)
        assertEquals("fun main() {", lines[0])
    }

    @Test
    fun testKeywordsRegexSet() {
        val keywords = setOf(
            "fun", "val", "var", "class", "object", "interface", "import", "package", "return",
            "if", "else", "for", "while", "do", "when", "try", "catch", "finally", "throw",
        )

        assertTrue(keywords.contains("fun"))
        assertTrue(keywords.contains("val"))
        assertTrue(keywords.contains("class"))
        assertFalse(keywords.contains("hello"))
    }
}
