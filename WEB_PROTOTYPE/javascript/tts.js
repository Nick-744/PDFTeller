// TTS Controller - Text-to-Speech functionality
class TTSController {
    constructor() {
        this.textBlocks = []
        this.currentIndex = 0
        this.utterance = null
        this.isPlaying = false
        this.isDyslexicMode = false
        
        this.initElements()
        this.loadPDFData()
        this.setupEventListeners()
        this.loadVoices()
        this.setupDyslexiaToggle()
    }

    initElements() {
        // Text display
        this.textDisplay = document.getElementById('textDisplay')
        this.currentBlockEl = document.getElementById('currentBlock')
        this.totalBlocksEl = document.getElementById('totalBlocks')
        
        // Controls
        this.playBtn = document.getElementById('playBtn')
        this.stopBtn = document.getElementById('stopBtn')
        this.prevBtn = document.getElementById('prevBtn')
        this.nextBtn = document.getElementById('nextBtn')
        
        // Settings
        this.speedRange = document.getElementById('speedRange')
        this.volumeRange = document.getElementById('volumeRange')
        this.voiceSelect = document.getElementById('voiceSelect')
        
        // Progress
        this.progressFill = document.getElementById('progressFill')
        
        // PDF info
        this.pdfName = document.getElementById('pdfName')
        this.pdfStats = document.getElementById('pdfStats')
        
        // Overlays
        this.loadingOverlay = document.getElementById('loadingOverlay')
        this.errorOverlay = document.getElementById('errorOverlay')
        
        // Dyslexia toggle
        this.dyslexiaToggle = document.getElementById('dyslexiaToggle')
    }

    setupDyslexiaToggle() {
        // Check for saved preference
        const savedMode = localStorage.getItem('dyslexicMode')
        if (savedMode === 'true') {
            this.enableDyslexicMode()
        }
        
        // Toggle button event
        this.dyslexiaToggle.onclick = () => {
            this.isDyslexicMode = !this.isDyslexicMode
            if (this.isDyslexicMode) {
                this.enableDyslexicMode()
            } else {
                this.disableDyslexicMode()
            }
            // Save preference
            localStorage.setItem('dyslexicMode', this.isDyslexicMode.toString())
        }
    }

    enableDyslexicMode() {
        document.body.classList.add('dyslexic-mode')
        this.dyslexiaToggle.innerHTML = '👁️ Standard Mode'
        this.isDyslexicMode = true
    }

    disableDyslexicMode() {
        document.body.classList.remove('dyslexic-mode')
        this.dyslexiaToggle.innerHTML = '👁️ Dyslexic Friendly'
        this.isDyslexicMode = false
        
        // Reset to default speed
        this.speedRange.value = '1.2'
        document.getElementById('speedValue').textContent = '1.2x'
    }

    loadPDFData() {
        try {
            const data = sessionStorage.getItem('pdfProcessedData')
            if (!data) {
                this.showError()
                return
            }
            
            const pdfData = JSON.parse(data)
            this.textBlocks = pdfData.textBlocks || []
            
            if (this.textBlocks.length === 0) {
                this.showError()
                return
            }
            
            // Update UI
            this.pdfName.textContent = pdfData.filename || 'PDF Document'
            this.pdfStats.textContent = `${this.textBlocks.length} text blocks`
            this.totalBlocksEl.textContent = this.textBlocks.length
            
            this.updateDisplay()
            this.hideLoading()
            
        } catch (error) {
            console.error('Error loading PDF:', error)
            this.showError()
        }
    }

