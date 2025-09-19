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

        // Process using Python Flask service
        const serverResult = await callPreprocessService(selectedFile.name)

        if (serverResult && Array.isArray(serverResult.data)) {
            const processedText = serverResult.data

            console.log('\n=== PROCESSED TEXT OUTPUT ===')
            console.log('Total sentences/blocks:', processedText.length)
            console.log(processedText)

            console.log('\n=== INDIVIDUAL SENTENCES/BLOCKS ===')
            processedText.forEach((sentence, index) => console.log(`[${index + 1}] ${sentence}`))

            showStatusMessage(`Successfully processed PDF. Found ${processedText.length} text blocks.`, 'success')
            return
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

// Export the function for use by other scripts
window.processUploadedPDF = processUploadedPDF;
