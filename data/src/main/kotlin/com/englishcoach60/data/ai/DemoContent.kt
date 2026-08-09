package com.englishcoach60.data.ai

import com.englishcoach60.domain.language.containsHanCharacters
import com.englishcoach60.domain.model.ConversationReply
import com.englishcoach60.domain.model.Correction
import com.englishcoach60.domain.model.CorrectionType
import com.englishcoach60.domain.model.DailyLesson
import com.englishcoach60.domain.model.Expression
import com.englishcoach60.domain.model.ListeningQuestion
import com.englishcoach60.domain.model.SpeakingScenario
import com.englishcoach60.domain.model.WordLookup

object DemoContent {
    fun lookup(query: String): WordLookup {
        val term = query.trim().ifBlank { "confident" }
        return when (term.lowercase()) {
            "confident", "自信", "自信的" -> confidentLookup()
            "schedule", "日程", "安排" -> scheduleLookup()
            "communication", "沟通", "交流" -> WordLookup(
                "communication",
                "/kəˌmjuːnɪˈkeɪʃən/",
                "noun",
                "沟通；交流",
                "the act of sharing information or ideas",
                "Clear communication helps the whole team work efficiently.",
                "清晰的沟通有助于整个团队高效工作。",
                listOf("clear communication", "communication skills"),
            )
            "responsibility", "责任", "职责" -> WordLookup(
                "responsibility",
                "/rɪˌspɒnsəˈbɪləti/",
                "noun",
                "责任；职责",
                "a duty that you are expected to deal with",
                "My main responsibility is reviewing product requirements.",
                "我的主要职责是审查产品需求。",
                listOf("take responsibility", "main responsibility"),
            )
            "improve", "提高", "改善" -> WordLookup(
                "improve",
                "/ɪmˈpruːv/",
                "verb",
                "提高；改善",
                "to become better or make something better",
                "Daily practice can improve your spoken English.",
                "每天练习可以提高你的英语口语。",
                listOf("improve significantly", "improve your skills"),
            )
            else -> fallbackLookup(term)
        }
    }

    private fun confidentLookup() = WordLookup(
        "confident",
        "/ˈkɒnfɪdənt/",
        "adjective",
        "自信的",
        "feeling sure about your abilities",
        "I feel more confident when I practice every day.",
        "每天练习时，我会更有自信。",
        listOf("feel confident", "be confident about"),
    )

    private fun scheduleLookup() = WordLookup(
        "schedule",
        "/ˈskedʒuːl/",
        "noun / verb",
        "日程；安排",
        "a plan of activities and times",
        "Can we schedule the meeting for Friday?",
        "我们可以把会议安排在周五吗？",
        listOf("on schedule", "busy schedule"),
    )

    private fun fallbackLookup(term: String): WordLookup = if (term.containsHanCharacters()) {
        WordLookup(
            "Translation unavailable in Demo Mode",
            "",
            "notice",
            "$term：演示模式暂无对应翻译",
            "Add an API key to translate Chinese words that are not included in the offline demo.",
            "Add an API key, then search for the Chinese word again.",
            "添加 API Key 后，再次搜索这个中文词。",
            emptyList(),
        )
    } else {
        WordLookup(
            term,
            "",
            "word / expression",
            "演示模式释义",
            "Connect an API key for a generated dictionary explanation.",
            "I want to use $term in a natural sentence.",
            "我想在自然的句子中使用 $term。",
            listOf(term),
        )
    }

