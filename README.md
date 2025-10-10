# PDFTeller

A dyslexia-friendly PDF reader that reads text **sentence by sentence** using text-to-speech.

## Overview

PDFTeller lets you upload PDFs, processes them into sentences, and reads them aloud with built-in voice options.
It uses a **FastAPI backend** and a **React frontend**, with all files stored in a local SQLite library.

## Features

* Sentence-by-sentence reading
* Dyslexia-friendly colors and font
* Voice and speed control
* Drag and drop PDF upload
* Remembers your library and reading progress

## How to Run

**Windows (1-click):**

```bash
startApp-1click.bat
```

**Manual:**

```bash
cd backend
python main.py

cd frontend
npm run dev -- --host
```

## API

* `POST /api/process_pdf`          – Upload and process PDF
* `GET /api/library`               – List all PDFs
* `GET /api/library/{id}`          – Get one PDF
* `PUT /api/library/{id}/bookmark` – Save progress
* `DELETE /api/library/{id}`       – Delete PDF
