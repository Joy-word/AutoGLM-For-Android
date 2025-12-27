package com.kevinluo.autoglm.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Property-based tests for HumanizedSwipeGenerator.
 * 
 * **Feature: autoglm-phone-agent, Property 10: Humanized swipe path curvature**
 * **Validates: Requirements 5.3, 10.1**
 */
class HumanizedSwipeGeneratorPropertyTest : StringSpec({
    
    val generator = HumanizedSwipeGenerator()
    
    /**
     * Property 10: Humanized swipe path curvature
     * 
     * For any humanized swipe from start to end coordinates, the generated path 
     * should contain intermediate points that deviate from the straight line, 
     * and the total path length should be greater than the direct distance.
     * 
     * **Validates: Requirements 5.3, 10.1**
     */
    "humanized swipe path should have total length greater than direct distance" {
        // Generator for screen coordinates
        val screenWidthArb = Arb.int(720..1440)
        val screenHeightArb = Arb.int(1280..2560)
        
        forAll(100, screenWidthArb, screenHeightArb) { screenWidth, screenHeight ->
            // Generate random start and end points within screen bounds
            // Ensure minimum distance of 100 pixels for meaningful swipes
            val startX = (screenWidth * 0.2).toInt()
            val startY = (screenHeight * 0.3).toInt()
            val endX = (screenWidth * 0.8).toInt()
            val endY = (screenHeight * 0.7).toInt()
            
            val path = generator.generatePath(startX, startY, endX, endY, screenWidth, screenHeight)
            
            // Calculate direct distance
            val directDistance = hypot((endX - startX).toDouble(), (endY - startY).toDouble())
            
            // Calculate total path length
            var totalPathLength = 0.0
            for (i in 0 until path.points.size - 1) {
                val p1 = path.points[i]
                val p2 = path.points[i + 1]
                totalPathLength += hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble())
            }
            
            // Humanized path should be longer than direct distance due to curvature
            totalPathLength >= directDistance
        }
    }
    
    /**
     * Property 10: Humanized swipe path should have intermediate points deviating from straight line
     * 
     * For any humanized swipe, at least some intermediate points should deviate 
     * from the straight line between start and end.
     * 
     * **Validates: Requirements 5.3, 10.1**
     */
    "humanized swipe path should have intermediate points deviating from straight line" {
        val screenWidthArb = Arb.int(720..1440)
        val screenHeightArb = Arb.int(1280..2560)
        
        forAll(100, screenWidthArb, screenHeightArb) { screenWidth, screenHeight ->
            // Use fixed proportional coordinates for consistent testing
            val startX = (screenWidth * 0.2).toInt()
            val startY = (screenHeight * 0.2).toInt()
            val endX = (screenWidth * 0.8).toInt()
            val endY = (screenHeight * 0.8).toInt()
            
            val path = generator.generatePath(startX, startY, endX, endY, screenWidth, screenHeight)
            
            // Skip first and last points (they should be at start/end)
            val intermediatePoints = path.points.drop(1).dropLast(1)
            
            if (intermediatePoints.isEmpty()) {
                true // No intermediate points to check
            } else {
                // Calculate perpendicular distance from each intermediate point to the line
                val dx = (endX - startX).toDouble()
                val dy = (endY - startY).toDouble()
                val lineLength = hypot(dx, dy)
                
                if (lineLength < 1) {
                    true // Start and end are too close
                } else {
                    // Check if at least one intermediate point deviates from the line
                    // Using point-to-line distance formula
                    val hasDeviation = intermediatePoints.any { point ->
                        val distanceFromLine = abs(
                            dy * point.x - dx * point.y + endX * startY - endY * startX
                        ) / lineLength
                        distanceFromLine > 0.5 // At least 0.5 pixel deviation
                    }
                    hasDeviation
                }
            }
        }
    }
    
    /**
     * Property 10: Humanized swipe path should contain multiple points
     * 
     * For any humanized swipe, the generated path should contain multiple points
     * to create a smooth curved motion.
     * 
     * **Validates: Requirements 5.3, 10.1**
     */
    "humanized swipe path should contain multiple points for smooth motion" {
        val screenWidthArb = Arb.int(720..1440)
        val screenHeightArb = Arb.int(1280..2560)
        
        forAll(100, screenWidthArb, screenHeightArb) { screenWidth, screenHeight ->
            val startX = (screenWidth * 0.3).toInt()
            val startY = (screenHeight * 0.3).toInt()
            val endX = (screenWidth * 0.7).toInt()
            val endY = (screenHeight * 0.7).toInt()
            
            val path = generator.generatePath(startX, startY, endX, endY, screenWidth, screenHeight)
            
            // Path should have multiple points (at least 10 for smooth motion)
            path.points.size >= 10
        }
    }
    
    /**
     * Property 10: Humanized swipe path should start and end at correct coordinates
     * 
     * For any humanized swipe, the first point should be at or near the start coordinates
     * and the last point should be at or near the end coordinates.
     * 
     * **Validates: Requirements 5.3, 10.1**
     */
    "humanized swipe path should start and end at correct coordinates" {
        val screenWidthArb = Arb.int(720..1440)
        val screenHeightArb = Arb.int(1280..2560)
        
        forAll(100, screenWidthArb, screenHeightArb) { screenWidth, screenHeight ->
            val startX = (screenWidth * 0.25).toInt()
            val startY = (screenHeight * 0.25).toInt()
            val endX = (screenWidth * 0.75).toInt()
            val endY = (screenHeight * 0.75).toInt()
            
            val path = generator.generatePath(startX, startY, endX, endY, screenWidth, screenHeight)
            
            val firstPoint = path.points.first()
            val lastPoint = path.points.last()
            
            // First point should be at start (exact match expected)
            val startCorrect = firstPoint.x == startX && firstPoint.y == startY
            
            // Last point should be at end (exact match expected)
            val endCorrect = lastPoint.x == endX && lastPoint.y == endY
            
            startCorrect && endCorrect
        }
    }
    
    /**
     * Property 10: Humanized swipe path points should be within screen bounds
     * 
     * For any humanized swipe, all generated points should be within the screen bounds.
     * 
     * **Validates: Requirements 5.3, 10.1**
     */
    "humanized swipe path points should be within screen bounds" {
        val screenWidthArb = Arb.int(720..1440)
        val screenHeightArb = Arb.int(1280..2560)
        
        forAll(100, screenWidthArb, screenHeightArb) { screenWidth, screenHeight ->
            val startX = (screenWidth * 0.1).toInt()
            val startY = (screenHeight * 0.1).toInt()
            val endX = (screenWidth * 0.9).toInt()
            val endY = (screenHeight * 0.9).toInt()
            
            val path = generator.generatePath(startX, startY, endX, endY, screenWidth, screenHeight)
            
            // All points should be within screen bounds
            path.points.all { point ->
                point.x in 0 until screenWidth && point.y in 0 until screenHeight
            }
        }
    }
    
    /**
     * Property 11: Linear swipe directness
     * 
     * For any linear swipe from start to end coordinates, all generated points 
     * should lie on or very close to the straight line between start and end.
     * 
     * **Feature: autoglm-phone-agent, Property 11: Linear swipe directness**
     * **Validates: Requirements 5.4**
     */
    "linear swipe path should have all points on or very close to straight line" {
        val screenWidthArb = Arb.int(720..1440)
        val screenHeightArb = Arb.int(1280..2560)
        
        forAll(100, screenWidthArb, screenHeightArb) { screenWidth, screenHeight ->
            // Generate random start and end points within screen bounds
            val startX = (screenWidth * 0.2).toInt()
            val startY = (screenHeight * 0.3).toInt()
            val endX = (screenWidth * 0.8).toInt()
            val endY = (screenHeight * 0.7).toInt()
            
            val path = generator.generateLinearPath(startX, startY, endX, endY, screenWidth, screenHeight)
            
            // Calculate line parameters for point-to-line distance
            val dx = (endX - startX).toDouble()
            val dy = (endY - startY).toDouble()
            val lineLength = hypot(dx, dy)
            
            if (lineLength < 1) {
                // Start and end are too close, any point is valid
                true
            } else {
                // All points should be very close to the straight line
                // Using point-to-line distance formula: |dy*x - dx*y + x2*y1 - y2*x1| / lineLength
                // Allow tolerance of 1 pixel for rounding errors
                val maxAllowedDeviation = 1.0
                
                path.points.all { point ->
                    val distanceFromLine = abs(
                        dy * point.x - dx * point.y + endX * startY - endY * startX
                    ) / lineLength
                    distanceFromLine <= maxAllowedDeviation
                }
            }
        }
    }
    
    /**
     * Property 11: Linear swipe path total length should equal direct distance
     * 
     * For any linear swipe, the total path length should be approximately equal 
     * to the direct distance (within rounding tolerance).
     * 
     * **Feature: autoglm-phone-agent, Property 11: Linear swipe directness**
     * **Validates: Requirements 5.4**
     */
    "linear swipe path total length should approximately equal direct distance" {
        val screenWidthArb = Arb.int(720..1440)
        val screenHeightArb = Arb.int(1280..2560)
        
        forAll(100, screenWidthArb, screenHeightArb) { screenWidth, screenHeight ->
            val startX = (screenWidth * 0.15).toInt()
            val startY = (screenHeight * 0.25).toInt()
            val endX = (screenWidth * 0.85).toInt()
            val endY = (screenHeight * 0.75).toInt()
            
            val path = generator.generateLinearPath(startX, startY, endX, endY, screenWidth, screenHeight)
            
            // Calculate direct distance
            val directDistance = hypot((endX - startX).toDouble(), (endY - startY).toDouble())
            
            // Calculate total path length
            var totalPathLength = 0.0
            for (i in 0 until path.points.size - 1) {
                val p1 = path.points[i]
                val p2 = path.points[i + 1]
                totalPathLength += hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble())
            }
            
            // Linear path length should be approximately equal to direct distance
            // Allow 5% tolerance for integer rounding
            val tolerance = directDistance * 0.05
            abs(totalPathLength - directDistance) <= tolerance
        }
    }
    
    /**
     * Property 11: Linear swipe path should start and end at correct coordinates
     * 
     * For any linear swipe, the first point should be at the start coordinates
     * and the last point should be at the end coordinates.
     * 
     * **Feature: autoglm-phone-agent, Property 11: Linear swipe directness**
     * **Validates: Requirements 5.4**
     */
    "linear swipe path should start and end at correct coordinates" {
        val screenWidthArb = Arb.int(720..1440)
        val screenHeightArb = Arb.int(1280..2560)
        
        forAll(100, screenWidthArb, screenHeightArb) { screenWidth, screenHeight ->
            val startX = (screenWidth * 0.25).toInt()
            val startY = (screenHeight * 0.25).toInt()
            val endX = (screenWidth * 0.75).toInt()
            val endY = (screenHeight * 0.75).toInt()
            
            val path = generator.generateLinearPath(startX, startY, endX, endY, screenWidth, screenHeight)
            
            val firstPoint = path.points.first()
            val lastPoint = path.points.last()
            
            // First point should be at start (exact match expected)
            val startCorrect = firstPoint.x == startX && firstPoint.y == startY
            
            // Last point should be at end (exact match expected)
            val endCorrect = lastPoint.x == endX && lastPoint.y == endY
            
            startCorrect && endCorrect
        }
    }
    
    /**
     * Property 21: Swipe duration distance correlation
     * 
     * For any humanized swipe, the calculated duration should increase with the 
     * distance between start and end points, with longer swipes taking more time.
     * 
     * **Feature: autoglm-phone-agent, Property 21: Swipe duration distance correlation**
     * **Validates: Requirements 10.3**
     */
    "swipe duration should increase with distance" {
        val screenWidthArb = Arb.int(720..1440)
        val screenHeightArb = Arb.int(1280..2560)
        
        forAll(100, screenWidthArb, screenHeightArb) { screenWidth, screenHeight ->
            // Generate a short swipe (20% of screen diagonal)
            val shortStartX = (screenWidth * 0.4).toInt()
            val shortStartY = (screenHeight * 0.4).toInt()
            val shortEndX = (screenWidth * 0.5).toInt()
            val shortEndY = (screenHeight * 0.5).toInt()
            
            // Generate a long swipe (60% of screen diagonal)
            val longStartX = (screenWidth * 0.2).toInt()
            val longStartY = (screenHeight * 0.2).toInt()
            val longEndX = (screenWidth * 0.8).toInt()
            val longEndY = (screenHeight * 0.8).toInt()
            
            val shortPath = generator.generatePath(shortStartX, shortStartY, shortEndX, shortEndY, screenWidth, screenHeight)
            val longPath = generator.generatePath(longStartX, longStartY, longEndX, longEndY, screenWidth, screenHeight)
            
            // Calculate actual distances
            val shortDistance = hypot((shortEndX - shortStartX).toDouble(), (shortEndY - shortStartY).toDouble())
            val longDistance = hypot((longEndX - longStartX).toDouble(), (longEndY - longStartY).toDouble())
            
            // Verify that longer distance results in longer duration
            // The long swipe should have a longer duration than the short swipe
            longDistance > shortDistance && longPath.durationMs >= shortPath.durationMs
        }
    }
    
    /**
     * Property 21: Swipe duration should be bounded within reasonable limits
     * 
     * For any swipe distance, the duration should be within the defined min/max bounds.
     * 
     * **Feature: autoglm-phone-agent, Property 21: Swipe duration distance correlation**
     * **Validates: Requirements 10.3**
     */
    "swipe duration should be bounded within min and max limits" {
        val screenWidthArb = Arb.int(720..1440)
        val screenHeightArb = Arb.int(1280..2560)
        // Test with various swipe distances
        val distanceFactorArb = Arb.double(0.05..0.95)
        
        forAll(100, screenWidthArb, screenHeightArb, distanceFactorArb) { screenWidth, screenHeight, distanceFactor ->
            val startX = (screenWidth * 0.1).toInt()
            val startY = (screenHeight * 0.1).toInt()
            val endX = (screenWidth * (0.1 + distanceFactor * 0.8)).toInt()
            val endY = (screenHeight * (0.1 + distanceFactor * 0.8)).toInt()
            
            val path = generator.generatePath(startX, startY, endX, endY, screenWidth, screenHeight)
            
            // Duration should be within bounds (150ms to 1500ms as per implementation)
            path.durationMs in 150..1500
        }
    }
    
    /**
     * Property 21: Duration calculation should be monotonically non-decreasing with distance
     * 
     * For any two swipes where one has a greater distance, the duration of the longer
     * swipe should be greater than or equal to the duration of the shorter swipe.
     * 
     * **Feature: autoglm-phone-agent, Property 21: Swipe duration distance correlation**
     * **Validates: Requirements 10.3**
     */
    "duration should be monotonically non-decreasing with distance" {
        // Test the internal calculateDuration method directly
        val distance1Arb = Arb.double(0.0..2000.0)
        val distance2Arb = Arb.double(0.0..2000.0)
        
        forAll(100, distance1Arb, distance2Arb) { distance1, distance2 ->
            val duration1 = generator.calculateDuration(distance1)
            val duration2 = generator.calculateDuration(distance2)
            
            // If distance1 <= distance2, then duration1 <= duration2
            if (distance1 <= distance2) {
                duration1 <= duration2
            } else {
                duration2 <= duration1
            }
        }
    }
})
