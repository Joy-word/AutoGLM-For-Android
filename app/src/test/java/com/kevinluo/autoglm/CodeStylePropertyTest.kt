package com.kevinluo.autoglm

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll
import java.io.File

/**
 * Property-based tests for code style unification.
 *
 * These tests verify that the codebase adheres to the code style guidelines
 * defined in the requirements document.
 *
 * **Feature: code-style-unification**
 */
class CodeStylePropertyTest : StringSpec({

    // ============================================================================
    // Property 1: No Direct Log Calls
    // For any Kotlin source file in the project (except Logger.kt), the file
    // SHALL NOT contain direct calls to `android.util.Log.*` methods.
    // **Feature: code-style-unification, Property 1: No Direct Log Calls**
    // **Validates: Requirements 1.1**
    // ============================================================================

    /**
     * Property 1: No Direct Log Calls
     *
     * For any Kotlin source file in the project (except Logger.kt),
     * the file SHALL NOT contain direct calls to `android.util.Log.*` methods.
     *
     * **Validates: Requirements 1.1**
     */
    "Property 1: No Kotlin source file (except Logger.kt) should contain direct Log calls" {
        val sourceDir = File("app/src/main/java/com/kevinluo/autoglm")
        val kotlinFiles = sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.name != "Logger.kt" }
            .toList()

        // Direct Log call patterns to detect
        val directLogPatterns = listOf(
            Regex("""(?<!Logger\.)\bLog\.v\s*\("""),
            Regex("""(?<!Logger\.)\bLog\.d\s*\("""),
            Regex("""(?<!Logger\.)\bLog\.i\s*\("""),
            Regex("""(?<!Logger\.)\bLog\.w\s*\("""),
            Regex("""(?<!Logger\.)\bLog\.e\s*\("""),
            Regex("""(?<!Logger\.)\bLog\.wtf\s*\("""),
            Regex("""(?<!Logger\.)\bLog\.println\s*\(""")
        )

        // Also check for import of android.util.Log (except in Logger.kt)
        val logImportPattern = Regex("""import\s+android\.util\.Log\b""")

        val violations = mutableListOf<String>()

        kotlinFiles.forEach { file ->
            val content = file.readText()
            val lines = content.lines()

            // Check for direct Log calls
            directLogPatterns.forEach { pattern ->
                lines.forEachIndexed { index, line ->
                    if (pattern.containsMatchIn(line)) {
                        violations.add("${file.name}:${index + 1} - Direct Log call found: ${line.trim()}")
                    }
                }
            }

            // Check for Log import (warning, not necessarily a violation if Logger wraps it)
            if (logImportPattern.containsMatchIn(content)) {
                // Only flag if there are also direct Log calls
                val hasDirectCalls = directLogPatterns.any { it.containsMatchIn(content) }
                if (hasDirectCalls) {
                    violations.add("${file.name} - Has android.util.Log import with direct Log calls")
                }
            }
        }

        if (violations.isNotEmpty()) {
            println("Code style violations found:")
            violations.forEach { println("  - $it") }
        }

        violations.isEmpty() shouldBe true
    }

    /**
     * Property 1 (Property-based): Random file sampling for Log calls
     *
     * For any randomly selected Kotlin source file (except Logger.kt),
     * the file should not contain direct Log.* method calls.
     *
     * **Validates: Requirements 1.1**
     */
    "Property 1 (PBT): Random sampling of source files should not contain direct Log calls" {
        val sourceDir = File("app/src/main/java/com/kevinluo/autoglm")
        val kotlinFiles = sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.name != "Logger.kt" }
            .toList()

        // Direct Log call pattern (simplified for property testing)
        val directLogPattern = Regex("""\bLog\.(v|d|i|w|e|wtf|println)\s*\(""")

        if (kotlinFiles.isNotEmpty()) {
            val fileArb = Arb.element(kotlinFiles)

            forAll(100, fileArb) { file ->
                val content = file.readText()
                !directLogPattern.containsMatchIn(content)
            }
        } else {
            // No files to test, pass
            true shouldBe true
        }
    }


    // ============================================================================
    // Property 5: Line Length Limit
    // For any line in any Kotlin source file, the line length SHALL NOT exceed
    // 120 characters.
    // **Feature: code-style-unification, Property 5: Line Length Limit**
    // **Validates: Requirements 3.8**
    // ============================================================================

    /**
     * Property 5: Line Length Limit
     *
     * For any line in any Kotlin source file, the line length SHALL NOT
     * exceed 120 characters.
     *
     * **Validates: Requirements 3.8**
     */
    "Property 5: No line in any Kotlin source file should exceed 120 characters" {
        val sourceDir = File("app/src/main/java/com/kevinluo/autoglm")
        val kotlinFiles = sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        val maxLineLength = 120
        val violations = mutableListOf<String>()

        kotlinFiles.forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                if (line.length > maxLineLength) {
                    violations.add(
                        "${file.name}:${index + 1} - Line length ${line.length} exceeds $maxLineLength: " +
                            "${line.take(50)}..."
                    )
                }
            }
        }

        if (violations.isNotEmpty()) {
            println("Line length violations found:")
            violations.take(10).forEach { println("  - $it") }
            if (violations.size > 10) {
                println("  ... and ${violations.size - 10} more violations")
            }
        }

        violations.isEmpty() shouldBe true
    }

    /**
     * Property 5 (Property-based): Random line sampling for length limit
     *
     * For any randomly selected line from any Kotlin source file,
     * the line length should not exceed 120 characters.
     *
     * **Validates: Requirements 3.8**
     */
    "Property 5 (PBT): Random sampling of lines should not exceed 120 characters" {
        val sourceDir = File("app/src/main/java/com/kevinluo/autoglm")
        val kotlinFiles = sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        // Collect all lines with file info
        data class LineInfo(val fileName: String, val lineNumber: Int, val content: String)

        val allLines = kotlinFiles.flatMap { file ->
            file.readLines().mapIndexed { index, line ->
                LineInfo(file.name, index + 1, line)
            }
        }

        val maxLineLength = 120

        if (allLines.isNotEmpty()) {
            val lineArb = Arb.element(allLines)

            forAll(100, lineArb) { lineInfo ->
                lineInfo.content.length <= maxLineLength
            }
        } else {
            true shouldBe true
        }
    }

    // ============================================================================
    // Property 7: Data Class Immutability
    // For any data class in the codebase, all properties SHALL be declared with
    // `val` (immutable) unless explicitly documented as requiring mutation.
    // **Feature: code-style-unification, Property 7: Data Class Immutability**
    // **Validates: Requirements 7.4**
    // ============================================================================

    /**
     * Property 7: Data Class Immutability
     *
     * For any data class in the codebase, all properties SHALL be declared
     * with `val` (immutable) unless explicitly documented as requiring mutation.
     *
     * **Validates: Requirements 7.4**
     */
    "Property 7: All data class properties should be immutable (val)" {
        val sourceDir = File("app/src/main/java/com/kevinluo/autoglm")
        val kotlinFiles = sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        // Pattern to match data class declarations with their properties
        val dataClassPattern = Regex(
            """data\s+class\s+(\w+)\s*(?:<[^>]+>)?\s*\(\s*([^)]+)\)""",
            RegexOption.DOT_MATCHES_ALL
        )

        // Pattern to find var properties in constructor parameters
        val varPropertyPattern = Regex("""\bvar\s+\w+\s*:""")

        // Pattern to check for mutation documentation
        val mutationDocPattern = Regex(
            """(?:@MutableProperty|/\*\*[^*]*\*[^/]*mutation[^*]*\*/|//.*mutation)""",
            RegexOption.IGNORE_CASE
        )

        val violations = mutableListOf<String>()

        kotlinFiles.forEach { file ->
            val content = file.readText()

            dataClassPattern.findAll(content).forEach { match ->
                val className = match.groupValues[1]
                val properties = match.groupValues[2]

                // Check if there are var properties
                if (varPropertyPattern.containsMatchIn(properties)) {
                    // Check if mutation is documented
                    val classStartIndex = match.range.first
                    val precedingContent = content.substring(
                        maxOf(0, classStartIndex - 500),
                        classStartIndex
                    )

                    if (!mutationDocPattern.containsMatchIn(precedingContent)) {
                        violations.add(
                            "${file.name} - Data class '$className' has mutable (var) properties " +
                                "without mutation documentation"
                        )
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            println("Data class immutability violations found:")
            violations.forEach { println("  - $it") }
        }

        violations.isEmpty() shouldBe true
    }

    /**
     * Property 7 (Property-based): Random data class sampling for immutability
     *
     * For any randomly selected data class from the codebase,
     * all its properties should be declared with val.
     *
     * **Validates: Requirements 7.4**
     */
    "Property 7 (PBT): Random sampling of data classes should have immutable properties" {
        val sourceDir = File("app/src/main/java/com/kevinluo/autoglm")
        val kotlinFiles = sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        // Pattern to match data class declarations
        val dataClassPattern = Regex(
            """data\s+class\s+(\w+)\s*(?:<[^>]+>)?\s*\(\s*([^)]+)\)""",
            RegexOption.DOT_MATCHES_ALL
        )

        // Pattern to find var properties
        val varPropertyPattern = Regex("""\bvar\s+\w+\s*:""")

        // Collect all data classes
        data class DataClassInfo(
            val fileName: String,
            val className: String,
            val properties: String
        )

        val dataClasses = kotlinFiles.flatMap { file ->
            val content = file.readText()
            dataClassPattern.findAll(content).map { match ->
                DataClassInfo(
                    fileName = file.name,
                    className = match.groupValues[1],
                    properties = match.groupValues[2]
                )
            }.toList()
        }

        if (dataClasses.isNotEmpty()) {
            val dataClassArb = Arb.element(dataClasses)

            forAll(100, dataClassArb) { dataClass ->
                !varPropertyPattern.containsMatchIn(dataClass.properties)
            }
        } else {
            true shouldBe true
        }
    }
})
