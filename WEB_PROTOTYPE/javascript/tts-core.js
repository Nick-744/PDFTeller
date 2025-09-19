// TTS core - contains TTSController class only (no DOM instantiation)
class TTSController {
    constructor() {
        this.textBlocks     = []
        this.currentIndex   = 0
        this.utterance      = null
        this.isPlaying      = false
        this.isDyslexicMode = false

        // Elements will be injected by page-specific initializer
        this.elements = {}
    }

    // Page should call this to connect DOM elements
    attachElements(el) {
        this.elements       = el
        this.textDisplay    = el.textDisplay
        this.currentBlockEl = el.currentBlockEl
        this.totalBlocksEl  = el.totalBlocksEl
        this.playBtn        = el.playBtn
        this.stopBtn        = el.stopBtn
        this.prevBtn        = el.prevBtn
        this.nextBtn        = el.nextBtn
        this.speedRange     = el.speedRange
        this.volumeRange    = el.volumeRange
        this.voiceSelect    = el.voiceSelect
        this.progressFill   = el.progressFill
        this.pdfName        = el.pdfName
        this.pdfStats       = el.pdfStats
        this.loadingOverlay = el.loadingOverlay
        this.errorOverlay   = el.errorOverlay
        this.dyslexiaToggle = el.dyslexiaToggle

        // Setup handlers that depend on elements
        this.setupEventListeners()
        this.loadVoices()
        this.setupDyslexiaToggle()
    }

    setupDyslexiaToggle() {
        // Check for saved preference
        const savedMode = localStorage.getItem('dyslexicMode')
        if (savedMode === 'true') {
            this.enableDyslexicMode()
        }

        // Toggle button event
        if (this.dyslexiaToggle) {
            this.dyslexiaToggle.onclick = () => {
                this.isDyslexicMode = !this.isDyslexicMode
                if (this.isDyslexicMode) {
                    this.enableDyslexicMode()
                }
                else {
                    this.disableDyslexicMode()
                }
                // Save preference
                localStorage.setItem('dyslexicMode', this.isDyslexicMode.toString())
            }
        }
    }

    enableDyslexicMode() {
        document.body.classList.add('dyslexic-mode')
        if (this.dyslexiaToggle) this.dyslexiaToggle.innerHTML = 'Standard Theme'
        this.isDyslexicMode = true
        // Ensure animations are enabled in dyslexic mode
        document.documentElement.classList.remove('no-animations')
    }

    disableDyslexicMode() {
        document.body.classList.remove('dyslexic-mode')
        if (this.dyslexiaToggle) this.dyslexiaToggle.innerHTML = 'Dyslexic Friendly Theme'
        this.isDyslexicMode = false
        // When switching to standard theme, remove animations for a calmer experience
        document.documentElement.classList.add('no-animations')
    }

    loadPDFData() {
        try {
            const data = sessionStorage.getItem('pdfProcessedData')
            if (!data) {
                this.showError()

                return;
            }

            const pdfData   = JSON.parse(data)
            this.textBlocks = pdfData.textBlocks || []

            if (this.textBlocks.length === 0) {
                this.showError()

                return;
            }

            // Update UI
            if (this.pdfName)
                this.pdfName.textContent       = pdfData.filename || 'PDF Document'
            if (this.pdfStats)
                this.pdfStats.textContent      = `${this.textBlocks.length} text blocks`
            if (this.totalBlocksEl)
                this.totalBlocksEl.textContent = this.textBlocks.length

            this.updateDisplay()
            this.hideLoading()

        } catch (error) {
            console.error('Error loading PDF:', error)
            this.showError()
        }
    }

