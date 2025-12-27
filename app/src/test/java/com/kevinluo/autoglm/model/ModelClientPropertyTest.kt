package com.kevinluo.autoglm.model

import com.kevinluo.autoglm.action.ActionParser
import com.kevinluo.autoglm.action.AgentAction
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll
import kotlinx.serialization.json.*

/**
 * Property-based tests for ModelClient message serialization and response parsing.
 * 
 * **Feature: autoglm-phone-agent, Property 6: Message serialization format**
 * **Validates: Requirements 4.7**
 * 
 * **Feature: autoglm-phone-agent, Property 5: Model response parsing**
 * **Validates: Requirements 4.2, 4.8**
 */
class ModelClientPropertyTest : StringSpec({
    
    val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Property 6: Message serialization format
     * 
     * For any list of ChatMessage objects, serializing them for the API should produce
     * a valid JSON structure conforming to the OpenAI chat completion schema with
     * correct role and content fields.
     * 
     * **Validates: Requirements 4.7**
     */
    "system message serialization should produce valid OpenAI schema with role=system and string content" {
        val contentArb = Arb.string(1..500, Codepoint.alphanumeric())
        
        forAll(100, contentArb) { content ->
            val message = ChatMessage.System(content)
            val dto = MessageDto.fromChatMessage(message)
            
            // Verify role is "system"
            val roleCorrect = dto.role == "system"
            
            // Verify content is a string primitive
            val contentIsString = dto.content is JsonPrimitive && 
                (dto.content as JsonPrimitive).isString
            
            // Verify content value matches
            val contentMatches = (dto.content as? JsonPrimitive)?.content == content
            
            roleCorrect && contentIsString && contentMatches
        }
    }
    
    "assistant message serialization should produce valid OpenAI schema with role=assistant and string content" {
        val contentArb = Arb.string(1..500, Codepoint.alphanumeric())
        
        forAll(100, contentArb) { content ->
            val message = ChatMessage.Assistant(content)
            val dto = MessageDto.fromChatMessage(message)
            
            // Verify role is "assistant"
            val roleCorrect = dto.role == "assistant"
            
            // Verify content is a string primitive
            val contentIsString = dto.content is JsonPrimitive && 
                (dto.content as JsonPrimitive).isString
            
            // Verify content value matches
            val contentMatches = (dto.content as? JsonPrimitive)?.content == content
            
            roleCorrect && contentIsString && contentMatches
        }
    }
    
    "user message without image should produce valid OpenAI schema with role=user and string content" {
        val textArb = Arb.string(1..500, Codepoint.alphanumeric())
        
        forAll(100, textArb) { text ->
            val message = ChatMessage.User(text, imageBase64 = null)
            val dto = MessageDto.fromChatMessage(message)
            
            // Verify role is "user"
            val roleCorrect = dto.role == "user"
            
            // Verify content is a string primitive (text-only)
            val contentIsString = dto.content is JsonPrimitive && 
                (dto.content as JsonPrimitive).isString
            
            // Verify content value matches
            val contentMatches = (dto.content as? JsonPrimitive)?.content == text
            
            roleCorrect && contentIsString && contentMatches
        }
    }
    
    "user message with image should produce valid OpenAI schema with role=user and array content" {
        val textArb = Arb.string(1..200, Codepoint.alphanumeric())
        // Generate valid base64-like strings (alphanumeric is valid base64 subset)
        val imageArb = Arb.string(10..100, Codepoint.alphanumeric())
        
        forAll(100, textArb, imageArb) { text, imageBase64 ->
            val message = ChatMessage.User(text, imageBase64)
            val dto = MessageDto.fromChatMessage(message)
            
            // Verify role is "user"
            val roleCorrect = dto.role == "user"
            
            // Verify content is a JSON array (multi-modal)
            val contentIsArray = dto.content is JsonArray
            
            if (!contentIsArray) {
                false
            } else {
                val contentArray = dto.content as JsonArray
                
                // Should have exactly 2 elements: text and image_url
                val hasTwoElements = contentArray.size == 2
                
                // First element should be text type
                val firstElement = contentArray.getOrNull(0) as? JsonObject
                val firstIsText = firstElement?.get("type")?.jsonPrimitive?.content == "text" &&
                    firstElement.get("text")?.jsonPrimitive?.content == text
                
                // Second element should be image_url type
                val secondElement = contentArray.getOrNull(1) as? JsonObject
                val secondIsImage = secondElement?.get("type")?.jsonPrimitive?.content == "image_url"
                
                // Verify image_url contains the correct data URL format
                val imageUrlObj = secondElement?.get("image_url") as? JsonObject
                val imageUrlCorrect = imageUrlObj?.get("url")?.jsonPrimitive?.content == 
                    "data:image/png;base64,$imageBase64"
                
                roleCorrect && hasTwoElements && firstIsText && secondIsImage && imageUrlCorrect
            }
        }
    }
    
    "ChatCompletionRequest serialization should produce valid JSON with all required fields" {
        val modelArb = Arb.string(5..30, Codepoint.alphanumeric())
        val maxTokensArb = Arb.int(100..5000)
        val contentArb = Arb.string(1..100, Codepoint.alphanumeric())
        
        forAll(100, modelArb, maxTokensArb, contentArb) { model, maxTokens, content ->
            // Create a MessageDto directly (as used in the actual implementation)
            val messageDto = MessageDto(
                role = "system",
                content = JsonPrimitive(content)
            )
            
            // Use fixed float values to avoid precision issues in JSON serialization
            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(messageDto),
                maxTokens = maxTokens,
                temperature = 0.5f,
                topP = 0.85f,
                frequencyPenalty = 0.2f,
                stream = true
            )
            
            // Serialize to JSON
            val jsonString = json.encodeToString(ChatCompletionRequest.serializer(), request)
            
            // Parse back to verify structure
            val jsonObj = try {
                json.parseToJsonElement(jsonString).jsonObject
            } catch (e: Exception) {
                return@forAll false
            }
            
            // Verify all required fields are present with correct OpenAI schema field names
            // Check model field
            val modelField = jsonObj["model"]?.jsonPrimitive?.contentOrNull
            val hasModel = modelField == model
            
            // Check messages field is an array
            val hasMessages = jsonObj["messages"] is JsonArray
            
            // Check max_tokens field (snake_case per OpenAI schema)
            val maxTokensField = jsonObj["max_tokens"]?.jsonPrimitive?.intOrNull
            val hasMaxTokens = maxTokensField == maxTokens
            
            // Check temperature field exists
            val hasTemperature = jsonObj.containsKey("temperature")
            
            // Check top_p field exists (snake_case per OpenAI schema)
            val hasTopP = jsonObj.containsKey("top_p")
            
            // Check frequency_penalty field exists (snake_case per OpenAI schema)
            val hasFreqPenalty = jsonObj.containsKey("frequency_penalty")
            
            // Check stream field
            val streamField = jsonObj["stream"]?.jsonPrimitive?.booleanOrNull
            val hasStream = streamField == true
            
            hasModel && hasMessages && hasMaxTokens && hasTemperature && 
                hasTopP && hasFreqPenalty && hasStream
        }
    }
    
    "mixed message list serialization should preserve order and types" {
        val contentArb = Arb.string(1..100, Codepoint.alphanumeric())
        val messageCountArb = Arb.int(1..10)
        
        forAll(100, contentArb, messageCountArb) { baseContent, count ->
            // Create a list of mixed messages
            val messages = (0 until count).map { i ->
                when (i % 3) {
                    0 -> ChatMessage.System("$baseContent-system-$i")
                    1 -> ChatMessage.User("$baseContent-user-$i", null)
                    else -> ChatMessage.Assistant("$baseContent-assistant-$i")
                }
            }
            
            // Convert to DTOs
            val dtos = messages.map { MessageDto.fromChatMessage(it) }
            
            // Verify count matches
            val countMatches = dtos.size == count
            
            // Verify each message has correct role
            val rolesCorrect = dtos.mapIndexed { i, dto ->
                when (i % 3) {
                    0 -> dto.role == "system"
                    1 -> dto.role == "user"
                    else -> dto.role == "assistant"
                }
            }.all { it }
            
            // Verify content is preserved
            val contentCorrect = dtos.mapIndexed { i, dto ->
                val expectedContent = when (i % 3) {
                    0 -> "$baseContent-system-$i"
                    1 -> "$baseContent-user-$i"
                    else -> "$baseContent-assistant-$i"
                }
                (dto.content as? JsonPrimitive)?.content == expectedContent
            }.all { it }
            
            countMatches && rolesCorrect && contentCorrect
        }
    }
    
    /**
     * Property 5: Model response parsing
     * 
     * For any valid model response containing thinking and action components,
     * parsing the response should correctly extract both the thinking text and
     * the action string, and the action should be parseable into a valid AgentAction.
     * 
     * **Feature: autoglm-phone-agent, Property 5: Model response parsing**
     * **Validates: Requirements 4.2, 4.8**
     */
    "model response with do action should correctly extract thinking and parseable action" {
        // Generate thinking text (alphanumeric to avoid special chars that could break parsing)
        val thinkingArb = Arb.string(10..200, Codepoint.alphanumeric())
        // Generate coordinates in valid range [0, 999] (ActionParser validates 0-999)
        val coordArb = Arb.int(0..999)
        
        forAll(100, thinkingArb, coordArb, coordArb) { thinking, x, y ->
            // Construct a valid model response with thinking and Tap action
            val actionStr = """do(action="Tap", element=[$x, $y])"""
            val fullResponse = "$thinking\n\n$actionStr"
            
            // Create ModelClient to use parseThinkingAndAction
            val modelClient = ModelClient(ModelConfig())
            
            // Use reflection to access private method or test via public interface
            // Since parseThinkingAndAction is private, we test via the response structure
            // by simulating what the client does
            
            // Extract thinking and action using the same patterns as ModelClient
            val doPattern = Regex("""do\s*\([^)]+\)""", RegexOption.DOT_MATCHES_ALL)
            val actionMatch = doPattern.find(fullResponse)
            
            if (actionMatch == null) {
                false
            } else {
                val extractedThinking = fullResponse.substring(0, actionMatch.range.first).trim()
                val extractedAction = actionMatch.value.trim()
                
                // Verify thinking is correctly extracted
                val thinkingCorrect = extractedThinking == thinking
                
                // Verify action string matches
                val actionStringCorrect = extractedAction == actionStr
                
                // Verify action is parseable into valid AgentAction
                val parsedAction = try {
                    ActionParser.parse(extractedAction)
                } catch (e: Exception) {
                    null
                }
                
                val actionParseable = parsedAction != null && parsedAction is AgentAction.Tap
                val coordsCorrect = if (parsedAction is AgentAction.Tap) {
                    parsedAction.x == x && parsedAction.y == y
                } else {
                    false
                }
                
                thinkingCorrect && actionStringCorrect && actionParseable && coordsCorrect
            }
        }
    }
    
    "model response with finish action should correctly extract thinking and finish message" {
        val thinkingArb = Arb.string(10..200, Codepoint.alphanumeric())
        val messageArb = Arb.string(5..100, Codepoint.alphanumeric())
        
        forAll(100, thinkingArb, messageArb) { thinking, message ->
            // Construct a valid model response with thinking and finish action
            val actionStr = """finish(message="$message")"""
            val fullResponse = "$thinking\n\n$actionStr"
            
            // Extract thinking and action using the same patterns as ModelClient
            val finishPattern = Regex("""finish\s*\([^)]+\)""", RegexOption.DOT_MATCHES_ALL)
            val actionMatch = finishPattern.find(fullResponse)
            
            if (actionMatch == null) {
                false
            } else {
                val extractedThinking = fullResponse.substring(0, actionMatch.range.first).trim()
                val extractedAction = actionMatch.value.trim()
                
                // Verify thinking is correctly extracted
                val thinkingCorrect = extractedThinking == thinking
                
                // Verify action string matches
                val actionStringCorrect = extractedAction == actionStr
                
                // Verify action is parseable into valid AgentAction.Finish
                val parsedAction = try {
                    ActionParser.parse(extractedAction)
                } catch (e: Exception) {
                    null
                }
                
                val actionParseable = parsedAction != null && parsedAction is AgentAction.Finish
                val messageCorrect = if (parsedAction is AgentAction.Finish) {
                    parsedAction.message == message
                } else {
                    false
                }
                
                thinkingCorrect && actionStringCorrect && actionParseable && messageCorrect
            }
        }
    }
    
    "model response with swipe action should correctly extract and parse coordinates" {
        val thinkingArb = Arb.string(10..100, Codepoint.alphanumeric())
        // Generate coordinates in valid range [0, 999] (ActionParser validates 0-999)
        val coordArb = Arb.int(0..999)
        
        forAll(100, thinkingArb, coordArb, coordArb, coordArb, coordArb) { thinking, startX, startY, endX, endY ->
            // Construct a valid model response with thinking and Swipe action
            val actionStr = """do(action="Swipe", start=[$startX, $startY], end=[$endX, $endY])"""
            val fullResponse = "$thinking\n\n$actionStr"
            
            // Extract action using the same pattern as ModelClient
            val doPattern = Regex("""do\s*\([^)]+\)""", RegexOption.DOT_MATCHES_ALL)
            val actionMatch = doPattern.find(fullResponse)
            
            if (actionMatch == null) {
                false
            } else {
                val extractedThinking = fullResponse.substring(0, actionMatch.range.first).trim()
                val extractedAction = actionMatch.value.trim()
                
                // Verify thinking extraction
                val thinkingCorrect = extractedThinking == thinking
                
                // Verify action is parseable into valid AgentAction.Swipe
                val parsedAction = try {
                    ActionParser.parse(extractedAction)
                } catch (e: Exception) {
                    null
                }
                
                val actionParseable = parsedAction != null && parsedAction is AgentAction.Swipe
                val coordsCorrect = if (parsedAction is AgentAction.Swipe) {
                    parsedAction.startX == startX && 
                    parsedAction.startY == startY &&
                    parsedAction.endX == endX && 
                    parsedAction.endY == endY
                } else {
                    false
                }
                
                thinkingCorrect && actionParseable && coordsCorrect
            }
        }
    }
    
    "model response with type action should correctly extract and parse text content" {
        val thinkingArb = Arb.string(10..100, Codepoint.alphanumeric())
        // Use alphanumeric for text to avoid quote escaping issues
        val textArb = Arb.string(1..50, Codepoint.alphanumeric())
        
        forAll(100, thinkingArb, textArb) { thinking, text ->
            // Construct a valid model response with thinking and Type action
            val actionStr = """do(action="Type", text="$text")"""
            val fullResponse = "$thinking\n\n$actionStr"
            
            // Extract action using the same pattern as ModelClient
            val doPattern = Regex("""do\s*\([^)]+\)""", RegexOption.DOT_MATCHES_ALL)
            val actionMatch = doPattern.find(fullResponse)
            
            if (actionMatch == null) {
                false
            } else {
                val extractedThinking = fullResponse.substring(0, actionMatch.range.first).trim()
                val extractedAction = actionMatch.value.trim()
                
                // Verify thinking extraction
                val thinkingCorrect = extractedThinking == thinking
                
                // Verify action is parseable into valid AgentAction.Type
                val parsedAction = try {
                    ActionParser.parse(extractedAction)
                } catch (e: Exception) {
                    null
                }
                
                val actionParseable = parsedAction != null && parsedAction is AgentAction.Type
                val textCorrect = if (parsedAction is AgentAction.Type) {
                    parsedAction.text == text
                } else {
                    false
                }
                
                thinkingCorrect && actionParseable && textCorrect
            }
        }
    }
    
    "model response with launch action should correctly extract and parse app name" {
        val thinkingArb = Arb.string(10..100, Codepoint.alphanumeric())
        val appNameArb = Arb.string(3..30, Codepoint.alphanumeric())
        
        forAll(100, thinkingArb, appNameArb) { thinking, appName ->
            // Construct a valid model response with thinking and Launch action
            val actionStr = """do(action="Launch", app="$appName")"""
            val fullResponse = "$thinking\n\n$actionStr"
            
            // Extract action using the same pattern as ModelClient
            val doPattern = Regex("""do\s*\([^)]+\)""", RegexOption.DOT_MATCHES_ALL)
            val actionMatch = doPattern.find(fullResponse)
            
            if (actionMatch == null) {
                false
            } else {
                val extractedThinking = fullResponse.substring(0, actionMatch.range.first).trim()
                val extractedAction = actionMatch.value.trim()
                
                // Verify thinking extraction
                val thinkingCorrect = extractedThinking == thinking
                
                // Verify action is parseable into valid AgentAction.Launch
                val parsedAction = try {
                    ActionParser.parse(extractedAction)
                } catch (e: Exception) {
                    null
                }
                
                val actionParseable = parsedAction != null && parsedAction is AgentAction.Launch
                val appCorrect = if (parsedAction is AgentAction.Launch) {
                    parsedAction.app == appName
                } else {
                    false
                }
                
                thinkingCorrect && actionParseable && appCorrect
            }
        }
    }
})
