# English Coach 60

English Coach 60 is a private, local-first Android speaking gym for one Chinese adult learner whose reading is stronger than listening and speaking. Its practical goal is to build independent everyday communication and basic Android/software-development work conversation over 60 **completed training days**. It is not an exam, vocabulary game, social product, or pronunciation scoring tool.

## 60-Day Training Logic

Every day follows one guided flow:

1. **Recall** — up to five due expressions, rated Again / Hard / Good.
2. **Listening** — transcript hidden initially, sentence-chunk TTS, three comprehension questions.
3. **Listen & Repeat** — five to seven sentences with Android speech recognition text comparison.
4. **Scenario Speaking** — microphone-first AI conversation, keyboard fallback, short replies, meaningful correction cards.
5. **Retelling** — multiple short ASR segments combined for language feedback.
6. **Daily Review** — real metrics, coaching, expressions, and tomorrow's focus.

Day 60 also compares the same real metrics with the saved Day 1 baseline; it does not invent an overall score.

Day progression is based on completed sessions, not calendar dates. The next day is advanced only after the daily review, metrics, and expressions have been saved. Speaking targets rise from 8 minutes (Days 1–10) to 12 minutes (Days 11–30) and 15 minutes (Days 31–60); they are goals, not locks.

The route is fixed rather than random: Foundation, Survival English, Social English, Android Work English I/II, and Independent Conversation. AI varies content within each day's topic.

## Word Studio

The Home screen includes a bilingual word search. English or Chinese queries return an English headword, IPA pronunciation, part of speech, Chinese meaning, concise English definition, a bilingual practical example, and useful combinations. The word and example can be read aloud with Android TextToSpeech, and a result can be saved directly to My Expressions for later recall practice.

## Adaptive Difficulty and Swipe Cards

Training difficulty has four user-adjustable levels: Gentle, Balanced, Stretch, and Challenge. The swipeable difficulty cards in Settings define the adaptive baseline, while the level chip inside a session can tune today's lesson. Difficulty changes affect listening length, TextToSpeech pace, AI reply length, scaffolding, and follow-up complexity. A change made during Recall refreshes the lesson before Listening; later changes apply to upcoming audio and AI turns without erasing completed work.

Revealed Recall cards also support gestures: swipe left for Again or right for Good. The visible Again / Hard / Good buttons remain available for accessibility and precise control.

## Architecture

The project uses Clean Architecture with UDF presentation state:

```text
Compose + ViewModel + StateFlow
             ↓
           domain
             ↑
            data
       ↙      ↓       ↘
 network  database  speech
```

The pure JVM `domain` module contains models, repository contracts, the 60-day plan, SRS, difficulty policy, metrics, and session progress rules. Android implementations live outside the domain. Room KSP and Hilt legacy-kapt are isolated in different Gradle modules.

## Modules

- `:app` — Application, Navigation 3, ViewModels, screens, UDF state/actions.
- `:domain` — platform-free models, policies, repository interfaces, tests.
- `:data` — repositories, DataStore, AI orchestration, prompts, demo providers, Hilt modules.
- `:core:designsystem` — calm light/dark theme, type, shapes, spacing, shared components.
- `:core:network` — Retrofit 3, OkHttp, DeepSeek DTOs, JSON sanitizer.
- `:core:database` — Room 3 entities, DAO, database, completion transaction.
- `:core:speech` — Android SpeechRecognizer and TextToSpeech lifecycle/state.

## Tech Stack

- compileSdk / targetSdk 37, minSdk 26
- Gradle 9.5.0, Android Gradle Plugin 9.3.1, JVM target 17
- AGP built-in Kotlin; Kotlin JVM/Serialization/Compose plugins 2.4.10
- Compose BOM 2026.06.00, Navigation 3 1.1.5
- Room 3.0.1 with KSP2; DataStore 1.2.1
- Hilt 2.60.1 with AGP legacy-kapt 9.3.1; AndroidX Hilt 1.4.0
- Retrofit 3.0.0, OkHttp, kotlinx.serialization

## API Key Setup

Add the key only to the ignored `local.properties` file:

```properties
sdk.dir=D\:\\sdkHome
DEEPSEEK_API_KEY=sk-your-private-key
```

Then rebuild the app. The default provider configuration is:

```text
Base URL: https://api.deepseek.com
Model:    deepseek-v4-flash
Endpoint: POST /chat/completions
```

Base URL and model can be changed in Settings. This personal-app approach embeds the key in the local APK and is **not suitable for public/commercial distribution**. Authorization headers are redacted from debug network logs.

## Demo Mode

When `DEEPSEEK_API_KEY` is absent, the app boots in Demo Mode. It includes a complete lesson, listening questions, expressions, scripted speaking (including the correction `I work Android developer.` → `I work as an Android developer.`), retelling feedback, and daily review. Existing cached lessons, history, and expression review remain usable without AI service access.

## How to Build

Requirements: Android SDK 37 and JDK 17 installed. The project emits Java 17-compatible bytecode.

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Storage and Privacy

Room stores lessons, training progress, conversation text, corrections, reviews, expressions, SRS metadata, and real metrics. DataStore stores preferences. Raw audio is never saved; only recognized text and speaking duration are retained. The manifest requests only `INTERNET` and `RECORD_AUDIO`. There is no login, backend, cloud sync, analytics, ads, location, contacts, or device identifier collection.

## Testing

Domain tests cover Again/Hard/Good SRS intervals, completed-day progression, adaptive difficulty bounds, and metrics computed from real evidence. Network tests cover JSON sanitizing and verify that JSON mode, disabled thinking, and non-streaming controls are serialized into every request. Connected Compose tests verify a fresh Home → real Day 1 lesson → Recall path and Review rendering on an Android emulator.
