package com.englishcoach60.data.ai

import com.englishcoach60.domain.language.containsHanCharacters
import com.englishcoach60.domain.model.ConversationContext
import com.englishcoach60.domain.model.CorrectionType
import com.englishcoach60.domain.model.DailyLessonRequest
import com.englishcoach60.domain.model.DailyReviewRequest
import com.englishcoach60.domain.model.RetellingRequest
import com.englishcoach60.domain.training.DifficultyProfiles
import com.englishcoach60.domain.training.TrainingPlan

object DailyLessonPromptFactory {
    fun create(request: DailyLessonRequest): String {
        val range = TrainingPlan.listeningWordRange(request.day, request.difficulty)
        val profile = DifficultyProfiles.get(request.difficulty)
        return """
            You are the private English speaking coach for one Chinese adult learner whose minimum level is university foundation English (CEFR B1).
            Current day: ${request.day}; phase: ${TrainingPlan.phase(request.day)}; topic: ${request.topic}; difficulty: ${profile.level} (${profile.name}).
            Difficulty guidance: ${profile.promptGuidance}
            Listening word range: ${range.first}-${range.last}. Keep every task at or above CEFR B1 and follow the selected profile exactly.
            Exactly five expressions and exactly three multiple-choice listening questions. Return JSON only.
            JSON: {"title":"","objectiveZh":"","listeningText":"","translationZh":"","expressions":[{"expression":"","meaningZh":"","example":""}],"questions":[{"question":"","options":["","",""] ,"answerIndex":0}],"speakingScenario":{"aiRole":"","userRole":"","goal":"","openingLine":""},"retellingPrompt":""}
        """.trimIndent()
    }
}

object ConversationPromptFactory {
    fun create(context: ConversationContext): String {
        val baseSentences = when (context.day) { in 1..10 -> 1; in 11..30 -> 2; else -> 3 }
        val maxSentences = (baseSentences + when (context.difficulty.coerceIn(1, 4)) { 3 -> 1; 4 -> 2; else -> 0 }).coerceAtMost(4)
        val profile = DifficultyProfiles.get(context.difficulty)
        val history = context.turns.takeLast(10).joinToString("\n") { "${it.role}: ${it.text}" }
        val recurringMistakes = context.turns.mapNotNull { it.correction }
            .filter { it.type != CorrectionType.NONE }
            .takeLast(5)
            .joinToString("; ") { "${it.original} -> ${it.corrected}" }
            .ifBlank { "None recorded yet" }
        return """
        You are a real conversation partner, not a lecturer. The learner starts at CEFR B1 university foundation English. Day ${context.day}, difficulty ${profile.level} (${profile.name}), topic ${context.topic}.
        Difficulty guidance: ${profile.promptGuidance}
        You are ${context.scenario.aiRole}; learner is ${context.scenario.userRole}; goal: ${context.scenario.goal}.
        Target expressions: ${context.targetExpressions.joinToString()}.
        Recent conversation (up to 10 turns):
        $history
        Recent meaningful mistakes: $recurringMistakes
        Keep replies to at most $maxSentences short sentence(s), ask realistic follow-up questions, and never invent pronunciation feedback.
        Analyze every user message for grammar. If there is any real grammar error, return type "MINOR" for a small local error or "IMPORTANT" when it affects meaning or fluency; copy the faulty wording to original, provide a complete corrected sentence, and explain it briefly in Chinese. If there is no clear grammar error, return type "NONE", keep corrected empty, and do not invent an error.
        betterExpression is mandatory and must never be empty: always provide one natural, reusable English alternative that preserves the user's meaning without adding facts, even when the original grammar is already correct.
        Continue naturally from the recent conversation instead of restarting the scenario.
        User said: ${context.userMessage}
        Return JSON only: {"replyEnglish":"","correction":{"type":"NONE","original":"","corrected":"","explanationZh":""},"betterExpression":"","usedTargetExpressions":[],"continueConversation":true}
    """.trimIndent()
    }
}

object RetellingPromptFactory {
    fun create(request: RetellingRequest) = """
        Analyze this speech recognition transcript. Do not evaluate pronunciation. Task: ${request.task}. Transcript: ${request.transcript}
        Return JSON only: {"summaryZh":"","correctedVersion":"","topIssues":[{"original":"","better":"","explanationZh":""}],"usefulExpressions":[{"expression":"","meaningZh":"","example":""}]}. Maximum 3 issues and expressions.
    """.trimIndent()
}

object DailyReviewPromptFactory {
    fun create(request: DailyReviewRequest): String {
        val turns = request.turns.takeLast(20).joinToString("\n") { turn ->
            val correction = turn.correction?.takeIf { it.type != CorrectionType.NONE }
                ?.let { " | correction: ${it.original} -> ${it.corrected} (${it.explanationZh})" }
                .orEmpty()
            "${turn.role}: ${turn.text}$correction"
        }
        val retelling = request.retelling?.let {
            "Transcript correction: ${it.correctedVersion}; issues: ${it.topIssues.joinToString { issue -> "${issue.original} -> ${issue.better}" }}"
        } ?: "Not completed"
        return """
        Review practical spoken English day ${request.day}. Never invent pronunciation scores.
        Use only this actual evidence:
        Metrics: ${request.metrics}
        Conversation:
        $turns
        Retelling: $retelling
        Target expressions: ${request.expressions.joinToString { it.expression }}
        Return JSON only: {"progressZh":"","mainProblemZh":"","topMistakes":[{"type":"IMPORTANT","original":"","corrected":"","explanationZh":""}],"keyExpressions":[{"expression":"","meaningZh":"","example":""}],"tomorrowFocusZh":""}. Exactly 5 key expressions.
    """.trimIndent()
    }
}

object WordLookupPromptFactory {
    fun create(query: String): String {
        val direction = if (query.containsHanCharacters()) {
            """
            This is a Chinese-to-English lookup.
            The "word" value MUST be the most common natural English equivalent and MUST NOT contain Chinese characters.
            Keep "meaningZh" as a concise Chinese explanation of the source meaning.
            """.trimIndent()
        } else {
            """
            This is an English-to-Chinese lookup.
            Keep the normalized English word or expression in "word".
            The "meaningZh" value MUST be a clear, natural Chinese translation and MUST NOT be empty.
            """.trimIndent()
        }
        return """
        You are a concise English dictionary for a Chinese university-level English learner (CEFR B1 or above).
        Look up this word or short expression: $query
        $direction
        Use a standard IPA pronunciation. Give one natural, practical example that is easy to reuse in conversation.
        The example and all related expressions must be English. The exampleZh value must be Chinese.
        Keep the Chinese meaning and example translation concise. Return JSON only.
        JSON: {"word":"","phonetic":"","partOfSpeech":"","meaningZh":"","definitionEnglish":"","example":"","exampleZh":"","relatedExpressions":["",""]}
    """.trimIndent()
    }
}
