from nltk.tokenize import sent_tokenize
from pathlib import Path
import pymupdf

def process_pdf_text_with_structure(pdf_data: bytes) -> list[str]:
    document       = pymupdf.open(stream = pdf_data, filename = 'pdf')
    processed_text = []
    
    for page in document:
        # Get lines from the page
        page_text = page.get_text()
        lines     = page_text.split('\n')

        current_block = [] # To accumulate lines of regular text!
        for line in lines:
            line = line.strip()

            # STUPID PDFs sometimes have empty lines...
            if not line:
                continue;
            
            # Check if this might be a chapter/section header!
            if (len(line) < 50) and not line.endswith('.'):
                # --- <> Process accumulated text <> --- #
                if current_block:
                    block_text    = ' '.join(current_block)
                    sentences     = sent_tokenize(block_text)
                    processed_text.extend(sentences)
                    current_block = []
                
                # Add the header as its own element...
                processed_text.append(line)
            else:
                current_block.append(line) # Will be processed later
        
        # Process any remaining text
        if current_block:
            block_text = ' '.join(current_block)
            sentences  = sent_tokenize(block_text)
            processed_text.extend(sentences)
    
    document.close()

    return processed_text;

if __name__ == '__main__':
    base_path = Path(__file__).parent.parent # Go up 1 directories!
    pdf_path  = base_path / 'DATA' / 'Understanding_Climate_Change.pdf'
    
    # Read the file as bytes
    with open(pdf_path, 'rb') as f:
        pdf_data = f.read()

    processed_text = process_pdf_text_with_structure(pdf_data)

    for (i, sentence) in enumerate(processed_text):
        print(f'{i+1:2}: {sentence}')
        if i >= 20:
            break;
