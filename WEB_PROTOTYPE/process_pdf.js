/**
 * Process PDF text with structure
 * @param {File} file                - The PDF file object from browser file input
 * @returns {Promise<Array<string>>} - Array of processed text sentences
 */
async function processPdfTextWithStructure(file) {
    // Read file as ArrayBuffer
    const dataBuffer = await file.arrayBuffer()
    
    // Use pdf.js library...
    const pdf                         = window.pdfjsLib
    pdf.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js'
    
    // Load the PDF document
    const pdfDocument = await pdf.getDocument({ data: dataBuffer }).promise
    
    // Extract text from all pages
    let fullText = ''
    for (let pageNum = 1; pageNum <= pdfDocument.numPages; pageNum++) {
        const page        = await pdfDocument.getPage(pageNum)
        const textContent = await page.getTextContent()
        const pageText    = textContent.items.map(item => item.str).join(' ')
        fullText         += pageText + '\n'
    }
    
    const data = { text: fullText }
    
    // As in the original Python script...
    const lines = data.text.split('\n').map(
        l => l.trim()
    ).filter(Boolean)

    const processedText = []
    let currentBlock    = []

    // Helper: split text into sentences with graceful fallback if `sbd` isn't available
    function splitIntoSentences(text) {
        // Prefer sbd (Sentence Boundary Detection) if the library is loaded
        if (window.sbd && typeof window.sbd.sentences === 'function') {
            try {
                return window.sbd.sentences(text)
            } catch (e) {
                // Fall through to regex when sbd throws for any reason
                console.warn('sbd.sentences failed, falling back to regex splitter', e)
            }
        }

        const re      = /([^.!?\n]+[.!?])(?=\s+|$)/g
        const matches = text.match(re)
        if (matches) {
            return matches.map(s => s.trim());
        }

        // As last resort, split on newlines and periods...
        return text.split(/\n+|\.\s+/).map(s => s.trim()).filter(Boolean);
    }

    for (const line of lines) {
        // Header detection: short line, not ending with '.'
        if ((line.length < 50) && !line.endsWith('.')) {
            if (currentBlock.length > 0) {
                const blockText = currentBlock.join(' ')
                const sentences = splitIntoSentences(blockText)
                processedText.push(...sentences)
                currentBlock    = []
            }
            
            processedText.push(line) // Keep header...
        }
        else {
            currentBlock.push(line)
        }
    }

    // Flush last block!
    if (currentBlock.length > 0) {
        const blockText = currentBlock.join(' ')
        const sentences = splitIntoSentences(blockText)
        processedText.push(...sentences)
    }

    return processedText;
}

/**
 * Process the uploaded PDF and print results to console
 * This function is called after a PDF is successfully uploaded
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
        console.log('Starting PDF text extraction and processing...')
        
        const processedText = await processPdfTextWithStructure(selectedFile)
        
        // Print the processedText to console
        console.log('\n=== PROCESSED TEXT OUTPUT ===')
        console.log('Total sentences/blocks:', processedText.length)
        console.log('\nProcessed Text Array:')
        console.log(processedText)
        
        // Also log each sentence separately for easier reading
        console.log('\n=== INDIVIDUAL SENTENCES/BLOCKS ===')
        processedText.forEach((sentence, index) => {
            console.log(`[${index + 1}] ${sentence}`)
        })
        
        // Show success message in UI
        showStatusMessage(`Successfully processed PDF! Found ${processedText.length} text blocks. Check console for results.`, 'success')
        
    } catch (error) {
        console.error('Error processing PDF:', error)
        showStatusMessage('Error processing PDF. Check console for details.', 'error')
    }

    return;
}

// Export the function for use by other scripts
window.processPdfTextWithStructure = processPdfTextWithStructure;
window.processUploadedPDF          = processUploadedPDF;
