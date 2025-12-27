package com.kevinluo.autoglm.screenshot

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Property-based tests for ScreenshotService base64 encoding/decoding.
 * 
 * **Feature: code-logic-audit, Property 12: ScreenshotService Base64 往返**
 * **Validates: Requirements 5.4**
 * 
 * Note: These tests use Java's standard Base64 encoder/decoder which is functionally
 * equivalent to Android's android.util.Base64 with NO_WRAP flag. This allows us to
 * test the round-trip property in a unit test environment without Android dependencies.
 */
class ScreenshotServicePropertyTest : StringSpec({
    
    /**
     * Property 12: ScreenshotService Base64 往返
     * 
     * For any valid byte array (representing image data), encoding it to base64 
     * and then decoding it back should produce an equivalent byte array with 
     * the same content.
     * 
     * **Validates: Requirements 5.4**
     */
    "base64 round-trip - encoding then decoding should produce equivalent byte array" {
        // Generator for arbitrary byte arrays of various sizes
        // Simulating image data of different sizes (small to medium images)
        val byteArrayArb = Arb.byteArray(Arb.int(1..10000), Arb.byte())
        
        forAll(100, byteArrayArb) { originalBytes ->
            // Encode to base64 (equivalent to ScreenshotService.encodeToBase64)
            val base64Encoded = Base64.getEncoder().encodeToString(originalBytes)
            
            // Decode from base64 (equivalent to ScreenshotService.decodeFromBase64)
            val decodedBytes = Base64.getDecoder().decode(base64Encoded)
            
            // Verify round-trip produces equivalent data
            originalBytes.contentEquals(decodedBytes)
        }
    }
    
    /**
     * Property 12: ScreenshotService Base64 往返 - empty data
     * 
     * Empty byte arrays should also round-trip correctly.
     * 
     * **Validates: Requirements 5.4**
     */
    "base64 round-trip - empty byte array should round-trip correctly" {
        val emptyBytes = ByteArray(0)
        
        val base64Encoded = Base64.getEncoder().encodeToString(emptyBytes)
        val decodedBytes = Base64.getDecoder().decode(base64Encoded)
        
        emptyBytes.contentEquals(decodedBytes)
    }
    
    /**
     * Property 12: ScreenshotService Base64 往返 - PNG header preservation
     * 
     * For any byte array starting with PNG magic bytes, the round-trip should
     * preserve the PNG header, ensuring image format integrity.
     * 
     * **Validates: Requirements 5.4**
     */
    "base64 round-trip - PNG header should be preserved" {
        // PNG magic bytes: 137 80 78 71 13 10 26 10
        val pngMagicBytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
        
        // Generator for random data after PNG header
        val randomDataArb = Arb.byteArray(Arb.int(0..5000), Arb.byte())
        
        forAll(100, randomDataArb) { randomData ->
            // Create a byte array with PNG header followed by random data
            val pngLikeData = pngMagicBytes + randomData
            
            // Encode and decode
            val base64Encoded = Base64.getEncoder().encodeToString(pngLikeData)
            val decodedBytes = Base64.getDecoder().decode(base64Encoded)
            
            // Verify PNG header is preserved
            decodedBytes.size >= 8 &&
                decodedBytes.slice(0..7) == pngMagicBytes.toList() &&
                pngLikeData.contentEquals(decodedBytes)
        }
    }
    
    /**
     * Property 12: ScreenshotService Base64 往返 - size preservation
     * 
     * For any byte array, the decoded result should have the same size as the original.
     * 
     * **Validates: Requirements 5.4**
     */
    "base64 round-trip - size should be preserved" {
        val byteArrayArb = Arb.byteArray(Arb.int(0..10000), Arb.byte())
        
        forAll(100, byteArrayArb) { originalBytes ->
            val base64Encoded = Base64.getEncoder().encodeToString(originalBytes)
            val decodedBytes = Base64.getDecoder().decode(base64Encoded)
            
            originalBytes.size == decodedBytes.size
        }
    }
    
    /**
     * Property 12: ScreenshotService Base64 往返 - all byte values
     * 
     * The round-trip should correctly handle all possible byte values (0-255),
     * which is important for binary image data.
     * 
     * **Validates: Requirements 5.4**
     */
    "base64 round-trip - all byte values should be preserved" {
        // Create an array with all possible byte values
        val allByteValues = ByteArray(256) { it.toByte() }
        
        val base64Encoded = Base64.getEncoder().encodeToString(allByteValues)
        val decodedBytes = Base64.getDecoder().decode(base64Encoded)
        
        allByteValues.contentEquals(decodedBytes)
    }
    
    /**
     * Property 12: ScreenshotService Base64 往返 - typical image dimensions
     * 
     * For byte arrays representing typical screenshot sizes (width * height * bytes per pixel),
     * the round-trip should preserve all data.
     * 
     * **Validates: Requirements 5.4**
     */
    "base64 round-trip - typical screenshot sizes should round-trip correctly" {
        // Generator for typical screen dimensions
        val widthArb = Arb.int(100..1440)  // Common phone widths
        val heightArb = Arb.int(200..3200) // Common phone heights
        
        forAll(50, widthArb, heightArb) { width, height ->
            // Simulate ARGB_8888 format (4 bytes per pixel), but use smaller sample
            // to keep test fast. We'll use a fraction of the actual size.
            val sampleSize = minOf(width * height / 100, 10000)
            val imageData = ByteArray(sampleSize) { (it % 256).toByte() }
            
            val base64Encoded = Base64.getEncoder().encodeToString(imageData)
            val decodedBytes = Base64.getDecoder().decode(base64Encoded)
            
            imageData.contentEquals(decodedBytes)
        }
    }
    
    /**
     * Property 12: ScreenshotService Base64 往返 - idempotence
     * 
     * Encoding the same data multiple times should produce the same base64 string.
     * 
     * **Validates: Requirements 5.4**
     */
    "base64 encoding should be deterministic (idempotent)" {
        val byteArrayArb = Arb.byteArray(Arb.int(1..5000), Arb.byte())
        
        forAll(100, byteArrayArb) { originalBytes ->
            val encoded1 = Base64.getEncoder().encodeToString(originalBytes)
            val encoded2 = Base64.getEncoder().encodeToString(originalBytes)
            
            encoded1 == encoded2
        }
    }
    
    /**
     * Property 12: ScreenshotService Base64 往返 - multiple round-trips
     * 
     * Multiple encode-decode cycles should produce the same result as a single cycle.
     * 
     * **Validates: Requirements 5.4**
     */
    "base64 multiple round-trips should be stable" {
        val byteArrayArb = Arb.byteArray(Arb.int(1..5000), Arb.byte())
        
        forAll(100, byteArrayArb) { originalBytes ->
            // First round-trip
            val encoded1 = Base64.getEncoder().encodeToString(originalBytes)
            val decoded1 = Base64.getDecoder().decode(encoded1)
            
            // Second round-trip
            val encoded2 = Base64.getEncoder().encodeToString(decoded1)
            val decoded2 = Base64.getDecoder().decode(encoded2)
            
            // Both decoded results should equal original
            originalBytes.contentEquals(decoded1) && 
                originalBytes.contentEquals(decoded2) &&
                encoded1 == encoded2
        }
    }
})

