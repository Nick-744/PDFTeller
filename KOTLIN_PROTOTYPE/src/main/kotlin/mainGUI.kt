import javafx.application.Application
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Book(
    val title: String,
    val sentenceCount: Int,
    val filePath: String,
    val addedDate: LocalDateTime,
    val originalPdfName: String
)

class MainApp : Application() {
    private val ttsHelper = TextToSpeechHelper()
    private val spokenSentences: ObservableList<String> = FXCollections.observableArrayList()
    private lateinit var sentenceListView: ListView<String>
    private lateinit var currentSentenceLabel: Label
    private lateinit var statusLabel: Label
    private lateinit var playButton: Button
    private lateinit var stopButton: Button

    // Library
    private val library: ObservableList<Book> = FXCollections.observableArrayList()
    private val libraryDir = File(System.getProperty("user.home"), ".pdfteller_library")

    @Volatile
    private var isPlaying = false
    @Volatile
    private var isStopped = true
    @Volatile
    private var stopRequested = false
    private var currentIndex = 0
    private var currentSentences: List<String> = emptyList()
    private var currentBookTitle: String? = null

    init {
        // Create library directory if it doesn't exist
        if (!libraryDir.exists()) {
            libraryDir.mkdirs()
        }
        loadLibrary()
    }

    override fun start(primaryStage: Stage) {
        // Current sentence display - will take up half the window
        currentSentenceLabel = Label("No sentence playing...").apply {
            style = "-fx-font-size: 24px; -fx-font-weight: bold; -fx-padding: 20px;"
            isWrapText = true
            alignment = Pos.CENTER
            maxWidth = Double.MAX_VALUE
            maxHeight = Double.MAX_VALUE
        }

        // Wrap the label in a VBox to center it properly
        val currentSentenceContainer = VBox(currentSentenceLabel).apply {
            alignment = Pos.CENTER
            padding = Insets(20.0)
            VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS)
        }

        // Play/Stop control buttons
        playButton = Button("▶ Play").apply {
            prefWidth = 100.0
            setOnAction { handlePlay() }
        }

        stopButton = Button("■ Stop").apply {
            prefWidth = 100.0
            isDisable = true
            setOnAction { handleStop() }
        }

        val controlButtons = HBox(10.0, playButton, stopButton).apply {
            alignment = Pos.CENTER
            padding = Insets(10.0, 10.0, 20.0, 10.0)
        }

        // Top half: Current sentence + controls
        val topHalf = VBox().apply {
            children.addAll(currentSentenceContainer, controlButtons)
            prefHeightProperty().bind(primaryStage.heightProperty().divide(2))
            style = "-fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;"
        }

        // Sentence history list
        sentenceListView = ListView(spokenSentences).apply {
            VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS)

