package utils

import kotlinx.coroutines.runBlocking
// https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/run-blocking.html

// - Note -
//   A suspend function is a function that can be paused
// and resumed later without blocking a thread!
// https://medium.com/@guruprasadhegde4/kotlin-coroutines-suspend-function-f98ebbbd3bd7

import nl.marc_apps.tts.TextToSpeechFactory
import nl.marc_apps.tts.TextToSpeechInstance
import nl.marc_apps.tts.experimental.ExperimentalDesktopTarget

@OptIn(ExperimentalDesktopTarget::class)
class TextToSpeechHelper
{
    private val ttsEngineCreator = TextToSpeechFactory()
    private var ttsEngine: TextToSpeechInstance? = null

    init
    {
        runBlocking {
            ttsEngine = ttsEngineCreator.create().getOrNull()
        }
    }

    fun speak(sentence: String)
    {
        runBlocking {
            ttsEngine!!.say(sentence)
        }
    }

    fun stop()
    {
        runBlocking {
            ttsEngine!!.stop()
        }
    }
}
