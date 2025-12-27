package com.kevinluo.autoglm.action

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll

/**
 * Property-based tests for ActionParser.
 * 
 * **Feature: autoglm-phone-agent, Property 7: Finish action detection**
 * **Validates: Requirements 4.5**
 * 
 * **Feature: autoglm-phone-agent, Property 8: Do action extraction**
 * **Validates: Requirements 4.6**
 * 
 * **Feature: code-logic-audit, Property 4: ActionParser 坐标验证**
 * **Validates: Requirements 2.1, 2.2**
 */
class ActionParserPropertyTest : StringSpec({
    
    /**
     * Property 7: Finish action detection
     * 
     * For any model response containing "finish(message=", the parsed action 
     * should be a Finish action with the correct message extracted.
     * 
     * **Validates: Requirements 4.5**
     */
    "finish action detection - any finish response should parse to Finish action with correct message" {
        // Generator for finish messages - alphanumeric strings to avoid quote escaping issues
        val messageArb = Arb.string(0..200, Codepoint.alphanumeric())
        
        // Generator for quote style (single or double)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, messageArb, quoteArb) { message, quote ->
            // Construct a valid finish action string
            val finishResponse = "finish(message=$quote$message$quote)"
            
            // Parse the response
            val action = ActionParser.parse(finishResponse)
            
            // Verify it's a Finish action with the correct message
            action is AgentAction.Finish && action.message == message
        }
    }
    
    "finish action detection - finish with leading/trailing spaces should still parse correctly" {
        val messageArb = Arb.string(1..100, Codepoint.alphanumeric())
        
        forAll(100, messageArb) { message ->
            // Construct finish action with leading/trailing whitespace
            val finishResponse = "  finish(message=\"$message\")  "
            
            // Parse the response
            val action = ActionParser.parse(finishResponse)
            
            // Verify it's a Finish action with the correct message
            action is AgentAction.Finish && action.message == message
        }
    }
    
    "finish action detection - empty message should parse correctly" {
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, quoteArb) { quote ->
            val finishResponse = "finish(message=$quote$quote)"
            
            val action = ActionParser.parse(finishResponse)
            
            action is AgentAction.Finish && action.message == ""
        }
    }
    
    "finish action detection - message with spaces should parse correctly" {
        // Generator for words
        val wordArb = Arb.string(1..20, Codepoint.alphanumeric())
        val wordCountArb = Arb.int(1..10)
        
        forAll(100, wordArb, wordCountArb) { word, wordCount ->
            // Create a message with spaces
            val message = (1..wordCount).joinToString(" ") { "$word$it" }
            val finishResponse = "finish(message=\"$message\")"
            
            val action = ActionParser.parse(finishResponse)
            
            action is AgentAction.Finish && action.message == message
        }
    }
    
    // ============================================================================
    // Property 8: Do action extraction
    // For any model response containing "do(action=", the parsed action should be
    // the corresponding AgentAction type with all parameters correctly extracted.
    // **Validates: Requirements 4.6**
    // ============================================================================
    
    /**
     * Property 8: Do action extraction - Tap action
     * 
     * For any valid Tap action response with coordinates in range [0, 999],
     * parsing should produce a Tap action with the correct coordinates.
     * 
     * **Validates: Requirements 4.6**
     */
    "do action extraction - Tap action should parse with correct coordinates" {
        val coordArb = Arb.int(0..999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, coordArb, coordArb, quoteArb) { x, y, quote ->
            val doResponse = "do(action=${quote}Tap${quote}, element=[$x, $y])"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Tap && action.x == x && action.y == y
        }
    }
    
    /**
     * Property 8: Do action extraction - Swipe action
     * 
     * For any valid Swipe action response with start and end coordinates,
     * parsing should produce a Swipe action with all coordinates correctly extracted.
     * 
     * **Validates: Requirements 4.6**
     */
    "do action extraction - Swipe action should parse with correct start and end coordinates" {
        val coordArb = Arb.int(0..999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, coordArb, coordArb, coordArb, coordArb, quoteArb) { startX, startY, endX, endY, quote ->
            val doResponse = "do(action=${quote}Swipe${quote}, start=[$startX, $startY], end=[$endX, $endY])"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Swipe &&
                action.startX == startX &&
                action.startY == startY &&
                action.endX == endX &&
                action.endY == endY
        }
    }
    
    /**
     * Property 8: Do action extraction - Type action
     * 
     * For any valid Type action response with alphanumeric text,
     * parsing should produce a Type action with the correct text.
     * 
     * **Validates: Requirements 4.6**
     */
    "do action extraction - Type action should parse with correct text" {
        val textArb = Arb.string(0..100, Codepoint.alphanumeric())
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, textArb, quoteArb) { text, quote ->
            val doResponse = "do(action=${quote}Type${quote}, text=${quote}$text${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Type && action.text == text
        }
    }
    
    /**
     * Property 8: Do action extraction - Type_Name action
     * 
     * For any valid Type_Name action response with alphanumeric text,
     * parsing should produce a TypeName action with the correct text.
     * 
     * **Validates: Requirements 4.6**
     */
    "do action extraction - Type_Name action should parse with correct text" {
        val textArb = Arb.string(1..50, Codepoint.alphanumeric())
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, textArb, quoteArb) { text, quote ->
            val doResponse = "do(action=${quote}Type_Name${quote}, text=${quote}$text${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.TypeName && action.text == text
        }
    }
    
    /**
     * Property 8: Do action extraction - Launch action
     * 
     * For any valid Launch action response with an app name,
     * parsing should produce a Launch action with the correct app name.
     * 
     * **Validates: Requirements 4.6**
     */
    "do action extraction - Launch action should parse with correct app name" {
        val appNameArb = Arb.string(1..50, Codepoint.alphanumeric())
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, appNameArb, quoteArb) { appName, quote ->
            val doResponse = "do(action=${quote}Launch${quote}, app=${quote}$appName${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Launch && action.app == appName
        }
    }
    
    /**
     * Property 8: Do action extraction - Simple actions (Back, Home, VolumeUp, VolumeDown, Power)
     * 
     * For any simple action without parameters, parsing should produce the correct action type.
     * 
     * **Validates: Requirements 4.6**
     */
    "do action extraction - Back action should parse correctly" {
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, quoteArb) { quote ->
            val doResponse = "do(action=${quote}Back${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Back
        }
    }
    
    "do action extraction - Home action should parse correctly" {
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, quoteArb) { quote ->
            val doResponse = "do(action=${quote}Home${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Home
        }
    }
    
    "do action extraction - VolumeUp action should parse correctly" {
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, quoteArb) { quote ->
            val doResponse = "do(action=${quote}VolumeUp${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.VolumeUp
        }
    }
    
    "do action extraction - VolumeDown action should parse correctly" {
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, quoteArb) { quote ->
            val doResponse = "do(action=${quote}VolumeDown${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.VolumeDown
        }
    }
    
    "do action extraction - Power action should parse correctly" {
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, quoteArb) { quote ->
            val doResponse = "do(action=${quote}Power${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Power
        }
    }
    
    /**
     * Property 8: Do action extraction - Long Press action
     * 
     * For any valid Long Press action with coordinates,
     * parsing should produce a LongPress action with correct coordinates.
     * 
     * **Validates: Requirements 4.6**
     */
    "do action extraction - Long Press action should parse with correct coordinates" {
        val coordArb = Arb.int(0..999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, coordArb, coordArb, quoteArb) { x, y, quote ->
            val doResponse = "do(action=${quote}Long Press${quote}, element=[$x, $y])"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.LongPress && action.x == x && action.y == y
        }
    }
    
    /**
     * Property 8: Do action extraction - Double Tap action
     * 
     * For any valid Double Tap action with coordinates,
     * parsing should produce a DoubleTap action with correct coordinates.
     * 
     * **Validates: Requirements 4.6**
     */
    "do action extraction - Double Tap action should parse with correct coordinates" {
        val coordArb = Arb.int(0..999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, coordArb, coordArb, quoteArb) { x, y, quote ->
            val doResponse = "do(action=${quote}Double Tap${quote}, element=[$x, $y])"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.DoubleTap && action.x == x && action.y == y
        }
    }
    
    /**
     * Property 8: Do action extraction - Wait action
     * 
     * For any valid Wait action with duration,
     * parsing should produce a Wait action with correct duration.
     * 
     * **Validates: Requirements 4.6**
     */
    "do action extraction - Wait action should parse with correct duration" {
        val durationArb = Arb.int(1..60)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, durationArb, quoteArb) { duration, quote ->
            val doResponse = "do(action=${quote}Wait${quote}, duration=${quote}$duration${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Wait && action.durationSeconds == duration.toFloat()
        }
    }
    
    /**
     * Property 8: Do action extraction - Take_over action
     * 
     * For any valid Take_over action with message,
     * parsing should produce a TakeOver action with correct message.
     * 
     * **Validates: Requirements 4.6**
     */
    "do action extraction - Take_over action should parse with correct message" {
        val messageArb = Arb.string(1..100, Codepoint.alphanumeric())
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, messageArb, quoteArb) { message, quote ->
            val doResponse = "do(action=${quote}Take_over${quote}, message=${quote}$message${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.TakeOver && action.message == message
        }
    }
    
    /**
     * Property 8: Do action extraction - Note action
     * 
     * For any valid Note action with message,
     * parsing should produce a Note action with correct message.
     * 
     * **Validates: Requirements 4.6**
     */
    "do action extraction - Note action should parse with correct message" {
        val messageArb = Arb.string(1..100, Codepoint.alphanumeric())
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, messageArb, quoteArb) { message, quote ->
            val doResponse = "do(action=${quote}Note${quote}, message=${quote}$message${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Note && action.message == message
        }
    }
    
    /**
     * Property 8: Do action extraction - Call_API action
     * 
     * For any valid Call_API action with instruction,
     * parsing should produce a CallApi action with correct instruction.
     * 
     * **Validates: Requirements 4.6**
     */
    "do action extraction - Call_API action should parse with correct instruction" {
        val instructionArb = Arb.string(1..100, Codepoint.alphanumeric())
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, instructionArb, quoteArb) { instruction, quote ->
            val doResponse = "do(action=${quote}Call_API${quote}, instruction=${quote}$instruction${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.CallApi && action.instruction == instruction
        }
    }
    
    /**
     * Property 8: Do action extraction - Tap with sensitive message
     * 
     * For any valid Tap action with coordinates and optional message,
     * parsing should produce a Tap action with correct coordinates and message.
     * 
     * **Validates: Requirements 4.6**
     */
    "do action extraction - Tap action with message should parse correctly" {
        val coordArb = Arb.int(0..999)
        val messageArb = Arb.string(1..50, Codepoint.alphanumeric())
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, coordArb, coordArb, messageArb, quoteArb) { x, y, message, quote ->
            val doResponse = "do(action=${quote}Tap${quote}, element=[$x, $y], message=${quote}$message${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Tap && 
                action.x == x && 
                action.y == y && 
                action.message == message
        }
    }
    
    // ============================================================================
    // Property 4: ActionParser 坐标验证
    // For any action string containing coordinates, if any coordinate is outside
    // the range [0, 999], the parser must throw CoordinateOutOfRangeException
    // with details about which coordinates are invalid.
    // **Feature: code-logic-audit, Property 4: ActionParser 坐标验证**
    // **Validates: Requirements 2.1, 2.2**
    // ============================================================================
    
    /**
     * Property 4: Coordinate validation - Tap action with out-of-range coordinates
     * 
     * For any Tap action with coordinates outside [0, 999], parsing should throw
     * CoordinateOutOfRangeException with the invalid coordinate details.
     * 
     * **Validates: Requirements 2.1, 2.2**
     */
    "coordinate validation - Tap with x > 999 should throw CoordinateOutOfRangeException" {
        // Generate x values > 999
        val invalidXArb = Arb.int(1000..9999)
        val validYArb = Arb.int(0..999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, invalidXArb, validYArb, quoteArb) { x, y, quote ->
            val doResponse = "do(action=${quote}Tap${quote}, element=[$x, $y])"
            
            val exception = shouldThrow<CoordinateOutOfRangeException> {
                ActionParser.parse(doResponse)
            }
            
            // Verify the exception contains the invalid x coordinate
            exception.invalidCoordinates.any { it.name == "x" && it.value == x } &&
                exception.originalAction == doResponse
        }
    }
    
    "coordinate validation - Tap with y > 999 should throw CoordinateOutOfRangeException" {
        val validXArb = Arb.int(0..999)
        val invalidYArb = Arb.int(1000..9999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, validXArb, invalidYArb, quoteArb) { x, y, quote ->
            val doResponse = "do(action=${quote}Tap${quote}, element=[$x, $y])"
            
            val exception = shouldThrow<CoordinateOutOfRangeException> {
                ActionParser.parse(doResponse)
            }
            
            exception.invalidCoordinates.any { it.name == "y" && it.value == y } &&
                exception.originalAction == doResponse
        }
    }
    
    "coordinate validation - Tap with both coordinates > 999 should report both in exception" {
        val invalidXArb = Arb.int(1000..9999)
        val invalidYArb = Arb.int(1000..9999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, invalidXArb, invalidYArb, quoteArb) { x, y, quote ->
            val doResponse = "do(action=${quote}Tap${quote}, element=[$x, $y])"
            
            val exception = shouldThrow<CoordinateOutOfRangeException> {
                ActionParser.parse(doResponse)
            }
            
            // Both coordinates should be reported as invalid
            exception.invalidCoordinates.size == 2 &&
                exception.invalidCoordinates.any { it.name == "x" && it.value == x } &&
                exception.invalidCoordinates.any { it.name == "y" && it.value == y }
        }
    }
    
    /**
     * Property 4: Coordinate validation - Swipe action with out-of-range coordinates
     * 
     * For any Swipe action with any coordinate outside [0, 999], parsing should throw
     * CoordinateOutOfRangeException with all invalid coordinate details.
     * 
     * **Validates: Requirements 2.1, 2.2**
     */
    "coordinate validation - Swipe with startX > 999 should throw CoordinateOutOfRangeException" {
        val invalidStartXArb = Arb.int(1000..9999)
        val validCoordArb = Arb.int(0..999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, invalidStartXArb, validCoordArb, validCoordArb, validCoordArb, quoteArb) { startX, startY, endX, endY, quote ->
            val doResponse = "do(action=${quote}Swipe${quote}, start=[$startX, $startY], end=[$endX, $endY])"
            
            val exception = shouldThrow<CoordinateOutOfRangeException> {
                ActionParser.parse(doResponse)
            }
            
            exception.invalidCoordinates.any { it.name == "startX" && it.value == startX }
        }
    }
    
    "coordinate validation - Swipe with multiple invalid coordinates should report all" {
        val invalidCoordArb = Arb.int(1000..9999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, invalidCoordArb, invalidCoordArb, invalidCoordArb, invalidCoordArb, quoteArb) { startX, startY, endX, endY, quote ->
            val doResponse = "do(action=${quote}Swipe${quote}, start=[$startX, $startY], end=[$endX, $endY])"
            
            val exception = shouldThrow<CoordinateOutOfRangeException> {
                ActionParser.parse(doResponse)
            }
            
            // All four coordinates should be reported as invalid
            exception.invalidCoordinates.size == 4 &&
                exception.invalidCoordinates.any { it.name == "startX" } &&
                exception.invalidCoordinates.any { it.name == "startY" } &&
                exception.invalidCoordinates.any { it.name == "endX" } &&
                exception.invalidCoordinates.any { it.name == "endY" }
        }
    }
    
    /**
     * Property 4: Coordinate validation - Long Press with out-of-range coordinates
     * 
     * **Validates: Requirements 2.1, 2.2**
     */
    "coordinate validation - Long Press with coordinates > 999 should throw CoordinateOutOfRangeException" {
        val invalidCoordArb = Arb.int(1000..9999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, invalidCoordArb, invalidCoordArb, quoteArb) { x, y, quote ->
            val doResponse = "do(action=${quote}Long Press${quote}, element=[$x, $y])"
            
            val exception = shouldThrow<CoordinateOutOfRangeException> {
                ActionParser.parse(doResponse)
            }
            
            exception.invalidCoordinates.size == 2 &&
                exception.invalidCoordinates.any { it.name == "x" && it.value == x } &&
                exception.invalidCoordinates.any { it.name == "y" && it.value == y }
        }
    }
    
    /**
     * Property 4: Coordinate validation - Double Tap with out-of-range coordinates
     * 
     * **Validates: Requirements 2.1, 2.2**
     */
    "coordinate validation - Double Tap with coordinates > 999 should throw CoordinateOutOfRangeException" {
        val invalidCoordArb = Arb.int(1000..9999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, invalidCoordArb, invalidCoordArb, quoteArb) { x, y, quote ->
            val doResponse = "do(action=${quote}Double Tap${quote}, element=[$x, $y])"
            
            val exception = shouldThrow<CoordinateOutOfRangeException> {
                ActionParser.parse(doResponse)
            }
            
            exception.invalidCoordinates.size == 2 &&
                exception.invalidCoordinates.any { it.name == "x" && it.value == x } &&
                exception.invalidCoordinates.any { it.name == "y" && it.value == y }
        }
    }
    
    /**
     * Property 4: Coordinate validation - Valid coordinates should parse successfully
     * 
     * For any action with coordinates in range [0, 999], parsing should succeed
     * without throwing CoordinateOutOfRangeException.
     * 
     * **Validates: Requirements 2.1, 2.2**
     */
    "coordinate validation - Tap with valid coordinates [0, 999] should parse successfully" {
        val validCoordArb = Arb.int(0..999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, validCoordArb, validCoordArb, quoteArb) { x, y, quote ->
            val doResponse = "do(action=${quote}Tap${quote}, element=[$x, $y])"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Tap && action.x == x && action.y == y
        }
    }
    
    "coordinate validation - Swipe with valid coordinates [0, 999] should parse successfully" {
        val validCoordArb = Arb.int(0..999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, validCoordArb, validCoordArb, validCoordArb, validCoordArb, quoteArb) { startX, startY, endX, endY, quote ->
            val doResponse = "do(action=${quote}Swipe${quote}, start=[$startX, $startY], end=[$endX, $endY])"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Swipe &&
                action.startX == startX &&
                action.startY == startY &&
                action.endX == endX &&
                action.endY == endY
        }
    }
    
    /**
     * Property 4: Coordinate validation - Boundary values
     * 
     * Coordinates at exact boundaries (0 and 999) should be valid.
     * 
     * **Validates: Requirements 2.1, 2.2**
     */
    "coordinate validation - boundary value 0 should be valid" {
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, quoteArb) { quote ->
            val doResponse = "do(action=${quote}Tap${quote}, element=[0, 0])"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Tap && action.x == 0 && action.y == 0
        }
    }
    
    "coordinate validation - boundary value 999 should be valid" {
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, quoteArb) { quote ->
            val doResponse = "do(action=${quote}Tap${quote}, element=[999, 999])"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Tap && action.x == 999 && action.y == 999
        }
    }
    
    "coordinate validation - boundary value 1000 should be invalid" {
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, quoteArb) { quote ->
            val doResponse = "do(action=${quote}Tap${quote}, element=[1000, 1000])"
            
            val exception = shouldThrow<CoordinateOutOfRangeException> {
                ActionParser.parse(doResponse)
            }
            
            exception.invalidCoordinates.size == 2
        }
    }
    
    /**
     * Property 4: Coordinate validation - Negative coordinates should be invalid
     * 
     * For any action with negative coordinates, parsing should throw
     * CoordinateOutOfRangeException.
     * 
     * **Validates: Requirements 2.1, 2.2**
     */
    "coordinate validation - negative coordinates should throw CoordinateOutOfRangeException" {
        val negativeCoordArb = Arb.int(-9999..-1)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, negativeCoordArb, negativeCoordArb, quoteArb) { x, y, quote ->
            val doResponse = "do(action=${quote}Tap${quote}, element=[$x, $y])"
            
            val exception = shouldThrow<CoordinateOutOfRangeException> {
                ActionParser.parse(doResponse)
            }
            
            exception.invalidCoordinates.size == 2 &&
                exception.invalidCoordinates.any { it.name == "x" && it.value == x } &&
                exception.invalidCoordinates.any { it.name == "y" && it.value == y }
        }
    }
    
    /**
     * Property 4: Coordinate validation - Large numbers should throw ActionParseException
     * 
     * For any action with coordinates that would overflow Int, parsing should throw
     * ActionParseException with overflow message.
     * 
     * **Validates: Requirements 2.1, 2.2**
     */
    "coordinate validation - overflow coordinates should throw ActionParseException" {
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, quoteArb) { quote ->
            // Use a number larger than Int.MAX_VALUE
            val doResponse = "do(action=${quote}Tap${quote}, element=[99999999999, 500])"
            
            val exception = shouldThrow<ActionParseException> {
                ActionParser.parse(doResponse)
            }
            
            exception.message?.contains("overflow") == true || 
                exception.message?.contains("too large") == true
        }
    }
    
    // ============================================================================
    // Property 5: ActionParser 文本解析完整性
    // For any Type action string with text content, the parsed text must exactly
    // match the original text including special characters, quotes, and whitespace.
    // **Feature: code-logic-audit, Property 5: ActionParser 文本解析完整性**
    // **Validates: Requirements 2.3**
    // ============================================================================
    
    /**
     * Property 5: Text parsing completeness - alphanumeric text
     * 
     * For any Type action with alphanumeric text, the parsed text must exactly
     * match the original text.
     * 
     * **Validates: Requirements 2.3**
     */
    "text parsing - Type action with alphanumeric text should preserve exact content" {
        val textArb = Arb.string(0..200, Codepoint.alphanumeric())
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, textArb, quoteArb) { text, quote ->
            val doResponse = "do(action=${quote}Type${quote}, text=${quote}$text${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Type && action.text == text
        }
    }
    
    /**
     * Property 5: Text parsing completeness - text with spaces
     * 
     * For any Type action with text containing spaces, the parsed text must
     * preserve all spaces exactly.
     * 
     * **Validates: Requirements 2.3**
     */
    "text parsing - Type action with spaces should preserve whitespace" {
        val wordArb = Arb.string(1..20, Codepoint.alphanumeric())
        val spaceCountArb = Arb.int(1..5)
        
        forAll(100, wordArb, wordArb, spaceCountArb) { word1, word2, spaceCount ->
            val spaces = " ".repeat(spaceCount)
            val text = "$word1$spaces$word2"
            val doResponse = """do(action="Type", text="$text")"""
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Type && action.text == text
        }
    }
    
    /**
     * Property 5: Text parsing completeness - empty text
     * 
     * For any Type action with empty text, the parsed text must be empty string.
     * 
     * **Validates: Requirements 2.3**
     */
    "text parsing - Type action with empty text should return empty string" {
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, quoteArb) { quote ->
            val doResponse = "do(action=${quote}Type${quote}, text=${quote}${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Type && action.text == ""
        }
    }
    
    /**
     * Property 5: Text parsing completeness - Unicode text (Chinese)
     * 
     * For any Type action with Chinese text, the parsed text must preserve
     * all Unicode characters exactly.
     * 
     * **Validates: Requirements 2.3**
     */
    "text parsing - Type action with Chinese text should preserve Unicode" {
        // Common Chinese characters for testing
        val chineseChars = listOf(
            "你好", "世界", "测试", "输入", "文本", "中文", "字符",
            "苹果", "香蕉", "橙子", "葡萄", "西瓜", "草莓", "樱桃"
        )
        val chineseArb = Arb.element(chineseChars)
        val countArb = Arb.int(1..5)
        
        forAll(100, chineseArb, chineseArb, countArb) { word1, word2, _ ->
            val text = "$word1$word2"
            val doResponse = """do(action="Type", text="$text")"""
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Type && action.text == text
        }
    }
    
    /**
     * Property 5: Text parsing completeness - Type_Name action
     * 
     * For any Type_Name action with text, the parsed text must exactly match.
     * 
     * **Validates: Requirements 2.3**
     */
    "text parsing - Type_Name action should preserve exact text content" {
        val textArb = Arb.string(1..100, Codepoint.alphanumeric())
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, textArb, quoteArb) { text, quote ->
            val doResponse = "do(action=${quote}Type_Name${quote}, text=${quote}$text${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.TypeName && action.text == text
        }
    }
    
    /**
     * Property 5: Text parsing completeness - text with numbers
     * 
     * For any Type action with numeric text, the parsed text must preserve
     * all digits exactly.
     * 
     * **Validates: Requirements 2.3**
     */
    "text parsing - Type action with numeric text should preserve digits" {
        val digitArb = Arb.string(1..20, Codepoint.az())
        val numberArb = Arb.int(0..999999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, numberArb, quoteArb) { number, quote ->
            val digits = number.toString()
            val doResponse = "do(action=${quote}Type${quote}, text=${quote}$digits${quote})"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Type && action.text == digits
        }
    }
    
    /**
     * Property 5: Text parsing completeness - text with mixed content
     * 
     * For any Type action with mixed alphanumeric and space content,
     * the parsed text must preserve all characters exactly.
     * 
     * **Validates: Requirements 2.3**
     */
    "text parsing - Type action with mixed alphanumeric and spaces should preserve all" {
        val wordArb = Arb.string(1..10, Codepoint.alphanumeric())
        val numberArb = Arb.int(0..99999)
        
        forAll(100, wordArb, numberArb, wordArb) { word1, number, word2 ->
            val digits = number.toString()
            val text = "$word1 $digits $word2"
            val doResponse = """do(action="Type", text="$text")"""
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Type && action.text == text
        }
    }
    
    /**
     * Property 5: Text parsing completeness - leading/trailing spaces
     * 
     * For any Type action with leading or trailing spaces in text,
     * the parsed text must preserve those spaces.
     * 
     * **Validates: Requirements 2.3**
     */
    "text parsing - Type action should preserve leading and trailing spaces in text" {
        val wordArb = Arb.string(1..20, Codepoint.alphanumeric())
        val leadingSpacesArb = Arb.int(0..3)
        val trailingSpacesArb = Arb.int(0..3)
        
        forAll(100, wordArb, leadingSpacesArb, trailingSpacesArb) { word, leading, trailing ->
            val text = " ".repeat(leading) + word + " ".repeat(trailing)
            val doResponse = """do(action="Type", text="$text")"""
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Type && action.text == text
        }
    }
    
    /**
     * Property 5: Text parsing completeness - escaped double quotes
     * 
     * For any Type action with escaped double quotes in text,
     * the parsed text must correctly unescape them.
     * 
     * **Validates: Requirements 2.3**
     */
    "text parsing - Type action with escaped double quotes should unescape correctly" {
        val wordArb = Arb.string(1..20, Codepoint.alphanumeric())
        
        forAll(100, wordArb, wordArb) { word1, word2 ->
            // Input: text="He said \"hello\""
            // Expected output: He said "hello"
            val escapedText = "$word1 \\\"$word2\\\""
            val expectedText = "$word1 \"$word2\""
            val doResponse = """do(action="Type", text="$escapedText")"""
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Type && action.text == expectedText
        }
    }
    
    /**
     * Property 5: Text parsing completeness - escaped single quotes
     * 
     * For any Type action with escaped single quotes in text,
     * the parsed text must correctly unescape them.
     * 
     * **Validates: Requirements 2.3**
     */
    "text parsing - Type action with escaped single quotes should unescape correctly" {
        val wordArb = Arb.string(1..20, Codepoint.alphanumeric())
        
        forAll(100, wordArb, wordArb) { word1, word2 ->
            // Input: text='He said \'hello\''
            // Expected output: He said 'hello'
            val escapedText = "$word1 \\'$word2\\'"
            val expectedText = "$word1 '$word2'"
            val doResponse = "do(action='Type', text='$escapedText')"
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Type && action.text == expectedText
        }
    }
    
    /**
     * Property 5: Text parsing completeness - escaped backslash
     * 
     * For any Type action with escaped backslash in text,
     * the parsed text must correctly unescape it.
     * 
     * **Validates: Requirements 2.3**
     */
    "text parsing - Type action with escaped backslash should unescape correctly" {
        val wordArb = Arb.string(1..20, Codepoint.alphanumeric())
        
        forAll(100, wordArb) { word ->
            // Input: text="path\\to\\file"
            // Expected output: path\to\file
            val escapedText = "$word\\\\path"
            val expectedText = "$word\\path"
            val doResponse = """do(action="Type", text="$escapedText")"""
            
            val action = ActionParser.parse(doResponse)
            
            action is AgentAction.Type && action.text == expectedText
        }
    }
})
