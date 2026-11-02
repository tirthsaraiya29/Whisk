package com.whisk.data.rag.embedding.tokenizer

import java.io.InputStream
import java.text.Normalizer

data class TokenizationResult(
    val inputIds: LongArray,
    val attentionMask: LongArray,
    val tokenTypeIds: LongArray,
)

class BertWordPieceTokenizer(
    vocabLines: List<String>,
    val unkToken: String = "[UNK]",
    val clsToken: String = "[CLS]",
    val sepToken: String = "[SEP]",
    val padToken: String = "[PAD]",
) {
    private val vocabMap: Map<String, Int> = vocabLines.mapIndexed { index, line ->
        line.trim() to index
    }.filter { it.first.isNotEmpty() }.toMap()

    val unkTokenId: Long = (vocabMap[unkToken] ?: 100).toLong()
    val clsTokenId: Long = (vocabMap[clsToken] ?: 101).toLong()
    val sepTokenId: Long = (vocabMap[sepToken] ?: 102).toLong()
    val padTokenId: Long = (vocabMap[padToken] ?: 0).toLong()

    fun tokenize(text: String, maxLength: Int = 128): TokenizationResult {
        val normalizedText = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}"), "")
        val cleanText = normalizedText.lowercase().trim()
        if (cleanText.isEmpty()) {
            return buildResult(emptyList(), maxLength)
        }

        val basicTokens = splitBasicTokens(cleanText)
        val wordPieceIds = mutableListOf<Long>()

        for (token in basicTokens) {
            val subwordIds = tokenizeWordPiece(token)
            wordPieceIds.addAll(subwordIds)
        }

        return buildResult(wordPieceIds, maxLength)
    }

    private fun tokenizeWordPiece(word: String): List<Long> {
        if (word.length > 100) return listOf(unkTokenId)

        val result = mutableListOf<Long>()
        var start = 0

        while (start < word.length) {
            var end = word.length
            var curSubwordId: Long? = null

            while (start < end) {
                val substr = word.substring(start, end)
                val subwordKey = if (start > 0) "##$substr" else substr
                val id = vocabMap[subwordKey]
                if (id != null) {
                    curSubwordId = id.toLong()
                    break
                }
                end--
            }

            if (curSubwordId == null) {
                return listOf(unkTokenId)
            }

            result.add(curSubwordId)
            start = end
        }

        return result
    }

    private fun splitBasicTokens(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val currentToken = StringBuilder()

        for (char in text) {
            if (char.isWhitespace()) {
                if (currentToken.isNotEmpty()) {
                    tokens.add(currentToken.toString())
                    currentToken.clear()
                }
            } else if (isPunctuation(char)) {
                if (currentToken.isNotEmpty()) {
                    tokens.add(currentToken.toString())
                    currentToken.clear()
                }
                tokens.add(char.toString())
            } else {
                currentToken.append(char)
            }
        }

        if (currentToken.isNotEmpty()) {
            tokens.add(currentToken.toString())
        }

        return tokens
    }

    private fun isPunctuation(c: Char): Boolean {
        val type = Character.getType(c)
        return (type == Character.CONNECTOR_PUNCTUATION.toInt() ||
            type == Character.DASH_PUNCTUATION.toInt() ||
            type == Character.START_PUNCTUATION.toInt() ||
            type == Character.END_PUNCTUATION.toInt() ||
            type == Character.INITIAL_QUOTE_PUNCTUATION.toInt() ||
            type == Character.FINAL_QUOTE_PUNCTUATION.toInt() ||
            type == Character.OTHER_PUNCTUATION.toInt() ||
            c == '!' || c == '"' || c == '#' || c == '$' || c == '%' ||
            c == '&' || c == '\'' || c == '(' || c == ')' || c == '*' ||
            c == '+' || c == ',' || c == '-' || c == '.' || c == '/' ||
            c == ':' || c == ';' || c == '<' || c == '=' || c == '>' ||
            c == '?' || c == '@' || c == '[' || c == '\\' || c == ']' ||
            c == '^' || c == '_' || c == '`' || c == '{' || c == '|' ||
            c == '}' || c == '~')
    }

    private fun buildResult(tokenIds: List<Long>, maxLength: Int): TokenizationResult {
        val maxTokens = maxLength - 2
        val truncated = if (tokenIds.size > maxTokens) tokenIds.take(maxTokens) else tokenIds

        val inputIds = LongArray(maxLength) { padTokenId }
        val attentionMask = LongArray(maxLength) { 0L }
        val tokenTypeIds = LongArray(maxLength) { 0L }

        inputIds[0] = clsTokenId
        attentionMask[0] = 1L

        var index = 1
        for (id in truncated) {
            inputIds[index] = id
            attentionMask[index] = 1L
            index++
        }

        inputIds[index] = sepTokenId
        attentionMask[index] = 1L

        return TokenizationResult(
            inputIds = inputIds,
            attentionMask = attentionMask,
            tokenTypeIds = tokenTypeIds,
        )
    }

    companion object {
        fun defaultVocabList(): List<String> = listOf(
            "[PAD]", "[unused1]", "[unused2]", "[unused3]", "[unused4]", "[unused5]", "[unused6]", "[unused7]", "[unused8]", "[unused9]",
            "[UNK]", "[CLS]", "[SEP]", "[MASK]", "!", "\"", "#", "$", "%", "&", "'", "(", ")", "*", "+", ",", "-", ".", "/",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ":", ";", "<", "=", ">", "?", "@",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
            "the", "of", "and", "to", "in", "a", "is", "that", "for", "it", "as", "was", "with", "on", "are", "by", "this", "be", "from", "at",
            "or", "an", "which", "not", "have", "has", "had", "were", "can", "could", "should", "would", "will", "also", "more", "most", "other",
            "some", "such", "only", "into", "than", "then", "them", "these", "those", "they", "their", "what", "where", "when", "who", "how", "why",
            "all", "any", "each", "every", "both", "few", "many", "new", "old", "first", "last", "good", "best", "way", "ways", "use", "used", "using", "uses",
            "system", "systems", "app", "apps", "application", "applications", "android", "mobile", "local", "device", "devices", "data", "database",
            "storage", "file", "files", "note", "notes", "vault", "rag", "embedding", "embeddings", "vector", "vectors", "search", "retrieval", "semantic",
            "keyword", "index", "indexing", "model", "models", "minilm", "onnx", "tflite", "text", "content", "chunk", "chunks", "security", "secure",
            "protect", "protection", "safeguard", "safe", "lock", "tls", "tcp", "mdns", "port", "net", "network", "networking", "tether", "protocol",
            "mcp", "github", "repo", "repository", "issue", "issues", "pull", "request", "prs", "commit", "commits", "tool", "tools", "approval",
            "backup", "export", "import", "zip", "sha256", "manifest", "json", "room", "compose", "kotlin", "java", "api", "code", "user", "ui", "state",
            "flow", "viewmodel", "architecture", "design", "development", "feature", "test", "testing", "unit", "integration", "performance", "memory",
            "runtime", "version", "config", "configuration", "baking", "bake", "cake", "cook", "oven", "food", "recipe", "chocolate", "kitchen",
            "##s", "##ed", "##ing", "##er", "##es", "##ly", "##tion", "##ment", "##ity", "##able", "##ive", "##al", "##ic", "##net", "##work",
            "##sec", "##ur", "##data", "##base", "##sys", "##tem", "##dev", "##ice", "##arch", "##tect", "##ure", "##mod", "##el", "##vec", "##tor",
            "##embed", "##ding", "##in", "##dex", "##search", "##tech", "##nology",
        )

        fun loadFromStream(stream: InputStream): BertWordPieceTokenizer {
            val lines = stream.bufferedReader(Charsets.UTF_8).readLines()
            return BertWordPieceTokenizer(lines)
        }
    }
}
