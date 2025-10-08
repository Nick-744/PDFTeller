import { useState } from 'react'
import MyDragAndDrop from './components/DragAndDrop'
import PDFResults from './components/PDFResults'
import './App.css'

function App() {
  const [isUploading, setIsUploading] = useState(false)
  const [results,     setResults    ] = useState(null)
  const [error,       setError      ] = useState(null)

  const reset = () => {
    setResults(null)
    setError(null)
  }

  return (
    <>
      {/* Show upload component only when there are no results */}
      {!results && (
        <MyDragAndDrop 
          isUploading    = {isUploading}
          setIsUploading = {setIsUploading}
          results        = {results}
          setResults     = {setResults}
          error          = {error}
          setError       = {setError}
          reset          = {reset}
        />
      )}
      
      {/* Show results component when PDF is processed */}
      {results && (
        <PDFResults 
          results={results} 
          reset={reset} 
        />
      )}
    </>
  )
}

export default App
