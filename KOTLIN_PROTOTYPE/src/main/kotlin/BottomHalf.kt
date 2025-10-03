import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage

class BottomHalf(
    private val onLoadPDF:     () -> Unit,
    private val onShowLibrary: () -> Unit
)
{
    // UI Components
    val sentenceListView:    ListView<String>
    val statusLabel:         Label
    val bottomHalfContainer: VBox
    val spokenSentences:     ObservableList<String> = FXCollections.observableArrayList()

    init
    {
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
                        maxWidth   = Double.MAX_VALUE
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
            setOnAction { onLoadPDF() }
        }

        val libraryButton = Button("📚 Library").apply {
            setOnAction { onShowLibrary() }
        }

        val bottomBar = HBox(10.0, loadButton, libraryButton, statusLabel).apply {
            alignment = Pos.CENTER
            padding   = Insets(10.0)
        }

        // Bottom half container
        bottomHalfContainer = VBox().apply {
            children.addAll(sentenceListView, bottomBar)
        }
    }

    fun bindHeightToStage(primaryStage: Stage)
    {
        bottomHalfContainer.prefHeightProperty().bind(primaryStage.heightProperty().divide(2))
    }

    fun updateStatus(text: String)
    {
        Platform.runLater {
            statusLabel.text = text
        }
    }

    fun clearSentences()
    {
        Platform.runLater {
            spokenSentences.clear()
        }
    }

    fun addSentence(sentence: String)
    {
        Platform.runLater {
            spokenSentences.add(sentence)
        }
    }

    fun addSentences(sentences: List<String>)
    {
        Platform.runLater {
            spokenSentences.addAll(sentences)
        }
    }
}