            // Custom cell factory for text wrapping
            setCellFactory {
                object : ListCell<String>() {
                    init {
                        isWrapText = true
                        prefWidthProperty().bind(this@apply.widthProperty().subtract(20))
                        maxWidth = Double.MAX_VALUE
                    }

                    override fun updateItem(item: String?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (empty || item == null) null else item
                        graphic = null
                    }
                }
            }
        }

        // Auto-scroll to latest sentence
        spokenSentences.addListener { _: javafx.collections.ListChangeListener.Change<out String> ->
            if (spokenSentences.isNotEmpty()) {
                Platform.runLater {
                    sentenceListView.scrollTo(spokenSentences.size - 1)
                }
            }
        }

        // Status and load controls at bottom
        statusLabel = Label("Ready")

        val loadButton = Button("Load PDF").apply {
            setOnAction { loadPDFFile(primaryStage) }
        }

        val libraryButton = Button("📚 Library").apply {
            setOnAction { showLibrary(primaryStage) }
        }

        val bottomBar = HBox(10.0, loadButton, libraryButton, statusLabel).apply {
            alignment = Pos.CENTER
            padding = Insets(10.0)
        }

        // Bottom half: List + bottom bar
        val bottomHalf = VBox().apply {
            children.addAll(sentenceListView, bottomBar)
            prefHeightProperty().bind(primaryStage.heightProperty().divide(2))
        }

        // Main layout using VBox to stack the two halves
        val root = VBox().apply {
            children.addAll(topHalf, bottomHalf)
        }

        val scene = Scene(root, 500.0, 700.0)
        primaryStage.title = "PDFTeller"
        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun handlePlay() {
        if (currentSentences.isEmpty()) {
            statusLabel.text = "No PDF loaded"
            return
        }

        isPlaying = true
        isStopped = false
        stopRequested = false
        updateButtonStates()

        speakSentences()
    }

    private fun handleStop() {
        stopRequested = true

        Platform.runLater {
            statusLabel.text = "Finishing current sentence..."
            stopButton.isDisable = true
        }
    }

    private fun updateButtonStates() {
        Platform.runLater {
            playButton.isDisable = isPlaying || currentSentences.isEmpty()
            stopButton.isDisable = !isPlaying
        }
    }

    private fun loadPDFFile(stage: Stage) {
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
    private fun processFile(file: File) {
        Platform.runLater {
            statusLabel.text = "Processing PDF..."
            spokenSentences.clear()
            currentSentenceLabel.text = "Loading..."
        }

        GlobalScope.launch(Dispatchers.IO) {
            val sentences = processPdfTextWithStructure(file)
            currentSentences = sentences
            currentIndex = 0

            // Save to library
            val bookTitle = file.nameWithoutExtension
            currentBookTitle = bookTitle
            saveBookToLibrary(bookTitle, sentences, file.name)

            Platform.runLater {
                statusLabel.text = "PDF loaded (${sentences.size} sentences)"
                currentSentenceLabel.text = "Ready to play"
                updateButtonStates()
            }
        }
    }

    private fun saveBookToLibrary(title: String, sentences: List<String>, originalPdfName: String) {
        // Check if book already exists (same title and sentence count)
        val existingBook = library.find {
            it.title == title && it.sentenceCount == sentences.size
        }

        if (existingBook != null) {
            // Book already exists, don't save again
            return
        }

        // Create unique filename
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "${title}_${timestamp}.txt"
        val filePath = File(libraryDir, fileName).absolutePath

        // Save sentences to file
        try {
            File(filePath).writeText(sentences.joinToString("\n"))

            // Add to library
            val book = Book(
                title = title,
                sentenceCount = sentences.size,
                filePath = filePath,
                addedDate = LocalDateTime.now(),
                originalPdfName = originalPdfName
            )

            Platform.runLater {
                library.add(book)
                saveLibraryMetadata()
            }
        } catch (e: Exception) {
            Platform.runLater {
                statusLabel.text = "Failed to save to library"
            }
        }
    }

    private fun loadLibrary() {
        val metadataFile = File(libraryDir, "library.json")
        if (!metadataFile.exists()) return

        try {
            // Simple parsing (you might want to use a JSON library)
            val lines = metadataFile.readLines()
            lines.forEach { line ->
                if (line.contains("title:")) {
                    // Parse each book entry (simplified - use proper JSON parsing in production)
                    val parts = line.split("|")
                    if (parts.size >= 5) {
                        val book = Book(
                            title = parts[0].substringAfter("title:"),
                            sentenceCount = parts[1].substringAfter("count:").toInt(),
                            filePath = parts[2].substringAfter("path:"),
                            addedDate = LocalDateTime.parse(parts[3].substringAfter("date:")),
                            originalPdfName = parts[4].substringAfter("pdf:")
                        )
                        if (File(book.filePath).exists()) {
                            library.add(book)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Failed to load library, start fresh
        }
    }

    private fun saveLibraryMetadata() {
        val metadataFile = File(libraryDir, "library.json")
        try {
            val content = library.joinToString("\n") { book ->
                "title:${book.title}|count:${book.sentenceCount}|path:${book.filePath}|date:${book.addedDate}|pdf:${book.originalPdfName}"
            }
            metadataFile.writeText(content)
        } catch (e: Exception) {
            // Failed to save metadata
        }
    }

    private fun showLibrary(primaryStage: Stage) {
        val dialog = Stage().apply {
            title = "Library"
            initModality(Modality.APPLICATION_MODAL)
            initOwner(primaryStage)
        }

        val listView = ListView(library).apply {
            setCellFactory {
                object : ListCell<Book>() {
                    override fun updateItem(item: Book?, empty: Boolean) {
                        super.updateItem(item, empty)
                        if (empty || item == null) {
                            text = null
                            graphic = null
                        } else {
                            val vbox = VBox(5.0).apply {
                                children.addAll(
                                    Label(item.title).apply {
                                        style = "-fx-font-weight: bold; -fx-font-size: 14px;"
                                    },
                                    Label("${item.sentenceCount} sentences"),
                                    Label("Added: ${item.addedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}")
                                )
                                padding = Insets(5.0)
                            }
                            graphic = vbox
                        }
                    }
                }
            }
            prefHeight = 400.0
            prefWidth = 400.0
        }

        val loadButton = Button("Load Selected").apply {
            prefWidth = 120.0
            setOnAction {
                val selected = listView.selectionModel.selectedItem
                if (selected != null) {
                    loadBookFromLibrary(selected)
                    dialog.close()
                }
            }
        }

        val deleteButton = Button("Delete").apply {
            prefWidth = 120.0
            setOnAction {
                val selected = listView.selectionModel.selectedItem
                if (selected != null) {
                    deleteBook(selected)
                }
            }
        }

        val closeButton = Button("Close").apply {
            prefWidth = 120.0
            setOnAction { dialog.close() }
        }

        val buttonBar = HBox(10.0, loadButton, deleteButton, closeButton).apply {
            alignment = Pos.CENTER
            padding = Insets(10.0)
        }

        val root = BorderPane().apply {
            center = listView
            bottom = buttonBar
            padding = Insets(10.0)
        }

        dialog.scene = Scene(root, 450.0, 500.0)
        dialog.show()
    }

    private fun loadBookFromLibrary(book: Book) {
        try {
            val sentences = File(book.filePath).readLines()
            currentSentences = sentences
            currentIndex = 0
            currentBookTitle = book.title

            Platform.runLater {
                spokenSentences.clear()
                statusLabel.text = "Loaded: ${book.title} (${sentences.size} sentences)"
                currentSentenceLabel.text = "Ready to play"
                updateButtonStates()
            }
        } catch (e: Exception) {
            Platform.runLater {
                statusLabel.text = "Failed to load book"
            }
        }
    }

    private fun deleteBook(book: Book) {
        val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
            title = "Delete Book"
            headerText = "Delete \"${book.title}\"?"
            contentText = "This action cannot be undone."
        }

        val result = alert.showAndWait()
        if (result.isPresent && result.get() == ButtonType.OK) {
            try {
                File(book.filePath).delete()
                library.remove(book)
                saveLibraryMetadata()
            } catch (e: Exception) {
                // Failed to delete
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun speakSentences() {
        GlobalScope.launch(Dispatchers.IO) {
            Platform.runLater {
                statusLabel.text = "Playing..."
            }

            while (currentIndex < currentSentences.size && isPlaying) {
                val sentence = currentSentences[currentIndex]

                Platform.runLater {
                    currentSentenceLabel.text = sentence
                    spokenSentences.add(sentence)
                }

                // Speak the sentence (blocking call)
                ttsHelper.speak(sentence)

                // After sentence completes, check if stop was requested
                if (stopRequested) {
                    Platform.runLater {
                        currentSentenceLabel.text = "Stopped"
                        statusLabel.text = "Stopped"
                        isPlaying = false
                        isStopped = true
                        stopRequested = false
                        currentIndex++ // Move to next sentence for resume
                        updateButtonStates()
                    }
                    return@launch
                }

                currentIndex++

                // Small delay between sentences
                delay(200)
            }

            // Check if we finished all sentences
            if (currentIndex >= currentSentences.size) {
                Platform.runLater {
                    currentSentenceLabel.text = "Completed"
                    statusLabel.text = "Finished"
                    currentIndex = 0  // Reset for replay
                    isPlaying = false
                    isStopped = true
                    stopRequested = false
                    updateButtonStates()
                }
            }
        }
    }
}

fun main() {
    Application.launch(MainApp::class.java)
}
