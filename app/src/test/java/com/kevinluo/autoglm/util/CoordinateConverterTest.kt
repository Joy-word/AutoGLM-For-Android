package com.kevinluo.autoglm.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

/**
 * Unit tests for CoordinateConverter.
 */
class CoordinateConverterTest : DescribeSpec({
    
    describe("CoordinateConverter") {
        
        describe("toAbsoluteX") {
            it("should convert 0 to 0") {
                CoordinateConverter.toAbsoluteX(0, 1080) shouldBe 0
            }
            
            it("should convert 500 to half of screen width") {
                CoordinateConverter.toAbsoluteX(500, 1080) shouldBe 540
            }
            
            it("should convert 999 to near screen width") {
                // 999 * 1080 / 1000 = 1078.92 -> 1078 (integer division)
                CoordinateConverter.toAbsoluteX(999, 1080) shouldBe 1078
            }
            
            it("should handle different screen widths") {
                CoordinateConverter.toAbsoluteX(500, 720) shouldBe 360
                CoordinateConverter.toAbsoluteX(500, 1440) shouldBe 720
            }
        }
        
        describe("toAbsoluteY") {
            it("should convert 0 to 0") {
                CoordinateConverter.toAbsoluteY(0, 1920) shouldBe 0
            }
            
            it("should convert 500 to half of screen height") {
                CoordinateConverter.toAbsoluteY(500, 1920) shouldBe 960
            }
            
            it("should convert 999 to near screen height") {
                CoordinateConverter.toAbsoluteY(999, 1920) shouldBe 1918
            }
        }
        
        describe("toAbsolute") {
            it("should convert both coordinates") {
                val (x, y) = CoordinateConverter.toAbsolute(500, 500, 1080, 1920)
                x shouldBe 540
                y shouldBe 960
            }
            
            it("should handle corner cases") {
                val (x1, y1) = CoordinateConverter.toAbsolute(0, 0, 1080, 1920)
                x1 shouldBe 0
                y1 shouldBe 0
                
                // 999 * 1080 / 1000 = 1078, 999 * 1920 / 1000 = 1918
                val (x2, y2) = CoordinateConverter.toAbsolute(999, 999, 1080, 1920)
                x2 shouldBe 1078
                y2 shouldBe 1918
            }
        }
        
        describe("toRelativeX") {
            it("should convert 0 to 0") {
                CoordinateConverter.toRelativeX(0, 1080) shouldBe 0
            }
            
            it("should convert half screen width to 500") {
                CoordinateConverter.toRelativeX(540, 1080) shouldBe 500
            }
        }
        
        describe("toRelativeY") {
            it("should convert 0 to 0") {
                CoordinateConverter.toRelativeY(0, 1920) shouldBe 0
            }
            
            it("should convert half screen height to 500") {
                CoordinateConverter.toRelativeY(960, 1920) shouldBe 500
            }
        }
        
        describe("round-trip conversion") {
            it("should approximately preserve values after round-trip") {
                // Note: Due to integer division, exact round-trip is not guaranteed
                // but should be close (within 1)
                val screenWidth = 1080
                val screenHeight = 1920
                
                for (relX in listOf(0, 100, 250, 500, 750, 999)) {
                    val absX = CoordinateConverter.toAbsoluteX(relX, screenWidth)
                    val backToRelX = CoordinateConverter.toRelativeX(absX, screenWidth)
                    // Allow small difference due to integer division
                    (kotlin.math.abs(backToRelX - relX) <= 1) shouldBe true
                }
            }
        }
        
        describe("property-based tests") {
            it("absolute coordinates should be within screen bounds") {
                checkAll(Arb.int(0, 999), Arb.int(0, 999)) { relX, relY ->
                    val screenWidth = 1080
                    val screenHeight = 1920
                    
                    val absX = CoordinateConverter.toAbsoluteX(relX, screenWidth)
                    val absY = CoordinateConverter.toAbsoluteY(relY, screenHeight)
                    
                    absX shouldBe (absX.coerceIn(0, screenWidth - 1))
                    absY shouldBe (absY.coerceIn(0, screenHeight - 1))
                }
            }
            
            it("conversion should be monotonic") {
                checkAll(Arb.int(0, 998)) { relX ->
                    val screenWidth = 1080
                    val absX1 = CoordinateConverter.toAbsoluteX(relX, screenWidth)
                    val absX2 = CoordinateConverter.toAbsoluteX(relX + 1, screenWidth)
                    
                    (absX2 >= absX1) shouldBe true
                }
            }
        }
    }
})
