package com.englishcoach60.network

import org.junit.Assert.assertEquals
import org.junit.Test

class AiJsonSanitizerTest {
    @Test fun keepsValidJson() = assertEquals("{\"ok\":true}", AiJsonSanitizer.sanitize("{\"ok\":true}"))
    @Test fun removesFence() = assertEquals("{\"ok\":true}", AiJsonSanitizer.sanitize("```json\n{\"ok\":true}\n```"))
    @Test fun leavesInvalidInputForParser() = assertEquals("not json", AiJsonSanitizer.sanitize("not json"))
}
