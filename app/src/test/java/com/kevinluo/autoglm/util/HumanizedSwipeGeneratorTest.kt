package com.kevinluo.autoglm.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.random.Random

/**
 * Unit tests for HumanizedSwipeGenerator.
 */
class HumanizedSwipeGeneratorTest : DescribeSpec({
    
    val screenWidth = 1080
    val screenHeight = 1920
    
    describe("HumanizedSwipeGenerator") {
        
        describe("generatePath") {
            it("should generate path with correct number of points") {
                val generator = HumanizedSwipeGenerator()
                val path = generator.generatePath(100, 500, 100, 200, screenWidth, screenHeight)
                
                path.points shouldHaveSize 20 // DEFAULT_POINT_COUNT
            }
            
            it("should start at the start point") {
                val generator = HumanizedSwipeGenerator()
                val path = generator.generatePath(100, 500, 100, 200, screenWidth, screenHeight)
                
                path.points.first().x shouldBe 100
                path.points.first().y shouldBe 500
            }
            
            it("should end at the end point") {
                val generator = HumanizedSwipeGenerator()
                val path = generator.generatePath(100, 500, 100, 200, screenWidth, screenHeight)
                
                path.points.last().x shouldBe 100
                path.points.last().y shouldBe 200
            }
            
            it("should keep all points within screen bounds") {
                val generator = HumanizedSwipeGenerator()
                val path = generator.generatePath(0, 0, screenWidth - 1, screenHeight - 1, screenWidth, screenHeight)
                
                path.points.forEach { point ->
                    point.x shouldBeGreaterThanOrEqual 0
                    point.x shouldBeLessThan screenWidth
                    point.y shouldBeGreaterThanOrEqual 0
                    point.y shouldBeLessThan screenHeight
                }
            }
            
            it("should generate reasonable duration based on distance") {
                val generator = HumanizedSwipeGenerator()
                
                // Short swipe
                val shortPath = generator.generatePath(500, 500, 500, 400, screenWidth, screenHeight)
                shortPath.durationMs shouldBeGreaterThanOrEqual 150
                shortPath.durationMs shouldBeLessThanOrEqual 500
                
                // Long swipe
                val longPath = generator.generatePath(0, 0, 1000, 1800, screenWidth, screenHeight)
                longPath.durationMs shouldBeGreaterThan shortPath.durationMs
            }
        }
        
        describe("generateLinearPath") {
            it("should generate straight line path") {
                val generator = HumanizedSwipeGenerator()
                val path = generator.generateLinearPath(100, 100, 100, 500, screenWidth, screenHeight)
                
                // All points should have the same X coordinate for vertical swipe
                path.points.forEach { point ->
                    point.x shouldBe 100
                }
            }
            
            it("should generate horizontal straight line") {
                val generator = HumanizedSwipeGenerator()
                val path = generator.generateLinearPath(100, 500, 900, 500, screenWidth, screenHeight)
                
                // All points should have the same Y coordinate for horizontal swipe
                path.points.forEach { point ->
                    point.y shouldBe 500
                }
            }
        }
        
        describe("calculateDuration") {
            it("should return minimum duration for very short distances") {
                val generator = HumanizedSwipeGenerator()
                val duration = generator.calculateDuration(10.0)
                
                duration shouldBeGreaterThanOrEqual 150
            }
            
            it("should return maximum duration for very long distances") {
                val generator = HumanizedSwipeGenerator()
                val duration = generator.calculateDuration(10000.0)
                
                duration shouldBeLessThanOrEqual 1500
            }
            
            it("should increase duration with distance") {
                val generator = HumanizedSwipeGenerator()
                
                val shortDuration = generator.calculateDuration(100.0)
                val longDuration = generator.calculateDuration(500.0)
                
                longDuration shouldBeGreaterThan shortDuration
            }
        }
        
        describe("deterministic behavior with seeded random") {
            it("should produce same path with same seed") {
                val generator1 = HumanizedSwipeGenerator(Random(42))
                val generator2 = HumanizedSwipeGenerator(Random(42))
                
                val path1 = generator1.generatePath(100, 500, 100, 200, screenWidth, screenHeight)
                val path2 = generator2.generatePath(100, 500, 100, 200, screenWidth, screenHeight)
                
                path1.points shouldBe path2.points
                path1.durationMs shouldBe path2.durationMs
            }
        }
        
        describe("edge cases") {
            it("should handle same start and end point") {
                val generator = HumanizedSwipeGenerator()
                val path = generator.generatePath(500, 500, 500, 500, screenWidth, screenHeight)
                
                path.points.first() shouldBe path.points.last()
            }
            
            it("should handle points at screen edges") {
                val generator = HumanizedSwipeGenerator()
                
                // Top-left to bottom-right
                val path = generator.generatePath(0, 0, screenWidth - 1, screenHeight - 1, screenWidth, screenHeight)
                
                path.points.first().x shouldBe 0
                path.points.first().y shouldBe 0
                path.points.last().x shouldBe screenWidth - 1
                path.points.last().y shouldBe screenHeight - 1
            }
        }
    }
})
