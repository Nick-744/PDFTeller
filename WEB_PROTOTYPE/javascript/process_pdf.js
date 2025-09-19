/**
 * Call the Flask preprocess service at http://127.0.0.1:5000/process
 * Expects JSON { filename: '<name>' } and returns parsed JSON response.
 * If the service is unreachable or returns non-200, this throws.
 */
async function callPreprocessService(filename) {
    // Some browsers include a fake path (C:\fakepath\filename.pdf) in input.value
    const cleanName = (typeof filename === 'string') ? filename.replace(/^.*[\\/]/, '') : filename

    const url = 'http://127.0.0.1:5000/process'

    const resp = await fetch(url, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ filename: cleanName })
    })

    if (!resp.ok) {
        // Try to capture server error message if present
        let text
        try { text = await resp.text() } catch (e) { text = resp.statusText }

        throw new Error(`Preprocess service error ${resp.status}: ${text}`);
    }

    const data = await resp.json()

    return data;
}

/**
 * Process the uploaded PDF using Python Flask service
 * This function is called after a PDF is successfully uploaded!
 */
async function processUploadedPDF() {
    // Get the selected file from the PDF handler
    const selectedFile = window.PDFHandler.getSelectedFile()
    
    if (!selectedFile) {
        console.error('No PDF file selected')

        return;
    }
    
    try {
        console.log('Processing PDF:', selectedFile.name)
        console.log('File size:', selectedFile.size, 'bytes')
        console.log('Processing PDF via Flask service...')

        // Show processing status
        showStatusMessage('Processing PDF... Please wait.', 'success')

        // Process using Python Flask service
        const serverResult = await callPreprocessService(selectedFile.name)

        if (serverResult && Array.isArray(serverResult.data)) {
            const processedText = serverResult.data

            console.log('\n=== PROCESSED TEXT OUTPUT ===')
            console.log('Total sentences/blocks:', processedText.length)
            console.log(processedText)

            console.log('\n=== INDIVIDUAL SENTENCES/BLOCKS ===')
            processedText.forEach((sentence, index) => console.log(`[${index + 1}] ${sentence}`))

            // Store processed data for TTS page
            const pdfData = {
                filename:    selectedFile.name,
                fileSize:    selectedFile.size,
                processedAt: new Date().toISOString(),
                textBlocks:  processedText,
                totalBlocks: processedText.length
            }

            // Store in sessionStorage (cleared when browser tab closes)
            sessionStorage.setItem('pdfProcessedData', JSON.stringify(pdfData))

            showStatusMessage(`Successfully processed PDF. Found ${processedText.length} text blocks.`, 'success')
            
            // Show navigation button to TTS page
            showTTSNavigationButton()
            
            return;
        }
        else {
            throw new Error('Server did not return processed data');
        }

    } catch (error) {
        console.error('Error processing PDF:', error)
        showStatusMessage('Error processing PDF. Make sure the Flask server is running...', 'error')
    }

    return;
}

// Show button to navigate to TTS page
function showTTSNavigationButton() {
    // Check if navigation button already exists
    let navContainer = document.getElementById('ttsNavContainer')
    
    if (!navContainer) {
        // Create navigation container
        navContainer           = document.createElement('div')
        navContainer.id        = 'ttsNavContainer'
        navContainer.className = 'tts-navigation'
        
        navContainer.innerHTML = `
            <div class="nav-content">
                <div class="nav-text">
                    <h3>✅ PDF Processing Complete!</h3>
                    <p>Your PDF has been successfully processed and is ready for text-to-speech playback.</p>
                </div>
                <button id="goToTTSBtn" class="nav-button">
                    🎧 Open Text-to-Speech Reader
                </button>
            </div>
        `
        
        // Insert after the status message
        const statusMessage = document.getElementById('statusMessage')
        statusMessage.parentNode.insertBefore(navContainer, statusMessage.nextSibling)
    }
    
    // Add event listener for navigation button
    const goToTTSBtn = document.getElementById('goToTTSBtn')
    goToTTSBtn.addEventListener('click', navigateToTTS)

    return;
}

// Navigate to the TTS page
function navigateToTTS() {
    // Verify that processed data exists
    const storedData = sessionStorage.getItem('pdfProcessedData')
    
    if (!storedData) {
        showStatusMessage('No processed PDF data found. Please process a PDF first.', 'error')
        return;
    }
    
    // Navigate to TTS page
    window.location.href = 'tts.html'

    return;
}

// Export the function for use by other scripts
window.processUploadedPDF = processUploadedPDF;
