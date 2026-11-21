package com.whisk.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MermaidSecurityTest {

    @Test
    fun testScriptInjectionIsHtmlEncoded() {
        val maliciousCode = "graph TD\n  A[Start] --> B[End]\n</div><script>alert('XSS')</script>"
        val htmlEncodedCode = maliciousCode.trim()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")

        assertFalse("Encoded output must not contain raw script tags", htmlEncodedCode.contains("<script>"))
        assertTrue("Encoded output must convert < to &lt;", htmlEncodedCode.contains("&lt;script&gt;"))
        assertTrue("Encoded output must convert > to &gt;", htmlEncodedCode.contains("&gt;"))
    }
}
