// TTS Page - Text-to-Speech functionality

// Global variables
let pdfData = null
let textBlocks = []
let currentBlockIndex = 0
let currentUtterance = null
let isPlaying = false
let isPaused = false
let isExpanded = false

// DOM Elements
let playBtn, pauseBtn, stopBtn, prevBtn, nextBtn
let speechRate, speechPitch, speechVolume, voiceSelect
let rateValue, pitchValue, volumeValue
let currentTextDisplay, progressFill, currentBlock, totalBlocks
let backBtn, copyTextBtn, expandTextBtn
let loadingOverlay, errorMessage

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    initializeElements()
    loadPDFData()
})

/**
 * Initialize DOM elements and event listeners
 */
function initializeElements() {
    // Control buttons
    playBtn = document.getElementById('playBtn')
    pauseBtn = document.getElementById('pauseBtn')
    stopBtn = document.getElementById('stopBtn')
    prevBtn = document.getElementById('prevBtn')
    nextBtn = document.getElementById('nextBtn')
    
    // Settings
    speechRate = document.getElementById('speechRate')
    speechPitch = document.getElementById('speechPitch')
    speechVolume = document.getElementById('speechVolume')
    voiceSelect = document.getElementById('voiceSelect')
    
    // Value displays
    rateValue = document.getElementById('rateValue')
    pitchValue = document.getElementById('pitchValue')
    volumeValue = document.getElementById('volumeValue')
    
    // Display elements
    currentTextDisplay = document.getElementById('currentTextDisplay')
    progressFill = document.getElementById('progressFill')
    currentBlock = document.getElementById('currentBlock')
    totalBlocks = document.getElementById('totalBlocks')
    
    // Other controls
    backBtn = document.getElementById('backBtn')
    copyTextBtn = document.getElementById('copyTextBtn')
    expandTextBtn = document.getElementById('expandTextBtn')
    
    // Overlays
    loadingOverlay = document.getElementById('loadingOverlay')
    errorMessage = document.getElementById('errorMessage')
    
    // Setup event listeners
    setupEventListeners()
    
    // Initialize voice options
    populateVoiceOptions()
    
    // Listen for voices changed event
    if (speechSynthesis) {
        speechSynthesis.addEventListener('voiceschanged', populateVoiceOptions)
    }
}

/**
 * Setup all event listeners
 */
function setupEventListeners() {
    // Playback controls
    playBtn.addEventListener('click', playTTS)
    pauseBtn.addEventListener('click', pauseTTS)
    stopBtn.addEventListener('click', stopTTS)
    prevBtn.addEventListener('click', previousBlock)
    nextBtn.addEventListener('click', nextBlock)
    
    // Settings with real-time value updates
    speechRate.addEventListener('input', (e) => {
        rateValue.textContent = parseFloat(e.target.value).toFixed(1) + 'x'
    })
    
    speechPitch.addEventListener('input', (e) => {
        pitchValue.textContent = parseFloat(e.target.value).toFixed(1)
    })
    
    speechVolume.addEventListener('input', (e) => {
        volumeValue.textContent = Math.round(parseFloat(e.target.value) * 100) + '%'
    })
    
    // Other controls
    backBtn.addEventListener('click', () => {
        window.location.href = 'index.html'
    })
    
    copyTextBtn.addEventListener('click', copyCurrentText)
    expandTextBtn.addEventListener('click', toggleTextSize)
    
    // Keyboard shortcuts
    document.addEventListener('keydown', handleKeyboardShortcuts)
}

/**
 * Handle keyboard shortcuts
 */
function handleKeyboardShortcuts(event) {
    // Don't trigger shortcuts if user is typing in an input
    if (event.target.tagName === 'INPUT' || event.target.tagName === 'SELECT') {
        return
    }
    
    switch (event.code) {
        case 'Space':
            event.preventDefault()
            if (isPlaying && !isPaused) {
                pauseTTS()
            } else {
                playTTS()
            }
            break
        case 'ArrowLeft':
            event.preventDefault()
            previousBlock()
            break
        case 'ArrowRight':
            event.preventDefault()
            nextBlock()
            break
        case 'Escape':
            stopTTS()
            break
    }
}

/**
 * Load PDF data from sessionStorage
 */
function loadPDFData() {
    try {
        const storedData = sessionStorage.getItem('pdfProcessedData')
        
        if (!storedData) {
            showError()
            return
        }
        
        pdfData = JSON.parse(storedData)
        textBlocks = pdfData.textBlocks || []
        
        if (textBlocks.length === 0) {
            showError()
            return
        }
        
        // Initialize UI with loaded data
        initializeUI()
        hideLoading()
        
    } catch (error) {
        console.error('Error loading PDF data:', error)
        showError()
    }
}

