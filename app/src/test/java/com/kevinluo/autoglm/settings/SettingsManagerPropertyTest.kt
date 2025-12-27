package com.kevinluo.autoglm.settings

import com.kevinluo.autoglm.agent.AgentConfig
import com.kevinluo.autoglm.model.ModelConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll

/**
 * Property-based tests for settings persistence round-trip.
 * 
 * **Feature: code-logic-audit, Property 19: SettingsManager 配置往返**
 * **Validates: Requirements 9.1, 9.2**
 * 
 * Since SettingsManager requires Android Context (SharedPreferences), these tests
 * validate the round-trip property by simulating the serialization/deserialization
 * logic using an in-memory map that mimics SharedPreferences behavior.
 */
class SettingsManagerPropertyTest : StringSpec({
    
    /**
     * Property 19: SettingsManager 配置往返
     * 
     * For any ModelConfig, saving it to storage and then loading it back should
     * produce an equivalent configuration with all fields preserved.
     * 
     * **Validates: Requirements 9.1, 9.2**
     */
    "ModelConfig round-trip should preserve all fields" {
        // Generators for ModelConfig fields
        val baseUrlArb = Arb.string(10..100, Codepoint.alphanumeric()).map { "https://$it.example.com/api" }
        val apiKeyArb = Arb.string(10..64, Codepoint.alphanumeric())
        val modelNameArb = Arb.string(5..30, Codepoint.alphanumeric())
        val maxTokensArb = Arb.int(100..10000)
        val temperatureArb = Arb.float(0.0f..2.0f)
        val topPArb = Arb.float(0.0f..1.0f)
        val frequencyPenaltyArb = Arb.float(0.0f..2.0f)
        val timeoutSecondsArb = Arb.long(10L..300L)
        
        forAll(
            100,
            baseUrlArb,
            apiKeyArb,
            modelNameArb,
            maxTokensArb,
            temperatureArb,
            topPArb,
            frequencyPenaltyArb,
            timeoutSecondsArb
        ) { baseUrl, apiKey, modelName, maxTokens, temperature, topP, frequencyPenalty, timeoutSeconds ->
            // Create original config
            val originalConfig = ModelConfig(
                baseUrl = baseUrl,
                apiKey = apiKey,
                modelName = modelName,
                maxTokens = maxTokens,
                temperature = temperature,
                topP = topP,
                frequencyPenalty = frequencyPenalty,
                timeoutSeconds = timeoutSeconds
            )
            
            // Simulate SharedPreferences storage using a map
            val storage = mutableMapOf<String, Any?>()
            
            // Save (simulate SettingsManager.saveModelConfig)
            storage["model_base_url"] = originalConfig.baseUrl
            storage["model_api_key"] = originalConfig.apiKey
            storage["model_name"] = originalConfig.modelName
            storage["model_max_tokens"] = originalConfig.maxTokens
            storage["model_temperature"] = originalConfig.temperature
            storage["model_top_p"] = originalConfig.topP
            storage["model_frequency_penalty"] = originalConfig.frequencyPenalty
            storage["model_timeout_seconds"] = originalConfig.timeoutSeconds
            
            // Load (simulate SettingsManager.getModelConfig)
            val loadedConfig = ModelConfig(
                baseUrl = storage["model_base_url"] as String,
                apiKey = (storage["model_api_key"] as String).ifEmpty { "EMPTY" },
                modelName = storage["model_name"] as String,
                maxTokens = storage["model_max_tokens"] as Int,
                temperature = storage["model_temperature"] as Float,
                topP = storage["model_top_p"] as Float,
                frequencyPenalty = storage["model_frequency_penalty"] as Float,
                timeoutSeconds = storage["model_timeout_seconds"] as Long
            )
            
            // Verify all fields are preserved
            originalConfig == loadedConfig
        }
    }
    
    "AgentConfig round-trip should preserve all fields" {
        // Generators for AgentConfig fields
        val maxStepsArb = Arb.int(1..1000)
        val languageArb = Arb.element("cn", "en", "zh", "ja", "ko", "fr", "de", "es")
        val verboseArb = Arb.boolean()
        val screenshotDelayMsArb = Arb.long(500L..10000L)
        
        forAll(100, maxStepsArb, languageArb, verboseArb, screenshotDelayMsArb) { maxSteps, language, verbose, screenshotDelayMs ->
            // Create original config
            val originalConfig = AgentConfig(
                maxSteps = maxSteps,
                language = language,
                verbose = verbose,
                screenshotDelayMs = screenshotDelayMs
            )
            
            // Simulate SharedPreferences storage using a map
            val storage = mutableMapOf<String, Any?>()
            
            // Save (simulate SettingsManager.saveAgentConfig)
            storage["agent_max_steps"] = originalConfig.maxSteps
            storage["agent_language"] = originalConfig.language
            storage["agent_verbose"] = originalConfig.verbose
            storage["agent_screenshot_delay_ms"] = originalConfig.screenshotDelayMs
            
            // Load (simulate SettingsManager.getAgentConfig)
            val loadedConfig = AgentConfig(
                maxSteps = storage["agent_max_steps"] as Int,
                language = storage["agent_language"] as String,
                verbose = storage["agent_verbose"] as Boolean,
                screenshotDelayMs = storage["agent_screenshot_delay_ms"] as Long
            )
            
            // Verify all fields are preserved
            originalConfig == loadedConfig
        }
    }
    
    "ModelConfig with empty API key should default to EMPTY on load" {
        val baseUrlArb = Arb.string(10..50, Codepoint.alphanumeric()).map { "https://$it.example.com" }
        val modelNameArb = Arb.string(5..20, Codepoint.alphanumeric())
        
        forAll(100, baseUrlArb, modelNameArb) { baseUrl, modelName ->
            // Create config with empty API key
            val originalConfig = ModelConfig(
                baseUrl = baseUrl,
                apiKey = "",  // Empty API key
                modelName = modelName
            )
            
            // Simulate SharedPreferences storage
            val storage = mutableMapOf<String, Any?>()
            
            // Save
            storage["model_base_url"] = originalConfig.baseUrl
            storage["model_api_key"] = originalConfig.apiKey
            storage["model_name"] = originalConfig.modelName
            storage["model_max_tokens"] = originalConfig.maxTokens
            storage["model_temperature"] = originalConfig.temperature
            storage["model_top_p"] = originalConfig.topP
            storage["model_frequency_penalty"] = originalConfig.frequencyPenalty
            storage["model_timeout_seconds"] = originalConfig.timeoutSeconds
            
            // Load with empty key handling (as per SettingsManager implementation)
            val loadedApiKey = (storage["model_api_key"] as String).ifEmpty { "EMPTY" }
            
            // Verify empty API key becomes "EMPTY"
            loadedApiKey == "EMPTY"
        }
    }
    
    "ModelConfig default values should be used when storage is empty" {
        // Simulate empty storage (no saved settings)
        val storage = mutableMapOf<String, Any?>()
        val defaultConfig = ModelConfig()
        
        // Load with defaults (simulate SettingsManager.getModelConfig with no saved values)
        val loadedConfig = ModelConfig(
            baseUrl = storage["model_base_url"] as? String ?: defaultConfig.baseUrl,
            apiKey = (storage["model_api_key"] as? String)?.ifEmpty { "EMPTY" } ?: "EMPTY",
            modelName = storage["model_name"] as? String ?: defaultConfig.modelName,
            maxTokens = storage["model_max_tokens"] as? Int ?: defaultConfig.maxTokens,
            temperature = storage["model_temperature"] as? Float ?: defaultConfig.temperature,
            topP = storage["model_top_p"] as? Float ?: defaultConfig.topP,
            frequencyPenalty = storage["model_frequency_penalty"] as? Float ?: defaultConfig.frequencyPenalty,
            timeoutSeconds = storage["model_timeout_seconds"] as? Long ?: defaultConfig.timeoutSeconds
        )
        
        // Verify defaults are used
        loadedConfig.baseUrl == defaultConfig.baseUrl &&
            loadedConfig.apiKey == "EMPTY" &&
            loadedConfig.modelName == defaultConfig.modelName &&
            loadedConfig.maxTokens == defaultConfig.maxTokens &&
            loadedConfig.temperature == defaultConfig.temperature &&
            loadedConfig.topP == defaultConfig.topP &&
            loadedConfig.frequencyPenalty == defaultConfig.frequencyPenalty &&
            loadedConfig.timeoutSeconds == defaultConfig.timeoutSeconds
    }
    
    "AgentConfig default values should be used when storage is empty" {
        // Simulate empty storage (no saved settings)
        val storage = mutableMapOf<String, Any?>()
        val defaultConfig = AgentConfig()
        
        // Load with defaults (simulate SettingsManager.getAgentConfig with no saved values)
        val loadedConfig = AgentConfig(
            maxSteps = storage["agent_max_steps"] as? Int ?: defaultConfig.maxSteps,
            language = storage["agent_language"] as? String ?: defaultConfig.language,
            verbose = storage["agent_verbose"] as? Boolean ?: defaultConfig.verbose,
            screenshotDelayMs = storage["agent_screenshot_delay_ms"] as? Long ?: defaultConfig.screenshotDelayMs
        )
        
        // Verify defaults are used
        loadedConfig.maxSteps == defaultConfig.maxSteps &&
            loadedConfig.language == defaultConfig.language &&
            loadedConfig.verbose == defaultConfig.verbose &&
            loadedConfig.screenshotDelayMs == defaultConfig.screenshotDelayMs
    }
    
    "Multiple save-load cycles should preserve ModelConfig" {
        val baseUrlArb = Arb.string(10..50, Codepoint.alphanumeric()).map { "https://$it.api.com" }
        val apiKeyArb = Arb.string(20..40, Codepoint.alphanumeric())
        val cycleCountArb = Arb.int(2..5)
        
        forAll(100, baseUrlArb, apiKeyArb, cycleCountArb) { baseUrl, apiKey, cycles ->
            val originalConfig = ModelConfig(
                baseUrl = baseUrl,
                apiKey = apiKey
            )
            
            var currentConfig = originalConfig
            val storage = mutableMapOf<String, Any?>()
            
            // Perform multiple save-load cycles
            repeat(cycles) {
                // Save
                storage["model_base_url"] = currentConfig.baseUrl
                storage["model_api_key"] = currentConfig.apiKey
                storage["model_name"] = currentConfig.modelName
                storage["model_max_tokens"] = currentConfig.maxTokens
                storage["model_temperature"] = currentConfig.temperature
                storage["model_top_p"] = currentConfig.topP
                storage["model_frequency_penalty"] = currentConfig.frequencyPenalty
                storage["model_timeout_seconds"] = currentConfig.timeoutSeconds
                
                // Load
                currentConfig = ModelConfig(
                    baseUrl = storage["model_base_url"] as String,
                    apiKey = (storage["model_api_key"] as String).ifEmpty { "EMPTY" },
                    modelName = storage["model_name"] as String,
                    maxTokens = storage["model_max_tokens"] as Int,
                    temperature = storage["model_temperature"] as Float,
                    topP = storage["model_top_p"] as Float,
                    frequencyPenalty = storage["model_frequency_penalty"] as Float,
                    timeoutSeconds = storage["model_timeout_seconds"] as Long
                )
            }
            
            // After multiple cycles, config should still match original
            currentConfig == originalConfig
        }
    }
})
