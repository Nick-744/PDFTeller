import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import java.time.format.DateTimeFormatter

class LibraryUI(private val libraryManager: LibraryManager)
{
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
        val listView = ListView(libraryManager.library).apply {
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
                    val sentences = libraryManager.loadBookFromLibrary(selected)
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
                        libraryManager.deleteBook(selected)
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
        val listView = ListView(libraryManager.checkpoints).apply {
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
                    val sentences = libraryManager.loadCheckpoint(selected)
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
                    libraryManager.deleteCheckpoint(selected)
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
