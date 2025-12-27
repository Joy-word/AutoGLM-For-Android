#!/usr/bin/env kotlin

/**
 * Code Style Check Script for AutoGLM Phone Agent
 *
 * This script checks the codebase for compliance with the code style guidelines:
 * - No direct android.util.Log calls (should use Logger instead)
 * - KDoc coverage for public classes and methods
 * - Naming conventions (SCREAMING_SNAKE_CASE for constants, camelCase for properties, PascalCase for classes)
 *
 * Requirements: 1.1, 2.1, 3.3, 3.4, 3.5
 *
 * Usage: kotlinc -script check_code_style.kts [source_directory]
 */

import java.io.File

// ANSI color codes for output
object Colors {
    const val RED = "\u001B[31m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val BLUE = "\u001B[34m"
    const val RESET = "\u001B[0m"
}

data class StyleViolation(
    val file: String,
    val line: Int,
    val rule: String,
    val message: String
)

class CodeStyleChecker(private val sourceDir: File) {
    private val violations = mutableListOf<StyleViolation>()
    private var totalFiles = 0
    private var filesWithViolations = 0

    // Files to exclude from direct Log check (Logger.kt itself uses Log)
    private val logCheckExclusions = setOf("Logger.kt")

    /**
     * Run all code style checks.
     */
    fun runAllChecks(): List<StyleViolation> {
        println("${Colors.BLUE}Starting code style checks...${Colors.RESET}")
        println("Source directory: ${sourceDir.absolutePath}\n")

        sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { !it.path.contains("/build/") }
            .filter { !it.path.contains("/test/") && !it.path.contains("/androidTest/") }
            .forEach { file ->
                totalFiles++
                val fileViolations = checkFile(file)
                if (fileViolations.isNotEmpty()) {
                    filesWithViolations++
                }
                violations.addAll(fileViolations)
            }

        return violations
    }

    private fun checkFile(file: File): List<StyleViolation> {
        val fileViolations = mutableListOf<StyleViolation>()
        val lines = file.readLines()
        val content = file.readText()
        val relativePath = file.relativeTo(sourceDir).path

        // Check 1: No direct Log calls (Requirement 1.1)
        if (file.name !in logCheckExclusions) {
            fileViolations.addAll(checkDirectLogCalls(relativePath, lines))
        }

        // Check 2: KDoc coverage (Requirement 2.1)
        fileViolations.addAll(checkKDocCoverage(relativePath, lines, content))

        // Check 3: Naming conventions (Requirements 3.3, 3.4, 3.5)
        fileViolations.addAll(checkNamingConventions(relativePath, lines))

        // Check 4: Line length (Requirement 3.8)
        fileViolations.addAll(checkLineLength(relativePath, lines))

        return fileViolations
    }


    /**
     * Check for direct android.util.Log calls.
     * Requirement 1.1: THE Logger SHALL be used for all logging operations
     */
    private fun checkDirectLogCalls(file: String, lines: List<String>): List<StyleViolation> {
        val violations = mutableListOf<StyleViolation>()
        val logPattern = Regex("""(?<!Logger\.)\bLog\.(v|d|i|w|e|wtf)\s*\(""")
        val importPattern = Regex("""import\s+android\.util\.Log\b""")

        lines.forEachIndexed { index, line ->
            // Check for import statement
            if (importPattern.containsMatchIn(line)) {
                violations.add(StyleViolation(
                    file = file,
                    line = index + 1,
                    rule = "NO_DIRECT_LOG",
                    message = "Direct import of android.util.Log found. Use Logger instead."
                ))
            }

            // Check for direct Log calls
            if (logPattern.containsMatchIn(line)) {
                violations.add(StyleViolation(
                    file = file,
                    line = index + 1,
                    rule = "NO_DIRECT_LOG",
                    message = "Direct Log call found. Use Logger.d/i/w/e instead."
                ))
            }
        }

        return violations
    }

