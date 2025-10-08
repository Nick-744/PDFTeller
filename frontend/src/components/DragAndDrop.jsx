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
  reset 
}) => {
  const handleFileUpload = async (file) => {
    setIsUploading(true)
    setError(null)
    setResults(null)

    try {
      const formData = new FormData()
      formData.append('pdf_file', file)
      const response = await api.post('/api/process_pdf', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
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
    onDrop: (acceptedFiles) => acceptedFiles[0] && handleFileUpload(acceptedFiles[0]),
    accept: { 'application/pdf': ['.pdf'] },
    multiple: false,
    disabled: isUploading
  })

  const renderUploadArea = () => (
    <div {...getRootProps()} className = {`dropzone ${isDragActive ? 'drag-active' : ''} ${isUploading ? 'uploading' : ''}`}>
      <input {...getInputProps()} />
      <div className = "dropzone-content">
        {isUploading ? (
          <>
            <div className = "loading-spinner"></div>
            <h3>Processing PDF...</h3>
            <p>Please wait while we extract the content</p>
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
      {error && renderError()}
      {!results && !error && renderUploadArea()}
    </div>
  );
}

export default MyDragAndDrop;
