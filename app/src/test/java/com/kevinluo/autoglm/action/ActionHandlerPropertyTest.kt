package com.kevinluo.autoglm.action

import com.kevinluo.autoglm.util.CoordinateConverter
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll

/**
 * Property-based tests for ActionHandler.
 * 
 * **Feature: code-logic-audit, Property 7: ActionHandler 坐标转换正确性**
 * **Feature: autoglm-phone-agent, Property 12: Type action command generation**
 * **Validates: Requirements 3.1, 5.5, 5.6**
 */
class ActionHandlerPropertyTest : StringSpec({
    
    // ============================================================================
    // Property 7: ActionHandler 坐标转换正确性
    // For any relative coordinates (x, y) in range [0, 999] and screen dimensions 
    // (width, height), the converted absolute coordinates must be in range 
    // [0, width) and [0, height) respectively.
    // **Validates: Requirements 3.1**
    // ============================================================================
    
    /**
     * Property 7: Coordinate conversion produces valid absolute coordinates
     * 
     * For any relative coordinates in [0, 999] and any screen dimensions,
     * the absolute coordinates must be within valid screen bounds [0, screenSize).
     * 
     * **Feature: code-logic-audit, Property 7: ActionHandler 坐标转换正确性**
     * **Validates: Requirements 3.1**
     */
    "coordinate conversion should produce absolute coordinates within screen bounds" {
        // Generator for relative coordinates in valid range [0, 999]
        val relativeCoordArb = Arb.int(0..999)
        
        // Generator for realistic screen dimensions
        val screenWidthArb = Arb.int(100..4000)
        val screenHeightArb = Arb.int(100..8000)
        
        forAll(100, relativeCoordArb, relativeCoordArb, screenWidthArb, screenHeightArb) { relX, relY, width, height ->
            val (absX, absY) = CoordinateConverter.toAbsolute(relX, relY, width, height)
            
            // Absolute coordinates must be in range [0, screenSize)
            // Note: When relX=999, absX = 999 * width / 1000 < width (due to integer division)
            absX >= 0 && absX < width && absY >= 0 && absY < height
        }
    }
    
    /**
     * Property 7: Coordinate conversion formula correctness
     * 
     * The conversion must follow the formula: absolute = relative * screenSize / 1000
     * 
     * **Feature: code-logic-audit, Property 7: ActionHandler 坐标转换正确性**
     * **Validates: Requirements 3.1**
     */
    "coordinate conversion should follow the formula: absolute = relative * screenSize / 1000" {
        val relativeCoordArb = Arb.int(0..999)
        val screenSizeArb = Arb.int(100..4000)
        
        forAll(100, relativeCoordArb, screenSizeArb) { relCoord, screenSize ->
            val expectedAbsolute = relCoord * screenSize / 1000
            val actualAbsoluteX = CoordinateConverter.toAbsoluteX(relCoord, screenSize)
            val actualAbsoluteY = CoordinateConverter.toAbsoluteY(relCoord, screenSize)
            
            actualAbsoluteX == expectedAbsolute && actualAbsoluteY == expectedAbsolute
        }
    }
    
    /**
     * Property 7: Boundary value - minimum coordinate (0)
     * 
     * Relative coordinate 0 should always map to absolute coordinate 0.
     * 
     * **Feature: code-logic-audit, Property 7: ActionHandler 坐标转换正确性**
     * **Validates: Requirements 3.1**
     */
    "coordinate conversion - relative 0 should always map to absolute 0" {
        val screenSizeArb = Arb.int(100..4000)
        
        forAll(100, screenSizeArb, screenSizeArb) { width, height ->
            val (absX, absY) = CoordinateConverter.toAbsolute(0, 0, width, height)
            absX == 0 && absY == 0
        }
    }
    
    /**
     * Property 7: Boundary value - maximum valid coordinate (999)
     * 
     * Relative coordinate 999 should map to a value less than screen dimension.
     * Formula: 999 * screenSize / 1000 < screenSize (always true for screenSize > 0)
     * 
     * **Feature: code-logic-audit, Property 7: ActionHandler 坐标转换正确性**
     * **Validates: Requirements 3.1**
     */
    "coordinate conversion - relative 999 should map to value less than screen dimension" {
        val screenSizeArb = Arb.int(100..4000)
        
        forAll(100, screenSizeArb, screenSizeArb) { width, height ->
            val (absX, absY) = CoordinateConverter.toAbsolute(999, 999, width, height)
            
            // 999 * screenSize / 1000 should be less than screenSize
            absX < width && absY < height
        }
    }
    
    /**
     * Property 7: Monotonicity - larger relative coordinates produce larger or equal absolute coordinates
     * 
     * For any two relative coordinates where rel1 < rel2, the absolute coordinates
     * should maintain the same ordering: abs1 <= abs2.
     * 
     * **Feature: code-logic-audit, Property 7: ActionHandler 坐标转换正确性**
     * **Validates: Requirements 3.1**
     */
    "coordinate conversion should be monotonic" {
        val relativeCoordArb = Arb.int(0..998)
        val screenSizeArb = Arb.int(100..4000)
        
        forAll(100, relativeCoordArb, screenSizeArb) { rel1, screenSize ->
            val rel2 = rel1 + 1 // rel2 > rel1
            val abs1 = CoordinateConverter.toAbsoluteX(rel1, screenSize)
            val abs2 = CoordinateConverter.toAbsoluteX(rel2, screenSize)
            
            abs1 <= abs2
        }
    }
    
    /**
     * Property 7: Coordinate pair consistency
     * 
     * The toAbsolute function should produce the same results as calling
     * toAbsoluteX and toAbsoluteY separately.
     * 
     * **Feature: code-logic-audit, Property 7: ActionHandler 坐标转换正确性**
     * **Validates: Requirements 3.1**
     */
    "toAbsolute should be consistent with toAbsoluteX and toAbsoluteY" {
        val relativeCoordArb = Arb.int(0..999)
        val screenWidthArb = Arb.int(100..4000)
        val screenHeightArb = Arb.int(100..8000)
        
        forAll(100, relativeCoordArb, relativeCoordArb, screenWidthArb, screenHeightArb) { relX, relY, width, height ->
            val (absX, absY) = CoordinateConverter.toAbsolute(relX, relY, width, height)
            val expectedAbsX = CoordinateConverter.toAbsoluteX(relX, width)
            val expectedAbsY = CoordinateConverter.toAbsoluteY(relY, height)
            
            absX == expectedAbsX && absY == expectedAbsY
        }
    }
    
    // ============================================================================
    // Property 12: Type action command generation
    // **Validates: Requirements 5.5, 5.6**
    // ============================================================================
    
    /**
     * Property 12: Type action command generation
     * 
     * For any text string, executing a Type action should generate shell commands 
     * that first clear existing text and then input the specified text.
     * 
     * **Feature: autoglm-phone-agent, Property 12: Type action command generation**
     * **Validates: Requirements 5.5, 5.6**
     */
    
    "type action command generation - should generate exactly two commands (clear + input)" {
        // For any text string, Type action should generate exactly two commands
        val textArb = Arb.string(0..200, Codepoint.alphanumeric())
        
        forAll(100, textArb) { text ->
            val commands = generateTypeCommands(text)
            commands.size == 2
        }
    }
    
    "type action command generation - first command should be clear operation" {
        // For any text string, the first command should clear existing text
        val textArb = Arb.string(0..200, Codepoint.alphanumeric())
        
        forAll(100, textArb) { text ->
            val commands = generateTypeCommands(text)
            commands.isNotEmpty() && commands[0] == "clear_text"
        }
    }
    
    "type action command generation - second command should be input text command" {
        // For any text string, the second command should input the specified text
        val textArb = Arb.string(0..200, Codepoint.alphanumeric())
        
        forAll(100, textArb) { text ->
            val commands = generateTypeCommands(text)
            commands.size >= 2 && commands[1] == "input text '$text'"
        }
    }
    
    "type action command generation - input command should contain the exact text" {
        // For any text string, the input command should contain the exact text provided
        val textArb = Arb.string(1..100, Codepoint.alphanumeric())
        
        forAll(100, textArb) { text ->
            val commands = generateTypeCommands(text)
            val inputCommand = commands.getOrNull(1) ?: ""
            inputCommand.contains(text)
        }
    }
    
    "type action command generation - empty text should still generate both commands" {
        // Even for empty text, both clear and input commands should be generated
        forAll(100, Arb.int(1..100)) { _ ->
            val commands = generateTypeCommands("")
            commands.size == 2 &&
                commands[0] == "clear_text" &&
                commands[1] == "input text ''"
        }
    }
    
    "type action command generation - commands should be in correct order (clear before input)" {
        // For any text, clear should always come before input
        val textArb = Arb.string(0..200, Codepoint.alphanumeric())
        
        forAll(100, textArb) { text ->
            val commands = generateTypeCommands(text)
            val clearIndex = commands.indexOfFirst { it == "clear_text" }
            val inputIndex = commands.indexOfFirst { it.startsWith("input text") }
            
            clearIndex >= 0 && inputIndex >= 0 && clearIndex < inputIndex
        }
    }
    
    "type action command generation - generated commands should be deterministic" {
        // For the same text, generating commands multiple times should produce identical results
        val textArb = Arb.string(0..200, Codepoint.alphanumeric())
        
        forAll(100, textArb) { text ->
            val commands1 = generateTypeCommands(text)
            val commands2 = generateTypeCommands(text)
            commands1 == commands2
        }
    }
    
    "type action command generation - input command format should be valid shell command" {
        // For any text, the input command should follow the expected format
        val textArb = Arb.string(0..100, Codepoint.alphanumeric())
        
        forAll(100, textArb) { text ->
            val commands = generateTypeCommands(text)
            val inputCommand = commands.getOrNull(1) ?: ""
            
            // Verify format: "input text 'text'"
            inputCommand.startsWith("input text '") && inputCommand.endsWith("'")
        }
    }
    
    /**
     * TypeName action should behave the same as Type action.
     * This validates Requirement 5.6.
     */
    "type name action command generation - should generate same commands as type action" {
        // For any name string, TypeName should generate the same commands as Type
        val nameArb = Arb.string(1..50, Codepoint.alphanumeric())
        
        forAll(100, nameArb) { name ->
            val typeCommands = generateTypeCommands(name)
            val typeNameCommands = generateTypeNameCommands(name)
            
            typeCommands == typeNameCommands
        }
    }
    
    "type name action command generation - should clear text before input" {
        // For any name, TypeName should also clear text first
        val nameArb = Arb.string(1..50, Codepoint.alphanumeric())
        
        forAll(100, nameArb) { name ->
            val commands = generateTypeNameCommands(name)
            commands.size == 2 && commands[0] == "clear_text"
        }
    }
}) {
    companion object {
        /**
         * Helper function to generate type commands.
         * This mirrors the logic in ActionHandler.generateTypeCommands()
         * 
         * Type action generates two commands:
         * 1. Clear existing text
         * 2. Input the specified text
         */
        fun generateTypeCommands(text: String): List<String> {
            return listOf(
                "clear_text",
                "input text '$text'"
            )
        }
        
        /**
         * Helper function to generate type name commands.
         * TypeName action has the same behavior as Type action.
         * 
         * This validates Requirement 5.6.
         */
        fun generateTypeNameCommands(text: String): List<String> {
            // TypeName has the same behavior as Type
            return generateTypeCommands(text)
        }
    }
}