    /**
     * Check for KDoc coverage on public classes and methods.
     * Requirement 2.1: THE KDoc format SHALL be used for all public classes, interfaces, and objects
     */
    private fun checkKDocCoverage(file: String, lines: List<String>, content: String): List<StyleViolation> {
        val violations = mutableListOf<StyleViolation>()

        // Pattern to match class/interface/object declarations
        val declarationPattern = Regex("""^\s*(public\s+|internal\s+|open\s+|abstract\s+|sealed\s+|data\s+)*(class|interface|object)\s+(\w+)""")
        
        // Pattern to match public/internal function declarations
        val functionPattern = Regex("""^\s*(public\s+|internal\s+|open\s+|override\s+|suspend\s+)*(fun)\s+(\w+)""")
        
        // Pattern to match KDoc comment
        val kdocPattern = Regex("""/\*\*[\s\S]*?\*/""")

        lines.forEachIndexed { index, line ->
            // Check class/interface/object declarations
            val declarationMatch = declarationPattern.find(line)
            if (declarationMatch != null) {
                val name = declarationMatch.groupValues[3]
                // Check if there's a KDoc comment before this line
                if (!hasKDocBefore(lines, index)) {
                    violations.add(StyleViolation(
                        file = file,
                        line = index + 1,
                        rule = "MISSING_KDOC_CLASS",
                        message = "Class/Interface/Object '$name' is missing KDoc documentation."
                    ))
                }
            }

            // Check function declarations (skip private and override functions)
            val functionMatch = functionPattern.find(line)
            if (functionMatch != null && !line.contains("private ") && !line.contains("override ")) {
                val name = functionMatch.groupValues[3]
                if (!hasKDocBefore(lines, index)) {
                    violations.add(StyleViolation(
                        file = file,
                        line = index + 1,
                        rule = "MISSING_KDOC_FUNCTION",
                        message = "Function '$name' is missing KDoc documentation."
                    ))
                }
            }
        }

        return violations
    }

    private fun hasKDocBefore(lines: List<String>, lineIndex: Int): Boolean {
        // Look backwards for KDoc comment (allowing for annotations between KDoc and declaration)
        var i = lineIndex - 1
        while (i >= 0) {
            val line = lines[i].trim()
            when {
                line.endsWith("*/") -> return true  // Found end of KDoc
                line.startsWith("@") -> i--  // Skip annotations
                line.isEmpty() -> i--  // Skip empty lines
                line.startsWith("//") -> i--  // Skip single-line comments
                else -> return false  // Found something else, no KDoc
            }
        }
        return false
    }


    /**
     * Check naming conventions.
     * Requirement 3.3: Constants SHALL use SCREAMING_SNAKE_CASE
     * Requirement 3.4: Properties SHALL use camelCase
     * Requirement 3.5: Classes and interfaces SHALL use PascalCase
     */
    private fun checkNamingConventions(file: String, lines: List<String>): List<StyleViolation> {
        val violations = mutableListOf<StyleViolation>()

        // Pattern for constants in companion object (const val or private const val)
        val constPattern = Regex("""^\s*(private\s+)?const\s+val\s+(\w+)""")
        
        // Pattern for class/interface/object names
        val classPattern = Regex("""^\s*(public\s+|internal\s+|open\s+|abstract\s+|sealed\s+|data\s+)*(class|interface|object)\s+(\w+)""")
        
        // Pattern for property declarations (val/var at class level)
        val propertyPattern = Regex("""^\s*(private\s+|public\s+|internal\s+|protected\s+|override\s+|lateinit\s+)*(val|var)\s+(\w+)""")

        var inCompanionObject = false
        var braceCount = 0

        lines.forEachIndexed { index, line ->
            // Track companion object scope
            if (line.contains("companion object")) {
                inCompanionObject = true
                braceCount = 0
            }
            
            if (inCompanionObject) {
                braceCount += line.count { it == '{' }
                braceCount -= line.count { it == '}' }
                if (braceCount <= 0) {
                    inCompanionObject = false
                }
            }

            // Check constant naming (SCREAMING_SNAKE_CASE)
            val constMatch = constPattern.find(line)
            if (constMatch != null) {
                val name = constMatch.groupValues[2]
                if (!name.matches(Regex("[A-Z][A-Z0-9_]*"))) {
                    violations.add(StyleViolation(
                        file = file,
                        line = index + 1,
                        rule = "CONSTANT_NAMING",
                        message = "Constant '$name' should use SCREAMING_SNAKE_CASE (e.g., MY_CONSTANT)."
                    ))
                }
            }

            // Check class/interface/object naming (PascalCase)
            val classMatch = classPattern.find(line)
            if (classMatch != null) {
                val name = classMatch.groupValues[3]
                if (!name.matches(Regex("[A-Z][a-zA-Z0-9]*"))) {
                    violations.add(StyleViolation(
                        file = file,
                        line = index + 1,
                        rule = "CLASS_NAMING",
                        message = "Class/Interface/Object '$name' should use PascalCase (e.g., MyClass)."
                    ))
                }
            }

            // Check property naming (camelCase) - skip constants
            val propertyMatch = propertyPattern.find(line)
            if (propertyMatch != null && !line.contains("const ")) {
                val name = propertyMatch.groupValues[3]
                // Skip if it's all uppercase (might be intentional constant-like)
                // Skip if it starts with underscore (backing property)
                if (!name.startsWith("_") && !name.matches(Regex("[A-Z][A-Z0-9_]*"))) {
                    if (!name.matches(Regex("[a-z][a-zA-Z0-9]*"))) {
                        violations.add(StyleViolation(
                            file = file,
                            line = index + 1,
                            rule = "PROPERTY_NAMING",
                            message = "Property '$name' should use camelCase (e.g., myProperty)."
                        ))
                    }
                }
            }
        }

        return violations
    }

