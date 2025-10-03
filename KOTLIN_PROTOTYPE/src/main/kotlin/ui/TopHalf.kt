package ui

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ToggleButton
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage

class TopHalf(
    private val onPlay: () -> Unit,
    private val onStop: () -> Unit,
    private val onSaveCheckpoint: () -> Unit,
    private val onToggleDyslexia: (Boolean) -> Unit
)
{
    // UI Components
    val currentSentenceLabel: Label
    val playButton:           Button
    val stopButton:           Button
    val checkpointButton:     Button
    val dyslexiaToggle:       ToggleButton
    val topHalfContainer:     VBox
    
    private var baseSentenceStyle: String = ""
    private var baseTopHalfStyle:  String = ""

    init
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
            VBox.setVgrow(this, Priority.ALWAYS)
        }

        // Play/Stop/Checkpoint control buttons
        playButton = Button("▶ Play").apply {
            prefWidth = 100.0
            setOnAction { onPlay() }
        }

        stopButton = Button("■ Stop").apply {
            prefWidth = 100.0
            isDisable = true
            setOnAction { onStop() }
        }

        checkpointButton = Button("🔖 Save Checkpoint").apply {
            prefWidth = 150.0
            isDisable = true
            setOnAction { onSaveCheckpoint() }
        }

        // Dyslexia-friendly toggle
        dyslexiaToggle = ToggleButton("Dyslexia-friendly").apply {
            prefWidth = 160.0
            isDisable = false
            setOnAction {
                onToggleDyslexia(isSelected)
            }
        }

        val controlButtons = HBox(10.0, playButton, stopButton, checkpointButton, dyslexiaToggle).apply {
            alignment = Pos.CENTER
            padding   = Insets(10.0, 10.0, 20.0, 10.0)
        }

        // Top half container
        topHalfContainer = VBox().apply {
            children.addAll(currentSentenceContainer, controlButtons)
            style            = "-fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;"
            baseTopHalfStyle = style // Save base style for restoring later...
        }
    }

    fun bindHeightToStage(primaryStage: Stage)
    {
        topHalfContainer.prefHeightProperty().bind(primaryStage.heightProperty().divide(2))
    }

    fun toggleDyslexiaMode(enabled: Boolean)
    {
        Platform.runLater {
            if (enabled)
            {
                topHalfContainer.style     = "$baseTopHalfStyle -fx-background-color: #1d0f0f;"
                currentSentenceLabel.style = "$baseSentenceStyle -fx-text-fill: #a08060; -fx-font-family: 'OpenDyslexic3';"
            }
            else
            {
                // Restore base styles
                topHalfContainer.style     = baseTopHalfStyle
                currentSentenceLabel.style = baseSentenceStyle
            }
        }
    }

    fun updateButtonStates(isPlaying: Boolean, currentSentences: List<String>, currentBookTitle: String?)
    {
        Platform.runLater {
            playButton.isDisable       = isPlaying || currentSentences.isEmpty()
            stopButton.isDisable       = !isPlaying
            checkpointButton.isDisable = currentSentences.isEmpty() || currentBookTitle == null
        }
    }

    fun updateCurrentSentence(text: String)
    {
        Platform.runLater {
            currentSentenceLabel.text = text
        }
    }
}
