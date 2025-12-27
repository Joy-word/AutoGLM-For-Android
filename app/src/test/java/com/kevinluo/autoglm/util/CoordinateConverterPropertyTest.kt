package com.kevinluo.autoglm.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll

/**
 * Property-based tests for CoordinateConverter.
 * 
 * **Feature: autoglm-phone-agent, Property 9: Coordinate conversion accuracy**
 * **Validates: Requirements 5.1**
 */
class CoordinateConverterPropertyTest : StringSpec({
    
    /**
     * Property 9: Coordinate conversion accuracy
     * 
     * For any relative coordinates (x, y) in the range [0, 1000], converting to absolute 
     * coordinates using screen dimensions (width, height) should produce values in the 
     * range [0, width] and [0, height] respectively, following the formula: 
     * absoluteX = relativeX * width / 1000.
     * 
     * Note: The range is inclusive [0, screenSize] because when relative coord is 1000,
     * the formula produces exactly screenSize (1000 * screenSize / 1000 = screenSize).
     * 
     * **Validates: Requirements 5.1**
     */
    "coordinate conversion should map relative [0,1000] to absolute [0,screenSize]" {
        // Generator for relative coordinates in valid range [0, 1000]
        val relativeCoordArb = Arb.int(0..1000)
        
        // Generator for realistic screen dimensions
        // Width: typical phone widths from 320 to 1440 pixels
        // Height: typical phone heights from 480 to 3200 pixels
        val screenWidthArb = Arb.int(100..4000)
        val screenHeightArb = Arb.int(100..8000)
        
        forAll(100, relativeCoordArb, relativeCoordArb, screenWidthArb, screenHeightArb) { relX, relY, width, height ->
            val absX = CoordinateConverter.toAbsoluteX(relX, width)
            val absY = CoordinateConverter.toAbsoluteY(relY, height)
            
            // Verify absolute coordinates are within valid screen bounds (inclusive)
            absX in 0..width && absY in 0..height
        }
    }
    
    /**
     * Property 9: Coordinate conversion formula correctness
     * 
     * For any relative coordinate and screen dimension, the conversion should follow
     * the exact formula: absolute = relative * screenSize / 1000
     * 
     * **Validates: Requirements 5.1**
     */
    "coordinate conversion should follow the formula: absolute = relative * screenSize / 1000" {
        val relativeCoordArb = Arb.int(0..1000)
        val screenSizeArb = Arb.int(100..4000)
        
        forAll(100, relativeCoordArb, screenSizeArb) { relCoord, screenSize ->
            val expectedAbsolute = relCoord * screenSize / 1000
            val actualAbsoluteX = CoordinateConverter.toAbsoluteX(relCoord, screenSize)
            val actualAbsoluteY = CoordinateConverter.toAbsoluteY(relCoord, screenSize)
            
            actualAbsoluteX == expectedAbsolute && actualAbsoluteY == expectedAbsolute
        }
    }
    
    /**
     * Property 9: Coordinate conversion pair consistency
     * 
     * The toAbsolute function should produce the same results as calling
     * toAbsoluteX and toAbsoluteY separately.
     * 
     * **Validates: Requirements 5.1**
     */
    "toAbsolute should be consistent with toAbsoluteX and toAbsoluteY" {
        val relativeCoordArb = Arb.int(0..1000)
        val screenWidthArb = Arb.int(100..4000)
        val screenHeightArb = Arb.int(100..8000)
        
        forAll(100, relativeCoordArb, relativeCoordArb, screenWidthArb, screenHeightArb) { relX, relY, width, height ->
            val (absX, absY) = CoordinateConverter.toAbsolute(relX, relY, width, height)
            val expectedAbsX = CoordinateConverter.toAbsoluteX(relX, width)
            val expectedAbsY = CoordinateConverter.toAbsoluteY(relY, height)
            
            absX == expectedAbsX && absY == expectedAbsY
        }
    }
    
    /**
     * Property 9: Boundary values - minimum coordinates
     * 
     * Relative coordinate 0 should always map to absolute coordinate 0.
     * 
     * **Validates: Requirements 5.1**
     */
    "relative coordinate 0 should always map to absolute coordinate 0" {
        val screenSizeArb = Arb.int(100..4000)
        
        forAll(100, screenSizeArb, screenSizeArb) { width, height ->
            val absX = CoordinateConverter.toAbsoluteX(0, width)
            val absY = CoordinateConverter.toAbsoluteY(0, height)
            
            absX == 0 && absY == 0
        }
    }
    
    /**
     * Property 9: Boundary values - maximum coordinates
     * 
     * Relative coordinate 1000 should map to the screen dimension value
     * (which is technically at the boundary, but due to integer division
     * it equals screenSize * 1000 / 1000 = screenSize).
     * 
     * **Validates: Requirements 5.1**
     */
    "relative coordinate 1000 should map to screen dimension" {
        val screenSizeArb = Arb.int(100..4000)
        
        forAll(100, screenSizeArb, screenSizeArb) { width, height ->
            val absX = CoordinateConverter.toAbsoluteX(1000, width)
            val absY = CoordinateConverter.toAbsoluteY(1000, height)
            
            // 1000 * screenSize / 1000 = screenSize
            absX == width && absY == height
        }
    }
    
    /**
     * Property 9: Monotonicity - larger relative coordinates produce larger absolute coordinates
     * 
     * For any two relative coordinates where rel1 < rel2, the absolute coordinates
     * should maintain the same ordering: abs1 <= abs2.
     * 
     * **Validates: Requirements 5.1**
     */
    "coordinate conversion should be monotonic" {
        val relativeCoordArb = Arb.int(0..999)
        val screenSizeArb = Arb.int(100..4000)
        
        forAll(100, relativeCoordArb, screenSizeArb) { rel1, screenSize ->
            val rel2 = rel1 + 1 // rel2 > rel1
            val abs1 = CoordinateConverter.toAbsoluteX(rel1, screenSize)
            val abs2 = CoordinateConverter.toAbsoluteX(rel2, screenSize)
            
            abs1 <= abs2
        }
    }
})
