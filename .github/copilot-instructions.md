# PDFTeller - AI Coding Instructions

## Architecture Overview
PDFTeller is a dyslexia-friendly PDF reader with sentence-by-sentence text-to-speech. Built as a FastAPI backend + React frontend with persistent PDF library storage.

**Core Flow**: Upload PDF → Process with PyMuPDF + NLTK → Store in SQLite → Stream to React TTS component → Browser Speech Synthesis API

## Key Components

### Backend (`backend/`)
- **FastAPI server** (`main.py`): CORS-enabled API with PDF processing and library management
- **PDF Processing** (`utils/process_pdf.py`): PyMuPDF extraction + NLTK sentence tokenization with smart header detection
- **Library Manager** (`utils/pdf_library_manager.py`): SQLite-based persistent storage with automatic deduplication

### Frontend (`frontend/src/`)
- **React + Vite**: Standard dev setup with axios for API calls
- **State Management**: View transitions (`upload` → `results` → `library`) with smooth CSS animations
- **TTS Component**: Browser Speech Synthesis API with voice selection and rate control
- **Drag & Drop**: react-dropzone for PDF uploads with loading states

## Development Workflow

### Quick Start
```bash
# Use the 1-click launcher (Windows)
startApp-1click.bat

# Or manually:
# Terminal 1: cd backend && python main.py
# Terminal 2: cd frontend && npm run dev -- --host
```

### API Endpoints
- `POST /api/process_pdf` - Upload & process (returns existing if already processed)
- `GET /api/library` - List all PDFs with metadata
- `GET /api/library/{id}` - Get specific PDF data
- `DELETE /api/library/{id}` - Remove PDF
- `PUT /api/library/{id}/bookmark` - Save reading position

## Project Conventions

### Code Style
- **Backend**: Snake_case, semicolons, descriptive comments with `<>` delimiters
- **Frontend**: CamelCase React patterns, custom CSS with component-specific files
- **Error Handling**: FastAPI HTTPExceptions, frontend axios error catching

### Data Flow Patterns
- **PDF Deduplication**: Filename-based check before processing (see `pdf_library.pdf_exists()`)
- **Sentence Processing**: Headers detected by length < 50 chars + no period ending
- **State Persistence**: SQLite with JSON serialized sentences array
- **TTS Integration**: Direct browser API usage, no external services

### File Organization
- `backend/utils/` - Reusable processing modules
- `frontend/src/components/` - React components with co-located CSS
- `DATA/` - Test PDFs storage
- Root level scripts for development convenience

## Integration Points
- **CORS**: Frontend (localhost:5173) ↔ Backend (localhost:8000)
- **File Upload**: multipart/form-data via FormData
- **TTS**: Browser Speech Synthesis API with voice preference detection
- **Database**: SQLite auto-initialization with schema migrations handled in constructor

## Testing & Debugging
- Backend has test runner (`post_test.py`) for API validation
- Frontend uses Vite dev server with hot reload
- PDF processing testable via command-line execution in `process_pdf.py`
- Library operations can be tested directly through SQLite CLI or Python REPL