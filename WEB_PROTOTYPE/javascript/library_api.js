// Lightweight library API shim for pages that don't load the full library UI.
// Provides minimal functions used by the TTS page: saveCurrentToLibrary and saveBookmarkForCurrentBook.
(function () {
    function loadLibrary() {
        const raw = localStorage.getItem('pdfLibrary')
        try { return raw ? JSON.parse(raw) : [] } catch (e) { return [] }
    }

    function saveLibrary(lib) {
        localStorage.setItem('pdfLibrary', JSON.stringify(lib))
    }

    function saveCurrentToLibrary() {
        const raw = sessionStorage.getItem('pdfProcessedData')
        if (!raw) return null
        const data = JSON.parse(raw)

        const lib = loadLibrary()

        const matchIndex = lib.findIndex(b => b.filename === data.filename && b.fileSize === data.fileSize)
        if (matchIndex !== -1) {
            lib[matchIndex].processedAt = new Date().toISOString()
            lib[matchIndex].textBlocks = data.textBlocks || []
            saveLibrary(lib)
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
        return book
    }

    function saveBookmarkForCurrentBook(index, name) {
        const raw = sessionStorage.getItem('pdfProcessedData')
        if (!raw) return { success: false, reason: 'no-data' }

        const data = JSON.parse(raw)
        let lib = loadLibrary()
        let matchIndex = lib.findIndex(b => b.filename === data.filename && b.fileSize === data.fileSize)

        if (matchIndex === -1) {
            const saved = saveCurrentToLibrary()
            if (!saved) return { success: false, reason: 'not-saved' }
            lib = loadLibrary()
            matchIndex = lib.findIndex(b => b.filename === data.filename && b.fileSize === data.fileSize)
            if (matchIndex === -1) return { success: false, reason: 'not-saved' }
        }

        const book = lib[matchIndex]
        const bmName = name || `Bookmark ${index + 1}`
        book.bookmarks = book.bookmarks || []

        const existing = book.bookmarks.find(b => b.index === index)
        if (existing) existing.name = bmName
        else book.bookmarks.push({ name: bmName, index })

        saveLibrary(lib.map(b => (b.id === book.id ? book : b)))
        return { success: true, bookId: book.id }
    }

    if (typeof window !== 'undefined') {
        window.libraryAPI = {
            saveCurrentToLibrary,
            saveBookmarkForCurrentBook,
            loadLibrary,
            saveLibrary
        }
    }
})();
