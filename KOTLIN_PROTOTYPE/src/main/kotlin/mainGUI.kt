import javafx.application.Application
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class MainApp : Application()
{
    private val ttsHelper = TextToSpeechHelper()
    private val spokenSentences:               ObservableList<String> = FXCollections.observableArrayList()
    private lateinit var sentenceListView:     ListView<String>
    private lateinit var currentSentenceLabel: Label
    private lateinit var statusLabel:          Label

    override fun start(primaryStage: Stage)
    {
        sentenceListView = ListView(spokenSentences).apply {
            prefHeight = 200.0
        }

        currentSentenceLabel = Label("No sentence playing...").apply {
            style = "-fx-font-size: 16px; -fx-font-weight: bold;"
        }

        statusLabel = Label("Ready.")

        val loadButton = Button("Load PDF").apply {
            setOnAction { loadPDFFile(primaryStage) }
        }

        val controlBar = HBox(10.0, loadButton).apply {
            alignment = Pos.CENTER
        }

        val root = BorderPane().apply {
            top    = sentenceListView
            center = currentSentenceLabel
            bottom = controlBar
        }

        val scene = Scene(root, 600.0, 400.0)
        primaryStage.title = "PDFTeller"
        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun loadPDFFile(stage: Stage)
    {
        val fileChooser   = FileChooser()
        fileChooser.title = "Select a PDF File"
        fileChooser.extensionFilters.add(
            FileChooser.ExtensionFilter("PDF Documents", "*.pdf")
        )

        val selectedFile = fileChooser.showOpenDialog(stage)
        if (selectedFile != null)
            if (selectedFile.extension.lowercase() == "pdf")
                loadAndProcessFile(selectedFile)
            else
                statusLabel.text = "Not a PDF document..."
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadAndProcessFile(file: File)
    {
        statusLabel.text = "Processing PDF..."
        GlobalScope.launch(Dispatchers.IO)
        {
            val sentences = processPdfTextWithStructure(file)

            Platform.runLater {
                spokenSentences.clear()
                currentSentenceLabel.text = "Starting playback..."
            }

            for (sentence in sentences)
            {
                Platform.runLater {
                    currentSentenceLabel.text = sentence
                    spokenSentences.add(sentence)
                }

                ttsHelper.speak(sentence) // blocks until TTS finishes
            }

            Platform.runLater {
                currentSentenceLabel.text = "All sentences spoken."
                statusLabel.text          = "Done."
            }
        }
    }
}

fun main()
{
    Application.launch(MainApp::class.java)
}
