package com.englishcoach60.data.ai

import com.englishcoach60.domain.model.*

object DemoContent {
    fun lookup(query: String): WordLookup {
        val term = query.trim().ifBlank { "confident" }
        return when (term.lowercase()) {
            "confident" -> WordLookup("confident", "/ˈkɒnfɪdənt/", "adjective", "自信的", "feeling sure about your abilities", "I feel more confident when I practice every day.", "每天练习时，我会更有自信。", listOf("feel confident", "be confident about"))
            "schedule" -> WordLookup("schedule", "/ˈskedʒuːl/", "noun / verb", "日程；安排", "a plan of activities and times", "Can we schedule the meeting for Friday?", "我们可以把会议安排在周五吗？", listOf("on schedule", "busy schedule"))
            else -> WordLookup(term, "", "word / expression", "演示模式释义", "Connect an API key for a generated dictionary explanation.", "I want to use $term in a natural sentence.", "我想在自然的句子中使用 $term。", listOf(term))
        }
    }

    fun day1(day: Int, topic: String): DailyLesson = DailyLesson(
        day = day,
        title = topic,
        objectiveZh = if (day == 1) "自然介绍自己的姓名、城市和工作。" else "在真实场景中使用简短、完整的英语句子。",
        listeningText = "Hi, I'm Alex. I live in Shanghai and I work as an Android developer. I usually start work at nine. I build mobile apps with my team. In my free time, I like walking, watching movies, and trying new coffee shops. I'm learning English because I want to travel and speak with coworkers more confidently. Nice to meet you. What do you do?",
        translationZh = "Alex 住在上海，是一名 Android 开发者。他学习英语是为了旅行并更自信地与同事交流。",
        expressions = listOf(
            Expression("I'm ...", "我是……", "I'm Alex.", day),
            Expression("I live in ...", "我住在……", "I live in Shanghai.", day),
            Expression("I work as ...", "我的工作是……", "I work as an Android developer.", day),
            Expression("In my free time ...", "空闲时……", "In my free time, I like walking.", day),
            Expression("Nice to meet you.", "很高兴认识你。", "Nice to meet you, too.", day),
        ),
        questions = listOf(
            ListeningQuestion("Where does Alex live?", listOf("Beijing", "Shanghai", "London"), 1),
            ListeningQuestion("What does Alex do?", listOf("Android developer", "Teacher", "Waiter"), 0),
            ListeningQuestion("Why is Alex learning English?", listOf("For an exam", "For travel and work", "To read novels"), 1),
        ),
        speakingScenario = SpeakingScenario("a friendly new coworker", "an Android developer meeting a coworker", "Introduce yourself and learn about each other", "Hi! I'm Maya. I'm new here. What's your name?"),
        retellingPrompt = "Introduce yourself: your name, city, job, and one thing you like.",
    )
}

class ScriptedConversationProvider {
    fun reply(message: String, turn: Int): ConversationReply {
        val normalized = message.lowercase()
        val correction = when {
            "work android developer" in normalized -> Correction(CorrectionType.IMPORTANT, message, "I work as an Android developer.", "职业前用 work as")
            "yesterday i go" in normalized -> Correction(CorrectionType.IMPORTANT, message, message.replace(" go ", " went ", ignoreCase = true), "过去发生的事用 went")
            else -> Correction()
        }
        val replies = listOf("Nice to meet you. Where do you live?", "What do you enjoy about your work?", "What do you usually do after work?", "That sounds interesting. Why are you learning English?", "Thanks for telling me. What would you like to practice next?")
        return ConversationReply(replies[turn.coerceAtLeast(0) % replies.size], correction, if (correction.type != CorrectionType.NONE) correction.corrected else "")
    }
}
