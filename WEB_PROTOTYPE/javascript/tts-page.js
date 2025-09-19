// Page-specific wiring for TTS (text2speech-main.html)
(function () {
    // Wait for DOM
    document.addEventListener('DOMContentLoaded', () => {
        // Collect elements used by TTSController
        const elements = {
            textDisplay:    document.getElementById('textDisplay'),
            currentBlockEl: document.getElementById('currentBlock'),
            totalBlocksEl:  document.getElementById('totalBlocks'),
            playBtn:        document.getElementById('playBtn'),
            stopBtn:        document.getElementById('stopBtn'),
            prevBtn:        document.getElementById('prevBtn'),
            nextBtn:        document.getElementById('nextBtn'),
            speedRange:     document.getElementById('speedRange'),
            volumeRange:    document.getElementById('volumeRange'),
            voiceSelect:    document.getElementById('voiceSelect'),
            progressFill:   document.getElementById('progressFill'),
            pdfName:        document.getElementById('pdfName'),
            pdfStats:       document.getElementById('pdfStats'),
            loadingOverlay: document.getElementById('loadingOverlay'),
            errorOverlay:   document.getElementById('errorOverlay'),
            dyslexiaToggle: document.getElementById('dyslexiaToggle')
        }

        // Instantiate shared controller class
        const controller = new window.TTSController()
        controller.attachElements(elements)

        // load pdf data after wiring
        controller.loadPDFData()

        // If a start index was provided (e.g., opened from Library bookmark), jump there
        const startIndexRaw = sessionStorage.getItem('pdfStartIndex')
        if (startIndexRaw !== null) {
            const idx = parseInt(startIndexRaw, 10)
            if (!Number.isNaN(idx) && idx >= 0 && idx < (controller.textBlocks || []).length) {
                controller.currentIndex = idx
                controller.updateDisplay()
                // remove it so subsequent opens don't jump
                sessionStorage.removeItem('pdfStartIndex')
            }
        }

        // Expose on window for console/debugging
        window.tts = controller
    })
})()
