package com.englishcoach60.network

import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRequestSerializationTest {
    @Test
    fun structuredOutputControlsAreSerialized() {
        val encoded = DeepSeekApiFactory.json.encodeToString(
            ChatRequest(
                model = "deepseek-v4-flash",
                messages = listOf(ChatMessage("user", "Return JSON")),
                temperature = 0.0,
                maxTokens = 30,
            ),
        )

        assertTrue(encoded.contains("\"response_format\":{\"type\":\"json_object\"}"))
        assertTrue(encoded.contains("\"thinking\":{\"type\":\"disabled\"}"))
        assertTrue(encoded.contains("\"stream\":false"))
    }
}