/**
 * Initialize UI with loaded PDF data
 */
function initializeUI() {
    // Update PDF info
    document.getElementById('pdfName').textContent = pdfData.filename
    document.getElementById('pdfStats').textContent = `${pdfData.totalBlocks} text blocks`
    
    // Update counters
    totalBlocks.textContent = pdfData.totalBlocks
    currentBlock.textContent = currentBlockIndex + 1
    
    // Display first text block
    updateTextDisplay()
    updateProgress()
    
    // Generate quick navigation
    generateQuickNavigation()
}

/**
 * Populate voice selection dropdown
 */
function populateVoiceOptions() {
    if (!voiceSelect) return
    
    const voices = speechSynthesis.getVoices()
    
    // Clear existing options
    voiceSelect.innerHTML = ''
    
    if (voices.length === 0) {
        voiceSelect.innerHTML = '<option>No voices available</option>'
        return
    }
    
    // Group voices by language
    const voicesByLang = {}
    voices.forEach((voice, index) => {
        const lang = voice.lang.split('-')[0] || 'other'
        if (!voicesByLang[lang]) {
            voicesByLang[lang] = []
        }
        voicesByLang[lang].push({ voice, index })
    })
    
    // Add grouped options
    Object.keys(voicesByLang).sort().forEach(lang => {
        const optgroup = document.createElement('optgroup')
        optgroup.label = lang.toUpperCase()
        
        voicesByLang[lang].forEach(({ voice, index }) => {
            const option = document.createElement('option')
            option.value = index
            option.textContent = voice.name
            if (voice.default) {
                option.selected = true
            }
            optgroup.appendChild(option)
        })
        
        voiceSelect.appendChild(optgroup)
    })
}

/**
 * Play TTS from current position
 */
function playTTS() {
    if (textBlocks.length === 0) return
    
    if (isPaused && currentUtterance) {
        // Resume paused speech
        speechSynthesis.resume()
        isPaused = false
        updateControlButtons()
        return
    }
    
    // Start speaking current block
    speakCurrentBlock()
}

/**
 * Pause TTS playback
 */
function pauseTTS() {
    if (isPlaying && !isPaused) {
        speechSynthesis.pause()
        isPaused = true
        updateControlButtons()
    }
}

/**
 * Stop TTS playback
 */
function stopTTS() {
    speechSynthesis.cancel()
    isPlaying = false
    isPaused = false
    currentUtterance = null
    updateControlButtons()
}

/**
 * Go to previous block
 */
function previousBlock() {
    if (currentBlockIndex > 0) {
        currentBlockIndex--
        updateTextDisplay()
        updateProgress()
        
        if (isPlaying) {
            speechSynthesis.cancel()
            speakCurrentBlock()
        }
    }
}

/**
 * Go to next block
 */
function nextBlock() {
    if (currentBlockIndex < textBlocks.length - 1) {
        currentBlockIndex++
        updateTextDisplay()
        updateProgress()
        
        if (isPlaying) {
            speechSynthesis.cancel()
            speakCurrentBlock()
        }
    }
}

/**
 * Speak the current text block
 */
function speakCurrentBlock() {
    if (currentBlockIndex >= textBlocks.length) {
        stopTTS()
        return
    }
    
    const text = textBlocks[currentBlockIndex]
    if (!text || text.trim() === '') {
        // Skip empty blocks
        nextBlock()
        return
    }
    
    // Create speech utterance
    const utterance = new SpeechSynthesisUtterance(text)
    
    // Apply current settings
    utterance.rate = parseFloat(speechRate.value)
    utterance.pitch = parseFloat(speechPitch.value)
    utterance.volume = parseFloat(speechVolume.value)
    
    // Set selected voice
    const voices = speechSynthesis.getVoices()
    if (voices.length > 0 && voiceSelect.selectedIndex >= 0) {
        utterance.voice = voices[voiceSelect.selectedIndex]
    }
    
    // Event handlers
    utterance.onstart = () => {
        isPlaying = true
        isPaused = false
        currentUtterance = utterance
        updateControlButtons()
        highlightCurrentText()
    }
    
    utterance.onend = () => {
        // Auto-advance to next block
        if (currentBlockIndex < textBlocks.length - 1) {
            currentBlockIndex++
            updateTextDisplay()
            updateProgress()
            speakCurrentBlock()
        } else {
            // Finished all blocks
            stopTTS()
            showCompletionMessage()
        }
    }
    
    utterance.onerror = (event) => {
        console.error('Speech synthesis error:', event)
        stopTTS()
    }
    
    // Start speaking
    speechSynthesis.speak(utterance)
}

