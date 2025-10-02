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
import kotlinx.coroutines.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Book
(
    val title:           String,
    val sentenceCount:   Int,
    val filePath:        String,
    val addedDate:       LocalDateTime,
    val originalPdfName: String
)

data class Checkpoint
(
    val bookTitle:     String,
    val sentenceIndex: Int,
    val savedDate:     LocalDateTime,
    val filePath:      String
)

class MainApp : Application()
{
    private          val ttsHelper = TextToSpeechHelper()
    private          val spokenSentences:      ObservableList<String> = FXCollections.observableArrayList()
    private lateinit var sentenceListView:     ListView<String>
    private lateinit var currentSentenceLabel: Label
    private lateinit var statusLabel:          Label
    private lateinit var playButton:           Button
    private lateinit var stopButton:           Button
    private lateinit var checkpointButton:     Button
    private lateinit var dyslexiaToggle:       ToggleButton
    private          var baseSentenceStyle:    String = ""
    private lateinit var topHalf:              VBox
    private          var baseTopHalfStyle:     String = ""

    // Library
    private val library:     ObservableList<Book>       = FXCollections.observableArrayList()
    private val checkpoints: ObservableList<Checkpoint> = FXCollections.observableArrayList()
    private val libraryDir = File(System.getProperty("user.home"), ".pdfteller_library")

    @Volatile
    private var isPlaying     = false
    @Volatile
    private var isStopped     = true
    @Volatile
    private var stopRequested = false
    private var currentIndex  = 0
    private var currentSentences: List<String> = emptyList()
    private var currentBookTitle: String?      = null

    init
    {
        // Create library directory if it doesn't exist
        if (!libraryDir.exists())
            libraryDir.mkdirs()
        loadLibrary()
        loadCheckpoints()
    }

    override fun start(primaryStage: Stage)
    {
        // Current sentence display
        currentSentenceLabel = Label("No sentence playing...").apply {
            style             = "-fx-font-size: 24px; -fx-padding: 20px;"
            baseSentenceStyle = style // Save the style so we can restore it!

            isWrapText = true
            alignment  = Pos.CENTER
            maxWidth   = Double.MAX_VALUE
            maxHeight  = Double.MAX_VALUE
        }

        val currentSentenceContainer = VBox(currentSentenceLabel).apply {
            alignment = Pos.CENTER
            padding   = Insets(20.0)
            VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS)
        }

        // Play/Stop/Checkpoint control buttons
        playButton = Button("▶ Play").apply {
            prefWidth = 100.0
            setOnAction { handlePlay() }
        }

        stopButton = Button("■ Stop").apply {
            prefWidth = 100.0
            isDisable = true
            setOnAction { handleStop() }
        }

        checkpointButton = Button("🔖 Save Checkpoint").apply {
            prefWidth = 150.0
            isDisable = true
            setOnAction { saveCheckpoint() }
        }

        // Dyslexia-friendly toggle
        dyslexiaToggle = ToggleButton("Dyslexia-friendly").apply {
            prefWidth = 160.0
            isDisable = false
            setOnAction {
                toggleDyslexiaMode(isSelected)
            }
        }

        val controlButtons = HBox(10.0, playButton, stopButton, checkpointButton, dyslexiaToggle).apply {
            alignment = Pos.CENTER
            padding   = Insets(10.0, 10.0, 20.0, 10.0)
        }

        // Top half: Current sentence + controls
        topHalf = VBox().apply {
            children.addAll(currentSentenceContainer, controlButtons)
            prefHeightProperty().bind(primaryStage.heightProperty().divide(2))
            style            = "-fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;"
            baseTopHalfStyle = style // Save base style for restoring later...
        }

        // Sentence history list
        sentenceListView = ListView(spokenSentences).apply {
            VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS)