    setupEventListeners() {
        // Playback controls
        this.playBtn.onclick = () => this.play()
        this.stopBtn.onclick = () => this.stop()
        this.prevBtn.onclick = () => this.previous()
        this.nextBtn.onclick = () => this.next()
        
        // Settings with smooth value updates
        this.speedRange.oninput = (e) => {
            const value = parseFloat(e.target.value).toFixed(1)
            document.getElementById('speedValue').textContent = `${value}x`
        }
        
        this.volumeRange.oninput = (e) => {
            const value = Math.round(e.target.value * 100)
            document.getElementById('volumeValue').textContent = `${value}%`
        }
        
        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            if (e.target.tagName === 'SELECT') return
            
            switch(e.code) {
                case 'Space':
                    e.preventDefault()
                    this.isPlaying ? this.stop() : this.play()
                    break
                case 'ArrowLeft':
                    e.preventDefault()
                    this.previous()
                    break
                case 'ArrowRight':
                    e.preventDefault()
                    this.next()
                    break
                case 'Escape':
                    this.stop()
                    break
            }
        })
    }

    loadVoices() {
        const loadVoiceOptions = () => {
            const voices = speechSynthesis.getVoices()
            this.voiceSelect.innerHTML = ''
            
            if (voices.length === 0) {
                this.voiceSelect.innerHTML = '<option>No voices available</option>'
                return
            }
            
            // Group voices by language
            const grouped = {}
            voices.forEach((voice, i) => {
                const lang = voice.lang.split('-')[0].toUpperCase()
                if (!grouped[lang]) grouped[lang] = []
                grouped[lang].push({ voice, index: i })
            })
            
            // Add English voices first if available
            if (grouped['EN']) {
                const optgroup = document.createElement('optgroup')
                optgroup.label = 'English'
                grouped['EN'].forEach(({ voice, index }) => {
                    const option = document.createElement('option')
                    option.value = index
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
                    const option = document.createElement('option')
                    option.value = index
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
        if (this.textBlocks.length === 0) return
        
        // If already playing, do nothing
        if (this.isPlaying) return
        
        this.speak()
    }

    stop() {
        // Stop speech synthesis at current sentence
        speechSynthesis.cancel()
        this.isPlaying = false
        this.updateButtons()
        this.textDisplay.classList.remove('speaking')
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
            return
        }
        
        const utterance = new SpeechSynthesisUtterance(text)
        utterance.rate = parseFloat(this.speedRange.value)
        utterance.pitch = 1.0  // Fixed pitch
        utterance.volume = parseFloat(this.volumeRange.value)
        
        const voices = speechSynthesis.getVoices()
        if (voices[this.voiceSelect.value]) {
            utterance.voice = voices[this.voiceSelect.value]
        }
        
        utterance.onstart = () => {
            this.isPlaying = true
            this.updateButtons()
            this.textDisplay.classList.add('speaking')
        }
        
        utterance.onend = () => {
            this.textDisplay.classList.remove('speaking')
            if (this.currentIndex < this.textBlocks.length - 1) {
                this.currentIndex++
                this.updateDisplay()
                setTimeout(() => this.speak(), 300) // Small pause between blocks
            } else {
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
        this.textDisplay.textContent = this.textBlocks[this.currentIndex] || 
            'No text available'
        this.currentBlockEl.textContent = this.currentIndex + 1
        this.updateProgress()
        this.updateButtons()
    }

    updateProgress() {
        const progress = ((this.currentIndex + 1) / this.textBlocks.length) * 100
        this.progressFill.style.width = `${progress}%`
    }

    updateButtons() {
        // Play button is disabled when playing
        this.playBtn.disabled = this.isPlaying
        
        // Stop button is enabled when playing
        this.stopBtn.disabled = !this.isPlaying
        
        // Navigation buttons
        this.prevBtn.disabled = this.currentIndex === 0
        this.nextBtn.disabled = this.currentIndex === this.textBlocks.length - 1
    }

    showCompletionMessage() {
        const originalText = this.textDisplay.textContent
        this.textDisplay.innerHTML = `
            <div style="text-align: center; animation: fadeIn 0.5s ease;">
                <h3 style="color: #28a745; margin-bottom: 10px;">🎉 Reading Complete!</h3>
                <p style="color: #6c757d;">Finished all ${this.textBlocks.length} text blocks</p>
                <button onclick="tts.currentIndex=0; tts.updateDisplay(); tts.play()" 
                        style="margin-top: 15px; padding: 10px 20px; 
                               background: linear-gradient(135deg, #28a745, #20c997);
                               color: white; border: none; border-radius: 8px;
                               cursor: pointer; font-weight: 600;">
                    🔄 Read Again
                </button>
            </div>
        `
        
        setTimeout(() => {
            this.textDisplay.textContent = originalText
        }, 5000)
    }

    showError() {
        this.loadingOverlay.classList.add('hidden')
        this.errorOverlay.classList.remove('hidden')
    }

    hideLoading() {
        this.loadingOverlay.classList.add('hidden')
    }
}

// Initialize TTS Controller when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.tts = new TTSController()
})

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    speechSynthesis.cancel()
})
