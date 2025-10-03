import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
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
}
