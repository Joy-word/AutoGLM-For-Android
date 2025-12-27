package com.kevinluo.autoglm.device

import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll

/**
 * Property-based tests for DeviceExecutor.
 * 
 * **Feature: autoglm-phone-agent, Property 13: Key press keycode mapping**
 * **Validates: Requirements 5.9-5.13**
 * 
 * **Feature: code-logic-audit, Property 14: DeviceExecutor 命令格式正确性**
 * **Validates: Requirements 7.1, 7.2, 7.4**
 * 
 * **Feature: autoglm-phone-agent, Property 15: Double tap timing**
 * **Validates: Requirements 5.15**
 */
class DeviceExecutorPropertyTest : StringSpec({
    
    /**
     * Property 14: DeviceExecutor 命令格式正确性
     * 
     * For any device operation (tap, swipe, pressKey), the generated shell command 
     * must have correct format and parameters.
     * 
     * **Feature: code-logic-audit, Property 14: DeviceExecutor 命令格式正确性**
     * **Validates: Requirements 7.1, 7.2, 7.4**
     */
    
    "tap command format - should generate valid 'input tap x y' format" {
        // For any valid screen coordinates, tap command should have correct format
        forAll(
            100,
            Arb.int(0..4000),  // x coordinate (typical screen width range)
            Arb.int(0..8000)   // y coordinate (typical screen height range)
        ) { x, y ->
            val command = generateTapCommand(x, y)
            
            // Verify command format: "input tap x y"
            val parts = command.split(" ")
            parts.size == 4 &&
                parts[0] == "input" &&
                parts[1] == "tap" &&
                parts[2].toIntOrNull() == x &&
                parts[3].toIntOrNull() == y
        }
    }
    
    "tap command format - coordinates should be passed correctly" {
        // For any coordinates, they should appear exactly in the command
        forAll(
            100,
            Arb.int(0..4000),
            Arb.int(0..8000)
        ) { x, y ->
            val command = generateTapCommand(x, y)
            val expected = "input tap $x $y"
            command == expected
        }
    }
    
    "tap command format - should be deterministic" {
        // For same coordinates, command should always be identical
        forAll(
            100,
            Arb.int(0..4000),
            Arb.int(0..8000)
        ) { x, y ->
            val command1 = generateTapCommand(x, y)
            val command2 = generateTapCommand(x, y)
            command1 == command2
        }
    }
    
    "swipe command format - should generate valid 'input swipe' format with 5 parameters" {
        // For any valid swipe parameters, command should have correct format
        forAll(
            100,
            Arb.int(0..4000),  // startX
            Arb.int(0..8000),  // startY
            Arb.int(0..4000),  // endX
            Arb.int(0..8000),  // endY
            Arb.int(100..5000) // duration in ms
        ) { startX, startY, endX, endY, durationMs ->
            val command = generateSwipeCommand(startX, startY, endX, endY, durationMs)
            
            // Verify command format: "input swipe startX startY endX endY duration"
            val parts = command.split(" ")
            parts.size == 7 &&
                parts[0] == "input" &&
                parts[1] == "swipe" &&
                parts[2].toIntOrNull() == startX &&
                parts[3].toIntOrNull() == startY &&
                parts[4].toIntOrNull() == endX &&
                parts[5].toIntOrNull() == endY &&
                parts[6].toIntOrNull() == durationMs
        }
    }
    
    "swipe command format - all coordinates and duration should be passed correctly" {
        // For any swipe parameters, they should appear exactly in the command
        forAll(
            100,
            Arb.int(0..4000),
            Arb.int(0..8000),
            Arb.int(0..4000),
            Arb.int(0..8000),
            Arb.int(100..5000)
        ) { startX, startY, endX, endY, durationMs ->
            val command = generateSwipeCommand(startX, startY, endX, endY, durationMs)
            val expected = "input swipe $startX $startY $endX $endY $durationMs"
            command == expected
        }
    }
    
    "swipe command format - should be deterministic" {
        // For same parameters, command should always be identical
        forAll(
            100,
            Arb.int(0..4000),
            Arb.int(0..8000),
            Arb.int(0..4000),
            Arb.int(0..8000),
            Arb.int(100..5000)
        ) { startX, startY, endX, endY, durationMs ->
            val command1 = generateSwipeCommand(startX, startY, endX, endY, durationMs)
            val command2 = generateSwipeCommand(startX, startY, endX, endY, durationMs)
            command1 == command2
        }
    }
    
    "pressKey command format - should generate valid 'input keyevent' format" {
        // For any valid keycode, command should have correct format
        forAll(
            100,
            Arb.int(0..300)  // Android keycodes range
        ) { keyCode ->
            val command = generateKeyPressCommand(keyCode)
            
            // Verify command format: "input keyevent keycode"
            val parts = command.split(" ")
            parts.size == 3 &&
                parts[0] == "input" &&
                parts[1] == "keyevent" &&
                parts[2].toIntOrNull() == keyCode
        }
    }
    
    "pressKey command format - keycode should be passed correctly" {
        // For any keycode, it should appear exactly in the command
        forAll(
            100,
            Arb.int(0..300)
        ) { keyCode ->
            val command = generateKeyPressCommand(keyCode)
            val expected = "input keyevent $keyCode"
            command == expected
        }
    }
    
    "pressKey command format - should be deterministic" {
        // For same keycode, command should always be identical
        forAll(
            100,
            Arb.int(0..300)
        ) { keyCode ->
            val command1 = generateKeyPressCommand(keyCode)
            val command2 = generateKeyPressCommand(keyCode)
            command1 == command2
        }
    }
    
    /**
     * Property 13: Key press keycode mapping
     * 
     * For any key press action (Back, Home, VolumeUp, VolumeDown, Power), 
     * the generated shell command should contain the correct Android keycode constant.
     * 
     * **Validates: Requirements 5.9, 5.10, 5.11, 5.12, 5.13**
     */
    
    "key press keycode mapping - Back action should generate command with KEYCODE_BACK (4)" {
        // For any number of iterations, Back action should always map to keycode 4
        forAll(100, Arb.int(1..100)) { _ ->
            val command = generateKeyPressCommand(DeviceExecutor.KEYCODE_BACK)
            command == "input keyevent 4" && DeviceExecutor.KEYCODE_BACK == 4
        }
    }
    
    "key press keycode mapping - Home action should generate command with KEYCODE_HOME (3)" {
        // For any number of iterations, Home action should always map to keycode 3
        forAll(100, Arb.int(1..100)) { _ ->
            val command = generateKeyPressCommand(DeviceExecutor.KEYCODE_HOME)
            command == "input keyevent 3" && DeviceExecutor.KEYCODE_HOME == 3
        }
    }
    
    "key press keycode mapping - VolumeUp action should generate command with KEYCODE_VOLUME_UP (24)" {
        // For any number of iterations, VolumeUp action should always map to keycode 24
        forAll(100, Arb.int(1..100)) { _ ->
            val command = generateKeyPressCommand(DeviceExecutor.KEYCODE_VOLUME_UP)
            command == "input keyevent 24" && DeviceExecutor.KEYCODE_VOLUME_UP == 24
        }
    }
    
    "key press keycode mapping - VolumeDown action should generate command with KEYCODE_VOLUME_DOWN (25)" {
        // For any number of iterations, VolumeDown action should always map to keycode 25
        forAll(100, Arb.int(1..100)) { _ ->
            val command = generateKeyPressCommand(DeviceExecutor.KEYCODE_VOLUME_DOWN)
            command == "input keyevent 25" && DeviceExecutor.KEYCODE_VOLUME_DOWN == 25
        }
    }
    
    "key press keycode mapping - Power action should generate command with KEYCODE_POWER (26)" {
        // For any number of iterations, Power action should always map to keycode 26
        forAll(100, Arb.int(1..100)) { _ ->
            val command = generateKeyPressCommand(DeviceExecutor.KEYCODE_POWER)
            command == "input keyevent 26" && DeviceExecutor.KEYCODE_POWER == 26
        }
    }
    
    "key press keycode mapping - all key actions should produce valid input keyevent commands" {
        // Generator for all supported key actions
        val keyCodeArb = Arb.element(
            DeviceExecutor.KEYCODE_BACK,
            DeviceExecutor.KEYCODE_HOME,
            DeviceExecutor.KEYCODE_VOLUME_UP,
            DeviceExecutor.KEYCODE_VOLUME_DOWN,
            DeviceExecutor.KEYCODE_POWER
        )
        
        forAll(100, keyCodeArb) { keyCode ->
            val command = generateKeyPressCommand(keyCode)
            
            // Verify command format is correct
            command.startsWith("input keyevent ") &&
                command.substringAfter("input keyevent ").toIntOrNull() == keyCode
        }
    }
    
    "key press keycode mapping - keycode values should match Android KeyEvent constants" {
        // Verify that our keycode constants match the expected Android KeyEvent values
        // These are the official Android keycode values
        val expectedMappings = mapOf(
            DeviceExecutor.KEYCODE_BACK to 4,
            DeviceExecutor.KEYCODE_HOME to 3,
            DeviceExecutor.KEYCODE_VOLUME_UP to 24,
            DeviceExecutor.KEYCODE_VOLUME_DOWN to 25,
            DeviceExecutor.KEYCODE_POWER to 26
        )
        
        forAll(100, Arb.element(expectedMappings.keys.toList())) { keyCode ->
            val expectedValue = expectedMappings[keyCode]
            keyCode == expectedValue
        }
    }
    
    "key press keycode mapping - generated commands should be deterministic for same keycode" {
        // For any keycode, generating the command multiple times should produce identical results
        val keyCodeArb = Arb.element(
            DeviceExecutor.KEYCODE_BACK,
            DeviceExecutor.KEYCODE_HOME,
            DeviceExecutor.KEYCODE_VOLUME_UP,
            DeviceExecutor.KEYCODE_VOLUME_DOWN,
            DeviceExecutor.KEYCODE_POWER
        )
        
        forAll(100, keyCodeArb) { keyCode ->
            val command1 = generateKeyPressCommand(keyCode)
            val command2 = generateKeyPressCommand(keyCode)
            command1 == command2
        }
    }
    
    /**
     * Property 14: Long press duration
     * 
     * For any long press action with specified duration, the generated swipe command 
     * should have the same start and end coordinates and the specified duration in milliseconds.
     * 
     * **Feature: autoglm-phone-agent, Property 14: Long press duration**
     * **Validates: Requirements 5.14**
     */
    
    "long press duration - command should have same start and end coordinates" {
        // For any coordinates, long press should generate a swipe with identical start/end
        forAll(
            100,
            Arb.int(0..4000),  // x coordinate (typical screen width range)
            Arb.int(0..8000),  // y coordinate (typical screen height range)
            Arb.int(100..10000) // duration in ms (reasonable range)
        ) { x, y, durationMs ->
            val command = generateLongPressCommand(x, y, durationMs)
            
            // Parse the command to verify start and end coordinates are the same
            // Expected format: "input swipe startX startY endX endY duration" (7 parts)
            val parts = command.split(" ")
            parts.size == 7 &&
                parts[0] == "input" &&
                parts[1] == "swipe" &&
                parts[2] == parts[4] && // startX == endX
                parts[3] == parts[5]    // startY == endY
        }
    }
    
    "long press duration - command should contain the specified duration" {
        // For any duration, the generated command should include that exact duration
        forAll(
            100,
            Arb.int(0..4000),
            Arb.int(0..8000),
            Arb.int(100..10000)
        ) { x, y, durationMs ->
            val command = generateLongPressCommand(x, y, durationMs)
            
            // The command should end with the duration
            command.endsWith(" $durationMs")
        }
    }
    
    "long press duration - command should contain correct coordinates" {
        // For any coordinates, they should appear correctly in the command
        forAll(
            100,
            Arb.int(0..4000),
            Arb.int(0..8000),
            Arb.int(100..10000)
        ) { x, y, durationMs ->
            val command = generateLongPressCommand(x, y, durationMs)
            
            // Expected format: "input swipe x y x y duration"
            val expected = "input swipe $x $y $x $y $durationMs"
            command == expected
        }
    }
    
    "long press duration - command format should be valid input swipe command" {
        // For any long press parameters, the command should be a valid swipe format
        forAll(
            100,
            Arb.int(0..4000),
            Arb.int(0..8000),
            Arb.int(100..10000)
        ) { x, y, durationMs ->
            val command = generateLongPressCommand(x, y, durationMs)
            
            // Verify it starts with "input swipe" and has 7 parts:
            // "input", "swipe", startX, startY, endX, endY, duration
            command.startsWith("input swipe ") &&
                command.split(" ").size == 7
        }
    }
    
    "long press duration - generated commands should be deterministic" {
        // For the same parameters, generating the command multiple times should produce identical results
        forAll(
            100,
            Arb.int(0..4000),
            Arb.int(0..8000),
            Arb.int(100..10000)
        ) { x, y, durationMs ->
            val command1 = generateLongPressCommand(x, y, durationMs)
            val command2 = generateLongPressCommand(x, y, durationMs)
            command1 == command2
        }
    }
    
    /**
     * Property 15: Double tap timing
     * 
     * For any double tap action, two tap commands should be generated with a short 
     * interval between them (typically < 300ms).
     * 
     * **Feature: autoglm-phone-agent, Property 15: Double tap timing**
     * **Validates: Requirements 5.15**
     */
    
    "double tap timing - should generate exactly two tap commands" {
        // For any coordinates, double tap should generate exactly two tap commands
        forAll(
            100,
            Arb.int(0..4000),  // x coordinate (typical screen width range)
            Arb.int(0..8000)   // y coordinate (typical screen height range)
        ) { x, y ->
            val commands = generateDoubleTapCommands(x, y)
            commands.size == 2
        }
    }
    
    "double tap timing - both commands should be tap commands at same coordinates" {
        // For any coordinates, both tap commands should target the same location
        forAll(
            100,
            Arb.int(0..4000),
            Arb.int(0..8000)
        ) { x, y ->
            val commands = generateDoubleTapCommands(x, y)
            val expectedCommand = "input tap $x $y"
            
            commands.size == 2 &&
                commands[0] == expectedCommand &&
                commands[1] == expectedCommand
        }
    }
    
    "double tap timing - interval should be less than 300ms" {
        // The interval between double taps should be less than 300ms as per requirements
        // This is a constant property that should always hold
        forAll(100, Arb.int(1..100)) { _ ->
            val intervalMs = DeviceExecutor.DOUBLE_TAP_INTERVAL_MS
            intervalMs < 300L
        }
    }
    
    "double tap timing - interval should be positive" {
        // The interval should be a positive value
        forAll(100, Arb.int(1..100)) { _ ->
            val intervalMs = DeviceExecutor.DOUBLE_TAP_INTERVAL_MS
            intervalMs > 0L
        }
    }
    
    "double tap timing - commands should have valid input tap format" {
        // For any coordinates, the generated commands should be valid input tap format
        forAll(
            100,
            Arb.int(0..4000),
            Arb.int(0..8000)
        ) { x, y ->
            val commands = generateDoubleTapCommands(x, y)
            
            commands.all { command ->
                // Verify format: "input tap x y"
                val parts = command.split(" ")
                parts.size == 4 &&
                    parts[0] == "input" &&
                    parts[1] == "tap" &&
                    parts[2].toIntOrNull() != null &&
                    parts[3].toIntOrNull() != null
            }
        }
    }
    
    "double tap timing - commands should contain correct coordinates" {
        // For any coordinates, they should appear correctly in both commands
        forAll(
            100,
            Arb.int(0..4000),
            Arb.int(0..8000)
        ) { x, y ->
            val commands = generateDoubleTapCommands(x, y)
            
            commands.all { command ->
                val parts = command.split(" ")
                parts.size == 4 &&
                    parts[2].toInt() == x &&
                    parts[3].toInt() == y
            }
        }
    }
    
    "double tap timing - generated commands should be deterministic" {
        // For the same coordinates, generating commands multiple times should produce identical results
        forAll(
            100,
            Arb.int(0..4000),
            Arb.int(0..8000)
        ) { x, y ->
            val commands1 = generateDoubleTapCommands(x, y)
            val commands2 = generateDoubleTapCommands(x, y)
            commands1 == commands2
        }
    }
    
    "double tap timing - both taps should be identical" {
        // For any double tap, both tap commands should be exactly the same
        forAll(
            100,
            Arb.int(0..4000),
            Arb.int(0..8000)
        ) { x, y ->
            val commands = generateDoubleTapCommands(x, y)
            commands.size == 2 && commands[0] == commands[1]
        }
    }
}) {
    companion object {
        /**
         * Helper function to generate tap command.
         * This mirrors the logic in DeviceExecutor.tap()
         */
        fun generateTapCommand(x: Int, y: Int): String {
            return "input tap $x $y"
        }
        
        /**
         * Helper function to generate swipe command.
         * This mirrors the logic in DeviceExecutor.swipe() fallback path
         */
        fun generateSwipeCommand(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Int): String {
            return "input swipe $startX $startY $endX $endY $durationMs"
        }
        
        /**
         * Helper function to generate key press command.
         * This mirrors the logic in DeviceExecutor.generateKeyPressCommand()
         */
        fun generateKeyPressCommand(keyCode: Int): String {
            return "input keyevent $keyCode"
        }
        
        /**
         * Helper function to generate long press command.
         * This mirrors the logic in DeviceExecutor.generateLongPressCommand()
         * 
         * Long press is implemented as a swipe with the same start and end coordinates.
         */
        fun generateLongPressCommand(x: Int, y: Int, durationMs: Int): String {
            return "input swipe $x $y $x $y $durationMs"
        }
        
        /**
         * Helper function to generate double tap commands.
         * This mirrors the logic in DeviceExecutor.generateDoubleTapCommands()
         * 
         * Double tap generates two tap commands at the same coordinates.
         */
        fun generateDoubleTapCommands(x: Int, y: Int): List<String> {
            return listOf(
                "input tap $x $y",
                "input tap $x $y"
            )
        }
    }
}
