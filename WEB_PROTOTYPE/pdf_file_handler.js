// PDF File Handler - Handles file selection, validation, and UI updates

// DOM Elements - Cache frequently used elements for better performance
const pdfInput      = document.getElementById('pdfInput')
const dropZone      = document.getElementById('dropZone')
const fileInfo      = document.getElementById('fileInfo')
const fileName      = document.getElementById('fileName')
const fileSize      = document.getElementById('fileSize')
const removeBtn     = document.getElementById('removeBtn')
const statusMessage = document.getElementById('statusMessage')

// Global variable to store the selected file
let selectedFile = null

// Initialize event listeners when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    initializeEventListeners()
})

// Set up all event listeners for the PDF input interface
function initializeEventListeners() {
    // File input change event
    pdfInput.addEventListener('change', handleFileSelect)
    
    // Drop zone drag and drop events
    dropZone.addEventListener('click', triggerFileInput)
    dropZone.addEventListener('dragover', handleDragOver)
    dropZone.addEventListener('dragleave', handleDragLeave)
    dropZone.addEventListener('drop', handleDrop)
    
    // Remove file button event
    removeBtn.addEventListener('click', removeFile)
    
    // Prevent default drag behaviors on the entire document
    document.addEventListener('dragover', preventDefault)
    document.addEventListener('drop', preventDefault)

    return;
}

/**
 * Handle file selection from input or drop
 * @param {Event} event - The file input change event
 */
function handleFileSelect(event) {
    const file = event.target.files[0]
    processSelectedFile(file)

    return;
}

// Trigger the hidden file input when drop zone is clicked
function triggerFileInput() {
    pdfInput.click()

    return;
}

/**
 * Handle drag over event for drop zone
 * @param {DragEvent} event - The drag event
 */
function handleDragOver(event) {
    event.preventDefault()
    event.stopPropagation()
    dropZone.classList.add('drag-over')

    return;
}

/**
 * Handle drag leave event for drop zone
 * @param {DragEvent} event - The drag event
 */
function handleDragLeave(event) {
    event.preventDefault()
    event.stopPropagation()
    dropZone.classList.remove('drag-over')

    return;
}

/**
 * Handle file drop event
 * @param {DragEvent} event - The drop event
 */
function handleDrop(event) {
    event.preventDefault()
    event.stopPropagation()
    dropZone.classList.remove('drag-over')
    
    const files = event.dataTransfer.files
    if (files.length > 0) {
        processSelectedFile(files[0])
    }

    return;
}

/**
 * Prevent default behavior for drag events
 * @param {Event} event - The event to prevent
 */
function preventDefault(event) {
    event.preventDefault()

    return;
}

/**
 * Process and validate the selected file
 * @param {File} file - The selected file object
 */
function processSelectedFile(file) {
    // Clear any existing status messages
    hideStatusMessage()
    
    // Validate file exists
    if (!file) {
        showStatusMessage('No file selected.', 'error')

        return;
    }
    
    // Validate file type
    if (!isValidPDFFile(file)) {
        showStatusMessage('Please select a valid PDF file.', 'error')
        clearFileInput()

        return;
    }
    
    // Validate file size
    if (!isValidFileSize(file)) {
        showStatusMessage('File size must be less than 20MB.', 'error')
        clearFileInput()

        return;
    }
    
    // File is valid, store it and update UI
    selectedFile = file
    displayFileInfo(file)
    showStatusMessage('PDF file selected successfully!', 'success')

    // Automatically process the PDF after successful upload
    if (window.processUploadedPDF) {
        // Small delay to let UI update first
        setTimeout(() => {
            window.processUploadedPDF()
        }, 500)
    }

    return;
}

/**
 * Validate if the file is a PDF
 * @param {File} file - File to validate
 * @returns {boolean} - True if valid PDF file
 */
function isValidPDFFile(file) {
    // Check file extension
    const validExtensions   = ['.pdf']
    const fileName          = file.name.toLowerCase()
    const hasValidExtension = validExtensions.some(ext => fileName.endsWith(ext))
    
    // Check MIME type
    const validMimeTypes   = ['application/pdf']
    const hasValidMimeType = validMimeTypes.includes(file.type)
    
    return hasValidExtension && hasValidMimeType;
}

/**
 * Validate file size
 * @param {File} file - File to validate
 * @returns {boolean} - True if file size is acceptable
 */
function isValidFileSize(file) {
    const maxSizeInBytes = 20 * 1024 * 1024 // 20MB limit

    return file.size <= maxSizeInBytes;
}

/**
 * Display file information in the UI
 * @param {File} file - The file to display information for
 */
function displayFileInfo(file) {
    // Update file name display
    fileName.textContent   = file.name
    
    // Update file size display with formatted size
    fileSize.textContent   = formatFileSize(file.size)
    
    // Show the file info section
    fileInfo.style.display = 'block'
    
    // Hide the drop zone since we have a file selected
    dropZone.style.display = 'none'

    return;
}

/**
 * Format file size in human readable format
 * @param {number} bytes - File size in bytes
 * @returns {string}     - Formatted file size string
 */
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    
    const k     = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i     = Math.floor(Math.log(bytes) / Math.log(k))
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// Remove the selected file and reset UI
function removeFile() {
    // Clear the selected file
    selectedFile = null
    
    // Clear the file input
    clearFileInput()
    
    // Hide file info section
    fileInfo.style.display = 'none'
    
    // Show drop zone again
    dropZone.style.display = 'block'
    
    // Hide status message
    hideStatusMessage()

    return;
}

// Clear the file input element
function clearFileInput() {
    pdfInput.value = ''

    return;
}

/**
 * Show status message with specified type
 * @param {string} message - Message to display
 * @param {string} type    - Message type ('success' or 'error')
 */
function showStatusMessage(message, type) {
    statusMessage.textContent   = message
    statusMessage.className     = `status-message ${type}`
    statusMessage.style.display = 'block'
    
    // Auto-hide success messages after 4 seconds
    if (type === 'success') {
        setTimeout(() => {
            hideStatusMessage();
        }, 4000);
    }

    return;
}

// Hide the status message
function hideStatusMessage() {
    statusMessage.style.display = 'none'
    statusMessage.className     = 'status-message'

    return;
}

/**
 * Get the currently selected file (utility function for external use)
 * @returns {File|null} - The selected file or null if none selected
 */
function getSelectedFile() {
    return selectedFile;
}

/**
 * Check if a file is currently selected (utility function for external use)
 * @returns {boolean} - True if a file is selected
 */
function hasFileSelected() {
    return selectedFile !== null;
}

// Export functions for potential use by other scripts
window.PDFHandler = {
    getSelectedFile,
    hasFileSelected,
    removeFile
};
