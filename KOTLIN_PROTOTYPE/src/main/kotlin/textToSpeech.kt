import nl.marc_apps.tts.experimental.ExperimentalDesktopTarget
import nl.marc_apps.tts.TextToSpeechInstance
import nl.marc_apps.tts.TextToSpeechFactory

import kotlinx.coroutines.runBlocking
// https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/run-blocking.html

@OptIn(ExperimentalDesktopTarget::class)
fun textToSpeech(sentence: String)
{
    val ttsEngineCreator = TextToSpeechFactory()

    var ttsEngine: TextToSpeechInstance?
    runBlocking {
        ttsEngine = ttsEngineCreator.create().getOrNull()
        ttsEngine!!.say(sentence)

    // A suspend function is a function that can be paused
    // and resumed later without blocking a thread!
    // https://medium.com/@guruprasadhegde4/kotlin-coroutines-suspend-function-f98ebbbd3bd7
    }
}
