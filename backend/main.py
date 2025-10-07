from fastapi.middleware.cors import CORSMiddleware
from fastapi import FastAPI, File, UploadFile
import uvicorn

from process_pdf import process_pdf_text_with_structure

from typing import List

# --- <> My Server Code <> --- #
app = FastAPI()

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

    return processed_text;

if __name__ == '__main__':
    uvicorn.run(app, host = '0.0.0.0', port = 8000)
