import javafx.application.Application
import javafx.stage.FileChooser
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.stage.Stage
import java.io.File

class GUI : Application()
{
    // UI Components
    private lateinit var statusLabel:    Label // Shows current file status and playback info
    private lateinit var playStopButton: Button
    private lateinit var previousButton: Button
    private lateinit var nextButton:     Button
    private lateinit var loadFileButton: Button
    private lateinit var libraryButton:  Button

    // Application state variables
    private var sentences: List<String> = emptyList() // Stores processed sentences from PDF
    private var currentSentenceIndex    = 0
    private var isPlaying               = false
    private var speechHelper: TextToSpeechHelper? = null // TTS engine instance

    override fun start(primaryStage: Stage) {
        primaryStage.title = "PDFTeller"

        speechHelper = TextToSpeechHelper()

        val mainLayout       = VBox(15.0)
        mainLayout.padding   = Insets(20.0)
        mainLayout.alignment = Pos.TOP_CENTER

        statusLabel           = Label("No file loaded")
        statusLabel.style     = "-fx-font-size: 14px; -fx-font-weight: bold;"
        statusLabel.maxWidth  = Double.MAX_VALUE
        statusLabel.alignment = Pos.CENTER

        val playbackControls = createPlaybackControls()
        val fileControls     = createFileControls()

        mainLayout.children.addAll(
            // Top    - Info panel
            statusLabel,
            // Middle - TTS navigation
            playbackControls,
            // Bottom - File management
            fileControls
        )

        val scene              = Scene(mainLayout, 500.0, 200.0)
        primaryStage.scene     = scene
        primaryStage.minWidth  = 450.0
        primaryStage.minHeight = 180.0

        updateButtonStates() // No file is loaded initially!

        primaryStage.show()
    }

    private fun createPlaybackControls(): HBox {
        val controlsBox       = HBox(10.0)
        controlsBox.alignment = Pos.CENTER

        previousButton = Button("⏮")
        previousButton.setOnAction { navigateToPrevious() }

        playStopButton = Button("▶")
        playStopButton.setOnAction { togglePlayback() }
        playStopButton.style = "-fx-font-weight: bold; -fx-font-size: 12px;"

        nextButton     = Button("⏭")
        nextButton.setOnAction     { navigateToNext() }

        controlsBox.children.addAll(previousButton, playStopButton, nextButton)

        return controlsBox
    }

    private fun createFileControls(): HBox {
        val fileBox       = HBox(15.0)
        fileBox.alignment = Pos.CENTER

        loadFileButton = Button("📁 Load PDF File")
        loadFileButton.setOnAction { loadPDFFile() }
        loadFileButton.style = "-fx-font-size: 12px;"

        libraryButton  = Button("📚 Go to Library")
        libraryButton.setOnAction  { showLibrary() }
        libraryButton.style = "-fx-font-size: 12px;"

        fileBox.children.addAll(loadFileButton, libraryButton)

        return fileBox
    }

    private fun loadPDFFile() {
        val fileChooser   = FileChooser()
        fileChooser.title = "Select a PDF File"

        val pdfFilter = FileChooser.ExtensionFilter("PDF Documents", "*.pdf")
        fileChooser.extensionFilters.add(pdfFilter)

        val selectedFile = fileChooser.showOpenDialog(statusLabel.scene.window)
        if (selectedFile != null)
            if (selectedFile.extension.lowercase() == "pdf")
                loadAndProcessFile(selectedFile)
            else
                println("Not a PDF document...")
    }

    private fun loadAndProcessFile(file: File) {
        try
        {
            statusLabel.text = "Loading file: ${file.name}..."

            stopPlayback()

            sentences            = processPdfTextWithStructure(file)
            currentSentenceIndex = 0

            statusLabel.text = "Loaded: ${file.name} (${sentences.size} sentences)"

            updateButtonStates()
        }
        catch (e: Exception)
        {
            statusLabel.text = "Error loading file: ${e.message}"
            sentences        = emptyList()
            updateButtonStates()
        }
    }

    private fun togglePlayback() {
        if (sentences.isEmpty())
        {
            statusLabel.text = "Please load a PDF file first!"
            return
        }

        if (isPlaying) stopPlayback()
        else           startPlayback()
    }

    private fun startPlayback() {
        if (currentSentenceIndex >= sentences.size)
            currentSentenceIndex = 0

        isPlaying           = true
        playStopButton.text = "⏹"

        val currentSentence = sentences[currentSentenceIndex]
        statusLabel.text    = "Playing sentence ${currentSentenceIndex + 1} of ${sentences.size}"

        speechHelper?.speak(currentSentence)

        updateButtonStates()
    }

    private fun stopPlayback() {
        isPlaying           = false
        playStopButton.text = "▶"

        speechHelper?.stop()

        if (sentences.isNotEmpty())
            statusLabel.text = "Stopped at sentence ${currentSentenceIndex + 1} of ${sentences.size}"

        updateButtonStates()
    }

    private fun navigateToPrevious() {
        if (sentences.isEmpty()) return

        if (currentSentenceIndex > 0)
        {
            currentSentenceIndex--
            updateSentenceDisplay()

            if (isPlaying)
                speechHelper?.speak(sentences[currentSentenceIndex])
        }
    }

    private fun navigateToNext() {
        if (sentences.isEmpty()) return

        // Go to next sentence, but don't exceed the list size
        if (currentSentenceIndex < sentences.size - 1)
        {
            currentSentenceIndex++
            updateSentenceDisplay()

            if (isPlaying)
                speechHelper?.speak(sentences[currentSentenceIndex])
        }
        else
        {
            stopPlayback()
            statusLabel.text = "Reached end of document"
        }
    }

    private fun updateSentenceDisplay() {
        if (sentences.isNotEmpty())
        {
            val status       = if (isPlaying) "Playing" else "At"
            statusLabel.text = "$status sentence ${currentSentenceIndex + 1} of ${sentences.size}"
        }
    }

    private fun updateButtonStates() {
        // Enable/disable buttons based on current state
        val hasContent = sentences.isNotEmpty()

        playStopButton.isDisable = !hasContent
        previousButton.isDisable = !hasContent || currentSentenceIndex == 0
        nextButton.isDisable     = !hasContent || currentSentenceIndex >= sentences.size - 1

        loadFileButton.isDisable = false
        libraryButton.isDisable  = false
    }

    private fun showLibrary() {
        val alert         = Alert(Alert.AlertType.INFORMATION)
        alert.title       = "Library"
        alert.headerText  = "Library Feature"
        alert.contentText = "..."
        alert.showAndWait()
    }

    override fun stop() {
        // Clean up when the application is closing
        speechHelper?.stop()
        super.stop()
    }
}

fun main()
{
    Application.launch(GUI::class.java)
}
