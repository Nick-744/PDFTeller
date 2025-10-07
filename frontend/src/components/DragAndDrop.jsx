import { useDropzone } from 'react-dropzone'
import { useState } from 'react'
import api from '../api'
import './DragAndDrop.css'

const MyDragAndDrop = () => {
  const [isUploading, setIsUploading] = useState(false)
  const [results, setResults] = useState(null)
  const [error, setError] = useState(null)

  const handleFileUpload = async (file) => {
    setIsUploading(true)
    setError(null)
    setResults(null)

    try {
      const formData = new FormData()
      formData.append('pdf_file', file)

      const response = await api.post('/api/process_pdf', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      })

      setResults(response.data)
    } catch (err) {
      console.error('Error uploading file:', err)
      setError('Failed to process PDF. Please try again.')
    } finally {
      setIsUploading(false)
    }
  }

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: (acceptedFiles) => {
      if (acceptedFiles.length > 0) {
        handleFileUpload(acceptedFiles[0])
      }
    },
    accept: {
      'application/pdf': ['.pdf']
    },
    multiple: false,
    disabled: isUploading
  })

  const resetState = () => {
    setResults(null)
    setError(null)
  }

  return (
    <div className="drag-drop-container">
      {!results && !error && (
        <div 
          {...getRootProps()} 
          className={`dropzone ${isDragActive ? 'drag-active' : ''} ${isUploading ? 'uploading' : ''}`}
        >
          <input {...getInputProps()} />
          <div className="dropzone-content">
            {isUploading ? (
              <>
                <div className="loading-spinner"></div>
                <h3>Processing PDF...</h3>
                <p>Please wait while we extract the content</p>
              </>
            ) : (
              <>
                <svg className="upload-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="17,8 12,3 7,8" />
                  <line x1="12" y1="3" x2="12" y2="15" />
                </svg>
                <h3>{isDragActive ? 'Drop the PDF here' : 'Drop your PDF here'}</h3>
                <p>or click to browse files</p>
                <span className="file-types">Supports PDF files</span>
              </>
            )}
          </div>
        </div>
      )}

      {error && (
        <div className="result-container error">
          <h3>Error</h3>
          <p>{error}</p>
          <button onClick={resetState} className="retry-button">Try Again</button>
        </div>
      )}

      {results && (
        <div className="result-container success">
          <h3>PDF Content Extracted</h3>
          <div className="results-content">
            {results.map((section, index) => (
              <div key={index} className="result-section">
                <p>{section}</p>
              </div>
            ))}
          </div>
          <button onClick={resetState} className="retry-button">Upload Another PDF</button>
        </div>
      )}
    </div>
  );
}

export default MyDragAndDrop;
