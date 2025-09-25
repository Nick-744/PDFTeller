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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainApp : Application()
{
    private val ttsHelper = TextToSpeechHelper()
    private val spokenSentences: ObservableList<String> = FXCollections.observableArrayList()
    private lateinit var sentenceListView: ListView<String>
    private lateinit var currentSentenceLabel: Label
    private lateinit var statusLabel: Label
    @Volatile
    private var paused: Boolean = false
    private lateinit var pauseButton: Button

    override fun start(primaryStage: Stage)
    {
        sentenceListView = ListView(spokenSentences).apply {
            prefHeight = 300.0

            setCellFactory { // Wraps long text so sentences aren't cut off!
                object : javafx.scene.control.ListCell<String>() {
                    private val wrapLabel = Label().apply {
                        isWrapText = true
                        maxWidth   = Double.MAX_VALUE
                        alignment  = javafx.geometry.Pos.CENTER
                    }

                    private val container = HBox(wrapLabel).apply {
                        alignment = Pos.CENTER
                        spacing   = 8.0
                        padding   = javafx.geometry.Insets(
                            4.0, 10.0, 4.0, 10.0
                        )
                    }

                    init
                    {
                        // Label width follows the list cell width minus padding!
                        this.widthProperty().addListener { _, _, newWidth ->
                            wrapLabel.maxWidth = newWidth.toDouble() - 40.0
                        }
                    }

                    override fun updateItem(item: String?, empty: Boolean)
                    {
                        super.updateItem(item, empty)
                        if (empty || item == null)
                        {
                            graphic = null
                            text    = null
                        }
                        else
                        {
                            wrapLabel.text = item
                            graphic        = container
                        }
                    }
                }
            }
        }

        // Keep the list view auto-scrolled to the bottom when new sentences are added...
        spokenSentences.addListener { change: javafx.collections.ListChangeListener.Change<out String> ->
            while (change.next()) {
                if (change.wasAdded())
                    Platform.runLater {
                        if (spokenSentences.isNotEmpty())
                            sentenceListView.scrollTo(spokenSentences.size - 1)
                    }
            }
        }

        currentSentenceLabel = Label("No sentence playing...").apply {
            style      = "-fx-font-size: 16px; -fx-font-weight: bold;"
            isWrapText = true
            alignment  = Pos.CENTER
            maxWidth   = Double.MAX_VALUE
        }

        statusLabel = Label("Ready.")

        val loadButton = Button("Load PDF").apply {
            setOnAction { loadPDFFile(primaryStage) }
        }

        pauseButton = Button("Stop").apply {
            setOnAction { togglePauseResume() }
        }

        val controlBar = HBox(10.0, loadButton, pauseButton, statusLabel).apply {
            alignment = Pos.CENTER
        }

        val root = BorderPane().apply {
            top    = sentenceListView
            center = currentSentenceLabel
            bottom = controlBar
        }

        // When scene becomes available, bind the current sentence label width to the scene width
        // so it wraps and remains centered reliably during resizes.
        root.sceneProperty().addListener { _, _, newScene ->
            if (newScene != null) {
                currentSentenceLabel.maxWidthProperty().bind(newScene.widthProperty().subtract(40.0))
            }
        }

        val scene = Scene(root, 500.0, 650.0)
        primaryStage.title = "PDFTeller"
        primaryStage.scene = scene
        primaryStage.show()
    }

    private var currentIndex: Int = 0

    private fun togglePauseResume()
    {
        if (!::pauseButton.isInitialized) return

        if (!paused) {
            // request pause
            paused = true
            ttsHelper.stop()
            Platform.runLater {
                currentSentenceLabel.text = "<...>"
                statusLabel.text          = "Paused."
                pauseButton.text          = "Continue"
            }
        } else {
            // resume
            paused = false
            Platform.runLater {
                currentSentenceLabel.text = "Resuming..."
                statusLabel.text = "Resuming."
                pauseButton.text = "Stop"
            }
        }
    }

    private fun loadPDFFile(stage: Stage)
    {
        val fileChooser   = FileChooser()
        fileChooser.title = "Select a PDF document"
        fileChooser.extensionFilters.add(
            FileChooser.ExtensionFilter("PDF documents", "*.pdf")
        )

        val selectedFile = fileChooser.showOpenDialog(stage)
        if (selectedFile != null)
            loadAndProcessFile(selectedFile)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadAndProcessFile(file: File)
    {
        // reset pause flag when starting a new file
        paused = false
        Platform.runLater {
            if (::pauseButton.isInitialized) pauseButton.text = "Stop"
            statusLabel.text = "Processing PDF..."
        }
        GlobalScope.launch(Dispatchers.IO)
        {
            val sentences = processPdfTextWithStructure(file)

            Platform.runLater {
                spokenSentences.clear()
                currentSentenceLabel.text = "Starting playback..."
            }

            currentIndex = 0
            while (currentIndex < sentences.size)
            {
                // if paused, wait until resumed
                while (paused)
                    delay(100)

                val sentence = sentences[currentIndex]

                Platform.runLater {
                    currentSentenceLabel.text = sentence
                    spokenSentences.add(sentence)
                }

                ttsHelper.speak(sentence) // blocks until TTS finishes or stopped
                currentIndex++
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
