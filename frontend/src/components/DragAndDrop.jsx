import { useState } from 'react'
import { useDropzone } from 'react-dropzone'
import api from '../api'
import './DragAndDrop.css'
import UploadIcon from '../assets/UploadIcon'

const MyDragAndDrop = ({ 
  isUploading, 
  setIsUploading, 
  results, 
  setResults, 
  error, 
  setError, 
  reset,
  showLibrary 
}) => {
  const [loadingMessage, setLoadingMessage] = useState('Processing PDF...')
  const [loadingSubtext, setLoadingSubtext] = useState(
    'Please wait while we extract the content'
  )

  const handleFileUpload = async (file) => {
    setIsUploading(true)
    setError(null)
    setResults(null)
    setLoadingMessage('Processing PDF...')
    setLoadingSubtext('Please wait while we extract the content')

    try
    {
      const formData = new FormData()
      formData.append('pdf_file', file)
      
      // Start the request
      const response = await api.post(
        '/api/process_pdf',
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      )
      
      // Update message based on response!
      if (response.data.from_library)
      {
        setLoadingMessage('Loading from library...')
        setLoadingSubtext('Found in library, loading existing content')
        // Give user a moment to see the library message
        await new Promise(resolve => setTimeout(resolve, 500))
      }
      
      setResults(response.data.sentences)
    }
    catch (err)
    {
      console.error('Error uploading file:', err)
      setError('Failed to process PDF. Please try again.')
    }
    finally
    {
      setIsUploading(false)
    }
  }

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: (acceptedFiles) => acceptedFiles[0] && handleFileUpload(acceptedFiles[0]),
    accept: { 'application/pdf': ['.pdf'] },
    multiple: false,
    disabled: isUploading
  })

  const renderUploadArea = () => (
    <div
    {...getRootProps()}
    className = {
      `dropzone ${isDragActive ? 'drag-active' : ''} ${isUploading ? 'uploading' : ''}`
    }
    >
      <input {...getInputProps()} />
      <div className = "dropzone-content">
        {isUploading ? (
          <>
            <div className = "loading-spinner"></div>
            <h3>{loadingMessage}</h3>
            <p>{loadingSubtext}</p>
          </>
        ) : (
          <>
            <UploadIcon />
            <h3>Drop your PDF here</h3>
            <p>or click to browse files</p>
            <span className = "file-types">Supports PDF files</span>
          </>
        )}
      </div>
    </div>
  )

  const renderError = () => (
    <div className = "result-container error">
      <h3>Error</h3>
      <p>{error}</p>
      <button onClick = {reset} className = "retry-button">Try Again</button>
    </div>
  )

  return (
    <div className = "drag-drop-container">
      <div className = "app-header">
        <h1>PDFTeller</h1>
        <button onClick = {showLibrary} className = "library-btn">
          View Library
        </button>
      </div>
      <div className = "dropzone-wrapper">
        {error && renderError()}
        {!results && !error && renderUploadArea()}
      </div>
    </div>
  );
}

export default MyDragAndDrop;
