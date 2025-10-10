from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from utils.process_pdf import process_pdf_text_with_structure
from utils.pdf_library_manager import PDFLibraryManager

from typing import List, Dict

# --- <> My Server Code <> --- #
app = FastAPI()

pdf_library = PDFLibraryManager()

origins = [
    'http://localhost:5173',
    'http://192.168.1.21:5173'
]

app.add_middleware(
    CORSMiddleware,
    allow_origins     = origins,
    allow_credentials = True,
    allow_methods     = ['*'],
    allow_headers     = ['*']
)

@app.post('/api/process_pdf', response_model = Dict)
async def process_pdf(pdf_file: UploadFile = File(...)):
    # Check if PDF already exists in library
    if pdf_library.pdf_exists(pdf_file.filename):
        # Return existing PDF data instead of reprocessing
        existing_pdfs = pdf_library.get_all_pdfs()
        existing_pdf  = next((pdf for pdf in existing_pdfs if pdf['filename'] == pdf_file.filename), None)
        if existing_pdf:
            existing_pdf_data = pdf_library.get_pdf_by_id(existing_pdf['id'])

            return {
                'sentences':    existing_pdf_data['sentences'],
                'from_library': True
            };
    
    # Process new PDF
    pdf_data       = await pdf_file.read()
    processed_text = process_pdf_text_with_structure(pdf_data)
    
    pdf_library.add_pdf(pdf_file.filename, processed_text)

    return {
        'sentences':    processed_text,
        'from_library': False
    };

# --- PDF Library Endpoints --- #

@app.get('/api/library', response_model = List[Dict])
async def get_library():
    return pdf_library.get_all_pdfs();

@app.get('/api/library/{pdf_id}', response_model = Dict)
async def get_pdf_by_id(pdf_id: int):
    pdf_record = pdf_library.get_pdf_by_id(pdf_id)
    if not pdf_record:
        raise HTTPException(status_code = 404, detail = 'PDF not found');
    
    return pdf_record;

@app.delete('/api/library/{pdf_id}')
async def delete_pdf(pdf_id: int):
    success = pdf_library.delete_pdf(pdf_id)
    if not success:
        raise HTTPException(status_code = 404, detail = 'PDF not found');
    
    return {'message': 'PDF deleted successfully'};

@app.put('/api/library/{pdf_id}/bookmark')
async def add_bookmark(pdf_id: int, sentence_index: int):
    # Verify PDF exists
    pdf_record = pdf_library.get_pdf_by_id(pdf_id)
    if not pdf_record:
        raise HTTPException(status_code = 404, detail = 'PDF not found');
    
    # Verify sentence index is valid
    if sentence_index < 0 or sentence_index >= pdf_record['sentence_count']:
        raise HTTPException(status_code = 400, detail = 'Invalid sentence index');
    
    success = pdf_library.add_bookmark(pdf_id, sentence_index)
    if not success:
        raise HTTPException(status_code = 500, detail = 'Failed to add bookmark');
    
    return {'message': 'Bookmark added successfully'};

if __name__ == '__main__':
    uvicorn.run(app, host = '0.0.0.0', port = 8000)