    fun lesson(day: Int, topic: String, difficulty: Int): DailyLesson {
        val level = difficulty.coerceIn(1, 4)
        val listeningText = when (level) {
            1 -> "Hello, I'm Alex, an Android developer based in Shanghai. I studied computer science at university, where I became interested in building useful mobile products. My current role involves developing app features, reviewing code, and discussing user feedback with my team. Although I can read technical documents in English, I want to communicate more confidently in meetings and when travelling. Outside work, I enjoy walking along the river, watching documentaries, and exploring independent coffee shops. This year, I have set myself a practical goal: to explain my ideas clearly, ask thoughtful questions, and maintain a natural conversation in English."
            2 -> "Hello, I'm Alex, an Android developer based in Shanghai. My academic background is in computer science, but university also taught me how valuable clear communication can be. At work, I develop mobile features, review code, and collaborate with designers to improve the user experience. Reading English documentation is manageable for me; speaking spontaneously is more demanding because I sometimes translate ideas in my head first. To address that weakness, I practise summarising technical decisions and giving reasons for my opinions. In my free time, I watch documentaries, explore independent coffee shops, and meet friends from different industries. My goal is to participate confidently in international meetings and handle unexpected questions without losing my train of thought."
            3 -> "Hello, I'm Alex, a Shanghai-based Android developer with a background in computer science. What initially attracted me to software was the possibility of turning an abstract idea into something people could use every day. My current responsibilities range from implementing features and reviewing pull requests to negotiating design trade-offs with colleagues. I can process technical material in English efficiently, yet spontaneous discussion remains challenging, particularly when I need to qualify an opinion or respond to a counterargument. I therefore practise presenting decisions in a structured way: stating the context, weighing alternatives, and explaining the likely impact. Beyond work, I enjoy documentaries and small coffee shops because both expose me to unfamiliar perspectives. Ultimately, I want English to become a working tool rather than a subject I consciously translate."
            else -> "Hello, I'm Alex, a Shanghai-based Android developer whose academic training is in computer science. I was drawn to software engineering not merely by the technology itself, but by the discipline of translating ambiguous human needs into reliable products. In my current role, I implement features, review architectural proposals, and mediate trade-offs among usability, delivery speed, and long-term maintenance. Although I read technical English fluently, high-pressure discussion can still expose a gap between comprehension and spontaneous expression. I am addressing it by rehearsing concise arguments, challenging assumptions politely, and reformulating ideas when an audience needs a different level of detail. Outside work, documentaries and conversations with people from other industries help me question familiar viewpoints. My objective is not flawless speech; it is the ability to think collaboratively in English, defend a position with appropriate nuance, and adapt when new evidence changes the discussion."
        }
        val expressions = when (level) {
            1 -> listOf(
                Expression("My academic background is in ...", "我的学术背景是……", "My academic background is in computer science.", day),
                Expression("My role involves ...", "我的职责包括……", "My role involves developing app features.", day),
                Expression("Although ..., I want to ...", "虽然……，但我想……", "Although I can read well, I want to speak more confidently.", day),
                Expression("I have set myself a goal", "我为自己设定了目标", "I have set myself a goal for this year.", day),
                Expression("communicate more confidently", "更自信地交流", "I want to communicate more confidently in meetings.", day),
            )
            2 -> listOf(
                Expression("collaborate with ...", "与……协作", "I collaborate with designers on new features.", day),
                Expression("speaking spontaneously", "即兴表达", "Speaking spontaneously is more demanding for me.", day),
                Expression("address a weakness", "改善一个弱项", "Regular practice helps me address this weakness.", day),
                Expression("give reasons for ...", "说明……的理由", "I give reasons for my technical decisions.", day),
                Expression("lose my train of thought", "打断思路", "I stay calm when I lose my train of thought.", day),
            )
            3 -> listOf(
                Expression("range from ... to ...", "范围从……到……", "My tasks range from coding to design discussions.", day),
                Expression("negotiate trade-offs", "权衡并协商取舍", "We negotiate trade-offs before implementation.", day),
                Expression("qualify an opinion", "对观点作限定说明", "It is useful to qualify an opinion with evidence.", day),
                Expression("weigh alternatives", "权衡不同方案", "We weigh alternatives before making a decision.", day),
                Expression("rather than ...", "而不是……", "English should be a working tool rather than a school subject.", day),
            )
            else -> listOf(
                Expression("translate ambiguous needs into ...", "将模糊需求转化为……", "Engineers translate ambiguous needs into reliable products.", day),
                Expression("mediate trade-offs", "协调多方取舍", "I mediate trade-offs among speed, quality, and maintenance.", day),
                Expression("expose a gap between ...", "暴露……之间的差距", "Pressure can expose a gap between knowledge and expression.", day),
                Expression("challenge assumptions politely", "礼貌地质疑假设", "Strong teams challenge assumptions politely.", day),
                Expression("defend a position with nuance", "有分寸地论证立场", "I want to defend a position with appropriate nuance.", day),
            )
        }
        return DailyLesson(
        day = day,
        title = "$topic · Level $level",
        objectiveZh = "以大学英语为起点，练习有逻辑地介绍背景、职责与学习目标。",
        listeningText = listeningText,
        translationZh = "Alex 是一名具有计算机专业背景的 Android 开发者。他希望把英语从阅读工具转化为能够支持会议、协作和复杂表达的工作语言。",
        expressions = expressions,
        questions = listOf(
            ListeningQuestion("Where does Alex live?", listOf("Beijing", "Shanghai", "London"), 1),
            ListeningQuestion("What does Alex do?", listOf("Android developer", "Teacher", "Waiter"), 0),
            ListeningQuestion("Why is Alex learning English?", listOf("For an exam", "For travel and work", "To read novels"), 1),
        ),
        speakingScenario = SpeakingScenario("a new international teammate", "an Android developer joining a project discussion", "Introduce your background, responsibilities, and communication goal with supporting details", "Hi, I'm Maya. Before we discuss the project, could you tell me about your background and what you currently work on?"),
        retellingPrompt = "Introduce your academic or professional background, explain your responsibilities, and give reasons for one communication goal.",
        difficulty = level,
    )
    }
}

class ScriptedConversationProvider {
    fun reply(message: String, turn: Int): ConversationReply {
        val normalized = message.lowercase()
        val correction = when {
            "work android developer" in normalized -> Correction(CorrectionType.IMPORTANT, message, "I work as an Android developer.", "职业前用 work as")
            "yesterday i go" in normalized -> Correction(CorrectionType.IMPORTANT, message, message.replace(" go ", " went ", ignoreCase = true), "过去发生的事用 went")
            else -> Correction()
        }
        val betterExpression = when {
            correction.type != CorrectionType.NONE -> correction.corrected
            normalized.startsWith("i want ") -> message.replaceFirst(Regex("(?i)^i want"), "I'd like")
            normalized.startsWith("i like ") -> message.replaceFirst(Regex("(?i)^i like"), "What I particularly like is")
            normalized.startsWith("i think ") -> message.replaceFirst(Regex("(?i)^i think"), "From my perspective,")
            else -> message.trim().replaceFirstChar(Char::uppercaseChar).let {
                if (it.endsWith('.') || it.endsWith('!') || it.endsWith('?')) it else "$it."
            }
        }
        val replies = listOf("Nice to meet you. Where do you live?", "What do you enjoy about your work?", "What do you usually do after work?", "That sounds interesting. Why are you learning English?", "Thanks for telling me. What would you like to practice next?")
        return ConversationReply(replies[turn.coerceAtLeast(0) % replies.size], correction, betterExpression)
    }
}
