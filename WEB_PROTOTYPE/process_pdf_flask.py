from flask import Flask, request, jsonify
from flask_cors import CORS
from pathlib import Path
import sys

# Add the PYTHON_PROTOTYPE directory to the Python path!
sys.path.append(str(Path(__file__).parent.parent / 'PYTHON_PROTOTYPE'))
from process_pdf import process_pdf_text_with_structure

app = Flask(__name__)
CORS(app) # This enables CORS for all routes!

@app.route('/', methods = ['GET'])
def index():
    return jsonify({'message': 'PDF to JSON Converter API'});

@app.route('/process', methods = ['POST'])
def process_pdf():
    data = request.get_json()
    
    if not data or ('filename' not in data):
        return (jsonify({'error': 'Filename required'}), 400);
    
    filename = data['filename']
    pdf_path = Path(__file__).parent.parent / 'DATA' / filename
    
    if not pdf_path.exists():
        return (jsonify({'error': f'File {filename} not found'}), 404);
    
    try:
        processed_text = process_pdf_text_with_structure(str(pdf_path))
        return jsonify({
            'segments_count': len(processed_text),
            'data':           processed_text
        });
    except Exception as e:
        return (jsonify({'error': str(e)}), 500);

if __name__ == '__main__':
    app.run(debug = True, port = 5000)
