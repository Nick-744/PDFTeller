import { useState, useEffect } from 'react'
import MyDragAndDrop from './components/DragAndDrop'
import PDFResults from './components/PDFResults'
import Library from './components/Library'
import './App.css'

function App()
{
  const [isUploading, setIsUploading] = useState(false)
  const [results,     setResults    ] = useState(null)
  const [error,       setError      ] = useState(null)

  // 'upload', 'results', 'library'
  const [currentView,     setCurrentView    ] = useState('upload')
  const [isTransitioning, setIsTransitioning] = useState(false)
  const [nextView,        setNextView       ] = useState(null)

  // Function to handle smooth transitions
  const transitionToView = (newView) => {
    if (newView === currentView) return
    
    setIsTransitioning(true)
    setNextView(newView)
    
    // After fade out completes, switch to new view
    setTimeout(() => {
      setCurrentView(newView)
      setIsTransitioning(false)
      setNextView(null)
    }, 300) // Half the total transition time
  }

  // Automatically switch to results view when PDF processing is complete
  useEffect(() => {
    if (results && !error && currentView === 'upload')
      transitionToView('results')
  }, [results, error, currentView])

  const reset = () => {
    setResults(null)
    setError(null)
    transitionToView('upload')
  }

  const showLibrary = () => { transitionToView('library') }

  const showUpload = () => {
    setResults(null)
    setError(null)
    transitionToView('upload')
  }

  const handlePDFSelect = (sentences) => {
    setResults(sentences)
    transitionToView('results')
  }

  return (
    <div className="app-container">
      {/* Upload View */}
      {currentView === 'upload' && (
        <div
        className = {
          `page-transition upload-view ${isTransitioning ? 'fade-out' : 'fade-in'}`
        }
        key = "upload"
        >
          <MyDragAndDrop 
          isUploading    = {isUploading}
          setIsUploading = {setIsUploading}
          results        = {results}
          setResults     = {setResults}
          error          = {error}
          setError       = {setError}
          reset          = {reset}
          showLibrary    = {showLibrary}
          />
        </div>
      )}
      
      {/* Results View */}
      {currentView === 'results' && results && (
        <div
        className = {
          `page-transition results-view ${isTransitioning ? 'fade-out' : 'fade-in'}`
        }
        key = "results"
        >
          <PDFResults 
          results     = {results} 
          reset       = {reset}
          showLibrary = {showLibrary}
          />
        </div>
      )}

      {/* Library View */}
      {currentView === 'library' && (
        <div
        className = {
          `page-transition library-view ${isTransitioning ? 'fade-out' : 'fade-in'}`
        }
        key = "library"
        >
          <Library 
          onSelectPDF    = {handlePDFSelect}
          onBackToUpload = {showUpload}
          />
        </div>
      )}
    </div>
  );
}

export default App;