    setupEventListeners() {
        if (!this.playBtn) return
        // Playback controls
        this.playBtn.onclick = () => this.play()
        this.stopBtn.onclick = () => this.stop()
        this.prevBtn.onclick = () => this.previous()
        this.nextBtn.onclick = () => this.next()

        // Settings with smooth value updates
        this.speedRange.oninput = (e) => {
            const value = parseFloat(e.target.value).toFixed(1)
            const el    = document.getElementById('speedValue')
            if (el) el.textContent = `${value}x`
        }

        this.volumeRange.oninput = (e) => {
            const value = Math.round(e.target.value * 100)
            const el    = document.getElementById('volumeValue')
            if (el) el.textContent = `${value}%`
        }

        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            if (e.target.tagName === 'SELECT') return;

            switch(e.code) {
                case 'Space':
                    e.preventDefault()
                    this.isPlaying ? this.stop() : this.play()
                    break;
                case 'ArrowLeft':
                    e.preventDefault()
                    this.previous()
                    break;
                case 'ArrowRight':
                    e.preventDefault()
                    this.next()
                    break;
                case 'Escape':
                    this.stop()
                    break;
            }
        })
    }

    loadVoices() {
        const loadVoiceOptions = () => {
            const voices = speechSynthesis.getVoices()
            if (!this.voiceSelect) return;
            this.voiceSelect.innerHTML = ''

            if (voices.length === 0) {
                this.voiceSelect.innerHTML = '<option>No voices available</option>'
                
                return;
            }

            // Group voices by language
            const grouped = {}
            voices.forEach((voice, i) => {
                const lang = (voice.lang || '').split('-')[0].toUpperCase()
                if (!grouped[lang]) grouped[lang] = []
                grouped[lang].push({ voice, index: i })
            })

            // Add English voices first if available
            if (grouped['EN']) {
                const optgroup = document.createElement('optgroup')
                optgroup.label = 'English'
                grouped['EN'].forEach(({ voice, index }) => {
                    const option       = document.createElement('option')
                    option.value       = index
                    option.textContent = voice.name
                    if (voice.default) option.selected = true
                    optgroup.appendChild(option)
                })
                this.voiceSelect.appendChild(optgroup)
                delete grouped['EN']
            }

            // Add other languages
            Object.keys(grouped).sort().forEach(lang => {
                const optgroup = document.createElement('optgroup')
                optgroup.label = lang
                grouped[lang].forEach(({ voice, index }) => {
                    const option       = document.createElement('option')
                    option.value       = index
                    option.textContent = voice.name
                    optgroup.appendChild(option)
                })
                this.voiceSelect.appendChild(optgroup)
            })
        }

        loadVoiceOptions()
        speechSynthesis.onvoiceschanged = loadVoiceOptions
    }

    play() {
        if (this.textBlocks.length === 0) return;

        // If already playing, do nothing
        if (this.isPlaying) return;

        this.speak()
    }

    stop() {
        // Stop speech synthesis at current sentence
        speechSynthesis.cancel()
        this.isPlaying = false
        this.updateButtons()
        if (this.textDisplay) this.textDisplay.classList.remove('speaking')
    }

    previous() {
        if (this.currentIndex > 0) {
            // Stop current speech if playing
            if (this.isPlaying) {
                speechSynthesis.cancel()
            }

            this.currentIndex--
            this.updateDisplay()

            // Resume playing if it was playing
            if (this.isPlaying) {
                this.speak()
            }
        }
    }

    next() {
        if (this.currentIndex < this.textBlocks.length - 1) {
            // Stop current speech if playing
            if (this.isPlaying) {
                speechSynthesis.cancel()
            }

            this.currentIndex++
            this.updateDisplay()

            // Resume playing if it was playing
            if (this.isPlaying) {
                this.speak()
            }
        }
    }

    speak() {
        const text = this.textBlocks[this.currentIndex]
        if (!text || text.trim() === '') {
            this.next()

            return;
        }

        const utterance  = new SpeechSynthesisUtterance(text)
        utterance.rate   = parseFloat(this.speedRange.value)
        utterance.pitch  = 1.0 // Fixed pitch
        utterance.volume = parseFloat(this.volumeRange.value)

        const voices = speechSynthesis.getVoices()
        if (voices[this.voiceSelect.value]) {
            utterance.voice = voices[this.voiceSelect.value]
        }

        utterance.onstart = () => {
            this.isPlaying = true
            this.updateButtons()
            if (this.textDisplay) this.textDisplay.classList.add('speaking')
        }

        utterance.onend = () => {
            if (this.textDisplay) this.textDisplay.classList.remove('speaking')
            if (this.currentIndex < this.textBlocks.length - 1) {
                this.currentIndex++
                this.updateDisplay()
                setTimeout(() => this.speak(), 200) // Small pause between blocks
            }
            else {
                this.stop()
                this.showCompletionMessage()
            }
        }

        utterance.onerror = (event) => {
            console.error('Speech error:', event)
            this.stop()
        }

        speechSynthesis.speak(utterance)
    }

    updateDisplay() {
        if (this.textDisplay)
            this.textDisplay.textContent    = this.textBlocks[this.currentIndex] || 'No text available'
        if (this.currentBlockEl)
            this.currentBlockEl.textContent = this.currentIndex + 1
        this.updateProgress()
        this.updateButtons()
    }

    updateProgress() {
        const progress = ((this.currentIndex + 1) / this.textBlocks.length) * 100
        if (this.progressFill) this.progressFill.style.width = `${progress}%`
    }

    updateButtons() {
        // Play button is disabled when playing
        if (this.playBtn) this.playBtn.disabled = this.isPlaying

        // Stop button is enabled when playing
        if (this.stopBtn) this.stopBtn.disabled = !this.isPlaying

        // Navigation buttons
        if (this.prevBtn) this.prevBtn.disabled = this.currentIndex === 0
        if (this.nextBtn) this.nextBtn.disabled = this.currentIndex === this.textBlocks.length - 1
    }

    showCompletionMessage() {
        if (!this.textDisplay) return
        const originalText = this.textDisplay.textContent
        this.textDisplay.innerHTML = `
            <div style="text-align: center; animation: fadeIn 0.5s ease;">
                <h3 style="color: #28a745; margin-bottom: 10px;">Reading Complete!</h3>
                <p style="color: #6c757d;">Finished all ${this.textBlocks.length} text blocks</p>
                <button id="readAgainBtn" 
                        style="margin-top: 15px; padding: 10px 20px; 
                               background: linear-gradient(135deg, #28a745, #20c997);
                               color: white; border: none; border-radius: 8px;
                               cursor: pointer; font-weight: 600;">
                    Read Again
                </button>
            </div>
        `

        // Attach event to the read again button
        const readAgainBtn = document.getElementById('readAgainBtn')
        if (readAgainBtn) {
            readAgainBtn.addEventListener('click', () => {
                this.currentIndex = 0
                this.updateDisplay()
                this.play()
            })
        }

        setTimeout(() => {
            if (this.textDisplay) this.textDisplay.textContent = originalText
        }, 5000)
    }

    showError() {
        if (this.loadingOverlay) this.loadingOverlay.classList.add('hidden')
        if (this.errorOverlay) this.errorOverlay.classList.remove('hidden')
    }

    hideLoading() {
        if (this.loadingOverlay) this.loadingOverlay.classList.add('hidden')
    }
}

// Export the class to window so page code can use it
if (typeof window !== 'undefined') window.TTSController = TTSController
