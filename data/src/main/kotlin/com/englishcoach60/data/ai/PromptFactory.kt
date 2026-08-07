package com.englishcoach60.data.ai

import com.englishcoach60.domain.model.*
import com.englishcoach60.domain.training.DifficultyProfiles
import com.englishcoach60.domain.training.TrainingPlan

object DailyLessonPromptFactory {
    fun create(request: DailyLessonRequest): String {
        val range = TrainingPlan.listeningWordRange(request.day, request.difficulty)
        val profile = DifficultyProfiles.get(request.difficulty)
        return """
            You are the private English speaking coach for one Chinese adult learner at beginner to elementary level.
            Current day: ${request.day}; phase: ${TrainingPlan.phase(request.day)}; topic: ${request.topic}; difficulty: ${profile.level} (${profile.name}).
            Difficulty guidance: ${profile.promptGuidance}
            Listening word range: ${range.first}-${range.last}. Use common spoken English and reusable phrases.
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
        You are a real conversation partner, not a lecturer. Day ${context.day}, difficulty ${profile.level} (${profile.name}), topic ${context.topic}.
        Difficulty guidance: ${profile.promptGuidance}
        You are ${context.scenario.aiRole}; learner is ${context.scenario.userRole}; goal: ${context.scenario.goal}.
        Target expressions: ${context.targetExpressions.joinToString()}.
        Recent conversation (up to 10 turns):
        $history
        Recent meaningful mistakes: $recurringMistakes
        Keep replies to at most $maxSentences short sentence(s), ask realistic follow-up questions, correct only meaningful mistakes, never invent pronunciation feedback.
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
    fun create(query: String) = """
        You are a concise English dictionary for a Chinese beginner-to-elementary learner.
        Look up this word or short expression: $query
        The query may be English or Chinese. If it is Chinese, return the most common natural English equivalent as "word".
        Use a standard IPA pronunciation. Give one natural, practical example that is easy to reuse in conversation.
        Keep the Chinese meaning and example translation concise. Return JSON only.
        JSON: {"word":"","phonetic":"","partOfSpeech":"","meaningZh":"","definitionEnglish":"","example":"","exampleZh":"","relatedExpressions":["",""]}
    """.trimIndent()
}
