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
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Models
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

class LibraryManager
{
    // Library collections
    val library:     ObservableList<Book>       = FXCollections.observableArrayList()
    val checkpoints: ObservableList<Checkpoint> = FXCollections.observableArrayList()
    
    private val libraryDir = File(System.getProperty("user.home"), ".pdfteller_library")

    init
    {
        if (!libraryDir.exists())
            libraryDir.mkdirs() // Create library directory if it doesn't exist...

        loadLibrary()
        loadCheckpoints()
    }

    fun saveBookToLibrary(title: String, sentences: List<String>, originalPdfName: String): Boolean
    {
        // Check if book already exists (same title and sentence count)
        val existingBook = library.find {
            it.title == title && it.sentenceCount == sentences.size
        }

        if (existingBook != null)
            return false // Book already exists, don't save again

        // Create unique filename
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName  = "${title}_${timestamp}.txt"
        val filePath  = File(libraryDir, fileName).absolutePath

        // Save sentences to file
        return try
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
            true
        }
        catch (e: Exception)
        {
            println(e)
            false
        }
    }

    fun saveCheckpoint(bookTitle: String, sentenceIndex: Int, filePath: String): Boolean
    {
        return try
        {
            val checkpoint = Checkpoint(
                bookTitle     = bookTitle,
                sentenceIndex = sentenceIndex,
                savedDate     = LocalDateTime.now(),
                filePath      = filePath
            )

            // Remove old checkpoints for the same book
            checkpoints.removeIf { it.bookTitle == bookTitle }
            checkpoints.add(checkpoint)
            saveCheckpointsMetadata()
            true
        }
        catch (e: Exception)
        {
            println(e)
            false
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
                    if (parts.size >= 4)
                    {
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

    fun loadBookFromLibrary(book: Book): List<String>?
    {
        return try
        {
            File(book.filePath).readLines()
        }
        catch (e: Exception)
        {
            println(e)
            null
        }
    }

    fun loadCheckpoint(checkpoint: Checkpoint): List<String>?
    {
        return try
        {
            File(checkpoint.filePath).readLines()
        }
        catch (e: Exception)
        {
            println(e)
            null
        }
    }

    fun deleteBook(book: Book): Boolean
    {
        return try
        {
            File(book.filePath).delete()
            library.remove(book)

            // Also remove associated checkpoints
            checkpoints.removeIf { it.bookTitle == book.title }

            saveLibraryMetadata()
            saveCheckpointsMetadata()
            
            true
        }
        catch (e: Exception)
        {
            println(e)
            false
        }
    }

    fun deleteCheckpoint(checkpoint: Checkpoint): Boolean
    {
        return try
        {
            checkpoints.remove(checkpoint)
            saveCheckpointsMetadata()
            true
        }
        catch (e: Exception)
        {
            println(e)
            false
        }
    }

    fun showLibrary(primaryStage: Stage, onBookLoaded: (Book, List<String>) -> Unit, onCheckpointLoaded: (Checkpoint, List<String>) -> Unit)
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
            content    = createBooksTab(dialog, onBookLoaded)
        }

        // Checkpoints Tab
        val checkpointsTab = Tab("Checkpoints").apply {
            isClosable = false
            content    = createCheckpointsTab(dialog, onCheckpointLoaded)
        }

        tabPane.tabs.addAll(booksTab, checkpointsTab)

        val root = BorderPane().apply {
            center  = tabPane
            padding = Insets(10.0)
        }

        dialog.scene = Scene(root, 500.0, 550.0)
        dialog.show()
    }

    private fun createBooksTab(dialog: Stage, onBookLoaded: (Book, List<String>) -> Unit): VBox
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
                    val sentences = loadBookFromLibrary(selected)
                    if (sentences != null)
                    {
                        onBookLoaded(selected, sentences)
                        dialog.close()
                    }
                }
            }
        }

        val deleteButton = Button("Delete").apply {
            prefWidth = 120.0
            setOnAction {
                val selected = listView.selectionModel.selectedItem
                if (selected != null)
                {
                    val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
                        title       = "Delete Book"
                        headerText  = "Delete \"${selected.title}\"?"
                        contentText = "This action cannot be undone."
                    }

                    val result = alert.showAndWait()
                    if (result.isPresent && result.get() == ButtonType.OK)
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
            padding   = Insets(10.0)
        }

        return VBox().apply {
            children.addAll(listView, buttonBar)
            VBox.setVgrow(listView, javafx.scene.layout.Priority.ALWAYS)
        }
    }

    private fun createCheckpointsTab(dialog: Stage, onCheckpointLoaded: (Checkpoint, List<String>) -> Unit): VBox
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
                    val sentences = loadCheckpoint(selected)
                    if (sentences != null)
                    {
                        onCheckpointLoaded(selected, sentences)
                        dialog.close()
                    }
                }
            }
        }

        val deleteButton = Button("Delete").apply {
            prefWidth = 120.0
            setOnAction {
                val selected = listView.selectionModel.selectedItem
                if (selected != null)
                    deleteCheckpoint(selected)
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
}
