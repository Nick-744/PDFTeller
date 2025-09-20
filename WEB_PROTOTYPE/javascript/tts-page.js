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

            // Add-to-library and save-bookmark button logic (moved from inline HTML)
            const addBtn = document.getElementById('addToLibraryBtn')
            const bmBtn = document.getElementById('saveBookmarkBtn')

            function updateButtons() {
                const has = !!sessionStorage.getItem('pdfProcessedData')
                if (addBtn) addBtn.style.display = has ? 'inline-block' : 'none'
                if (bmBtn) bmBtn.style.display = has ? 'inline-block' : 'none'
            }

            updateButtons()

            if (addBtn) addBtn.addEventListener('click', () => {
                if (window.libraryAPI && window.libraryAPI.saveCurrentToLibrary) {
                    window.libraryAPI.saveCurrentToLibrary()
                } else {
                    alert('Library unavailable')
                }
            })

            if (bmBtn) bmBtn.addEventListener('click', () => {
                if (!window.libraryAPI || !window.libraryAPI.saveBookmarkForCurrentBook) {
                    alert('Library unavailable')
                    return
                }

                const idxEl = document.getElementById('currentBlock')
                const idx = idxEl ? parseInt(idxEl.textContent, 10) - 1 : 0

                const defaultName = `Block ${idx + 1}`
                const name = window.prompt('Enter a name for this bookmark', defaultName)
                if (name === null) return

                const res = window.libraryAPI.saveBookmarkForCurrentBook(idx, name)

                if (res && res.success) {
                    if (confirm('Bookmark saved. Open library to view?')) {
                        window.location.href = 'library.html'
                    }
                } else if (res && res.reason === 'not-saved') {
                    if (confirm('This book is not saved to the library yet. Save now and add bookmark?')) {
                        const saved = window.libraryAPI.saveCurrentToLibrary && window.libraryAPI.saveCurrentToLibrary()
                        if (saved) {
                            const retry = window.libraryAPI.saveBookmarkForCurrentBook(idx, name)
                            if (retry && retry.success) alert('Bookmark saved')
                            else alert('Unable to save bookmark')
                        } else {
                            alert('Unable to save book to library')
                        }
                    }
                } else {
                    alert('Unable to save bookmark')
                }
            })

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