/**
 * Update control button states
 */
function updateControlButtons() {
    playBtn.disabled = isPlaying && !isPaused
    pauseBtn.disabled = !isPlaying || isPaused
    stopBtn.disabled = !isPlaying
    
    // Update play button text
    if (isPaused) {
        playBtn.innerHTML = '▶️'
        playBtn.title = 'Resume'
    } else {
        playBtn.innerHTML = '▶️'
        playBtn.title = 'Play'
    }
    
    // Update navigation buttons
    prevBtn.disabled = currentBlockIndex === 0
    nextBtn.disabled = currentBlockIndex === textBlocks.length - 1
}

/**
 * Update text display
 */
function updateTextDisplay() {
    if (textBlocks.length === 0) return
    
    const text = textBlocks[currentBlockIndex]
    currentTextDisplay.innerHTML = `<p>${text || 'No text available'}</p>`
    
    // Update block counter
    currentBlock.textContent = currentBlockIndex + 1
    
    // Update control buttons
    updateControlButtons()
}

/**
 * Update progress bar
 */
function updateProgress() {
    if (textBlocks.length === 0) return
    
    const progress = ((currentBlockIndex + 1) / textBlocks.length) * 100
    progressFill.style.width = `${progress}%`
}

/**
 * Highlight current text during speech
 */
function highlightCurrentText() {
    currentTextDisplay.classList.add('speaking')
    setTimeout(() => {
        currentTextDisplay.classList.remove('speaking')
    }, 300)
}

/**
 * Generate quick navigation buttons
 */
function generateQuickNavigation() {
    const navButtons = document.getElementById('navButtons')
    if (!navButtons || textBlocks.length === 0) return
    
    navButtons.innerHTML = ''
    
    // Create navigation buttons (show max 20 for performance)
    const maxButtons = Math.min(textBlocks.length, 20)
    const step = Math.ceil(textBlocks.length / maxButtons)
    
    for (let i = 0; i < textBlocks.length; i += step) {
        const button = document.createElement('button')
        button.className = 'nav-btn'
        button.textContent = `${i + 1}`
        button.title = `Jump to block ${i + 1}`
        button.addEventListener('click', () => jumpToBlock(i))
        navButtons.appendChild(button)
    }
}

/**
 * Jump to specific block
 */
function jumpToBlock(index) {
    if (index >= 0 && index < textBlocks.length) {
        currentBlockIndex = index
        updateTextDisplay()
        updateProgress()
        
        if (isPlaying) {
            speechSynthesis.cancel()
            speakCurrentBlock()
        }
    }
}

/**
 * Copy current text to clipboard
 */
function copyCurrentText() {
    if (textBlocks.length === 0) return
    
    const text = textBlocks[currentBlockIndex]
    navigator.clipboard.writeText(text).then(() => {
        // Visual feedback
        copyTextBtn.textContent = '✅'
        setTimeout(() => {
            copyTextBtn.textContent = '📋'
        }, 1000)
    }).catch(err => {
        console.error('Failed to copy text:', err)
    })
}

/**
 * Toggle text size
 */
function toggleTextSize() {
    isExpanded = !isExpanded
    currentTextDisplay.classList.toggle('expanded', isExpanded)
    expandTextBtn.textContent = isExpanded ? '🔍-' : '🔍+'
}

/**
 * Show completion message
 */
function showCompletionMessage() {
    const originalText = currentTextDisplay.innerHTML
    currentTextDisplay.innerHTML = `
        <div class="completion-message">
            <h3>🎉 Reading Complete!</h3>
            <p>Finished reading all ${textBlocks.length} text blocks.</p>
            <button onclick="jumpToBlock(0); playTTS();" class="completion-btn">
                🔄 Read Again
            </button>
        </div>
    `
    
    // Restore original text after 5 seconds
    setTimeout(() => {
        currentTextDisplay.innerHTML = originalText
    }, 5000)
}

/**
 * Show error state
 */
function showError() {
    hideLoading()
    errorMessage.style.display = 'flex'
}

/**
 * Hide loading overlay
 */
function hideLoading() {
    loadingOverlay.style.display = 'none'
}

// Cleanup when leaving page
window.addEventListener('beforeunload', () => {
    if (speechSynthesis.speaking) {
        speechSynthesis.cancel()
    }
})