import { useState, useEffect } from 'react'
import TTSComponent from './TTSComponent'
import { addBookmark } from '../api'
import './PDFResults.css'

const PDFResults = ({ results, reset, showLibrary, currentPdfId, startFromIndex = 0 }) => {
  const [sentences,       setSentences      ] = useState([])
  const [currentIndex,    setCurrentIndex   ] = useState(0)
  const [spokenSentences, setSpokenSentences] = useState([])
  const [isPlaying,       setIsPlaying      ] = useState(false)
  const [bookmarkLoading, setBookmarkLoading] = useState(false)

  // Use results directly as returned by backend...
  useEffect(() => {
    setSentences(results)
    setCurrentIndex(startFromIndex)
    
    // Build spoken sentences list if starting from bookmark
    const spokenFromBookmark = startFromIndex > 0 ? results.slice(0, startFromIndex).reverse() : []
    setSpokenSentences(spokenFromBookmark)
  }, [results, startFromIndex])

  // Handle when a sentence is completed by TTS
  const handleSentenceComplete = () => {
    if (currentIndex < sentences.length)
    {
      // Add current sentence to spoken sentences stack
      setSpokenSentences(prev => [sentences[currentIndex], ...prev])
      
      // Move to next sentence
      const nextIndex = currentIndex + 1
      if (nextIndex < sentences.length)
        setCurrentIndex(nextIndex)
      else
        setIsPlaying(false) // All sentences completed
    }
  }

  const getCurrentSentence = () => {
    return sentences[currentIndex] || "";
  }

  const handleBookmark = async () => {
    if (!currentPdfId || currentIndex >= sentences.length) return;
    
    setBookmarkLoading(true)
    try
    {
      await addBookmark(currentPdfId, currentIndex)
      alert(`Bookmark saved at sentence ${currentIndex + 1}!`)
    }
    catch (error)
    {
      console.error('Failed to save bookmark:', error)
      alert('Failed to save bookmark. Please try again.')
    }
    finally
    {
      setBookmarkLoading(false)
    }
  }

  return (
    <div className = "pdf-results-container">  
      <div className = "two-column-layout">
        {/* Left Column - TTS Component and Reading Stats */}
        <div className = "left-column">
          <div className = "left-column-top">
            <TTSComponent 
            sentences          = {sentences}
            currentIndex       = {currentIndex}
            onSentenceComplete = {handleSentenceComplete}
            isPlaying          = {isPlaying}
            setIsPlaying       = {setIsPlaying}
            />

            <div className = "reading-stats">
              <div className = "stat">
                <span className = "stat-label">Total Sentences:</span>
                <span className = "stat-value">{sentences.length}</span>
              </div>
              <div className = "stat">
                <span className = "stat-label">Read:</span>
                <span className = "stat-value">{spokenSentences.length}</span>
              </div>
              <div className = "stat">
                <span className = "stat-label">Remaining:</span>
                <span className = "stat-value">{Math.max(0, sentences.length - currentIndex)}</span>
              </div>
            </div>
          </div>

          <div className = "results-header">
            <h2>PDFTeller</h2>
            <div className = "header-buttons">
              {currentPdfId && currentIndex < sentences.length && (
                <button 
                  onClick   = {handleBookmark}
                  disabled  = {bookmarkLoading}
                  className = "bookmark-btn"
                  title     = {`Bookmark current sentence (${currentIndex + 1})`}
                >
                  {bookmarkLoading ? 'Saving...' : 'Bookmark'}
                </button>
              )}
              <button onClick = {showLibrary} className = "library-btn">
                Library
              </button>
            </div>
          </div>
        </div>

        {/* Right Column - Sentence Stack */}
        <div className = "right-column">
          <div className = "sentence-stack">
            {/* Current sentence */}
            {sentences.length > 0 && currentIndex < sentences.length && (
              <div className = "current-sentence-container">
                <h3>Currently Reading</h3>
                <div className = "current-sentence">
                  <span className = "sentence-number">{currentIndex + 1}</span>
                  <p>{getCurrentSentence()}</p>
                </div>
              </div>
            )}

            {/* Spoken sentences stack */}
            {spokenSentences.length > 0 && (
              <div className = "spoken-sentences">
                <h3>Previously Read</h3>
                <div className = "spoken-list">
                  {spokenSentences.map((sentence, index) => (
                    <div key = {index} className = "spoken-sentence">
                      <span className = "sentence-number">{spokenSentences.length - index}</span>
                      <p>{sentence}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Completion message */}
            {currentIndex >= sentences.length && sentences.length > 0 && (
              <div className = "completion-message">
                <h3>✅ Reading Complete!</h3>
                <p>All {sentences.length} sentences have been read.</p>
                <button onClick={reset} className = "read-another-btn">
                  Read Another PDF
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default PDFResults;
