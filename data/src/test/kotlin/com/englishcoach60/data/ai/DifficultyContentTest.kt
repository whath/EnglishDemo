package com.englishcoach60.data.ai

import com.englishcoach60.domain.model.DailyLessonRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyContentTest {
    @Test
    fun demoLessonChangesWithSelectedDifficulty() {
        val foundation = DemoContent.lesson(1, "Introduction", 1)
        val professional = DemoContent.lesson(1, "Introduction", 4)

        assertEquals(1, foundation.difficulty)
        assertEquals(4, professional.difficulty)
        assertNotEquals(foundation.listeningText, professional.listeningText)
        assertNotEquals(foundation.expressions, professional.expressions)
        assertTrue(professional.listeningText.length > foundation.listeningText.length)
    }

    @Test
    fun generatedLessonPromptHasUniversityFoundationFloor() {
        val prompt = DailyLessonPromptFactory.create(DailyLessonRequest(1, "Introduction", 1))

        assertTrue(prompt.contains("university foundation English"))
        assertTrue(prompt.contains("CEFR B1"))
        assertTrue(prompt.contains("University Foundation"))
    }

    @Test
    fun demoConversationAlwaysReturnsABetterExpression() {
        val reply = ScriptedConversationProvider().reply("I enjoy learning English", 0)

        assertTrue(reply.betterExpression.isNotBlank())
    }

    @Test
    fun chineseLookupReturnsAnEnglishHeadword() {
        val result = DemoContent.lookup("自信")

        assertEquals("confident", result.word)
        assertFalse(result.word.any { it.code in 0x4E00..0x9FFF })
        assertEquals("自信的", result.meaningZh)
    }

    @Test
    fun lookupPromptEnforcesTheDirectionForEachLanguage() {
        val chinesePrompt = WordLookupPromptFactory.create("自信")
        val englishPrompt = WordLookupPromptFactory.create("confident")

        assertTrue(chinesePrompt.contains("Chinese-to-English"))
        assertTrue(chinesePrompt.contains("MUST NOT contain Chinese characters"))
        assertTrue(englishPrompt.contains("English-to-Chinese"))
        assertTrue(englishPrompt.contains("MUST be a clear, natural Chinese translation"))
    }
}
