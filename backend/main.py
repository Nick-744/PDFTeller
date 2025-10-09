from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from process_pdf import process_pdf_text_with_structure
from pdf_library_manager import PDFLibraryManager

from typing import List, Dict

# --- <> My Server Code <> --- #
app = FastAPI()

pdf_library = PDFLibraryManager()

origins = [
    'http://localhost:5173'
]

app.add_middleware(
    CORSMiddleware,
    allow_origins     = origins,
    allow_credentials = True,
    allow_methods     = ['*'],
    allow_headers     = ['*']
)

@app.post('/api/process_pdf', response_model = List[str])
async def process_pdf(pdf_file: UploadFile = File(...)):
    pdf_data       = await pdf_file.read()
    processed_text = process_pdf_text_with_structure(pdf_data)
    
    pdf_library.add_pdf(pdf_file.filename, processed_text)

    return processed_text;

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

if __name__ == '__main__':
    uvicorn.run(app, host = '0.0.0.0', port = 8000)
