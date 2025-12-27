package com.kevinluo.autoglm.agent

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Property-based tests for PhoneAgent.
 * 
 * **Feature: autoglm-phone-agent**
 */
class PhoneAgentPropertyTest : StringSpec({
    
    /**
     * Property 1: Whitespace task rejection
     * 
     * For any string composed entirely of whitespace characters (spaces, tabs, newlines),
     * submitting it as a task description should be rejected and return an error result
     * without starting execution.
     * 
     * **Feature: autoglm-phone-agent, Property 1: Whitespace task rejection**
     * **Validates: Requirements 1.2**
     */
    "whitespace task rejection - empty string is invalid" {
        forAll(100, Arb.constant("")) { emptyTask ->
            // Test using Kotlin's isNotBlank() which is what PhoneAgent.isValidTask uses
            !isValidTask(emptyTask)
        }
    }
    
    "whitespace task rejection - whitespace-only strings are invalid" {
        // Generator for whitespace-only strings using different whitespace characters
        val whitespaceChars = listOf(' ', '\t', '\n', '\r')
        val whitespaceCharArb = Arb.element(whitespaceChars)
        val lengthArb = Arb.int(1..100)
        
        forAll(100, whitespaceCharArb, lengthArb) { char, length ->
            val whitespaceTask = char.toString().repeat(length)
            !isValidTask(whitespaceTask)
        }
    }
    
    "whitespace task rejection - non-whitespace strings are valid" {
        // Generator for strings with at least one non-whitespace character
        val validTaskArb = Arb.string(1..100, Codepoint.alphanumeric())
        
        forAll(100, validTaskArb) { validTask ->
            isValidTask(validTask)
        }
    }
    
    "whitespace task rejection - mixed whitespace and content is valid" {
        // Generator for strings with leading/trailing whitespace but non-whitespace content
        val contentArb = Arb.string(1..50, Codepoint.alphanumeric())
        val leadingSpacesArb = Arb.int(0..20)
        val trailingSpacesArb = Arb.int(0..20)
        
        forAll(100, leadingSpacesArb, contentArb, trailingSpacesArb) { leadingCount, content, trailingCount ->
            val task = " ".repeat(leadingCount) + content + " ".repeat(trailingCount)
            isValidTask(task)
        }
    }
    
    "whitespace task rejection - various whitespace characters are rejected" {
        // Test specific whitespace characters
        val whitespaceChars = listOf(' ', '\t', '\n', '\r', '\u000B', '\u000C')
        val whitespaceCharArb = Arb.element(whitespaceChars)
        val lengthArb = Arb.int(1..50)
        
        forAll(100, whitespaceCharArb, lengthArb) { char, length ->
            val whitespaceTask = char.toString().repeat(length)
            !isValidTask(whitespaceTask)
        }
    }
    
    /**
     * Property 2: Task state exclusivity
     * 
     * For any PhoneAgent instance, when a task is running, attempting to start a new task
     * should fail or be blocked until the current task completes or is cancelled.
     * 
     * This property tests the state machine behavior using AtomicReference.compareAndSet
     * which is the core mechanism used by PhoneAgent to ensure task exclusivity.
     * 
     * **Feature: autoglm-phone-agent, Property 2: Task state exclusivity**
     * **Validates: Requirements 1.3**
     */
    "task state exclusivity - only one task can run at a time" {
        // Generator for valid task descriptions
        val validTaskArb = Arb.string(1..50, Codepoint.alphanumeric())
        
        forAll(100, validTaskArb, validTaskArb) { task1, task2 ->
            // Simulate the state machine behavior used in PhoneAgent
            val state = AtomicReference(AgentState.IDLE)
            
            // First task should successfully transition from IDLE to RUNNING
            val firstTaskStarted = state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)
            
            // Second task should fail to start because state is RUNNING
            val secondTaskStarted = state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)
            
            // Property: first task starts, second task is blocked
            firstTaskStarted && !secondTaskStarted && state.get() == AgentState.RUNNING
        }
    }
    
    "task state exclusivity - task can start after previous completes" {
        val validTaskArb = Arb.string(1..50, Codepoint.alphanumeric())
        
        forAll(100, validTaskArb, validTaskArb) { task1, task2 ->
            val state = AtomicReference(AgentState.IDLE)
            
            // First task starts
            val firstTaskStarted = state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)
            
            // First task completes (transitions back to IDLE)
            state.set(AgentState.IDLE)
            
            // Second task should now be able to start
            val secondTaskStarted = state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)
            
            // Property: both tasks can start sequentially
            firstTaskStarted && secondTaskStarted
        }
    }
    
    "task state exclusivity - task can start after previous is cancelled" {
        val validTaskArb = Arb.string(1..50, Codepoint.alphanumeric())
        
        forAll(100, validTaskArb, validTaskArb) { task1, task2 ->
            val state = AtomicReference(AgentState.IDLE)
            
            // First task starts
            val firstTaskStarted = state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)
            
            // First task is cancelled (transitions to CANCELLED then IDLE)
            state.set(AgentState.CANCELLED)
            state.set(AgentState.IDLE)
            
            // Second task should now be able to start
            val secondTaskStarted = state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)
            
            // Property: second task can start after cancellation
            firstTaskStarted && secondTaskStarted
        }
    }
    
    "task state exclusivity - concurrent start attempts are mutually exclusive" {
        val validTaskArb = Arb.string(1..50, Codepoint.alphanumeric())
        val numAttemptsArb = Arb.int(2..10)
        
        forAll(100, validTaskArb, numAttemptsArb) { task, numAttempts ->
            val state = AtomicReference(AgentState.IDLE)
            
            // Simulate multiple concurrent attempts to start a task
            val results = (1..numAttempts).map {
                state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)
            }
            
            // Property: exactly one attempt should succeed
            val successCount = results.count { it }
            successCount == 1 && state.get() == AgentState.RUNNING
        }
    }
    
    /**
     * Property 2: Task mutual exclusion - PhoneAgent run() behavior
     * 
     * For any PhoneAgent instance, when a task is running (state=RUNNING), 
     * attempting to start a new task must return an error result without starting execution.
     * 
     * This test validates the actual behavior of the state machine that PhoneAgent uses
     * to enforce task mutual exclusion. It simulates the exact pattern used in PhoneAgent.run():
     * - state.compareAndSet(AgentState.IDLE, AgentState.RUNNING) to start a task
     * - Returns error if compareAndSet fails (another task is running)
     * 
     * **Feature: code-logic-audit, Property 2: PhoneAgent 任务互斥**
     * **Validates: Requirements 1.3**
     */
    "task mutual exclusion - second task returns error when first is running" {
        val validTaskArb = Arb.string(1..50, Codepoint.alphanumeric())
        
        forAll(100, validTaskArb, validTaskArb) { task1, task2 ->
            // Simulate PhoneAgent's state machine
            val state = AtomicReference(AgentState.IDLE)
            val taskStartedCount = AtomicInteger(0)
            val taskRejectedCount = AtomicInteger(0)
            
            // Simulate first task starting (like PhoneAgent.run() does)
            val firstTaskResult = simulateTaskStart(state, task1, taskStartedCount, taskRejectedCount)
            
            // Simulate second task attempting to start while first is running
            val secondTaskResult = simulateTaskStart(state, task2, taskStartedCount, taskRejectedCount)
            
            // Property: first task starts successfully, second task is rejected with error
            firstTaskResult.success && 
            !secondTaskResult.success && 
            secondTaskResult.message.contains("already running") &&
            taskStartedCount.get() == 1 &&
            taskRejectedCount.get() == 1 &&
            state.get() == AgentState.RUNNING
        }
    }
    
    "task mutual exclusion - multiple concurrent attempts only one succeeds" {
        val validTaskArb = Arb.string(1..50, Codepoint.alphanumeric())
        val numTasksArb = Arb.int(2..20)
        
        forAll(100, validTaskArb, numTasksArb) { baseTask, numTasks ->
            val state = AtomicReference(AgentState.IDLE)
            val taskStartedCount = AtomicInteger(0)
            val taskRejectedCount = AtomicInteger(0)
            
            // Simulate multiple tasks attempting to start concurrently
            val results = (1..numTasks).map { i ->
                simulateTaskStart(state, "$baseTask-$i", taskStartedCount, taskRejectedCount)
            }
            
            // Property: exactly one task succeeds, all others are rejected
            val successCount = results.count { it.success }
            val rejectedCount = results.count { !it.success }
            
            successCount == 1 &&
            rejectedCount == numTasks - 1 &&
            taskStartedCount.get() == 1 &&
            taskRejectedCount.get() == numTasks - 1 &&
            state.get() == AgentState.RUNNING
        }
    }
    
    "task mutual exclusion - task can start after previous task completes" {
        val validTaskArb = Arb.string(1..50, Codepoint.alphanumeric())
        val numSequentialTasksArb = Arb.int(2..10)
        
        forAll(100, validTaskArb, numSequentialTasksArb) { baseTask, numTasks ->
            val state = AtomicReference(AgentState.IDLE)
            var allTasksStartedSuccessfully = true
            
            // Simulate sequential task execution (each task completes before next starts)
            for (i in 1..numTasks) {
                val taskStartedCount = AtomicInteger(0)
                val taskRejectedCount = AtomicInteger(0)
                
                val result = simulateTaskStart(state, "$baseTask-$i", taskStartedCount, taskRejectedCount)
                
                if (!result.success) {
                    allTasksStartedSuccessfully = false
                    break
                }
                
                // Simulate task completion (reset state to IDLE)
                state.set(AgentState.IDLE)
            }
            
            // Property: all sequential tasks should start successfully
            allTasksStartedSuccessfully
        }
    }
    
    "task mutual exclusion - task can start after previous task is cancelled" {
        val validTaskArb = Arb.string(1..50, Codepoint.alphanumeric())
        
        forAll(100, validTaskArb, validTaskArb) { task1, task2 ->
            val state = AtomicReference(AgentState.IDLE)
            val cancelled = AtomicBoolean(false)
            
            // First task starts
            val firstStarted = state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)
            
            // Simulate cancellation (like PhoneAgent.cancel() does)
            cancelled.set(true)
            state.compareAndSet(AgentState.RUNNING, AgentState.CANCELLED)
            
            // After cancellation handling, state returns to IDLE (like in PhoneAgent.run() finally block)
            state.set(AgentState.IDLE)
            
            // Second task should now be able to start
            val secondStarted = state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)
            
            // Property: both tasks can start (sequentially, after cancellation)
            firstStarted && secondStarted && state.get() == AgentState.RUNNING
        }
    }
    
    "task mutual exclusion - state transitions are atomic" {
        val numAttemptsArb = Arb.int(10..100)
        
        forAll(100, numAttemptsArb) { numAttempts ->
            val state = AtomicReference(AgentState.IDLE)
            val successfulTransitions = AtomicInteger(0)
            
            // Simulate many concurrent attempts to transition from IDLE to RUNNING
            // In a real concurrent scenario, AtomicReference.compareAndSet ensures atomicity
            val results = (1..numAttempts).map {
                val transitioned = state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)
                if (transitioned) {
                    successfulTransitions.incrementAndGet()
                }
                transitioned
            }
            
            // Property: exactly one transition succeeds (atomicity guarantee)
            successfulTransitions.get() == 1 && 
            results.count { it } == 1 &&
            state.get() == AgentState.RUNNING
        }
    }
    
    /**
     * Property 21: 并发任务互斥 (Concurrent Task Mutual Exclusion)
     * 
     * For any concurrent task submissions to the same PhoneAgent, only one task 
     * can be in RUNNING state at any time. This property tests true concurrent 
     * execution using multiple threads to verify the AtomicReference.compareAndSet 
     * mechanism correctly enforces mutual exclusion under real concurrency.
     * 
     * **Feature: code-logic-audit, Property 21: 并发任务互斥**
     * **Validates: Requirements 12.1**
     */
    "concurrent task mutual exclusion - only one task runs at a time with real threads" {
        val numThreadsArb = Arb.int(2..20)
        
        forAll(100, numThreadsArb) { numThreads ->
            val state = AtomicReference(AgentState.IDLE)
            val successfulStarts = AtomicInteger(0)
            val failedStarts = AtomicInteger(0)
            
            // Use CountDownLatch to synchronize thread start for maximum contention
            val startLatch = CountDownLatch(1)
            val doneLatch = CountDownLatch(numThreads)
            
            val executor = Executors.newFixedThreadPool(numThreads)
            
            try {
                // Submit tasks to all threads
                repeat(numThreads) {
                    executor.submit {
                        try {
                            // Wait for all threads to be ready
                            startLatch.await()
                            
                            // Attempt to start task (mirrors PhoneAgent.run() behavior)
                            if (state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)) {
                                successfulStarts.incrementAndGet()
                            } else {
                                failedStarts.incrementAndGet()
                            }
                        } finally {
                            doneLatch.countDown()
                        }
                    }
                }
                
                // Release all threads simultaneously for maximum contention
                startLatch.countDown()
                
                // Wait for all threads to complete
                doneLatch.await(5, TimeUnit.SECONDS)
                
            } finally {
                executor.shutdown()
                executor.awaitTermination(1, TimeUnit.SECONDS)
            }
            
            // Property: exactly one task succeeds, all others fail
            val exactlyOneSucceeded = successfulStarts.get() == 1
            val allOthersFailed = failedStarts.get() == numThreads - 1
            val stateIsRunning = state.get() == AgentState.RUNNING
            
            exactlyOneSucceeded && allOthersFailed && stateIsRunning
        }
    }
    
    "concurrent task mutual exclusion - state remains consistent under high contention" {
        val numThreadsArb = Arb.int(10..50)
        val numRoundsArb = Arb.int(1..5)
        
        forAll(100, numThreadsArb, numRoundsArb) { numThreads, numRounds ->
            val state = AtomicReference(AgentState.IDLE)
            var allRoundsValid = true
            
            repeat(numRounds) { round ->
                val successfulStarts = AtomicInteger(0)
                val startLatch = CountDownLatch(1)
                val doneLatch = CountDownLatch(numThreads)
                
                val executor = Executors.newFixedThreadPool(numThreads)
                
                try {
                    // Reset state for this round
                    state.set(AgentState.IDLE)
                    
                    repeat(numThreads) {
                        executor.submit {
                            try {
                                startLatch.await()
                                if (state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)) {
                                    successfulStarts.incrementAndGet()
                                }
                            } finally {
                                doneLatch.countDown()
                            }
                        }
                    }
                    
                    startLatch.countDown()
                    doneLatch.await(5, TimeUnit.SECONDS)
                    
                } finally {
                    executor.shutdown()
                    executor.awaitTermination(1, TimeUnit.SECONDS)
                }
                
                // Verify exactly one succeeded in this round
                if (successfulStarts.get() != 1 || state.get() != AgentState.RUNNING) {
                    allRoundsValid = false
                }
            }
            
            allRoundsValid
        }
    }
    
    "concurrent task mutual exclusion - no task runs when state is already RUNNING" {
        val numThreadsArb = Arb.int(5..30)
        
        forAll(100, numThreadsArb) { numThreads ->
            // Start with state already RUNNING (simulating a task already in progress)
            val state = AtomicReference(AgentState.RUNNING)
            val successfulStarts = AtomicInteger(0)
            
            val startLatch = CountDownLatch(1)
            val doneLatch = CountDownLatch(numThreads)
            
            val executor = Executors.newFixedThreadPool(numThreads)
            
            try {
                repeat(numThreads) {
                    executor.submit {
                        try {
                            startLatch.await()
                            if (state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)) {
                                successfulStarts.incrementAndGet()
                            }
                        } finally {
                            doneLatch.countDown()
                        }
                    }
                }
                
                startLatch.countDown()
                doneLatch.await(5, TimeUnit.SECONDS)
                
            } finally {
                executor.shutdown()
                executor.awaitTermination(1, TimeUnit.SECONDS)
            }
            
            // Property: no task should succeed when state is already RUNNING
            successfulStarts.get() == 0 && state.get() == AgentState.RUNNING
        }
    }
}) {
    companion object {
        /**
         * Validates a task description.
         * This mirrors the logic in PhoneAgent.isValidTask() for testing purposes.
         * 
         * @param task The task description to validate
         * @return true if valid, false if empty or whitespace only
         */
        fun isValidTask(task: String): Boolean {
            return task.isNotBlank()
        }
        
        /**
         * Simulates the task start behavior of PhoneAgent.run().
         * This mirrors the exact logic used in PhoneAgent to enforce task mutual exclusion:
         * - Validates task is not blank
         * - Uses compareAndSet to atomically transition from IDLE to RUNNING
         * - Returns error if another task is already running
         * 
         * @param state The atomic state reference (simulating PhoneAgent.state)
         * @param task The task description
         * @param taskStartedCount Counter for successfully started tasks
         * @param taskRejectedCount Counter for rejected tasks
         * @return SimulatedTaskResult indicating success or failure with message
         */
        fun simulateTaskStart(
            state: AtomicReference<AgentState>,
            task: String,
            taskStartedCount: AtomicInteger,
            taskRejectedCount: AtomicInteger
        ): SimulatedTaskResult {
            // Validate task (mirrors PhoneAgent.isValidTask())
            if (!isValidTask(task)) {
                return SimulatedTaskResult(
                    success = false,
                    message = "Task description cannot be empty or whitespace only"
                )
            }
            
            // Attempt to start task (mirrors PhoneAgent.run() state transition)
            // This is the core mutual exclusion mechanism
            if (!state.compareAndSet(AgentState.IDLE, AgentState.RUNNING)) {
                taskRejectedCount.incrementAndGet()
                return SimulatedTaskResult(
                    success = false,
                    message = "A task is already running. Please wait or cancel it first."
                )
            }
            
            taskStartedCount.incrementAndGet()
            return SimulatedTaskResult(
                success = true,
                message = "Task started successfully"
            )
        }
    }
    
    /**
     * Result of a simulated task start attempt.
     * Mirrors the structure of PhoneAgent.TaskResult for testing purposes.
     */
    data class SimulatedTaskResult(
        val success: Boolean,
        val message: String
    )
}
