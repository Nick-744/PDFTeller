// Simple library manager using localStorage
// Structure in localStorage under key 'pdfLibrary':
// [ { id, filename, fileSize, processedAt, textBlocks: [], bookmarks: [ { name, index } ] } ]

function loadLibrary() {
    const raw = localStorage.getItem('pdfLibrary')
    try {
        return raw ? JSON.parse(raw) : []
    } catch (e) {
        console.error('Invalid library data', e)
        return []
    }
}

function saveLibrary(lib) {
    localStorage.setItem('pdfLibrary', JSON.stringify(lib))
}

function renderLibrary() {
    const booksEl = document.getElementById('books')
    const lib = loadLibrary()
    booksEl.innerHTML = ''

    if (lib.length === 0) {
        booksEl.innerHTML = '<p>No saved books yet. Process a PDF and save it here.</p>'
        return
    }

    lib.forEach((book) => {
        const bookEl = document.createElement('div')
        bookEl.className = 'book'
        bookEl.innerHTML = `
            <div class="book-info">
                <div class="book-meta">📄</div>
                <div>
                    <div class="book-title">${escapeHtml(book.filename)}</div>
                    <div class="book-meta">${book.processedAt} · ${book.textBlocks.length} blocks</div>
                    <div class="bookmark-list" id="book-${book.id}-bookmarks"></div>
                </div>
            </div>
            <div class="book-actions">
                <button class="open-btn">Open</button>
                <button class="delete-btn">Delete</button>
            </div>
        `

        booksEl.appendChild(bookEl)

        const openBtn = bookEl.querySelector('.open-btn')
        const deleteBtn = bookEl.querySelector('.delete-btn')
        const bookmarksContainer = bookEl.querySelector(`#book-${book.id}-bookmarks`)

        openBtn.addEventListener('click', () => openBook(book))
        deleteBtn.addEventListener('click', () => {
            if (!confirm('Delete this saved book from your library?')) return
            const newLib = loadLibrary().filter(b => b.id !== book.id)
            saveLibrary(newLib)
            renderLibrary()
        })

        // render bookmarks (only one expected per book)
        if (book.bookmarks && book.bookmarks.length) {
            book.bookmarks.forEach((bm) => {
                const bmEl = document.createElement('span')
                bmEl.className = 'bookmark'
                bmEl.textContent = `${bm.name} (${bm.index + 1})`
                bmEl.addEventListener('click', () => openBookmark(book, bm.index))
                bookmarksContainer.appendChild(bmEl)
            })
        }
    })
}

function escapeHtml(str) {
    if (!str) return ''
    return String(str).replace(/[&<>\"]/g, function (s) {
        return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[s]
    })
}

function openBook(book) {
    // Put the processed data into sessionStorage and open reader
    sessionStorage.setItem('pdfProcessedData', JSON.stringify({
        filename: book.filename,
        fileSize: book.fileSize,
        processedAt: book.processedAt,
        textBlocks: book.textBlocks,
        totalBlocks: book.textBlocks.length
    }))

    // Clear any start index
    sessionStorage.removeItem('pdfStartIndex')

    window.location.href = 'text2speech-main.html'
}

function openBookmark(book, index) {
    sessionStorage.setItem('pdfProcessedData', JSON.stringify({
        filename: book.filename,
        fileSize: book.fileSize,
        processedAt: book.processedAt,
        textBlocks: book.textBlocks,
        totalBlocks: book.textBlocks.length
    }))

    // Set start index so reader opens at the bookmarked block
    sessionStorage.setItem('pdfStartIndex', String(index))

    window.location.href = 'text2speech-main.html'
}

// Save current processed PDF to library
function saveCurrentToLibrary() {
    const raw = sessionStorage.getItem('pdfProcessedData')
    if (!raw) {
        alert('No processed PDF data in this browser session. Process a PDF first.')
        return
    }

    const data = JSON.parse(raw)
    const lib = loadLibrary()

    // Try to find existing book by filename+size
    const matchIndex = lib.findIndex(b => b.filename === data.filename && b.fileSize === data.fileSize)
    if (matchIndex !== -1) {
        // Update existing
        lib[matchIndex].processedAt = new Date().toISOString()
        lib[matchIndex].textBlocks = data.textBlocks || []
        saveLibrary(lib)
        renderLibrary()
        alert('Updated existing book in library')
        return lib[matchIndex]
    }

    const id = Date.now().toString(36)
    const book = {
        id,
        filename: data.filename || `PDF-${id}`,
        fileSize: data.fileSize || 0,
        processedAt: new Date().toISOString(),
        textBlocks: data.textBlocks || [],
        bookmarks: []
    }

    lib.unshift(book)
    saveLibrary(lib)
    renderLibrary()
    alert('Saved to library')
    return book
}

// Save a single bookmark for the current saved book (one bookmark per book)
function saveBookmarkForCurrentBook(index, name) {
    const raw = sessionStorage.getItem('pdfProcessedData')
    if (!raw) {
        return { success: false, reason: 'no-data' }
    }

    const data = JSON.parse(raw)
    const lib = loadLibrary()
    const matchIndex = lib.findIndex(b => b.filename === data.filename && b.fileSize === data.fileSize)
    if (matchIndex === -1) {
        return { success: false, reason: 'not-saved' }
    }

    const book = lib[matchIndex]
    // Keep only one bookmark per book: overwrite or set
    const bmName = name || `Bookmark ${index + 1}`
    book.bookmarks = [{ name: bmName, index: index }]
    saveLibrary(lib)
    renderLibrary()
    return { success: true, bookId: book.id }
}

// Expose a small API for other pages
if (typeof window !== 'undefined') {
    window.libraryAPI = {
        saveCurrentToLibrary,
        saveBookmarkForCurrentBook,
        loadLibrary,
        saveLibrary
    }
}

// Wire up
document.addEventListener('DOMContentLoaded', () => {
    renderLibrary()
    const saveBtn = document.getElementById('saveCurrentBtn')
    saveBtn.addEventListener('click', saveCurrentToLibrary)
})
