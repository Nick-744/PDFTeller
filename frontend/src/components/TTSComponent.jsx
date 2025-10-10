import { useState, useEffect, useRef } from 'react'
import './TTSComponent.css'

const TTSComponent = ({ 
  sentences, 
  currentIndex, 
  onSentenceComplete, 
  isPlaying, 
  setIsPlaying 
}) => {
  const [speechRate,      setSpeechRate     ] = useState(1.0)
  const [availableVoices, setAvailableVoices] = useState([])
  const [selectedVoice,   setSelectedVoice  ] = useState(null)
  const [showSettings,    setShowSettings   ] = useState(false)
  const utteranceRef = useRef(null)

  // Load available voices
  useEffect(() => {
    const loadVoices = () => {
      const voices = speechSynthesis.getVoices()
      setAvailableVoices(voices)
      
      // Set default voice (prefer English voices)
      const englishVoice = voices.find(voice => 
        voice.lang.startsWith('en') && voice.name.includes('Google')
      ) || voices.find(voice => voice.lang.startsWith('en')) || voices[0]
      
      setSelectedVoice(englishVoice)
    }

    loadVoices()
    speechSynthesis.addEventListener('voiceschanged', loadVoices)
    
    return () => {
      speechSynthesis.removeEventListener('voiceschanged', loadVoices)
    };
  }, [])

  // Speak current sentence
  useEffect(() => {
    if (isPlaying && sentences[currentIndex] && selectedVoice)
      speakSentence(sentences[currentIndex])
  }, [currentIndex, isPlaying, selectedVoice])

  const speakSentence = (text) => {
    // Cancel any ongoing speech
    speechSynthesis.cancel()

    const utterance  = new SpeechSynthesisUtterance(text)
    utterance.voice  = selectedVoice
    utterance.rate   = speechRate
    utterance.volume = 1.0

    utterance.onend = () => { onSentenceComplete() }

    utterance.onerror = (event) => {
      console.error('Speech synthesis error:', event.error)
      setIsPlaying(false)
    }

    utteranceRef.current = utterance
    speechSynthesis.speak(utterance)
  }

  const handlePlay = () => {
    if (!isPlaying)
      setIsPlaying(true)
  }

  const handleStop = () => {
    speechSynthesis.cancel()
    setIsPlaying(false)
  }

  const handleVoiceChange = (e) => {
    const voice = availableVoices.find(v => v.name === e.target.value)
    setSelectedVoice(voice)
  }

  return (
    <div className = "tts-controls">
      <div className = "tts-buttons">
        {/* Mobile-only toggle for expanded settings - placed left of play */}
        <button
        type          = "button"
        className     = "tts-settings-toggle"
        onClick       = {() => setShowSettings(s => !s)}
        aria-expanded = {showSettings}
        aria-controls = "tts-settings-panel"
        >
          {showSettings ? 'Hide settings' : 'Show settings'}
        </button>

        <button 
        onClick   = {isPlaying ? handleStop : handlePlay} 
        className = {`tts-btn ${isPlaying ? 'stop-btn' : 'play-btn'}`}
        disabled  = {!sentences.length || currentIndex >= sentences.length}
        >
          {isPlaying ? 'Stop' : 'Play'}
        </button>
      </div>

      <div id = "tts-settings-panel" className = {`tts-settings ${showSettings ? 'open' : ''}`}>
        <div className = "setting-group">
          <label htmlFor = "voice-select">Voice:</label>
          <select 
          id        = "voice-select"
          value     = {selectedVoice?.name || ''} 
          onChange  = {handleVoiceChange}
          className = "voice-select"
          >
            {availableVoices.map((voice) => (
              <option key = {voice.name} value = {voice.name}>
                {voice.name} ({voice.lang})
              </option>
            ))}
          </select>
        </div>

        <div className = "setting-group">
          <label htmlFor = "rate-slider">Speed: {speechRate.toFixed(1)}x</label>
          <input
          id   = "rate-slider"
          type = "range"
          min  = "0.5"
          max  = "2.0"
          step = "0.1"
          value     = {speechRate}
          onChange  = {(e) => setSpeechRate(parseFloat(e.target.value))}
          className = "slider"
          />
        </div>
      </div>

      <div className = "tts-status">
        <div className = "progress-bar">
          <div
            className = "progress-fill"
            style     = {{ width: `${((currentIndex + 1) / sentences.length) * 100}%` }}
          ></div>
        </div>
      </div>
    </div>
  );
}

export default TTSComponent;