    /**
     * Check line length limit.
     * Requirement 3.8: THE line length SHALL NOT exceed 120 characters
     */
    private fun checkLineLength(file: String, lines: List<String>): List<StyleViolation> {
        val violations = mutableListOf<StyleViolation>()
        val maxLength = 120

        lines.forEachIndexed { index, line ->
            if (line.length > maxLength) {
                violations.add(StyleViolation(
                    file = file,
                    line = index + 1,
                    rule = "LINE_LENGTH",
                    message = "Line exceeds $maxLength characters (${line.length} chars)."
                ))
            }
        }

        return violations
    }


    /**
     * Print the results of the code style check.
     */
    fun printResults() {
        println("\n${Colors.BLUE}═══════════════════════════════════════════════════════════════${Colors.RESET}")
        println("${Colors.BLUE}                    CODE STYLE CHECK RESULTS                    ${Colors.RESET}")
        println("${Colors.BLUE}═══════════════════════════════════════════════════════════════${Colors.RESET}\n")

        if (violations.isEmpty()) {
            println("${Colors.GREEN}✓ All checks passed! No violations found.${Colors.RESET}")
        } else {
            // Group violations by rule
            val byRule = violations.groupBy { it.rule }
            
            println("${Colors.RED}Found ${violations.size} violation(s) in $filesWithViolations file(s):${Colors.RESET}\n")

            byRule.forEach { (rule, ruleViolations) ->
                val ruleDescription = when (rule) {
                    "NO_DIRECT_LOG" -> "Direct Log Calls (Requirement 1.1)"
                    "MISSING_KDOC_CLASS" -> "Missing KDoc on Class/Interface/Object (Requirement 2.1)"
                    "MISSING_KDOC_FUNCTION" -> "Missing KDoc on Function (Requirement 2.2)"
                    "CONSTANT_NAMING" -> "Constant Naming Convention (Requirement 3.3)"
                    "CLASS_NAMING" -> "Class Naming Convention (Requirement 3.5)"
                    "PROPERTY_NAMING" -> "Property Naming Convention (Requirement 3.4)"
                    "LINE_LENGTH" -> "Line Length Limit (Requirement 3.8)"
                    else -> rule
                }

                println("${Colors.YELLOW}[$rule] $ruleDescription - ${ruleViolations.size} violation(s)${Colors.RESET}")
                ruleViolations.take(10).forEach { v ->
                    println("  ${v.file}:${v.line} - ${v.message}")
                }
                if (ruleViolations.size > 10) {
                    println("  ... and ${ruleViolations.size - 10} more")
                }
                println()
            }
        }

        println("${Colors.BLUE}───────────────────────────────────────────────────────────────${Colors.RESET}")
        println("Summary: Checked $totalFiles files, $filesWithViolations with violations")
        println("${Colors.BLUE}───────────────────────────────────────────────────────────────${Colors.RESET}")
    }

    /**
     * Get exit code based on violations.
     */
    fun getExitCode(): Int = if (violations.isEmpty()) 0 else 1
}

// Main execution
fun main(args: Array<String>) {
    val sourceDir = if (args.isNotEmpty()) {
        File(args[0])
    } else {
        // Default to app/src/main/java
        File("app/src/main/java")
    }

    if (!sourceDir.exists() || !sourceDir.isDirectory) {
        println("${Colors.RED}Error: Source directory does not exist: ${sourceDir.absolutePath}${Colors.RESET}")
        kotlin.system.exitProcess(1)
    }

    val checker = CodeStyleChecker(sourceDir)
    checker.runAllChecks()
    checker.printResults()
    kotlin.system.exitProcess(checker.getExitCode())
}

// Run main
main(args)
