import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlinx.coroutines.*
import utils.TextToSpeechHelper
import utils.processPdfTextWithStructure
import java.io.File

class MainApp : Application()
{
    private          val ttsHelper = TextToSpeechHelper()
    private lateinit var topHalf:    TopHalf
    private lateinit var bottomHalf: BottomHalf

    private val libraryManager = LibraryManager()

    @Volatile
    private var isPlaying     = false
    @Volatile
    private var isStopped     = true
    @Volatile
    private var stopRequested = false
    private var currentIndex  = 0
    private var currentSentences: List<String> = emptyList()
    private var currentBookTitle: String?      = null

    override fun start(primaryStage: Stage)
    {
        // Initialize top half with callbacks
        topHalf = TopHalf(
            onPlay = { handlePlay() },
            onStop = { handleStop() },
            onSaveCheckpoint = { saveCheckpoint() },
            onToggleDyslexia = { enabled -> toggleDyslexiaMode(enabled) }
        )
        topHalf.bindHeightToStage(primaryStage)

        // Initialize bottom half with callbacks
        bottomHalf = BottomHalf(
            onLoadPDF     = { loadPDFFile(primaryStage) },
            onShowLibrary = { showLibrary(primaryStage) }
        )
        bottomHalf.bindHeightToStage(primaryStage)

        // Main layout using VBox to stack the two halves
        val root = VBox().apply {
            children.addAll(topHalf.topHalfContainer, bottomHalf.bottomHalfContainer)
        }

        val scene = Scene(root, 1000.0, 740.0)
        primaryStage.title = "PDFTeller"
        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun toggleDyslexiaMode(enabled: Boolean)
    {
        topHalf.toggleDyslexiaMode(enabled)
    }

    private fun handlePlay()
    {
        if (currentSentences.isEmpty())
        {
            bottomHalf.updateStatus("No PDF loaded")
            return
        }

        isPlaying     = true
        isStopped     = false
        stopRequested = false
        updateButtonStates()

        speakSentences()
    }

    private fun handleStop()
    {
        stopRequested = true

        bottomHalf.updateStatus("Finishing current sentence...")
        Platform.runLater {
            topHalf.stopButton.isDisable = true
        }
    }

    private fun updateButtonStates()
    {
        topHalf.updateButtonStates(isPlaying, currentSentences, currentBookTitle)
    }

    private fun saveCheckpoint()
    {
        val bookTitle = currentBookTitle ?: return

        // Find the book's file path
        val book = libraryManager.library.find { it.title == bookTitle } ?: return

        val success = libraryManager.saveCheckpoint(bookTitle, currentIndex, book.filePath)
        if (success)
        {
            bottomHalf.updateStatus("Checkpoint saved at sentence ${currentIndex + 1}")
            showTemporaryMessage()
        }
        else
            bottomHalf.updateStatus("Failed to save checkpoint")
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun showTemporaryMessage()
    {
        val originalText = bottomHalf.statusLabel.text
        val message      = "✓ Checkpoint saved!"
        bottomHalf.updateStatus(message)
        GlobalScope.launch(Dispatchers.IO)
        {
            delay(2000)
            Platform.runLater {
                if (bottomHalf.statusLabel.text == message)
                    bottomHalf.updateStatus(originalText)
            }
        }
    }

    private fun loadPDFFile(stage: Stage)
    {
        val fileChooser = FileChooser().apply {
            title = "Select a PDF document"
            extensionFilters.add(
                FileChooser.ExtensionFilter("PDF documents", "*.pdf")
            )
        }

        fileChooser.showOpenDialog(stage)?.let { file ->
            processFile(file)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun processFile(file: File)
    {
        bottomHalf.updateStatus("Processing PDF...")
        bottomHalf.clearSentences()
        topHalf.updateCurrentSentence("Loading...")

        GlobalScope.launch(Dispatchers.IO) {
            val sentences = processPdfTextWithStructure(file)
            currentSentences = sentences
            currentIndex     = 0

            // Save to library
            val bookTitle    = file.nameWithoutExtension
            currentBookTitle = bookTitle
            saveBookToLibrary(bookTitle, sentences, file.name)

            bottomHalf.updateStatus("PDF loaded (${sentences.size} sentences)")
            topHalf.updateCurrentSentence("Ready to play")
            updateButtonStates()
        }
    }

    private fun saveBookToLibrary(title: String, sentences: List<String>, originalPdfName: String)
    {
        val success = libraryManager.saveBookToLibrary(title, sentences, originalPdfName)
        if (!success)
            bottomHalf.updateStatus("Failed to save to library")
    }

    private fun showLibrary(primaryStage: Stage)
    {
        libraryManager.showLibrary(
            primaryStage,
            onBookLoaded = { book, sentences ->
                loadBookFromLibrary(book, sentences)
            },
            onCheckpointLoaded = { checkpoint, sentences ->
                loadCheckpoint(checkpoint, sentences)
            }
        )
    }

    private fun loadCheckpoint(checkpoint: Checkpoint, sentences: List<String>)
    {
        currentSentences = sentences
        currentIndex     = checkpoint.sentenceIndex
        currentBookTitle = checkpoint.bookTitle

        bottomHalf.clearSentences()

        // Add previous sentences to history
        val previousSentences = mutableListOf<String>()
        for (i in 0 until checkpoint.sentenceIndex)
            if (i < sentences.size)
                previousSentences.add(sentences[i])

        bottomHalf.addSentences(previousSentences)
        bottomHalf.updateStatus("Loaded checkpoint: ${checkpoint.bookTitle} at sentence ${checkpoint.sentenceIndex + 1}")
        topHalf.updateCurrentSentence("Ready to resume from checkpoint")
        updateButtonStates()
    }

    private fun loadBookFromLibrary(book: Book, sentences: List<String>)
    {
        currentSentences = sentences
        currentIndex     = 0
        currentBookTitle = book.title

        bottomHalf.clearSentences()
        bottomHalf.updateStatus("Loaded: ${book.title} (${sentences.size} sentences)")
        topHalf.updateCurrentSentence("Ready to play")
        updateButtonStates()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun speakSentences()
    {
        GlobalScope.launch(Dispatchers.IO) {
            bottomHalf.updateStatus("Playing...")

            while (currentIndex < currentSentences.size && isPlaying)
            {
                val sentence = currentSentences[currentIndex]

                topHalf.updateCurrentSentence(sentence)
                bottomHalf.addSentence(sentence)

                // Speak the sentence (blocking call)
                ttsHelper.speak(sentence)

                // After sentence completes, check if stop was requested
                if (stopRequested)
                {
                    topHalf.updateCurrentSentence("Stopped")
                    bottomHalf.updateStatus("Stopped")
                    isPlaying     = false
                    isStopped     = true
                    stopRequested = false
                    currentIndex++ // Move to next sentence for resume
                    updateButtonStates()
                    return@launch
                }

                currentIndex++

                // Small delay between sentences
                delay(200)
            }

            // Check if we finished all sentences
            if (currentIndex >= currentSentences.size)
            {
                topHalf.updateCurrentSentence("Completed")
                bottomHalf.updateStatus("Finished")
                currentIndex  = 0  // Reset for replay
                isPlaying     = false
                isStopped     = true
                stopRequested = false
                updateButtonStates()
            }
        }
    }
}

fun main()
{
    Application.launch(MainApp::class.java)
}
