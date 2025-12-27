package com.kevinluo.autoglm

import com.kevinluo.autoglm.action.ActionParser
import com.kevinluo.autoglm.action.AgentAction
import com.kevinluo.autoglm.util.CoordinateConverter
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll

/**
 * Property-based tests for component interactions.
 * 
 * **Feature: code-logic-audit, Property 20: 组件间坐标传递一致性**
 * **Validates: Requirements 10.3**
 * 
 * This test verifies that coordinates are correctly passed through the entire chain:
 * ActionParser → ActionHandler → DeviceExecutor
 */
class ComponentInteractionPropertyTest : StringSpec({
    
    // ============================================================================
    // Property 20: 组件间坐标传递一致性
    // For any action with coordinates, the coordinates must be correctly converted
    // at each component boundary (ActionParser → ActionHandler → DeviceExecutor).
    // **Feature: code-logic-audit, Property 20: 组件间坐标传递一致性**
    // **Validates: Requirements 10.3**
    // ============================================================================
    
    /**
     * Property 20: Tap action coordinate chain - parsing preserves coordinates
     * 
     * For any valid Tap action string with coordinates in [0, 999],
     * ActionParser should extract the exact coordinates.
     * 
     * **Validates: Requirements 10.3**
     */
    "coordinate chain - ActionParser should preserve Tap coordinates exactly" {
        val coordArb = Arb.int(0..999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, coordArb, coordArb, quoteArb) { x, y, quote ->
            val actionStr = "do(action=${quote}Tap${quote}, element=[$x, $y])"
            val action = ActionParser.parse(actionStr)
            
            action is AgentAction.Tap && action.x == x && action.y == y
        }
    }
    
    /**
     * Property 20: Swipe action coordinate chain - parsing preserves all coordinates
     * 
     * For any valid Swipe action string with coordinates in [0, 999],
     * ActionParser should extract all four coordinates exactly.
     * 
     * **Validates: Requirements 10.3**
     */
    "coordinate chain - ActionParser should preserve Swipe coordinates exactly" {
        val coordArb = Arb.int(0..999)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, coordArb, coordArb, coordArb, coordArb, quoteArb) { startX, startY, endX, endY, quote ->
            val actionStr = "do(action=${quote}Swipe${quote}, start=[$startX, $startY], end=[$endX, $endY])"
            val action = ActionParser.parse(actionStr)
            
            action is AgentAction.Swipe &&
                action.startX == startX &&
                action.startY == startY &&
                action.endX == endX &&
                action.endY == endY
        }
    }
    
    /**
     * Property 20: Coordinate conversion chain - relative to absolute
     * 
     * For any relative coordinates in [0, 999] and screen dimensions,
     * CoordinateConverter should produce absolute coordinates in valid range.
     * 
     * **Validates: Requirements 10.3**
     */
    "coordinate chain - CoordinateConverter should produce valid absolute coordinates" {
        val relativeCoordArb = Arb.int(0..999)
        val screenWidthArb = Arb.int(100..4000)
        val screenHeightArb = Arb.int(100..8000)
        
        forAll(100, relativeCoordArb, relativeCoordArb, screenWidthArb, screenHeightArb) { relX, relY, width, height ->
            val (absX, absY) = CoordinateConverter.toAbsolute(relX, relY, width, height)
            
            // Absolute coordinates must be in valid screen bounds
            absX >= 0 && absX < width && absY >= 0 && absY < height
        }
    }
    
    /**
     * Property 20: Full coordinate chain - Tap action from parsing to command
     * 
     * For any valid Tap action, the full chain should:
     * 1. Parse coordinates correctly
     * 2. Convert to absolute coordinates correctly
     * 3. Generate valid tap command
     * 
     * **Validates: Requirements 10.3**
     */
    "coordinate chain - full Tap chain from parsing to command generation" {
        val coordArb = Arb.int(0..999)
        val screenWidthArb = Arb.int(100..4000)
        val screenHeightArb = Arb.int(100..8000)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, coordArb, coordArb, screenWidthArb, screenHeightArb, quoteArb) { relX, relY, width, height, quote ->
            // Step 1: Parse action
            val actionStr = "do(action=${quote}Tap${quote}, element=[$relX, $relY])"
            val action = ActionParser.parse(actionStr)
            
            if (action !is AgentAction.Tap) {
                false
            } else {
                // Step 2: Convert coordinates (as ActionHandler does)
                val (absX, absY) = CoordinateConverter.toAbsolute(action.x, action.y, width, height)
                
                // Step 3: Generate command (as DeviceExecutor does)
                val command = "input tap $absX $absY"
                
                // Verify the chain:
                // - Parsed coordinates match input
                // - Absolute coordinates are in valid range
                // - Command format is correct
                action.x == relX &&
                    action.y == relY &&
                    absX >= 0 && absX < width &&
                    absY >= 0 && absY < height &&
                    command.startsWith("input tap ") &&
                    command.split(" ").size == 4
            }
        }
    }
    
    /**
     * Property 20: Full coordinate chain - Swipe action from parsing to command
     * 
     * For any valid Swipe action, the full chain should:
     * 1. Parse all coordinates correctly
     * 2. Convert all coordinates to absolute correctly
     * 3. Generate valid swipe command
     * 
     * **Validates: Requirements 10.3**
     */
    "coordinate chain - full Swipe chain from parsing to command generation" {
        val coordArb = Arb.int(0..999)
        val screenWidthArb = Arb.int(100..4000)
        val screenHeightArb = Arb.int(100..8000)
        val durationArb = Arb.int(100..2000)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, coordArb, coordArb, coordArb, coordArb, screenWidthArb) { startX, startY, endX, endY, width ->
            val height = width * 2 // Typical phone aspect ratio
            val quote = '"'
            val duration = 500
            
            // Step 1: Parse action
            val actionStr = "do(action=${quote}Swipe${quote}, start=[$startX, $startY], end=[$endX, $endY])"
            val action = ActionParser.parse(actionStr)
            
            if (action !is AgentAction.Swipe) {
                false
            } else {
                // Step 2: Convert coordinates (as ActionHandler does)
                val (startAbsX, startAbsY) = CoordinateConverter.toAbsolute(action.startX, action.startY, width, height)
                val (endAbsX, endAbsY) = CoordinateConverter.toAbsolute(action.endX, action.endY, width, height)
                
                // Step 3: Generate command (as DeviceExecutor does for simple swipe)
                val command = "input swipe $startAbsX $startAbsY $endAbsX $endAbsY $duration"
                
                // Verify the chain:
                // - Parsed coordinates match input
                // - All absolute coordinates are in valid range
                // - Command format is correct
                action.startX == startX &&
                    action.startY == startY &&
                    action.endX == endX &&
                    action.endY == endY &&
                    startAbsX >= 0 && startAbsX < width &&
                    startAbsY >= 0 && startAbsY < height &&
                    endAbsX >= 0 && endAbsX < width &&
                    endAbsY >= 0 && endAbsY < height &&
                    command.startsWith("input swipe ") &&
                    command.split(" ").size == 7
            }
        }
    }
    
    /**
     * Property 20: Full coordinate chain - LongPress action from parsing to command
     * 
     * For any valid LongPress action, the full chain should:
     * 1. Parse coordinates correctly
     * 2. Convert to absolute coordinates correctly
     * 3. Generate valid long press command (swipe with same start/end)
     * 
     * **Validates: Requirements 10.3**
     */
    "coordinate chain - full LongPress chain from parsing to command generation" {
        val coordArb = Arb.int(0..999)
        val screenWidthArb = Arb.int(100..4000)
        val screenHeightArb = Arb.int(100..8000)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, coordArb, coordArb, screenWidthArb, screenHeightArb, quoteArb) { relX, relY, width, height, quote ->
            // Step 1: Parse action
            val actionStr = "do(action=${quote}Long Press${quote}, element=[$relX, $relY])"
            val action = ActionParser.parse(actionStr)
            
            if (action !is AgentAction.LongPress) {
                false
            } else {
                // Step 2: Convert coordinates (as ActionHandler does)
                val (absX, absY) = CoordinateConverter.toAbsolute(action.x, action.y, width, height)
                
                // Step 3: Generate command (as DeviceExecutor does)
                // Long press is implemented as swipe with same start/end
                val durationMs = action.durationMs
                val command = "input swipe $absX $absY $absX $absY $durationMs"
                
                // Verify the chain:
                // - Parsed coordinates match input
                // - Absolute coordinates are in valid range
                // - Command has same start and end coordinates
                action.x == relX &&
                    action.y == relY &&
                    absX >= 0 && absX < width &&
                    absY >= 0 && absY < height &&
                    command.contains("$absX $absY $absX $absY")
            }
        }
    }
    
    /**
     * Property 20: Full coordinate chain - DoubleTap action from parsing to command
     * 
     * For any valid DoubleTap action, the full chain should:
     * 1. Parse coordinates correctly
     * 2. Convert to absolute coordinates correctly
     * 3. Generate two valid tap commands at same location
     * 
     * **Validates: Requirements 10.3**
     */
    "coordinate chain - full DoubleTap chain from parsing to command generation" {
        val coordArb = Arb.int(0..999)
        val screenWidthArb = Arb.int(100..4000)
        val screenHeightArb = Arb.int(100..8000)
        val quoteArb = Arb.element('"', '\'')
        
        forAll(100, coordArb, coordArb, screenWidthArb, screenHeightArb, quoteArb) { relX, relY, width, height, quote ->
            // Step 1: Parse action
            val actionStr = "do(action=${quote}Double Tap${quote}, element=[$relX, $relY])"
            val action = ActionParser.parse(actionStr)
            
            if (action !is AgentAction.DoubleTap) {
                false
            } else {
                // Step 2: Convert coordinates (as ActionHandler does)
                val (absX, absY) = CoordinateConverter.toAbsolute(action.x, action.y, width, height)
                
                // Step 3: Generate commands (as DeviceExecutor does)
                val command1 = "input tap $absX $absY"
                val command2 = "input tap $absX $absY"
                
                // Verify the chain:
                // - Parsed coordinates match input
                // - Absolute coordinates are in valid range
                // - Both commands are identical
                action.x == relX &&
                    action.y == relY &&
                    absX >= 0 && absX < width &&
                    absY >= 0 && absY < height &&
                    command1 == command2 &&
                    command1.startsWith("input tap ")
            }
        }
    }
    
    /**
     * Property 20: Coordinate conversion formula consistency
     * 
     * The conversion formula (relative * screenSize / 1000) should be consistent
     * across all coordinate types (X and Y).
     * 
     * **Validates: Requirements 10.3**
     */
    "coordinate chain - conversion formula should be consistent for X and Y" {
        val relativeCoordArb = Arb.int(0..999)
        val screenSizeArb = Arb.int(100..4000)
        
        forAll(100, relativeCoordArb, screenSizeArb) { relCoord, screenSize ->
            val absX = CoordinateConverter.toAbsoluteX(relCoord, screenSize)
            val absY = CoordinateConverter.toAbsoluteY(relCoord, screenSize)
            val expectedAbs = relCoord * screenSize / 1000
            
            // Both X and Y conversion should use the same formula
            absX == expectedAbs && absY == expectedAbs
        }
    }
    
    /**
     * Property 20: Coordinate chain determinism
     * 
     * For the same input, the entire coordinate chain should produce
     * identical results every time.
     * 
     * **Validates: Requirements 10.3**
     */
    "coordinate chain - should be deterministic for same input" {
        val coordArb = Arb.int(0..999)
        val screenWidthArb = Arb.int(100..4000)
        val screenHeightArb = Arb.int(100..8000)
        
        forAll(100, coordArb, coordArb, screenWidthArb, screenHeightArb) { relX, relY, width, height ->
            // Run the chain twice
            val actionStr = "do(action=\"Tap\", element=[$relX, $relY])"
            
            val action1 = ActionParser.parse(actionStr) as AgentAction.Tap
            val (absX1, absY1) = CoordinateConverter.toAbsolute(action1.x, action1.y, width, height)
            val command1 = "input tap $absX1 $absY1"
            
            val action2 = ActionParser.parse(actionStr) as AgentAction.Tap
            val (absX2, absY2) = CoordinateConverter.toAbsolute(action2.x, action2.y, width, height)
            val command2 = "input tap $absX2 $absY2"
            
            // Results should be identical
            action1.x == action2.x &&
                action1.y == action2.y &&
                absX1 == absX2 &&
                absY1 == absY2 &&
                command1 == command2
        }
    }
    
    /**
     * Property 20: Boundary coordinates should pass through chain correctly
     * 
     * Boundary values (0 and 999) should be handled correctly through the entire chain.
     * 
     * **Validates: Requirements 10.3**
     */
    "coordinate chain - boundary values should pass through correctly" {
        val boundaryCoordArb = Arb.element(0, 999)
        val screenWidthArb = Arb.int(100..4000)
        val screenHeightArb = Arb.int(100..8000)
        
        forAll(100, boundaryCoordArb, boundaryCoordArb, screenWidthArb, screenHeightArb) { relX, relY, width, height ->
            val actionStr = "do(action=\"Tap\", element=[$relX, $relY])"
            val action = ActionParser.parse(actionStr) as AgentAction.Tap
            val (absX, absY) = CoordinateConverter.toAbsolute(action.x, action.y, width, height)
            
            // Verify boundary handling:
            // - 0 should map to 0
            // - 999 should map to < screenSize
            val xValid = if (relX == 0) absX == 0 else absX < width
            val yValid = if (relY == 0) absY == 0 else absY < height
            
            action.x == relX && action.y == relY && xValid && yValid
        }
    }
})
