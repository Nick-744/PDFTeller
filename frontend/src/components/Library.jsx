import { useState, useEffect } from 'react'
import api from '../api'
import './Library.css'

const Library = ({ onSelectPDF, onBackToUpload }) => {
  const [libraryData,  setLibraryData ] = useState([])
  const [loading,      setLoading     ] = useState(true)
  const [error,        setError       ] = useState(null)
  const [searchTerm,   setSearchTerm  ] = useState('')
  const [filteredData, setFilteredData] = useState([])

  // Fetch library data on component mount
  useEffect(() => { fetchLibrary() }, [])

  // Filter library data based on search term
  useEffect(() => {
    if (searchTerm.trim() === '')
      setFilteredData(libraryData)
    else
    {
      const filtered = libraryData.filter(pdf => 
        pdf.filename.toLowerCase().includes(searchTerm.toLowerCase())
      )
      setFilteredData(filtered)
    }
  }, [searchTerm, libraryData])

  const fetchLibrary = async () => {
    try
    {
      setLoading(true)
      const response = await api.get('/api/library')
      setLibraryData(response.data)
      setError(null)
    }
    catch (err)
    {
      setError(err.response?.data?.detail || err.message || 'Failed to fetch library')
    }
    finally
    {
      setLoading(false)
    }
  }

  const handleSelectPDF = async (pdfId, startFromBookmark = null) => {
    try
    {
      const response = await api.get(`/api/library/${pdfId}`)
      const pdfData = {
        sentences: response.data.sentences,
        pdfId: pdfId,
        startFromIndex: startFromBookmark !== null ? startFromBookmark : 0
      }
      onSelectPDF(pdfData)
    }
    catch (err)
    {
      setError(err.response?.data?.detail || err.message || 'Failed to fetch PDF data')
    }
  }

  const handleDeletePDF = async (pdfId, filename) => {
    if (window.confirm(`Are you sure you want to delete "${filename}"?`))
    {
      try
      {
        await api.delete(`/api/library/${pdfId}`)
        // Refresh library after deletion
        fetchLibrary()
      }
      catch (err)
      {
        setError(err.response?.data?.detail || err.message || 'Failed to delete PDF')
      }
    }
  }

  const formatDate = (dateString) => {
    const date = new Date(dateString)
    return date.toLocaleDateString('en-US', {
      year:   'numeric',
      month:  'short',
      day:    'numeric',
      hour:   '2-digit',
      minute: '2-digit'
    })
  }

  if (loading)
  {
    return (
      <div className = "library-container">
        <div className = "loading-message">
          <h2>Loading Library...</h2>
          <div className = "loading-spinner"></div>
        </div>
      </div>
    );
  }

  return (
    <div className = "library-container">
      <div className = "two-column-layout">
        {/* Left Column - Controls and Navigation */}
        <div className = "left-col-library">
          <div className = "left-col-top-library">
            <div className = "library-header">
              <div className = "library-title">
                <h1>Library</h1>
                <p>Manage your processed PDFs</p>
              </div>
            </div>

            {error && (
              <div className = "error-message">
                <p>Error: {error}</p>
                <button onClick = {fetchLibrary} className = "retry-btn">
                  Retry
                </button>
              </div>
            )}

            <div className = "library-controls">
              <div className = "search-container">
                <input
                type        = "text"
                placeholder = "🔍 Search PDFs by filename..."
                value       = {searchTerm}
                onChange    = {(e) => setSearchTerm(e.target.value)}
                className   = "search-input"
                />
              </div>
              <div className = "library-stats">
                <span className = "stat">
                  📁 Total: {libraryData.length}
                </span>
                <span className = "stat">
                  👁️ Showing: {filteredData.length}
                </span>
              </div>
            </div>
          </div>

          <div className = "library-navigation">
            <button onClick = {onBackToUpload} className = "back-btn">
              Back to Upload
            </button>
            {filteredData.length > 0 && (
              <button onClick = {fetchLibrary} className = "refresh-btn">
                Refresh Library
              </button>
            )}
          </div>
        </div>

        {/* Right Column - PDF Cards */}
        <div className = "right-column">
          <div className = "pdf-content">
            {filteredData.length === 0 ? (
              <div className = "empty-library">
                {searchTerm ? (
                  <div>
                    <h3>🔍 No PDFs found</h3>
                    <p>No PDFs match your search term "{searchTerm}"</p>
                  </div>
                ) : (
                  <div>
                    <h3>Your library is empty</h3>
                    <p>Process some PDFs to see them here!</p>
                    <button onClick = {onBackToUpload} className = "upload-first-btn">
                      📤 Upload Your First PDF
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className = "library-grid">
                {filteredData.map((pdf) => (
                  <div key = {pdf.id} className = "pdf-card">
                    <div className = "pdf-card-header">
                      <h3 className = "pdf-filename">{pdf.filename}</h3>
                      <button 
                      onClick   = {() => handleDeletePDF(pdf.id, pdf.filename)}
                      className = "delete-btn"
                      title     = "Delete PDF"
                      >
                        🔫
                      </button>
                    </div>
                    
                    <div className = "pdf-card-info">
                      <div className = "pdf-stat">
                        <span className = "stat-label">📝 Sentences:</span>
                        <span className = "stat-value">{pdf.sentence_count}</span>
                      </div>
                      <div className = "pdf-stat">
                        <span className = "stat-label">📅 Added:</span>
                        <span className = "stat-value">{formatDate(pdf.date_added)}</span>
                      </div>
                      {pdf.bookmark !== null && (
                        <div className = "pdf-stat bookmark-stat">
                          <span className = "stat-label">🔖 Bookmark:</span>
                          <span className = "stat-value">Sentence {pdf.bookmark + 1}</span>
                        </div>
                      )}
                    </div>

                    <div className = "pdf-card-actions">
                      <button 
                      onClick   = {() => handleSelectPDF(pdf.id)}
                      className = "load-pdf-btn"
                      >
                        Load & Read
                      </button>
                      {pdf.bookmark !== null && (
                        <button 
                        onClick   = {() => handleSelectPDF(pdf.id, pdf.bookmark)}
                        className = "resume-bookmark-btn"
                        title     = {`Resume from sentence ${pdf.bookmark + 1}`}
                        >
                          Resume
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default Library;