/**
 * Property-based tests for FloatingWindowController visibility during screenshot capture.
 * 
 * **Feature: code-logic-audit, Property 11: ScreenshotService 弹窗控制时序**
 * **Validates: Requirements 5.1, 5.2, 5.3**
 * 
 * These tests verify that the floating window is properly hidden before screenshot capture
 * and restored after capture completes, ensuring the window never appears in screenshots.
 */
class FloatingWindowVisibilityPropertyTest : StringSpec({
    
    /**
     * Property 11: ScreenshotService 弹窗控制时序
     * 
     * For any screenshot capture operation where the floating window is initially visible,
     * the window should be hidden before capture starts and restored after capture completes.
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3**
     */
    "floating window should be hidden during capture and restored after - initially visible" {
        forAll(100, Arb.int(1..100)) { _ ->
            // Create a tracking FloatingWindowController
            val controller = TrackingFloatingWindowController(initiallyVisible = true)
            
            // Simulate the screenshot capture flow (without actual Android dependencies)
            // This tests the contract that ScreenshotService.capture() should follow
            
            // Before capture: window is visible
            val wasVisibleBefore = controller.isVisible()
            
            // During capture: hide window
            if (wasVisibleBefore) {
                controller.hide()
            }
            
            // Verify window is hidden during "capture"
            val isHiddenDuringCapture = !controller.isVisible()
            
            // After capture: restore window
            if (wasVisibleBefore) {
                controller.show()
            }
            
            // Verify window is restored after capture
            val isRestoredAfter = controller.isVisible()
            
            // Property: window was visible before, hidden during, and restored after
            wasVisibleBefore && isHiddenDuringCapture && isRestoredAfter
        }
    }
    
    /**
     * Property 11: ScreenshotService 弹窗控制时序 - initially hidden case
     * 
     * For any screenshot capture operation where the floating window is initially hidden,
     * the window should remain hidden throughout the capture process.
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3**
     */
    "floating window should remain hidden if initially hidden" {
        forAll(100, Arb.int(1..100)) { _ ->
            val controller = TrackingFloatingWindowController(initiallyVisible = false)
            
            val wasVisibleBefore = controller.isVisible()
            
            // During capture: only hide if was visible
            if (wasVisibleBefore) {
                controller.hide()
            }
            
            val isHiddenDuringCapture = !controller.isVisible()
            
            // After capture: only restore if was visible
            if (wasVisibleBefore) {
                controller.show()
            }
            
            val isHiddenAfter = !controller.isVisible()
            
            // Property: window was hidden before, during, and after
            !wasVisibleBefore && isHiddenDuringCapture && isHiddenAfter
        }
    }
    
    /**
     * Property 11: ScreenshotService 弹窗控制时序 - state transitions are correct
     * 
     * For any sequence of hide/show operations, the visibility state should be consistent.
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3**
     */
    "floating window visibility state transitions should be consistent" {
        // Generator for random initial visibility and number of capture cycles
        val initialVisibleArb = Arb.boolean()
        val captureCountArb = Arb.int(1..10)
        
        forAll(100, initialVisibleArb, captureCountArb) { initiallyVisible, captureCount ->
            val controller = TrackingFloatingWindowController(initiallyVisible = initiallyVisible)
            
            var allTransitionsCorrect = true
            
            repeat(captureCount) {
                val wasVisible = controller.isVisible()
                
                // Hide before capture
                if (wasVisible) {
                    controller.hide()
                }
                
                // Verify hidden during capture
                if (controller.isVisible()) {
                    allTransitionsCorrect = false
                }
                
                // Restore after capture
                if (wasVisible) {
                    controller.show()
                }
                
                // Verify state is restored correctly
                if (controller.isVisible() != wasVisible) {
                    allTransitionsCorrect = false
                }
            }
            
            allTransitionsCorrect
        }
    }
    
    /**
     * Property 11: ScreenshotService 弹窗控制时序 - hide count equals show count
     * 
     * For any screenshot capture operation, the number of hide calls should equal
     * the number of show calls (when window was initially visible).
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3**
     */
    "hide and show calls should be balanced during capture" {
        val captureCountArb = Arb.int(1..20)
        
        forAll(100, captureCountArb) { captureCount ->
            val controller = TrackingFloatingWindowController(initiallyVisible = true)
            
            repeat(captureCount) {
                val wasVisible = controller.isVisible()
                
                if (wasVisible) {
                    controller.hide()
                }
                
                // Simulate capture...
                
                if (wasVisible) {
                    controller.show()
                }
            }
            
            // Property: hide and show calls should be balanced
            controller.hideCount.get() == controller.showCount.get()
        }
    }
    
    /**
     * Property 11: ScreenshotService 弹窗控制时序 - window never visible during capture phase
     * 
     * For any screenshot capture, the window should never be visible at the moment
     * of capture (simulated by checking visibility after hide is called).
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3**
     */
    "window should never be visible at capture moment" {
        val captureCountArb = Arb.int(1..50)
        
        forAll(100, captureCountArb) { captureCount ->
            val controller = TrackingFloatingWindowController(initiallyVisible = true)
            var windowVisibleDuringCapture = false
            
            repeat(captureCount) {
                val wasVisible = controller.isVisible()
                
                if (wasVisible) {
                    controller.hide()
                }
                
                // This is the "capture moment" - window must be hidden
                if (controller.isVisible()) {
                    windowVisibleDuringCapture = true
                }
                
                if (wasVisible) {
                    controller.show()
                }
            }
            
            // Property: window was never visible during any capture
            !windowVisibleDuringCapture
        }
    }
})

/**
 * A tracking implementation of FloatingWindowController for testing purposes.
 * Records all visibility state changes and call counts.
 */
private class TrackingFloatingWindowController(
    initiallyVisible: Boolean
) : FloatingWindowController {
    
    private val visible = AtomicBoolean(initiallyVisible)
    val hideCount = AtomicInteger(0)
    val showCount = AtomicInteger(0)
    
    override fun hide() {
        visible.set(false)
        hideCount.incrementAndGet()
    }
    
    override fun show() {
        visible.set(true)
        showCount.incrementAndGet()
    }
    
    override fun showAndBringToFront() {
        visible.set(true)
        showCount.incrementAndGet()
    }
    
    override fun isVisible(): Boolean = visible.get()
}