            // Custom cell factory for text wrapping
            setCellFactory {
                object : ListCell<String>()
                {
                    init
                    {
                        isWrapText = true
                        prefWidthProperty().bind(this@apply.widthProperty().subtract(20))
                        maxWidth = Double.MAX_VALUE
                    }

                    override fun updateItem(item: String?, empty: Boolean)
                    {
                        super.updateItem(item, empty)
                        text    = if (empty || item == null) null else item
                        graphic = null
                    }
                }
            }
        }

        // Auto-scroll to latest sentence
        spokenSentences.addListener { _: javafx.collections.ListChangeListener.Change<out String> ->
            if (spokenSentences.isNotEmpty())
                Platform.runLater {
                    sentenceListView.scrollTo(spokenSentences.size - 1)
                }
        }

        // Status and load controls at bottom
        statusLabel = Label("Ready for your pdf!")

        val loadButton = Button("Load PDF").apply {
            setOnAction { loadPDFFile(primaryStage) }
        }

        val libraryButton = Button("📚 Library").apply {
            setOnAction { showLibrary(primaryStage) }
        }

        val bottomBar = HBox(10.0, loadButton, libraryButton, statusLabel).apply {
            alignment = Pos.CENTER
            padding   = Insets(10.0)
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

        val scene = Scene(root, 1000.0, 740.0)
        primaryStage.title = "PDFTeller"
        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun toggleDyslexiaMode(enabled: Boolean)
    {
        Platform.runLater {
            if (enabled)
            {
                if (this@MainApp::topHalf.isInitialized)
                    topHalf.style = "$baseTopHalfStyle -fx-background-color: #1d0f0f;"
                // Apply text color and font for the current sentence label
                currentSentenceLabel.style = "$baseSentenceStyle -fx-text-fill: #a08060; -fx-font-family: 'OpenDyslexic3';"
            }
            else
            { // Restore base styles
                if (this@MainApp::topHalf.isInitialized)
                    topHalf.style = baseTopHalfStyle
                currentSentenceLabel.style = baseSentenceStyle
            }
        }
    }

    private fun handlePlay()
    {
        if (currentSentences.isEmpty())
        {
            statusLabel.text = "No PDF loaded"
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

        Platform.runLater {
            statusLabel.text     = "Finishing current sentence..."
            stopButton.isDisable = true
        }
    }

    private fun updateButtonStates() {
        Platform.runLater {
            playButton.isDisable       = isPlaying || currentSentences.isEmpty()
            stopButton.isDisable       = !isPlaying
            checkpointButton.isDisable = currentSentences.isEmpty() || currentBookTitle == null
        }
    }

    private fun saveCheckpoint()
    {
        val bookTitle = currentBookTitle ?: return

        // Find the book's file path
        val book = library.find { it.title == bookTitle } ?: return

        val checkpoint = Checkpoint(
            bookTitle     = bookTitle,
            sentenceIndex = currentIndex,
            savedDate     = LocalDateTime.now(),
            filePath      = book.filePath
        )

        // Remove old checkpoints for the same book
        checkpoints.removeIf { it.bookTitle == bookTitle }
        checkpoints.add(checkpoint)
        saveCheckpointsMetadata()

        Platform.runLater {
            statusLabel.text = "Checkpoint saved at sentence ${currentIndex + 1}"
            showTemporaryMessage()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun showTemporaryMessage()
    {
        val originalText = statusLabel.text
        val message      = "✓ Checkpoint saved!"
        statusLabel.text = message
        GlobalScope.launch(Dispatchers.IO) {
            delay(2000)
            Platform.runLater {
                if (statusLabel.text == message) {
                    statusLabel.text = originalText
                }
            }
        }
    }

    private fun loadCheckpoints()
    {
        val checkpointsFile = File(libraryDir, "checkpoints.txt")
        if (!checkpointsFile.exists()) return

        try
        {
            val lines = checkpointsFile.readLines()
            lines.forEach { line ->
                if (line.contains("title:"))
                {
                    val parts = line.split("|")
                    if (parts.size >= 4) {
                        val checkpoint = Checkpoint(
                            bookTitle     = parts[0].substringAfter("title:"),
                            sentenceIndex = parts[1].substringAfter("index:").toInt(),
                            savedDate     = LocalDateTime.parse(parts[2].substringAfter("date:")),
                            filePath      = parts[3].substringAfter("path:")
                        )
                        if (File(checkpoint.filePath).exists())
                            checkpoints.add(checkpoint)
                    }
                }
            }
        }
        catch (e: Exception) { println(e) }
    }

    private fun saveCheckpointsMetadata()
    {
        val checkpointsFile = File(libraryDir, "checkpoints.txt")
        try
        {
            val content = checkpoints.joinToString("\n") { checkpoint ->
                "title:${checkpoint.bookTitle}|index:${checkpoint.sentenceIndex}|date:${checkpoint.savedDate}|path:${checkpoint.filePath}"
            }
            checkpointsFile.writeText(content)
        }
        catch (e: Exception) { println(e) }
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
        Platform.runLater {
            statusLabel.text          = "Processing PDF..."
            spokenSentences.clear()
            currentSentenceLabel.text = "Loading..."
        }

        GlobalScope.launch(Dispatchers.IO) {
            val sentences = processPdfTextWithStructure(file)
            currentSentences = sentences
            currentIndex     = 0

            // Save to library
            val bookTitle    = file.nameWithoutExtension
            currentBookTitle = bookTitle
            saveBookToLibrary(bookTitle, sentences, file.name)

            Platform.runLater {
                statusLabel.text          = "PDF loaded (${sentences.size} sentences)"
                currentSentenceLabel.text = "Ready to play"
                updateButtonStates()
            }
        }
    }

    private fun saveBookToLibrary(title: String, sentences: List<String>, originalPdfName: String)
    {
        // Check if book already exists (same title and sentence count)
        val existingBook = library.find {
            it.title == title && it.sentenceCount == sentences.size
        }

        if (existingBook != null)
            return // Book already exists, don't save again

        // Create unique filename
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName  = "${title}_${timestamp}.txt"
        val filePath  = File(libraryDir, fileName).absolutePath

        // Save sentences to file
        try
        {
            File(filePath).writeText(sentences.joinToString("\n"))

            // Add to library
            val book = Book(
                title           = title,
                sentenceCount   = sentences.size,
                filePath        = filePath,
                addedDate       = LocalDateTime.now(),
                originalPdfName = originalPdfName
            )

            Platform.runLater {
                library.add(book)
                saveLibraryMetadata()
            }
        }
        catch (e: Exception) {
            Platform.runLater {
                statusLabel.text = "Failed to save to library"
            }
        }
    }

    private fun loadLibrary()
    {
        val metadataFile = File(libraryDir, "library.json")
        if (!metadataFile.exists()) return

        try
        {
            val lines = metadataFile.readLines()
            lines.forEach { line ->
                if (line.contains("title:"))
                {
                    val parts = line.split("|")
                    if (parts.size >= 5)
                    {
                        val book = Book(
                            title = parts[0].substringAfter("title:"),
                            sentenceCount = parts[1].substringAfter("count:").toInt(),
                            filePath = parts[2].substringAfter("path:"),
                            addedDate = LocalDateTime.parse(parts[3].substringAfter("date:")),
                            originalPdfName = parts[4].substringAfter("pdf:")
                        )
                        if (File(book.filePath).exists())
                            library.add(book)
                    }
                }
            }
        }
        catch (e: Exception) { println(e) }
    }

    private fun saveLibraryMetadata()
    {
        val metadataFile = File(libraryDir, "library.json")
        try
        {
            val content = library.joinToString("\n") { book ->
                "title:${book.title}|count:${book.sentenceCount}|path:${book.filePath}|date:${book.addedDate}|pdf:${book.originalPdfName}"
            }
            metadataFile.writeText(content)
        }
        catch (e: Exception) { println(e) }
    }

    private fun showLibrary(primaryStage: Stage)
    {
        val dialog = Stage().apply {
            title = "Library"
            initModality(Modality.APPLICATION_MODAL)
            initOwner(primaryStage)
        }

        val tabPane = TabPane()

        // Books Tab
        val booksTab = Tab("Books").apply {
            isClosable = false
            content    = createBooksTab(dialog)
        }

        // Checkpoints Tab
        val checkpointsTab = Tab("Checkpoints").apply {
            isClosable = false
            content    = createCheckpointsTab(dialog)
        }

        tabPane.tabs.addAll(booksTab, checkpointsTab)

        val root = BorderPane().apply {
            center  = tabPane
            padding = Insets(10.0)
        }

        dialog.scene = Scene(root, 500.0, 550.0)
        dialog.show()
    }

    private fun createBooksTab(dialog: Stage): VBox
    {
        val listView = ListView(library).apply {
            setCellFactory {
                object : ListCell<Book>()
                {
                    override fun updateItem(item: Book?, empty: Boolean)
                    {
                        super.updateItem(item, empty)
                        if (empty || item == null)
                        {
                            text    = null
                            graphic = null
                        }
                        else
                        {
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
        }

        val loadButton = Button("Load Selected").apply {
            prefWidth = 120.0
            setOnAction {
                val selected = listView.selectionModel.selectedItem
                if (selected != null)
                {
                    loadBookFromLibrary(selected)
                    dialog.close()
                }
            }
        }

        val deleteButton = Button("Delete").apply {
            prefWidth = 120.0
            setOnAction {
                val selected = listView.selectionModel.selectedItem
                if (selected != null)
                    deleteBook(selected)
            }
        }

        val closeButton = Button("Close").apply {
            prefWidth = 120.0
            setOnAction { dialog.close() }
        }

        val buttonBar = HBox(10.0, loadButton, deleteButton, closeButton).apply {
            alignment = Pos.CENTER
            padding   = Insets(10.0)
        }

        return VBox().apply {
            children.addAll(listView, buttonBar)
            VBox.setVgrow(listView, javafx.scene.layout.Priority.ALWAYS)
        }
    }

    private fun createCheckpointsTab(dialog: Stage): VBox
    {
        val listView = ListView(checkpoints).apply {
            setCellFactory {
                object : ListCell<Checkpoint>()
                {
                    override fun updateItem(item: Checkpoint?, empty: Boolean)
                    {
                        super.updateItem(item, empty)
                        if (empty || item == null)
                        {
                            text    = null
                            graphic = null
                        }
                        else
                        {
                            val vbox = VBox(5.0).apply {
                                children.addAll(
                                    Label(item.bookTitle).apply {
                                        style = "-fx-font-weight: bold; -fx-font-size: 14px;"
                                    },
                                    Label("At sentence: ${item.sentenceIndex + 1}"),
                                    Label("Saved: ${item.savedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}")
                                )
                                padding = Insets(5.0)
                            }
                            graphic = vbox
                        }
                    }
                }
            }
            prefHeight = 400.0
        }

        val loadButton = Button("Load Checkpoint").apply {
            prefWidth = 130.0
            setOnAction {
                val selected = listView.selectionModel.selectedItem
                if (selected != null)
                {
                    loadCheckpoint(selected)
                    dialog.close()
                }
            }
        }

        val deleteButton = Button("Delete").apply {
            prefWidth = 120.0
            setOnAction {
                val selected = listView.selectionModel.selectedItem
                if (selected != null)
                {
                    checkpoints.remove(selected)
                    saveCheckpointsMetadata()
                }
            }
        }

        val closeButton = Button("Close").apply {
            prefWidth = 120.0
            setOnAction { dialog.close() }
        }

        val buttonBar = HBox(10.0, loadButton, deleteButton, closeButton).apply {
            alignment = Pos.CENTER
            padding   = Insets(10.0)
        }

        return VBox().apply {
            children.addAll(listView, buttonBar)
            VBox.setVgrow(listView, javafx.scene.layout.Priority.ALWAYS)
        }
    }

    private fun loadCheckpoint(checkpoint: Checkpoint)
    {
        try
        {
            val sentences = File(checkpoint.filePath).readLines()
            currentSentences = sentences
            currentIndex     = checkpoint.sentenceIndex
            currentBookTitle = checkpoint.bookTitle

            Platform.runLater {
                spokenSentences.clear()
                // Add previous sentences to history
                for (i in 0 until checkpoint.sentenceIndex) {
                    if (i < sentences.size) {
                        spokenSentences.add(sentences[i])
                    }
                }
                statusLabel.text          = "Loaded checkpoint: ${checkpoint.bookTitle} at sentence ${checkpoint.sentenceIndex + 1}"
                currentSentenceLabel.text = "Ready to resume from checkpoint"
                updateButtonStates()
            }
        }
        catch (e: Exception)
        {
            Platform.runLater {
                statusLabel.text = "Failed to load checkpoint"
            }
        }
    }

    private fun loadBookFromLibrary(book: Book)
    {
        try
        {
            val sentences = File(book.filePath).readLines()
            currentSentences = sentences
            currentIndex     = 0
            currentBookTitle = book.title

            Platform.runLater {
                spokenSentences.clear()
                statusLabel.text          = "Loaded: ${book.title} (${sentences.size} sentences)"
                currentSentenceLabel.text = "Ready to play"
                updateButtonStates()
            }
        }
        catch (e: Exception)
        {
            Platform.runLater {
                statusLabel.text = "Failed to load book"
            }
        }
    }

    private fun deleteBook(book: Book)
    {
        val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
            title       = "Delete Book"
            headerText  = "Delete \"${book.title}\"?"
            contentText = "This action cannot be undone."
        }

        val result = alert.showAndWait()
        if (result.isPresent && result.get() == ButtonType.OK)
            try
            {
                File(book.filePath).delete()
                library.remove(book)
                // Also remove associated checkpoints
                checkpoints.removeIf { it.bookTitle == book.title }
                saveLibraryMetadata()
                saveCheckpointsMetadata()
            }
            catch (e: Exception) { println(e) }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun speakSentences()
    {
        GlobalScope.launch(Dispatchers.IO) {
            Platform.runLater {
                statusLabel.text = "Playing..."
            }

            while (currentIndex < currentSentences.size && isPlaying)
            {
                val sentence = currentSentences[currentIndex]

                Platform.runLater {
                    currentSentenceLabel.text = sentence
                    spokenSentences.add(sentence)
                }

                // Speak the sentence (blocking call)
                ttsHelper.speak(sentence)

                // After sentence completes, check if stop was requested
                if (stopRequested)
                {
                    Platform.runLater {
                        currentSentenceLabel.text = "Stopped"
                        statusLabel.text = "Stopped"
                        isPlaying        = false
                        isStopped        = true
                        stopRequested    = false
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
            if (currentIndex >= currentSentences.size)
                Platform.runLater {
                    currentSentenceLabel.text = "Completed"
                    statusLabel.text = "Finished"
                    currentIndex     = 0  // Reset for replay
                    isPlaying        = false
                    isStopped        = true
                    stopRequested    = false
                    updateButtonStates()
                }
        }
    }
}

fun main()
{
    Application.launch(MainApp::class.java)
}
